from pathlib import Path
import json,subprocess,zipfile
b=json.loads(Path('.cache/last-build.json').read_text('utf-8-sig'));out=Path('.cache/110/nature-probe');out.mkdir(exist_ok=True)
r=subprocess.run([str(Path(b['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-cp',b['classes']+';'+b['classpath'],'-d',str(out),'tests/Nature110Probe.java'],capture_output=True)
if r.returncode:print(r.stderr.decode('gbk',errors='replace')[:3500]);raise SystemExit(1)
p=next(Path('D:/游戏/宝可梦/.minecraft/versions/宝可梦：轻快之旅 V2.3.3 电脑端/mods').glob('*Pixelmon-1.16.5-9.1.13-universal.jar'))
with zipfile.ZipFile(p) as z:paths=[n for n in z.namelist() if n.endswith('.nbt') and '/structures/gyms/' in n]
paths+=json.loads(Path('resources/backport/gym_templates.json').read_text())['templates']
with zipfile.ZipFile('.cache/110/nature110-probe.jar','w') as z:
 z.writestr('META-INF/mods.toml','modLoader="javafml"\nloaderVersion="[36,)"\nlicense="MIT"\n[[mods]]\nmodId="backport_nature110_probe"\nversion="1.0"\n');z.writestr('pack.mcmeta','{"pack":{"pack_format":6,"description":"Private gym nature audit"}}')
 z.writestr('nature110-templates.json',json.dumps(sorted(set(paths))))
 for p in out.rglob('*.class'):z.write(p,p.relative_to(out).as_posix())
