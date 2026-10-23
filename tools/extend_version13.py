"""Generate translated labels and truthful badge sources from supported native data."""
import argparse, json, pathlib, zipfile, hashlib
from extend_advancements import sprite_icon, advancement, write
ROOT=pathlib.Path(__file__).resolve().parents[1]
REGIONS=['','关都','城都','丰缘','神奥','合众','卡洛斯','阿罗拉','伽勒尔','帕底亚']
EN_REGIONS=['','Kanto','Johto','Hoenn','Sinnoh','Unova','Kalos','Alola','Galar','Paldea']
BIOMES={'badlands':'恶地','beach':'海滩','desert':'沙漠','end':'末地','forest':'森林','ice':'冰原','icy':'冰原','jungle':'丛林','mountain':'山地','hills':'丘陵','mushroom':'蘑菇岛','nether':'下界','plains':'平原','snow':'雪原','taiga':'针叶林','water':'水域','swamp':'沼泽','dark_forest':'黑森林','oceans':'海洋','savanna':'热带草原','ultra_deep_sea':'究极深海','ultra_desert':'究极沙漠','ultra_forest':'究极森林','ultra_jungle':'究极丛林','ultra_plant':'究极植物世界'}
SPECIAL={'uno_shrine':'急冻鸟神坛','dos_shrine':'闪电鸟神坛','tres_shrine':'火焰鸟神坛','ilex_shrine':'栎树林祠堂','land_chalice':'大地圣杯','water_chalice':'海洋圣杯','spear_pillar':'枪之柱','plasma_ship':'等离子团船','boats':'宝可梦船只','graveyard':'墓园','haunted_tower':'幽灵塔','tower_water':'水之塔','tower_darkness':'恶之塔','ultra_desert_deco':'究极沙漠遗迹','ultra_jungle_deco':'究极丛林遗迹','ultra_crater_den':'究极陨坑巢穴','pumpkin_house':'南瓜屋','gyms':'宝可梦道馆城市','moving':'宝可梦移动建筑','advanced_moving':'大型宝可梦移动建筑','crater_geyser':'陨坑间歇泉'}
LOCATIONS={'normal':'平原、向日葵平原','fighting':'山地、繁茂山地','poison':'沼泽、沼泽丘陵','flying':'砾石山地、变种砾石山地','psychic':'末地外岛（不在中心主岛生成）','bug':'丛林、丛林丘陵、竹林、竹林丘陵','rock':'山地边缘、石岸、沙漠湖泊','dark':'黑森林、黑森林丘陵','grass':'森林类群系（黑森林中的草与恶城市均可能生成）','fire':'下界荒地（下界）','ghost':'灵魂沙峡谷（下界）','fairy':'桦木森林','ice':'积雪冻原','electric':'热带草原高原、破碎热带草原、破碎热带草原高原','steel':'热带草原','dragon':'针叶林类群系','water':'深海类群系','ground':'恶地类群系'}
TYPE_NAMES=dict(zip(['normal','fire','water','electric','grass','ice','fighting','poison','ground','flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy'],['一般','火','水','电','草','冰','格斗','毒','地面','飞行','超能力','虫','岩石','幽灵','龙','恶','钢','妖精']))

def structure_name(name):
    if name in SPECIAL:return SPECIAL[name]
    if name.startswith('grotto_'):return BIOMES[name[7:]]+'隐秘洞穴'
    if name.endswith('_den'):return BIOMES[name[:-4]]+'极巨巢穴'
    if name.endswith('_way_point'):return BIOMES[name[:-10]]+'路标'
    raise ValueError(('Untranslated native structure',name))

