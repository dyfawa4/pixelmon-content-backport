from pathlib import Path
import json,subprocess,zipfile
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/recipe-probe');out.mkdir(exist_ok=True)
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Recipe110Probe.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace')[:3000]);raise SystemExit(1)
with zipfile.ZipFile('.cache/110/recipe110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_recipe110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private 1.10 crafting and tooltip audit"}}')
 for p in out.rglob('*.class'):z.write(p,p.relative_to(out).as_posix())
