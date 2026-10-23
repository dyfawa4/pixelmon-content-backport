"""Publish the already reviewed 1.7.0 core with unified, non-chronological notes."""
import argparse,hashlib,io,json,shutil,struct,subprocess
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
from package_release_161 import verify_source

ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
VERSION='1.7.0'
CORE_SHA256='2fbb51440b1566356b524e17b9e653b19c36dbea8be39fa16c1927dfe7629f6e'
def sha(data):return hashlib.sha256(data).hexdigest()
def zip_entries(path,entries):
    with ZipFile(path,'w',ZIP_DEFLATED,compresslevel=6) as bundle:
        for name,data in entries.items():bundle.writestr(name,data)
    with ZipFile(path) as bundle:
        assert bundle.testzip() is None
        assert set(bundle.namelist())==set(entries)
        for name,data in entries.items():assert bundle.read(name)==data,name

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--source-ref',required=True);args=parser.parse_args()
    ref=subprocess.check_output(['git','rev-parse',args.source_ref+'^{commit}'],cwd=ROOT).decode().strip()
    core=DIST/f'pixelmon-content-backport-1.16.5-{VERSION}.jar';data=core.read_bytes()
    assert sha(data)==CORE_SHA256,'Must publish the reviewed title/icon core unchanged'
    with ZipFile(io.BytesIO(data)) as jar:
        assert jar.testzip() is None
        assert 'version="1.7.0"' in jar.read('META-INF/mods.toml').decode()
        classes=[n for n in jar.namelist() if n.endswith('.class')]
        assert len(classes)==643 and all(struct.unpack('>H',jar.read(n)[6:8])[0]==55 for n in classes)
        for path in (ROOT/'resources').rglob('*'):
            if path.is_file() and path.relative_to(ROOT/'resources').as_posix()!='META-INF/MANIFEST.MF':
                assert jar.read(path.relative_to(ROOT/'resources').as_posix())==path.read_bytes(),path
    notes=(ROOT/'docs/RELEASE_1.7.0.md').read_bytes()
    entries={'mods/'+core.name:data,'mods/'+core.name+'.sha256':(sha(data)+'  '+core.name+'\n').encode()}
    for name in ['README.md','HANDBOOK.md','BUILDING.md','LICENSE.txt','THIRD_PARTY.md',
                 'docs/RELEASE_1.7.0.md','docs/UPDATE_1.7.0_SUMMARY.md','docs/VALIDATION_1.7.0.md',
                 'docs/GENERATED_WILD_DISC_2026-10-23.md']:
        entries[name]=(ROOT/name).read_bytes()
    for path in (ROOT/'pack-overrides').rglob('*.snbt'):entries[path.relative_to(ROOT).as_posix()]=path.read_bytes()
    addon=DIST/'ohmygod-greninja-1.16.5-1.0.0.jar'
    entries['可选模组/'+addon.name]=addon.read_bytes()
    entries['可选模组/安装说明.md']=(ROOT/'addons/ohmygod-greninja/README.md').read_bytes()
    entries['安装说明.txt']=('重铸内容回迁 1.7.0\n'
        'Minecraft 1.16.5 / Forge 36.2.42 / Java 11 / Pixelmon 9.1.12～9.1.13。\n'
        '关闭游戏和服务端后，双方删除旧回迁核心，只保留mods中的1.7.0。保留原生Pixelmon、依赖和存档。\n'
        '可选模组默认不启用；要启用忍蛙增强时，把可选目录中的JAR另放入双方mods。\n'
        '不需要独立资源包；本包不是完整整合包，不含玩家数据或自动测试助手。\n'
        '更新内容统一见docs/RELEASE_1.7.0.md；可选任务书章节按需合并，不覆盖个人任务进度。\n').encode('utf-8-sig')
    entries['构建信息.json']=json.dumps({'version':VERSION,'source_commit':ref,'core_sha256':sha(data),
        'java11_classes':len(classes),'optional_addon_enabled_by_default':False,'whole_regression_rerun_for_publication':False},ensure_ascii=False,indent=2).encode()
    package=DIST/f'Pixelmon-Backport-{VERSION}.zip';zip_entries(package,entries)
    map_input=DIST/'Pixelmon-Backport-1.7.0-boss-test-map-title-20261024.zip'
    with ZipFile(map_input) as bundle:
        map_entries={n:bundle.read(n) for n in bundle.namelist() if n not in {'更新说明.md','10月23日追加说明.md','SHA256SUMS.txt'}}
    assert map_entries['mods/'+core.name]==data
    map_entries['更新说明.md']=notes
    instructions=map_entries['安装与地图说明.txt'].decode('utf-8-sig')
    instructions=instructions.replace('已完成离线回归与独立客户端专项检查','已完成分项离线和原生专项检查，最终修订未追加整套回归')
    map_entries['安装与地图说明.txt']=instructions.encode('utf-8-sig')
    map_entries['SHA256SUMS.txt']=''.join(sha(v)+'  '+k+'\n' for k,v in map_entries.items()).encode()
    map_archive=DIST/f'Pixelmon-Backport-Author-Test-Map-{VERSION}.zip';zip_entries(map_archive,map_entries)
    source=DIST/f'Pixelmon-Backport-source-{VERSION}.zip'
    subprocess.run(['git','-c','core.autocrlf=false','-c','core.eol=lf','archive','--format=zip',f'--output={source}',ref],cwd=ROOT,check=True)
    verify_source(source,ref)
    artifacts=[core,package,map_archive,source,addon]
    for src,label in [('HANDBOOK.md','Handbook'),('docs/RELEASE_1.7.0.md','Release-Notes')]:
        path=DIST/f'Pixelmon-Backport-{label}-{VERSION}.md';shutil.copyfile(ROOT/src,path);artifacts.append(path)
    checksum=DIST/f'SHA256SUMS-{VERSION}.txt'
    checksum.write_text(''.join(sha(p.read_bytes())+'  '+p.name+'\n' for p in artifacts),'utf8')
    manifest={'version':VERSION,'source_commit':ref,'core_sha256':sha(data),'artifacts':[str(p) for p in artifacts+[checksum]],
        'package_integrity':'PASS','source_blobs':'PASS','reviewed_core_unchanged':True,'new_game_or_regression_run':False}
    (ROOT/'.cache/release-170-assets.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2),'utf8')
    print(json.dumps(manifest,ensure_ascii=False,indent=2))
if __name__=='__main__':main()