def main():
    p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);a=p.parse_args()
    catalogue=json.loads((ROOT/'docs/NATIVE_LANGUAGE_CATALOGUE_1.3.0.json').read_text(encoding='utf-8'))
    with zipfile.ZipFile(a.pixelmon) as jar:
        zh=json.loads(jar.read('assets/pixelmon/lang/zh_cn.json'));en=json.loads(jar.read('assets/pixelmon/lang/en_us.json'));assets=set(jar.namelist())
        species={json.loads(jar.read(n))['dex']:json.loads(jar.read(n)) for n in assets if n.startswith('data/pixelmon/species/') and n.endswith('.json')}
    own=json.loads((ROOT/'resources/assets/pixelmon_backport/lang/zh_cn.json').read_text(encoding='utf-8'));own_en=json.loads((ROOT/'resources/assets/pixelmon_backport/lang/en_us.json').read_text(encoding='utf-8'))
    merged=dict(zh);merged.update(own);native={};native_en={};mismatches=[]
    # Direct classloader reads do not merge same-path language overlays. Give
    # the manual map helper a separate, complete species-name catalogue.
    write(ROOT/'resources/backport/species_names_zh.json',{key:value for key,value in merged.items() if key.startswith('pixelmon.') and key.count('.')==1})
    for row in catalogue['machines']:
        key='attack.'+row['attack'].lower().replace(' ','_');name=merged.get(key)
        if name is None:raise ValueError(('Missing attack translation',row,key))
        kind='TR' if row['prefix'].startswith('tr') else 'TM';number=f'{row["id"]:02d}' if row['generation']>=8 else str(row['id'])
        item=f'item.pixelmon.{row["prefix"]}.{row["id"]}';value=f'{REGIONS[row["generation"]]} {kind}{number}: {name}'
        native[item]=value;native_en[item]=f'{EN_REGIONS[row["generation"]]} {kind}{number}: {row["attack"]}'
        if zh.get(item)!=value:mismatches.append({'key':item,'before':zh.get(item),'after':value})
    for name in catalogue['structures']:native['structure.pixelmon.'+name]=structure_name(name)
    native['item.pixelmon.den_spawner']='极巨巢穴生成器';native_en['item.pixelmon.den_spawner']='Max Den Generator'
    native['dimension.pixelmon.ultra_space']='究极空间';native['dimension.pixelmon.drowned_world']='溺水世界'
    native_en['dimension.pixelmon.ultra_space']='Ultra Space';native_en['dimension.pixelmon.drowned_world']='Drowned World'
    write(ROOT/'resources/assets/pixelmon/lang/zh_cn.json',native);write(ROOT/'resources/assets/pixelmon/lang/en_us.json',native_en)
    own['item.pixelmon_backport.tera_den_spawner']='太晶巢穴生成器';own_en['item.pixelmon_backport.tera_den_spawner']='Tera Den Generator'
    own.update({'pixelmon_backport.field_mega.no_stone':'未携带 Mega 石','pixelmon_backport.field_mega.stone':'携带的 Mega 石与这只宝可梦不匹配。','pixelmon_backport.field_mega.blue_orb':'请给盖欧卡携带靛蓝色宝珠。','pixelmon_backport.field_mega.red_orb':'请给固拉多携带朱红色宝珠。','pixelmon_backport.field_mega.primal_started':'开始场外原始回归。','pixelmon_backport.field_mega.reverted':'已还原场外形态。','pixelmon_backport.field_wheel.primal':'场外原始回归','pixelmon_backport.field_wheel.revert':'还原场外形态'})
    own_en.update({'pixelmon_backport.field_mega.no_stone':'No Mega Stone held','pixelmon_backport.field_mega.stone':'The held Mega Stone does not match this Pokemon.','pixelmon_backport.field_mega.blue_orb':'Kyogre must hold a Blue Orb.','pixelmon_backport.field_mega.red_orb':'Groudon must hold a Red Orb.','pixelmon_backport.field_mega.primal_started':'Starting field Primal Reversion.','pixelmon_backport.field_mega.reverted':'Field form restored.','pixelmon_backport.field_wheel.primal':'Field Primal Reversion','pixelmon_backport.field_wheel.revert':'Restore field form'})
    own.update({'pixelmon_backport.field_mega.rusted_sword':'请给苍响携带腐朽的剑。','pixelmon_backport.field_mega.rusted_shield':'请给藏玛然特携带腐朽的盾。','pixelmon_backport.field_mega.crowned_started':'开始剑之王／盾之王形态变化。','pixelmon_backport.field_wheel.crowned_sword':'场外剑之王变化','pixelmon_backport.field_wheel.crowned_shield':'场外盾之王变化'})
    own_en.update({'pixelmon_backport.field_mega.rusted_sword':'Zacian must hold a Rusted Sword.','pixelmon_backport.field_mega.rusted_shield':'Zamazenta must hold a Rusted Shield.','pixelmon_backport.field_mega.crowned_started':'Starting Crowned form change.','pixelmon_backport.field_wheel.crowned_sword':'Field Crowned Sword Change','pixelmon_backport.field_wheel.crowned_shield':'Field Crowned Shield Change'})
    own['pixelmon_backport.field_mega.already']='已处于特殊形态，无需再次变化。';own_en['pixelmon_backport.field_mega.already']='Already in a special form.'
    for data,regions in [(own,REGIONS),(own_en,EN_REGIONS)]:
        data['pixelmon.backport.technical_move.name']='%s %s%s: %s'
        for gen in range(1,10):data['pixelmon.backport.region.gen'+str(gen)]=regions[gen]
    write(ROOT/'resources/assets/pixelmon_backport/lang/zh_cn.json',own);write(ROOT/'resources/assets/pixelmon_backport/lang/en_us.json',own_en)
    help_path=ROOT/'resources/backport/item_help.json';help_data=json.loads(help_path.read_text(encoding='utf-8'));gyms=json.loads((ROOT/'resources/backport/gym_cities.json').read_text(encoding='utf-8'))
    for theme,location in LOCATIONS.items():
        if theme in gyms['newThemes']:
            definition=gyms['newThemes'][theme];item=f'pixelmon_backport:{theme}_gym_badge';label=definition['city']
        else:item='pixelmon:'+gyms['nativeBadges'][theme];label=TYPE_NAMES[theme]+'属性道馆'
        help_data[item]=[f'用途：{label}的挑战纪念徽章，可收藏或展示。',f'获取：击败{label}馆主后，从战利品界面领取对应胜利奖励。',f'道馆地点：{location}。需探索启用结构生成的未生成区域。']
    help_data['pixelmon_backport:tera_den_spawner']=['创造模式：右键方块生成太晶巢穴，手持左击可移除太晶巢穴；不能移除极巨巢穴。','获取：创造物品栏；自然太晶巢穴不需要生成器。']
    help_data['pixelmon:den_spawner']=['创造模式：右键生成极巨巢穴，手持左击可移除极巨巢穴；不能移除太晶巢穴。','获取：原生创造物品栏；保留整合包的管理员权限设置。']
    write(help_path,help_data)
    target=ROOT/'resources/data/pixelmon_backport/advancements/legends';master={'item':'pixelmon:master_ball'}
    write(target/'primal.json',advancement('古始之力','让自己拥有的固拉多与盖欧卡在真实对战或场外变化中完成原始回归。',master,'root'))
    transformations=[]
    for dex,title,name,orb in [(383,'大地初醒','固拉多','朱红色宝珠'),(382,'沧海溯源','盖欧卡','靛蓝色宝珠')]:
        ident=f'form_{dex}_primal';icon=sprite_icon(species,dex,assets,'primal')
        write(target/(ident+'.json'),advancement(title,f'让自己拥有的{name}携带{orb}，在支持原始回归的真实对战中，或通过场外原始回归，实际完成形态变化。回归解除后保留成就。',icon,'primal'))
        transformations.append({'dex':dex,'form':'primal','id':ident,'title':title})
    cpath=ROOT/'resources/backport/advancements.json';c=json.loads(cpath.read_text(encoding='utf-8'));c['transformations']=[x for x in c['transformations'] if x['form']!='primal']+transformations
    c['captures']['494']['title']='满天星斗'
    victini=json.loads((target/'capture_494.json').read_text(encoding='utf-8'));victini['display']['title']={'text':'满天星斗'};write(target/'capture_494.json',victini)
    write(cpath,c);write(ROOT/'docs/ADVANCEMENT_CATALOGUE_1.3.0.json',c)
    write(ROOT/'docs/LANGUAGE_REVIEW_1.3.0.json',{'native_sha256':hashlib.sha256(pathlib.Path(a.pixelmon).read_bytes()).hexdigest(),'technical_moves':len(catalogue['machines']),'native_structure_names':len(catalogue['structures']),'name_changes':mismatches,'badges':18,'primal_advancements':transformations})
    print(f'Generated {len(native)} native labels, {len(catalogue["machines"])} TM/TR names, 18 badge descriptions and two primal advancements.')
if __name__=='__main__':main()
