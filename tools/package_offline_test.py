"""Consolidate reviewed addon/maps into a clean manual tester handoff."""
import argparse
from datetime import datetime, timezone, timedelta
import gzip
import hashlib
import io
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import struct
import sys
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument('--output-dir', required=True, type=Path)
parser.add_argument('--build-id', default='1.4.3-offline-test-20261015')
args = parser.parse_args()
out = args.output_dir.resolve()
assert not out.exists(), 'Use a fresh output directory; existing deliveries are not overwritten'
version = re.search(r'^version="([^"]+)"', (root/'resources/META-INF/mods.toml').read_text(), re.M).group(1)
assert version == '1.4.3', 'Review the test instructions before packaging a different version'
dist = root/'dist'
production = dist/f'pixelmon-content-backport-1.16.5-{version}.jar'
helper_name = f'backport-test-maps-1.16.5-{version}.jar'
rock = 'backport-gym-v143-rock-vanilla'
sources = [(dist/f'Pixelmon-Backport-test-maps-{version}.zip', None, 9),
           (dist/f'Pixelmon-Backport-gym-test-maps-{version}.zip', rock, 1),
           (dist/f'Pixelmon-Backport-cap-test-map-{version}.zip', None, 1)]


def digest(data):
    return hashlib.sha256(data).hexdigest()


def inspect_jar(data, helper=False):
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 6
        assert f'version="{version}"' in jar.read('META-INF/mods.toml').decode().replace(' ', '')
        classes = [n for n in jar.namelist() if n.endswith('.class')]
        assert classes and all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
        assert not any('DenAudit' in n or 'RuntimeDenAudit' in n or 'ManualWorldOpen' in n for n in classes)
        if helper:
            assert 'MapSupport.class' in classes and 'FeatureStations.class' in classes
            assert not any('Verification' in n or n == 'LiveVerification.class' for n in classes)


main_bytes = production.read_bytes()
inspect_jar(main_bytes)
sum_entry = next(line.split('  ', 1)[0] for line in (dist/f'SHA256SUMS-{version}.txt').read_text().splitlines()
                 if line.endswith('  '+production.name))
assert digest(main_bytes) == sum_entry, 'Production build differs from reviewed release staging'
out.mkdir(parents=True)
(out/'mods').mkdir()
(out/'mods'/production.name).write_bytes(main_bytes)
worlds = []
helper_bytes = None
for path, selected_world, expected in sources:
    with ZipFile(path) as source:
        assert source.testzip() is None
        assert source.read('mods/'+production.name) == main_bytes, f'Stale production JAR in {path.name}'
        candidate = source.read('mods/'+helper_name)
        inspect_jar(candidate, helper=True)
        if helper_bytes is None:
            helper_bytes = candidate
        # Separate map builds may differ only in JAR entry timestamps.
        with ZipFile(io.BytesIO(helper_bytes)) as a, ZipFile(io.BytesIO(candidate)) as b:
            assert set(a.namelist()) == set(b.namelist())
            assert all(a.read(n) == b.read(n) for n in a.namelist())
        selected = [n.split('/')[1] for n in source.namelist()
                    if n.startswith('saves/') and n.endswith('/level.dat')
                    and (selected_world is None or n.split('/')[1] == selected_world)]
        assert len(selected) == expected
        worlds.extend(selected)
        for name in source.namelist():
            parts = PurePosixPath(name).parts
            if len(parts) < 3 or parts[0] != 'saves' or parts[1] not in selected or name.endswith('/'):
                continue
            assert not PurePosixPath(name).is_absolute() and '..' not in parts
            assert not any(p in {'playerdata', 'stats', 'advancements', 'pokemon', 'ftbteams', 'ftbchunks'} for p in parts)
            assert parts[-1] not in {'session.lock', 'usercache.json', 'options.txt', 'servers.dat'}
            target = out.joinpath(*parts)
            assert not target.exists(), f'Duplicate world file {name}'
            target.parent.mkdir(parents=True, exist_ok=True)
            target.write_bytes(source.read(name))
(out/'mods'/helper_name).write_bytes(helper_bytes)
assert len(worlds) == len(set(worlds)) == 11

# All maps come from public clean initial templates, never the manual instance.
# Check embedded player data as well as paths before handing them to testers.
sys.path.insert(0, str(root/'.cache/temple-python'))
import nbtlib
for world in worlds:
    path = out/'saves'/world/'level.dat'
    level = nbtlib.File.parse(io.BytesIO(gzip.decompress(path.read_bytes())))
    assert 'Player' not in level['Data'], f'Embedded player data in {world}'
