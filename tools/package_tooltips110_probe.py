from pathlib import Path
import json,subprocess,zipfile,uuid
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/tooltips-probe-'+uuid.uuid4().hex);out.mkdir()
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Tooltips110Probe.java','tests/FridgeSounds110Cases.java','tests/CollectorRules110Cases.java','tests/Candy110Cases.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace'));raise SystemExit(1)
with zipfile.ZipFile('.cache/110/tooltips110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_tooltips110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private direct-tooltip verification"}}')
 for p in out.rglob('*.class'):z.write(p,p.relative_to(out).as_posix())
