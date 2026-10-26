"""Package the current 1.8.0 local candidate without gameplay helpers or personal data."""
from pathlib import Path
from zipfile import ZipFile,ZIP_DEFLATED
import hashlib,io,json,argparse
parser=argparse.ArgumentParser();parser.add_argument('--revision',default='r15');args=parser.parse_args();revision=args.revision
assert revision.startswith('r') and revision[1:].isdigit()
ROOT=Path(__file__).resolve().parents[1];DIST=ROOT/'dist'
def sha(data):return hashlib.sha256(data).hexdigest()
core=DIST/'pixelmon-content-backport-1.16.5-1.8.0.jar';data=core.read_bytes()
assert core.with_suffix('.jar.sha256').read_text().split()[0]==sha(data)
with ZipFile(io.BytesIO(data)) as jar:
    assert jar.testzip() is None
    assert 'version="1.8.0"' in jar.read('META-INF/mods.toml').decode()
    assert 'local/pixelmon/backport/NpcHeldItems.class' in jar.namelist()
    assert 'local/pixelmon/backport/mixin/AuthorTrainerInvulnerabilityMixin.class' in jar.namelist()
    names=jar.namelist()
    assert not any(n.startswith('qa/') or 'autoreel' in n.lower() or 'autofishing' in n.lower() or 'super_rod_auto_reel' in n for n in names)
    assert not any('MegaBossDropChanceMixin' in n for n in names)
    prizes=json.loads(jar.read('backport/author_boss_rewards.json'))
    assert prizes['extra_draws']==7 and prizes['master_ball_percent']==50 and not prizes['replacement']
    assert 'local/pixelmon/backport/GrowthLamp.class' in names
    for component in ('GenderPotion','DayCareFootprint','GymLegendaryPair','NpcTeamKnowledge','NpcStrategicControl','CaptureStorageSync','StorageSnapshot','client/PCStorageSync'):
        assert 'local/pixelmon/backport/'+component+'.class' in names
    assert 'DayCareFootprintMixin' in json.loads(jar.read('pixelmon_backport.mixins.json'))['mixins']
    assert 'assets/pixelmon_backport/textures/item/gender_potion.png' in names
    assert 'JadeNpcNameMixin' in json.loads(jar.read('pixelmon_backport.mixins.json'))['client']
    assert json.loads(jar.read('assets/pixelmon_backport/lang/zh_cn.json'))['pixelmon.backport.npc.level']=='等级 %s'
    assert json.loads(jar.read('assets/pixelmon_backport/lang/zh_cn.json'))['item.pixelmon_backport.gender_potion']=='变性魔药'
    for lamp in ('shrinking_lamp','enlarging_lamp'):
        assert json.loads(jar.read('assets/pixelmon_backport/models/item/'+lamp+'.json'))['loader']=='forge:separate-perspective'
        assert len(json.loads(jar.read('assets/pixelmon_backport/models/item/'+lamp+'_handheld.json'))['elements'])>=20
    assert 'local/pixelmon/backport/MovingAgreement.class' in names
    assert 'local/pixelmon/backport/TeraMachineLearning.class' in names
    assert 'TeraRelearnerMixin' in json.loads(jar.read('pixelmon_backport.mixins.json'))['mixins']
    assert 'local/pixelmon/backport/MovingAgreementScreen.class' in names
    assert 'assets/pixelmon_backport/textures/item/moving_agreement.png' in names
    assert 'local/pixelmon/backport/mixin/RidingStandardInputMixin.class' in names
    assert 'local/pixelmon/backport/FishingEnchantments.class' in names
    for cls in ('BallLidIngredient','UtilityItemLoot','LegendWeightPairs','TumblestoneFootprint','BattleHealthSync','AltarSpacing','AltarRegionIndex','Calamity','CalamityBattle','CalamityMath','CalamityNetwork','CalamityPartySync','CalamityClient','BattleStatModifiers','BattleStatusIcons'):
        assert 'local/pixelmon/backport/'+cls+'.class' in names
    mixins=json.loads(jar.read('pixelmon_backport.mixins.json'))
    assert 'PCInitializeMixin' in mixins['client'] and 'StoragePacketGuardMixin' in mixins['client']
    assert b'19' in jar.read('local/pixelmon/backport/TeraNetwork.class')
    assert 'InventoryDrawerMixin' in mixins['client'] and 'TumblestoneFootprintMixin' in mixins['mixins']
    assert all(m in mixins['mixins'] for m in ('TumblestoneHammerMixin','AltarChanceMixin','AltarPillarGateMixin','AltarSpacingMixin'))
    portal=json.loads(jar.read('data/pixelmon_backport/recipes/ultra_space_teleporter.json'))
    assert portal['key']['F']['item']=='pixelmon:ultra_forest_flower' and portal['pattern']==['EFJ','SBS','DCD']
    protocol=json.loads(jar.read('data/pixelmon_backport/recipes/moving_agreement.json'))
    assert protocol['key']['L']['type']=='pixelmon_backport:ball_lid' and protocol['key']['S']['item']=='minecraft:stone_button'
    assert protocol['key']['B']=={'tag':'minecraft:beds'} and protocol['key']['N']=={'item':'minecraft:jukebox'}
    assert not any(n.startswith('local/pixelmon/backport/DrownedErosion') for n in names)
    for n in names:
        if n.startswith('local/pixelmon/backport/Calamity') and n.endswith('.class'):
            assert b'local/pixelmon/backport/Erosion' not in jar.read(n),n
        if n.startswith('local/pixelmon/backport/Erosion') and n.endswith('.class'):
            assert b'local/pixelmon/backport/Calamity' not in jar.read(n),n
    zh_status=json.loads(jar.read('assets/pixelmon_backport/lang/zh_cn.json'))
    assert zh_status['pixelmon_backport.erosion.badge']=='蚀' and zh_status['pixelmon_backport.calamity.badge']=='祸'
    wrappers=[n for n in names if n.startswith('data/pixelmon_backport/loot_tables/chests/pokemart/') and n.endswith('.json')]
    assert len(wrappers)==10
    for n in wrappers:
        table=json.loads(jar.read(n));assert len(table['pools'])==3
        assert all(pool['conditions'][0]['chance']==.05 for pool in table['pools'][1:])
    zh=json.loads(jar.read('assets/pixelmon/lang/zh_cn.json'))
    assert zh['block.pixelmon.blue_tumblestone']=='天蓝玉石原矿' and zh['block.pixelmon.red_tumblestone']=='玉石原矿' and zh['block.pixelmon.black_tumblestone']=='黑玉石原矿'
    for component in ('MysteriousPotion','MysteriousPotionLoot','AuthorNameTag','BattleVictorySound','RockGymGeneration','mixin/PotionCandyMixin','mixin/BattleExitMusicMixin'):
        assert 'local/pixelmon/backport/'+component+'.class' in names
    assert json.loads(jar.read('assets/pixelmon_backport/lang/zh_cn.json'))['item.pixelmon_backport.mysterious_potion']=='神秘药剂'
    assert 'assets/pixelmon_backport/textures/item/mysterious_potion.png' in names
    potion=json.loads(jar.read('data/pixelmon_backport/recipes/mysterious_potion.json'))
    assert potion['pattern']==['LWA','JGC','SPF'] and potion['key']['S']['item']=='minecraft:grass' and potion['key']['A']['item']=='minecraft:acacia_sapling'
    assert potion['key']['P']=={'type':'forge:nbt','item':'minecraft:potion','nbt':{'Potion':'minecraft:water'}}
    hidden=json.loads(jar.read('data/pixelmon_backport/advancements/adventure/forbidden_name_tag.json'))['display']
    assert hidden['hidden'] and hidden['frame']=='challenge'
    assert json.loads(jar.read('backport/gym_cities.json'))['newThemes']['rock']['biomes']==['pixelmon:ultra_desert']
    assert 'BattleExitMusicMixin' in json.loads(jar.read('pixelmon_backport.mixins.json'))['client']
    if int(revision[1:])>=15:
        for cls in ('GogglesCurios','UltraMaterialRecipes','mixin/GogglesFogDensityMixin','mixin/GogglesFogDistanceMixin'):
            assert 'local/pixelmon/backport/'+cls+'.class' in names
        for tag in ('minecraft/stone_crafting_materials','minecraft/stone_tool_materials','forge/cobblestone','forge/cobblestone/normal'):
            ns,path=tag.split('/',1)
            assert 'pixelmon:ultra_space_stone' in json.loads(jar.read('data/'+ns+'/tags/items/'+path+'.json'))['values']
        assert 'pixelmon:safety_goggles' in json.loads(jar.read('data/curios/tags/items/head.json'))['values']
        crystal=json.loads(jar.read('data/pixelmon_backport/recipes/crystal_from_ultra_desert_gilded_sand.json'))
        blast=json.loads(jar.read('data/pixelmon_backport/recipes/crystal_from_ultra_desert_gilded_sand_blasting.json'))
        glass=json.loads(jar.read('data/pixelmon_backport/recipes/glass_from_ultra_desert_sand.json'))
        assert crystal['result']==blast['result']=='pixelmon:crystal' and crystal['cookingtime']==200 and blast['cookingtime']==100
        assert glass['type']=='minecraft:smelting' and glass['result']=={'item':'minecraft:glass','count':2}
        fixes=json.loads(jar.read('backport/description_fixes_zh_cn.json'))
        assert fixes['block.pixelmon.blue_tumblestone']=='天蓝玉石原矿' and fixes['block.pixelmon.red_tumblestone']=='玉石原矿' and fixes['block.pixelmon.black_tumblestone']=='黑玉石原矿'
