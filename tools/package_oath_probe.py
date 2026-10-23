from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import json,subprocess
ROOT=Path(__file__).resolve().parents[1];build=json.loads((ROOT/'.cache/last-build.json').read_text('utf-8-sig'));work=ROOT/'.cache/update-20261023';classes=work/'oath-probe-classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(Path(build['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',build['classes']+';'+build['classpath'],'-d',str(classes),str(ROOT/'tests/OathMegaLiveProbe.java'),str(ROOT/'tests/OathMoveColorCases.java')],check=True)
target=ROOT/'.cache/update-20261021/backport-followup-probe.jar'
with ZipFile(target,'w',ZIP_DEFLATED) as jar:
 jar.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="backport_followup_probe"\nversion="1.7.0"\ndisplayName="Private Mega Oath Probe"\n')
 jar.writestr('pack.mcmeta',json.dumps({'pack':{'pack_format':6,'description':'Private Mega Oath regression'}}))
 for pattern in ('OathMegaLiveProbe*.class','OathMoveColorCases*.class'):
  for p in (classes/'qa').glob(pattern):jar.write(p,p.relative_to(classes).as_posix())
print(target)
