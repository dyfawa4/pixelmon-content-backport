"""Read installed BOP bytecode; verify every explicit mansion biome registration.

Does not initialize Minecraft or generate/load/save any world.
"""
import argparse
import hashlib
import json
from pathlib import Path
import re
import subprocess
from zipfile import ZipFile

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--bop-jar', type=Path, required=True)
parser.add_argument('--biome-config', type=Path)
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
build = json.loads((root / '.cache/last-build.json').read_text(encoding='utf-8-sig'))
javap = str(Path(build['java']).with_name('javap.exe'))
classpath = build['classpath'] + ';' + str(args.bop_jar)


def inspect(name):
    return subprocess.check_output([javap, '-classpath', classpath, '-c', '-p', name],
                                   text=True, encoding='utf-8', errors='replace')


configured = inspect('net.minecraft.world.gen.feature.structure.StructureFeatures')
match = re.search(r'// String mansion\s+.*?putstatic\s+#\d+\s+// Field (\w+):',
                  configured, re.S)
assert match, 'Native configured mansion registration missing'
configured_field = match.group(1)
assert 'Structure.field_236368_d_' in match.group(0), 'Native mansion structure mapping changed'
registrations = inspect('biomesoplenty.init.ModBiomes')
names = dict(re.findall(
    r'// class (biomesoplenty/common/biome/\w+/\w+)\s+.*?'
    r'// String ([a-z_]+)\s+\d+: invokestatic\s+#\d+\s+// Method registerBiome:',
    registrations, re.S))
config = json.loads(args.biome_config.read_text(encoding='utf-8-sig')) if args.biome_config else {}


def config_entry(value, name):
    if isinstance(value, dict):
        if name in value:
            return value[name]
        for child in value.values():
            found = config_entry(child, name)
            if found is not None:
                return found
    return None


rows = []
with ZipFile(args.bop_jar) as jar:
    biome_classes = [n for n in jar.namelist()
                     if n.startswith('biomesoplenty/common/biome/') and n.endswith('Biome.class')]
    for path in sorted(biome_classes):
        raw = jar.read(path)
        if configured_field.encode() not in raw:
            continue
        name = path[:-6]
        text = inspect(name.replace('/', '.'))
        generation = re.search(r'protected void configureGeneration\([^\n]+\);\s+Code:(.*?)(?=\n  (?:public|protected|private)|\n})', text, re.S)
        assert generation, f'Generation method missing: {name}'
        assert f'StructureFeatures.{configured_field}' in generation.group(1), name
        assert 'Builder.func_242516_a:' in generation.group(1), name
        assert name in names, f'Mansion biome template is not registered: {name}'
        biome = 'biomesoplenty:' + names[name]
        rows.append(dict(biome=biome, template=name, structure='minecraft:mansion',
                         biome_config=config_entry(config, biome),
                         class_sha256=hashlib.sha256(raw).hexdigest()))
assert rows, 'No mansion registrations found; inspect changed BOP generation code'
report = dict(scope='Installed BOP bytecode and optional configuration, no runtime world generation',
              bop_jar=args.bop_jar.name, bop_sha256=hashlib.sha256(args.bop_jar.read_bytes()).hexdigest(),
              biome_templates_scanned=len(biome_classes), configured_mansion_field=configured_field,
              structure_field='field_236368_d_', registered_mansion_biomes=rows,
              limits='Direct generation registrations checked; separate structure mods and future BOP versions require a new audit')
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
