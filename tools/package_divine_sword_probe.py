"""Compile the disposable 1.9 sword native probe; never add this JAR to player packages."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json
import subprocess

ROOT=Path(__file__).resolve().parents[1]
build=json.loads((ROOT/'.cache/last-build.json').read_text('utf-8-sig'))
work=ROOT/'.cache/update-190/divine-probe-classes'
work.mkdir(parents=True,exist_ok=True)
subprocess.run([str(Path(build['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8',
                '-cp',build['classes']+';'+build['classpath'],'-d',str(work),str(ROOT/'tests/DivineSword190Probe.java')],check=True)
target=ROOT/'.cache/update-190/divine190-probe.jar'
with ZipFile(target,'w',ZIP_DEFLATED) as jar:
    jar.writestr('META-INF/mods.toml','''modLoader="javafml"
loaderVersion="[36,)"
license="MIT"
[[mods]]
modId="backport_divine190_probe"
version="1.0"
[[dependencies.backport_divine190_probe]]
modId="pixelmon_backport"
mandatory=true
versionRange="[1.9.0,1.10.0)"
ordering="AFTER"
side="BOTH"
''')
    jar.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':6,'description':'Private disposable sword checks'}}))
    for path in work.rglob('*.class'):
        jar.write(path,path.relative_to(work).as_posix())
print(target)
