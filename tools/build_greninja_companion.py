"""Build the explicitly optional two-effect companion with the core's Java 11 toolchain."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import json, subprocess, hashlib
ROOT=Path(__file__).resolve().parents[1]
build=json.loads((ROOT/'.cache/last-build.json').read_text('utf-8-sig'))
source=ROOT/'addons/ohmygod-greninja'
classes=ROOT/'.cache/ohmygod-greninja-classes';classes.mkdir(parents=True,exist_ok=True)
subprocess.run([str(Path(build['java']).with_name('javac.exe')),'--release','11','-proc:none','-encoding','UTF-8','-classpath',build['classpath'],'-d',str(classes),str(source/'src/local/pixelmon/greninja/OhMyGodGreninja.java')],check=True)
target=ROOT/'dist/ohmygod-greninja-1.16.5-1.0.0.jar'
with ZipFile(target,'w',ZIP_DEFLATED) as jar:
 for base in (classes,source/'resources'):
  for path in sorted(base.rglob('*')):
   if path.is_file():jar.write(path,path.relative_to(base).as_posix())
target.with_suffix('.jar.sha256').write_text(hashlib.sha256(target.read_bytes()).hexdigest()+'  '+target.name+'\n','utf-8')
print(target)
