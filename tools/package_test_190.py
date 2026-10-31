"""Package the checked local r8 candidate from an explicit player-file allowlist."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import argparse,hashlib,io,json,struct,tomllib
ROOT=Path(__file__).resolve().parents[1]
DIST=ROOT/'dist'
CORE_SHA='e29d13eda8603db8fd60bbd969dcf52acd4fb450044c9caff59f6619ec0b352f'
def sha(data):return hashlib.sha256(data).hexdigest()
def main():
    parser=argparse.ArgumentParser();parser.add_argument('--revision',default='r8',choices=['r8']);args=parser.parse_args()
    core=DIST/'pixelmon-content-backport-1.16.5-1.9.0.jar';data=core.read_bytes()
    assert sha(data)==CORE_SHA and core.with_suffix('.jar.sha256').read_text().split()[0].lower()==CORE_SHA
    with ZipFile(io.BytesIO(data)) as jar:
        names=jar.namelist();classes=[n for n in names if n.endswith('.class')]
        assert jar.testzip() is None and len(classes)==838
        assert all(struct.unpack('>H',jar.read(n)[6:8])[0]==55 for n in classes)
        assert 'version="1.9.0"' in jar.read('META-INF/mods.toml').decode()
        assert not any(n.startswith('qa/') or any(x in n.lower() for x in ('autoreel','autofishing','super_rod_auto_reel')) for n in names)
        for path in (ROOT/'resources').rglob('*'):
            if path.is_file() and path.name!='MANIFEST.MF':assert jar.read(path.relative_to(ROOT/'resources').as_posix())==path.read_bytes(),path
        for name in ['DivineSword','RadiantBallLogic','GlowScaleDust','UltraDenDensity','DayCareRequirementText','CharmStateSync']:
            assert 'local/pixelmon/backport/'+name+'.class' in names
        assert json.loads(jar.read('data/pixelmon/pokeballs/radiant_ball.json'))['catchBonus']==255
        assert json.loads(jar.read('data/pixelmon_backport/recipes/radiant_ball.json'))['result']['nbt']['PokeBallID']=='radiant_ball'
    addon=DIST/'ohmygod-greninja-1.16.5-1.0.2.jar';addon_data=addon.read_bytes()
    assert addon.with_suffix('.jar.sha256').read_text().split()[0].lower()==sha(addon_data)
    with ZipFile(io.BytesIO(addon_data)) as jar:
        text=jar.read('META-INF/mods.toml').decode();assert 'version="1.0.2"' in text and 'versionRange="[1.7.0,1.10.0)"' in text
    config=ROOT/'pack-overrides/config/ironfurnaces-client.toml';assert tomllib.loads(config.read_text())['updates']['check_updates']['updates'] is False
    entries={
        'mods/'+core.name:data,'mods/'+core.name+'.sha256':(CORE_SHA+'  '+core.name+'\n').encode(),
        'config/ironfurnaces-client.toml':config.read_bytes(),
        '可选模组/'+addon.name:addon_data,'可选模组/'+addon.name+'.sha256':(sha(addon_data)+'  '+addon.name+'\n').encode(),
        '可选模组/安装说明.md':(ROOT/'addons/ohmygod-greninja/README.md').read_bytes(),
        '更新说明.md':(ROOT/'docs/UPDATE_1.9.0_SUMMARY.md').read_bytes(),
        '安装说明.txt':(ROOT/'docs/PLAYER_NOTES_1.9.0.md').read_bytes(),
        'docs/DIVINE_SWORD_1.9.0.md':(ROOT/'docs/DIVINE_SWORD_1.9.0.md').read_bytes(),
    }
    for name in ['divine-sword-190.png','divine-sword-190-r1.png','divine-sword-190-r2.png']:
        entries['docs/previews/'+name]=(ROOT/'docs/previews'/name).read_bytes()
    entries['构建信息.json']=json.dumps({'version':'1.9.0','revision':args.revision,'date':'2026-10-28','core_sha256':CORE_SHA,'java11_classes':838,'local_candidate':True,'github_published':False,'standalone_jei_included':False,'optional_addon_enabled_by_default':False,'historical_full_check_passed':False,'focused_native_checks_passed':True},ensure_ascii=False,indent=2).encode()
    entries['SHA256SUMS.txt']=''.join(sha(value)+'  '+name+'\n' for name,value in entries.items()).encode()
    output=DIST/f'Pixelmon-Backport-1.9.0-test-{args.revision}.zip';assert not output.exists(),'Preserve old revisions'
    with ZipFile(output,'w',ZIP_DEFLATED,compresslevel=6) as jar:
        for name,value in entries.items():jar.writestr(name,value)
    with ZipFile(output) as jar:
        assert jar.testzip() is None and set(jar.namelist())==set(entries)
        assert {n for n in jar.namelist() if n.endswith('.jar')}=={'mods/'+core.name,'可选模组/'+addon.name}
        for line in jar.read('SHA256SUMS.txt').decode().splitlines():
            digest,name=line.split('  ',1);assert sha(jar.read(name))==digest,name
        assert not any(any(word in n for word in ['AI_','MATCHES','HANDOFF','data/','VALIDATION','JEI','Charm190','BossBalance']) for n in jar.namelist())
    digest=sha(output.read_bytes());output.with_suffix('.zip.sha256').write_text(digest+'  '+output.name+'\n',encoding='utf-8')
    report={'file':str(output),'sha256':digest,'core_sha256':CORE_SHA,'entries':len(entries),'classes':838,'player_allowlist':list(entries),'qa_excluded':True,'standalone_jei_excluded':True}
    (ROOT/'.cache/final190-checks/package-r8.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k!='player_allowlist'},ensure_ascii=False))
if __name__=='__main__':main()
