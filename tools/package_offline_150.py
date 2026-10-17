"""Clean 1.5.0 tester bundle assembled only from current reviewed map packages."""
import hashlib, io, json, shutil, struct
from pathlib import Path, PurePosixPath
from zipfile import ZipFile, ZIP_DEFLATED

def package(root, out, build_id):
    import nbtlib
    assert build_id.startswith('1.5.0-') and not out.exists()
    dist=root/'dist';production=dist/'pixelmon-content-backport-1.16.5-1.5.0.jar';main=production.read_bytes()
    sources=[('test-maps-1.5.0.zip',None,9),('gym-test-maps-1.5.0.zip','backport-gym-v143-rock-vanilla',1),
             ('cap-test-map-1.5.0.zip',None,1),('npc-quest-test-map-1.5.0.zip',None,1),
             ('tera-visual-test-map-1.5.0-r10.zip',None,1),('riding-test-map-1.5.0-r9.zip',None,1)]
    out.mkdir(parents=True);(out/'mods').mkdir();(out/'mods'/production.name).write_bytes(main)
    helpers={};worlds=[]
    for suffix,selected,count in sources:
        with ZipFile(dist/('Pixelmon-Backport-'+suffix)) as z:
            assert z.testzip() is None and z.read('mods/'+production.name)==main
            names=z.namelist();chosen=[n.split('/')[1] for n in names if n.startswith('saves/') and n.endswith('/level.dat') and (selected is None or n.split('/')[1]==selected)]
            assert len(chosen)==count
            worlds.extend(chosen)
            for name in names:
                if name.endswith('/'):continue
                parts=PurePosixPath(name).parts
                if parts[0]=='mods' and name.endswith('.jar') and parts[-1]!=production.name:
                    data=z.read(name)
                    with ZipFile(io.BytesIO(data)) as jar:
                        assert jar.testzip() is None
                        assert 'version="1.5.0"' in jar.read('META-INF/mods.toml').decode()
                        classes=[n for n in jar.namelist() if n.endswith('.class')]
                        assert classes and all(struct.unpack('>H',jar.read(n)[6:8])[0]==55 for n in classes)
                        assert not any('GameTest' in n or 'Verification' in n or n.startswith('Live') for n in classes)
                        content={n:jar.read(n) for n in jar.namelist() if not n.endswith('/')}
                    if parts[-1] in helpers:assert helpers[parts[-1]]==content
                    else:helpers[parts[-1]]=content;(out/'mods'/parts[-1]).write_bytes(data)
                if len(parts)<3 or parts[0]!='saves' or parts[1] not in chosen:continue
                assert '..' not in parts and not PurePosixPath(name).is_absolute()
                assert not any(p in {'playerdata','stats','advancements','pokemon','ftbteams','ftbchunks','logs'} for p in parts)
                assert parts[-1] not in {'session.lock','usercache.json','options.txt','servers.dat'}
                target=out.joinpath(*parts);assert not target.exists();target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(z.read(name))
    assert len(worlds)==len(set(worlds))==14 and len(helpers)==4
    for world in worlds:assert 'Player' not in nbtlib.load(out/'saves'/world/'level.dat')['Data']
    rock=out/'saves/backport-gym-v143-rock-vanilla';approved=json.loads((rock/'APPROVED_LAYOUT.json').read_text())
    assert approved['cityPieces']==97
    for name,digest in approved['files'].items():assert hashlib.sha256((rock/name).read_bytes()).hexdigest()==digest
    documents=['HANDBOOK.md','RIDING_TEST_MAP.md','NPC_QUEST_TEST_MAP.md','TERA_VISUAL_TEST_MAP.md','TEST_MAPS.md','FEATURE_TEST_MAPS.md','GYM_TEST_MAPS.md','CAP_TEST_MAP.md','LICENSE.txt','THIRD_PARTY.md','docs/RELEASE_1.5.0.md','docs/VALIDATION_1.5.0.md']
    for name in documents:
        dest=out/name;dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(root/name,dest)
    (out/'README.md').write_text('''# 1.5.0 离线测试包

将 mods 与所需 saves 合入独立 Minecraft 1.16.5 / Forge 36.2.42 / Java 11 测试实例，需要自行安装 Pixelmon 本体。先移除旧回迁 JAR 及旧版本地图辅助模组，不能同时装两版。

包含 14 张干净初始地图：9 张功能图、已验收岩石城、帽皮、NPC 任务、太晶外观图鉴和乘骑。正式实例只安装核心 JAR，不安装测试辅助模组。F8 图鉴仅在测试实例使用；乘骑地图可用 /ridingtest 1~6，4 为大岩蛇。

保留既有两份任务书覆盖，按需复制 config。任务和成就进度无需清空。本包不含 Minecraft／Pixelmon 本体、账号或玩家进度。更新说明见 docs/RELEASE_1.5.0.md。
''',encoding='utf-8')
    for p in (root/'pack-overrides').rglob('*.snbt'):
        dest=out/p.relative_to(root/'pack-overrides');dest.parent.mkdir(parents=True,exist_ok=True);shutil.copy2(p,dest)
    manifest={'version':'1.5.0','build_id':build_id,'worlds':worlds,'main_jar_sha256':hashlib.sha256(main).hexdigest(),'helper_mods':sorted(helpers),'approved_rock_city_pieces':97,'player_data_included':False,'original_client_included':False}
    (out/'构建信息.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    sums={p.relative_to(out).as_posix():hashlib.sha256(p.read_bytes()).hexdigest() for p in sorted(out.rglob('*')) if p.is_file()}
    (out/'SHA256SUMS.txt').write_text(''.join(f'{h}  {n}\n' for n,h in sums.items()),encoding='utf-8')
    archive=dist/'1.5.0-离线测试包-最新.zip';assert not archive.exists()
    with ZipFile(archive,'w',ZIP_DEFLATED,compresslevel=6) as z:
        for p in sorted(out.rglob('*')):
            if p.is_file():z.write(p,p.relative_to(out).as_posix())
    with ZipFile(archive) as z:
        assert z.testzip() is None
        for n,h in sums.items():assert hashlib.sha256(z.read(n)).hexdigest()==h
    digest=hashlib.sha256(archive.read_bytes()).hexdigest()
    archive.with_name(archive.name+'.sha256').write_text(f'{digest}  {archive.name}\n',encoding='utf-8')
    print(f'Clean 1.5.0 bundle verified: {len(worlds)} worlds, 5 mods, original rock snapshot; no player data.')