addon=DIST/'ohmygod-greninja-1.16.5-1.0.1.jar'
with ZipFile(addon) as jar:assert 'versionRange="[1.7.0,1.9.0)"' in jar.read('META-INF/mods.toml').decode()
entries={'mods/'+core.name:data,'mods/'+core.name+'.sha256':(sha(data)+'  '+core.name+'\n').encode(),
    '更新说明.md':(ROOT/'docs/UPDATE_1.8.0_SUMMARY.md').read_bytes(),
    '可选模组/'+addon.name:addon.read_bytes(),'可选模组/安装说明.md':(ROOT/'addons/ohmygod-greninja/README.md').read_bytes(),
    '安装说明.txt':('重铸内容回迁1.8.0本地测试版 '+revision+'（2026-10-26）\n关闭双方游戏/服务端后替换旧回迁核心，只保留mods内这一份。\n此包替代此前1.8.0测试包，请勿同时安装两份同名核心；以本包及SHA256为准。\nMinecraft1.16.5 / Forge36.2.42 / Java11 / Pixelmon9.1.12～9.1.13。\n可选忍蛙增强默认不启用；启用时请使用1.0.1替换旧1.0.0，双方版本一致。\n不需要额外资源包，不包含个人存档或自动化测试助手。本版尚未发布GitHub。\n').encode('utf-8-sig')}
