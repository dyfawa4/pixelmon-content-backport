"""Private, property-and-save-guarded native regression helper. Not a delivery mod."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import json
import subprocess
ROOT=Path(__file__).resolve().parents[1]
build=json.loads((ROOT/'.cache/last-build.json').read_text('utf-8-sig'))
work=ROOT/'.cache/update-20261021'
classes=work/'followup-probe-classes'
classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(Path(build['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',build['classes']+';'+build['classpath'],'-d',str(classes),str(ROOT/'tests/Followup170LiveProbe.java'),str(ROOT/'tests/BossFollowupCases.java'),str(ROOT/'tests/AppendFollowupCases.java')],check=True)
target=work/'backport-followup-probe.jar'
with ZipFile(target,'w',ZIP_DEFLATED) as jar:
 jar.writestr('META-INF/mods.toml','''modLoader="javafml"
loaderVersion="[36,)"
license="All Rights Reserved"
[[mods]]
modId="backport_followup_probe"
version="1.7.0"
displayName="Private followup regression probe"
[[dependencies.backport_followup_probe]]
modId="pixelmon_backport"
mandatory=true
versionRange="[1.7.0,1.7.1)"
ordering="AFTER"
side="BOTH"
''')
 jar.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':6,'description':'Private followup regression probe'}}))
 for path in sorted((classes/'qa').glob('Followup170LiveProbe*.class')):jar.write(path,path.relative_to(classes).as_posix())
 for path in sorted((classes/'qa').glob('BossFollowupCases*.class')):jar.write(path,path.relative_to(classes).as_posix())
 for path in sorted((classes/'qa').glob('AppendFollowupCases*.class')):jar.write(path,path.relative_to(classes).as_posix())
print(target)
