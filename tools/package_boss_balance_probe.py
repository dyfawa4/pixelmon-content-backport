"""Build the isolated native boss balance/spawn probe, never a player package."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json
import subprocess,tempfile

ROOT=Path(__file__).resolve().parents[1]
build=json.loads((ROOT/'.cache/last-build.json').read_text('utf-8-sig'))
work=Path(tempfile.mkdtemp(prefix='probe-',dir=ROOT/'.cache/boss-natural-190'))
work.mkdir(parents=True,exist_ok=True)
subprocess.run([str(Path(build['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8',
                '-cp',build['classes']+';'+build['classpath'],'-d',str(work)]+[str(ROOT/'tests'/name) for name in ['Support110Cases.java','R8BossTimeCases.java','R7SpawnCases.java','Resume190SoundProbe.java','Resume190Cases.java','Trail190ParticleProbe.java','Trail190Cases.java','Final190Cases.java','DayCare190Cases.java','LightBall190Cases.java','Den190Cases.java','Zygarde190Cases.java','Radiant190Cases.java','Charm190Cases.java','Ohko190Cases.java','AuthorRoster190Cases.java','Author190Cases.java','Status190Cases.java','Match190Scripts.java','Guard190Cases.java','Conventional190Matches.java','AuthorTrial190Cases.java','Stall190Cases.java','Update180Cases.java','Update180R9TeamCases.java','Update180R6Cases.java','Update180MomentumCases.java','BossBalance190Probe.java']],check=True)
target=ROOT/'.cache/boss-natural-190/bossbalance190-probe.jar'
with ZipFile(target,'w',ZIP_DEFLATED) as jar:
    jar.writestr('META-INF/mods.toml','''modLoader="javafml"
loaderVersion="[36,)"
license="MIT"
[[mods]]
modId="backport_bossbalance190_probe"
version="1.0"
[[dependencies.backport_bossbalance190_probe]]
modId="pixelmon_backport"
mandatory=true
versionRange="[1.9.0,1.11.0)"
ordering="AFTER"
side="BOTH"
''')
    jar.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMixinConfigs: private_trail190.mixins.json\n\n')
    jar.writestr('private_trail190.mixins.json',json.dumps({'required':True,'minVersion':'0.8','package':'qa.trailmixin','compatibilityLevel':'JAVA_11','mixins':['Resume190SoundProbe'],'client':['Trail190ParticleProbe'],'injectors':{'defaultRequire':1}}))
    jar.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':6,'description':'Private disposable boss balance check'}}))
    for path in work.rglob('*.class'):
        jar.write(path,path.relative_to(work).as_posix())
print(target)
