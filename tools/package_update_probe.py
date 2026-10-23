"""Build the opt-in disposable update regression helper, never a delivery mod."""
import json
from pathlib import Path
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED

root = Path(__file__).resolve().parents[1]
build = json.loads((root / '.cache/last-build.json').read_text(encoding='utf-8-sig'))
work = root / '.cache/update-20261021'
classes = work / 'probe-classes'
classes.mkdir(parents=True, exist_ok=True)
names = ('Update170LiveProbe', 'NpcDecisionVerification', 'FeedbackVerification',
         'TeraVerification', 'GymCityVerification')
subprocess.run([str(Path(build['java']).with_name('javac.exe')), '--release', '11',
                '-proc:none', '-encoding', 'UTF-8', '-cp',
                build['classes'] + ';' + build['classpath'], '-d', str(classes),
                *(str(root / 'tests' / (name + '.java')) for name in names)],
               check=True, cwd=root)
meta = '''modLoader="javafml"
loaderVersion="[36,)"
license="All Rights Reserved"
[[mods]]
modId="backport_update_probe"
version="1.7.0"
displayName="Private update regression probe"
[[dependencies.backport_update_probe]]
modId="pixelmon_backport"
mandatory=true
versionRange="[1.7.0,1.7.1)"
ordering="AFTER"
side="BOTH"
'''
helper = work / 'backport-update-probe.jar'
with ZipFile(helper, 'w', ZIP_DEFLATED) as archive:
    archive.writestr('META-INF/mods.toml', meta)
    archive.writestr('pack.mcmeta', json.dumps({'pack': {
        'pack_format': 6, 'description': 'Private update regression probe'}}))
    for name in names:
        for source in sorted(classes.glob(name + '*.class')):
            archive.write(source, source.name)
with ZipFile(helper) as archive:
    assert archive.testzip() is None
    assert json.loads(archive.read('pack.mcmeta'))['pack']['pack_format'] == 6
print(helper)
