from pathlib import Path
import json,subprocess,zipfile,sys
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/health-probe');out.mkdir(exist_ok=True)
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Health110Probe.java','tests/Health110TraceMixin.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace')[:2200]);raise SystemExit(1)
with zipfile.ZipFile('.cache/110/health110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_health110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private author HP observer"}}')
 if '--trace' in sys.argv:
  z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMixinConfigs: health110.mixins.json\n\n')
  z.writestr('health110.mixins.json',json.dumps({'required':True,'minVersion':'0.8','package':'qa.mixin','compatibilityLevel':'JAVA_11','mixins':['Health110TraceMixin'],'injectors':{'defaultRequire':1}}))
 for p in (out/'qa').glob('Health110Probe*.class'):z.write(p,p.relative_to(out).as_posix())
 if '--trace' in sys.argv:
  for p in (out/'qa/mixin').glob('Health110TraceMixin*.class'):z.write(p,p.relative_to(out).as_posix())
