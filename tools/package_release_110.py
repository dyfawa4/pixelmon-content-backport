"""Publishable 1.10.0 artifacts from the byte-identical reviewed r12 core."""
import argparse,hashlib,io,json,shutil,struct,subprocess
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from package_release_161 import verify_source
ROOT=Path(__file__).resolve().parents[1];DIST=ROOT/'dist';VERSION='1.10.0'
CORE_SHA='799496cb75b034f8d0cd4ba666cf3737d51cf06f3d3cfa539f18c844d75c9886'
def sha(data):return hashlib.sha256(data).hexdigest()
def main():
 parser=argparse.ArgumentParser();parser.add_argument('--source-ref',required=True);args=parser.parse_args()
 ref=subprocess.check_output(['git','rev-parse',args.source_ref+'^{commit}'],cwd=ROOT).decode().strip()
 core=DIST/f'pixelmon-content-backport-1.16.5-{VERSION}.jar';raw=core.read_bytes();assert sha(raw)==CORE_SHA
 with ZipFile(core) as z:
  assert z.testzip() is None
  assert 'version="1.10.0"' in z.read('META-INF/mods.toml').decode()
  classes=[n for n in z.namelist() if n.endswith('.class')];assert len(classes)==894
  assert all(struct.unpack('>H',z.read(n)[6:8])[0]==55 for n in classes)
  assert not any(n.startswith('qa/') for n in z.namelist())
  for p in (ROOT/'resources').rglob('*'):
   if p.is_file() and p.relative_to(ROOT/'resources').as_posix()!='META-INF/MANIFEST.MF':assert p.read_bytes()==z.read(p.relative_to(ROOT/'resources').as_posix())
 with ZipFile(DIST/'Pixelmon-Backport-1.10.0-candidate-r12.zip') as z:
  assert z.read('mods/'+core.name)==raw
  entries={n:z.read(n) for n in z.namelist() if n not in ('SHA256SUMS.txt','安装与更新说明.md')}
 for name in ['README.md','HANDBOOK.md','BUILDING.md','docs/RELEASE_1.10.0.md']:
  entries[name]=(ROOT/name).read_bytes()
 entries['安装与更新说明.md']=(ROOT/'docs/RELEASE_1.10.0.md').read_bytes()
 entries['config/ironfurnaces-client.toml']=(ROOT/'pack-overrides/config/ironfurnaces-client.toml').read_bytes()
 assert b'updates = false' in entries['config/ironfurnaces-client.toml']
 entries['构建信息.json']=json.dumps({'version':VERSION,'source_commit':ref,'core_sha256':CORE_SHA,'reviewed_candidate':'r12','java11_classes':894,'optional_addon_enabled_by_default':False,'includes_player_data':False,'whole_regression_rerun_for_publication':False},ensure_ascii=False,indent=2).encode()
 entries['SHA256SUMS.txt']=''.join(sha(v)+'  '+k+'\n' for k,v in entries.items()).encode()
 package=DIST/f'Pixelmon-Backport-{VERSION}.zip'
 with ZipFile(package,'w',ZIP_DEFLATED) as z:
  for n,v in entries.items():z.writestr(n,v)
 with ZipFile(package) as z:
  assert z.testzip() is None
  assert set(z.namelist())==set(entries)
  for n,v in entries.items():assert z.read(n)==v
 source=DIST/f'Pixelmon-Backport-source-{VERSION}.zip'
 subprocess.run(['git','-c','core.autocrlf=false','-c','core.eol=lf','archive','--format=zip',f'--output={source}',ref],cwd=ROOT,check=True);verify_source(source,ref)
 with ZipFile(source) as z:
  assert not any(n.startswith(('.cache/','local-only/','docs/data/','docs/AI_')) for n in z.namelist())
  for p in (ROOT/'src').rglob('*.java'):assert z.read(p.relative_to(ROOT).as_posix()).replace(b'\r\n',b'\n')==p.read_bytes().replace(b'\r\n',b'\n')
 artifacts=[core,package,source,DIST/'ohmygod-greninja-1.16.5-1.0.3.jar']
 for src,label in [('HANDBOOK.md','Handbook'),('docs/RELEASE_1.10.0.md','Release-Notes')]:
  p=DIST/f'Pixelmon-Backport-{label}-{VERSION}.md';shutil.copyfile(ROOT/src,p);artifacts.append(p)
 checksums=DIST/f'SHA256SUMS-{VERSION}.txt';checksums.write_text(''.join(sha(p.read_bytes())+'  '+p.name+'\n' for p in artifacts),'utf-8');artifacts.append(checksums)
 manifest={'version':VERSION,'source_commit':ref,'core_sha256':CORE_SHA,'artifacts':[{'path':str(p),'size':p.stat().st_size,'sha256':sha(p.read_bytes())} for p in artifacts],'package_integrity':'PASS','source_blobs':'PASS'}
 (ROOT/'.cache/release-110-assets.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),'utf-8');print(json.dumps(manifest,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