if int(revision[1:])>=14:
    import tomllib
    furnace=ROOT/'pack-overrides/config/ironfurnaces-client.toml'
    assert tomllib.loads(furnace.read_text('utf-8-sig'))['updates']['check_updates']['updates'] is False
    entries['config/ironfurnaces-client.toml']=furnace.read_bytes()
    entries['安装说明.txt']+='\n本包config文件夹也须覆盖到客户端实例：关闭万能熔炉更新检查及提醒，重启游戏生效。只替换核心JAR不会应用此配置。\n'.encode('utf-8')
out=DIST/('Pixelmon-Backport-1.8.0-test-'+revision+'.zip')
with ZipFile(out,'w',ZIP_DEFLATED,compresslevel=6) as jar:
    for name,value in entries.items():jar.writestr(name,value)
with ZipFile(out) as jar:
    assert jar.testzip() is None and jar.read('mods/'+core.name)==data
out.with_suffix('.zip.sha256').write_text(sha(out.read_bytes())+'  '+out.name+'\n','ascii')
receipt={'version':'1.8.0','revision':revision,'core':str(core),'core_sha256':sha(data),'package':str(out),'package_sha256':sha(out.read_bytes()),'github_published':False,'whole_regression_run':False,'private_auto_reel_included':False}
(ROOT/'.cache/update-180/package-receipt.json').write_text(json.dumps(receipt,ensure_ascii=False,indent=2),'utf8')
print(json.dumps(receipt,ensure_ascii=False,indent=2))