accepted = json.loads((out/'saves'/rock/'APPROVED_LAYOUT.json').read_text())
assert accepted['cityPieces'] == 97 and accepted['seed'] == 143143
for relative, expected in accepted['files'].items():
    assert digest((out/'saves'/rock/relative).read_bytes()) == expected, relative

documents = {
    'README.md': 'docs/OFFLINE_TEST_1.4.3.md',
    '测试清单.md': 'docs/OFFLINE_TEST_CHECKLIST_1.4.3.md',
    '测试反馈模板.md': 'docs/OFFLINE_TEST_FEEDBACK_1.4.3.md',
}
for name in ['HANDBOOK.md', 'TEST_MAPS.md', 'FEATURE_TEST_MAPS.md', 'GYM_TEST_MAPS.md',
             'CAP_TEST_MAP.md', 'LICENSE.txt', 'THIRD_PARTY.md']:
    documents[name] = name
for name in ['RELEASE_1.4.3.md', 'REPAIR_1.4.3.md', 'VALIDATION_1.4.3.md',
             'REVISION2_1.4.3.md',
             'MAGEARNA_MANSIONS_1.4.3.md', 'DEN_AUDIT_2026-10-15.md',
             'GRENINJA_1.4.3.md', 'CAP_PIKACHU_1.4.3.md', 'ROCK_VANILLA_TEMPLE_1.4.3.md']:
    documents['docs/'+name] = 'docs/'+name
for target, source in documents.items():
    destination = out/target
    destination.parent.mkdir(parents=True, exist_ok=True)
    shutil.copy2(root/source, destination)
shutil.copy2(root/'.cache/bop-mansion-registry.json', out/'docs/BOP府邸登记检查.json')
build_time = datetime.now(timezone(timedelta(hours=8))).isoformat(timespec='seconds')
identity = dict(version=version, build_id=args.build_id, built_at=build_time,
                status='Manual tester build; not yet published to GitHub',
                minecraft='1.16.5', forge='36.2.42', java=11,
                pixelmon='file 9.1.13 / internal 9.1.12',
                main_jar_sha256=digest(main_bytes), helper_jar_sha256=digest(helper_bytes),
                worlds=worlds, approved_rock_city_pieces=97,
                tests='Java11 build, offline regressions and live unspawned Tera probes passed; human acceptance remains pending',
                private_diagnostics_included=False, original_game_client_included=False)
(out/'构建信息.json').write_text(json.dumps(identity, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
files = sorted(p for p in out.rglob('*') if p.is_file())
checksums = {p.relative_to(out).as_posix(): digest(p.read_bytes()) for p in files}
(out/'SHA256SUMS.txt').write_text(''.join(f'{v}  {k}\n' for k, v in checksums.items()), encoding='utf-8')
archive_path = out.with_name(out.name+'.zip')
assert not archive_path.exists(), 'Existing archive not overwritten'
with ZipFile(archive_path, 'w', ZIP_DEFLATED, compresslevel=6) as archive:
    for path in sorted(out.rglob('*')):
        if path.is_file(): archive.write(path, path.relative_to(out).as_posix())
with ZipFile(archive_path) as archive:
    assert archive.testzip() is None
    assert sorted(n for n in archive.namelist() if n.startswith('mods/')) == sorted(['mods/'+production.name, 'mods/'+helper_name])
    assert sum(n.startswith('saves/') and n.endswith('/level.dat') for n in archive.namelist()) == 11
    for name, expected in checksums.items(): assert digest(archive.read(name)) == expected, name
archive_hash = digest(archive_path.read_bytes())
archive_path.with_name(archive_path.name+'.sha256').write_text(f'{archive_hash}  {archive_path.name}\n', encoding='utf-8')
report = dict(build_id=args.build_id, archive=str(archive_path), bytes=archive_path.stat().st_size, sha256=archive_hash,
              main_jar_sha256=digest(main_bytes), worlds=11, mods=2,
              approved_rock_snapshot_verified=True, player_files_included=False,
              private_diagnostic_classes_included=False, crc_and_all_file_hashes_verified=True)
assert re.fullmatch(r'[A-Za-z0-9_.-]+', args.build_id), 'Build ID must be a safe filename component'
(root/f'.cache/offline-test-delivery-{args.build_id}.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
