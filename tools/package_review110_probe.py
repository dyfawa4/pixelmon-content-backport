from pathlib import Path
import json,subprocess,zipfile,uuid
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/review-probe-'+uuid.uuid4().hex);out.mkdir()
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Review110Probe.java','tests/Review110ShiftMixin.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace'));raise SystemExit(1)
with zipfile.ZipFile('.cache/110/review110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_review110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private review verification"}}')
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMixinConfigs: review110.mixins.json\n\n')
 z.writestr('review110.mixins.json',json.dumps({'required':True,'minVersion':'0.8','package':'qa.reviewmixin','compatibilityLevel':'JAVA_11','client':['Review110ShiftMixin'],'injectors':{'defaultRequire':1}}))
 for p in out.rglob('*.class'):z.write(p,p.relative_to(out).as_posix())
