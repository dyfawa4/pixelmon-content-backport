from pathlib import Path
import subprocess,json,zipfile
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/probe');out.mkdir(exist_ok=True)
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Clone110Probe.java','tests/Clone110DepthProbe.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace')[:2000]);raise SystemExit(1)
with zipfile.ZipFile('.cache/110/clone110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_clone110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private clone test"}}')
 z.writestr('META-INF/MANIFEST.MF','Manifest-Version: 1.0\nMixinConfigs: private_clone110.mixins.json\n\n')
 z.writestr('private_clone110.mixins.json',json.dumps({'required':True,'package':'qa.clone110mixin','compatibilityLevel':'JAVA_11','client':['Clone110DepthProbe'],'injectors':{'defaultRequire':1}}))
 for p in out.rglob('*.class'):z.write(p,p.relative_to(out).as_posix())
