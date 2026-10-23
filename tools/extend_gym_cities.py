"""Create eight themed gym cities from the supplied legacy Pixelmon template library.

Requires nbtlib. Source templates remain Pixelmon assets; generated additions and
conversion code are in this repository. No external world or player data is read.
"""
import argparse, copy, gzip, io, json, pathlib, re, zipfile
import nbtlib
from city_architecture import rebuild

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / 'resources'
THEMES = {
    'poison': dict(source='steel', anchor='ghost', city='瘴辉城', badge='瘴辉徽章', icon='soul_badge', color='purple', accent='lime', trim='dark_prismarine', feature='净化实验室与玻璃反应塔', roster=[94,89,110,169,545,591,748,34,31,454,452,849]),
    'dark': dict(source='ghost', anchor='ghost', city='暮影城', badge='暮影徽章', icon='dark_badge', color='black', accent='gray', trim='polished_blackstone_bricks', feature='黑石街区与夜灯钟塔', roster=[197,229,248,359,461,625,635,687,727,861,430,452]),
    'fighting': dict(source='fire', anchor='fire', city='武心城', badge='武心徽章', icon='cobble_badge', color='red', accent='white', trim='polished_andesite', feature='演武街区与道场门楼', roster=[68,106,107,237,214,286,308,448,534,620,701,784]),
    'flying': dict(source='dragon', anchor='dragon', city='风穹城', badge='风穹徽章', icon='feather_badge', color='light_blue', accent='white', trim='quartz_block', feature='山城街巷与展翼观景台', roster=[18,130,149,169,227,277,398,468,581,663,715,823]),
    'normal': dict(source='grass', anchor='grass', city='常青埠', badge='日常徽章', icon='plain_badge', color='white', accent='brown', trim='bricks', feature='生活商街与市民钟楼', roster=[143,242,233,474,217,241,289,397,424,626,660,760]),
    'bug': dict(source='fairy', anchor='grass', city='翠茧城', badge='翠茧徽章', icon='hive_badge', color='lime', accent='yellow', trim='mossy_stone_bricks', feature='庄园花园街区与蜂巢温室', roster=[123,127,212,214,469,545,596,637,752,768,738,542]),
    'psychic': dict(source='electric', anchor='fairy', city='星念城', badge='星念徽章', icon='mind_badge', color='magenta', accent='purple', trim='purpur_block', feature='高塔街区与星轨观测台', roster=[65,80,103,121,196,282,308,376,437,576,579,858]),
    'rock': dict(source='ground', anchor='ground', city='磐岚城', badge='磐岚徽章', icon='boulder_badge', color='brown', accent='orange', trim='cut_sandstone', feature='石砌商街与阶梯岩塔', roster=[76,141,142,248,306,346,348,464,526,567,697,774]),
}
NATIVE_BADGES = dict(electric='bolt_badge', fire='fire_badge', ice='ice_badge', dragon='dragon_badge', ground='jimen_badge', steel='mineral_badge', ghost='ghost_badge', fairy='fairy_badge_galar', grass='grass_badge', water='water_badge')
MEGA_ANCHORS=dict(electric=181,fire=6,ice=362,dragon=373,ground=445,steel=376,ghost=94,fairy=282,grass=3,water=9,poison=94,dark=229,fighting=448,flying=18,normal=115,bug=212,psychic=65,rock=142)
BIOMES = dict(normal=['plains','sunflower_plains'],fighting=['mountains','wooded_mountains'],poison=['swamp','swamp_hills'],flying=['gravelly_mountains','modified_gravelly_mountains'],psychic=['small_end_islands','end_midlands','end_highlands','end_barrens'],bug=['jungle','jungle_hills','bamboo_jungle','bamboo_jungle_hills'],rock=['mountain_edge','stone_shore','desert_lakes'],dark=['dark_forest','dark_forest_hills'])
for theme,definition in THEMES.items():definition['biomes']=['minecraft:'+b for b in BIOMES[theme]]

def write_json(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def read_nbt(blob):
    return nbtlib.File.parse(io.BytesIO(gzip.decompress(blob)))

def save_nbt(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    stream = io.BytesIO(); data.write(stream)
    path.write_bytes(gzip.compress(stream.getvalue(), mtime=0))

def remap(text, old, new):
    return text.replace(f'pixelmon:gyms/{old}/', f'pixelmon_backport:gyms/{new}/').replace(f'pixelmon:npc/gyms/{old}', f'pixelmon_backport:npc/gyms/{new}').replace(f'gym_{old}_', f'gym_{new}_').replace(f'gym_{old}"', f'gym_{new}"')

def strings(tag, old, new):
    if isinstance(tag, nbtlib.String):
        text = remap(str(tag), old, new)
        if text == f'pixelmon:gyms/{old}':text = f'pixelmon_backport:gyms/{new}'
        if text == f'pixelmon:gyms/{new}/gym_{old}':text = f'pixelmon_backport:gyms/{new}/gym_{new}'
        return nbtlib.String(text)
    if isinstance(tag, nbtlib.Compound):
        for key in list(tag):tag[key] = strings(tag[key], old, new)
    elif isinstance(tag, nbtlib.List):
        for index in range(len(tag)):tag[index] = strings(tag[index], old, new)
    return tag

def mark_npcs(data, theme, fresh=False, role=''):
    from embedded_npc_zh_cn import translate
    translate(data)
    for entry in data.get('entities', []):
        entity = entry.get('nbt', {})
        if str(entity.get('id', '')) != 'pixelmon:npc_trainer':continue
        persistent = entity.setdefault('ForgeData', nbtlib.Compound())
        persistent['BackportGymType'] = nbtlib.String(theme)
        persistent['BackportGymRole'] = nbtlib.String(role)
        entity['GymLeader']=nbtlib.Byte(role=='leader')
        if role=='leader':entity['WinMoney']=nbtlib.Int(200000)
        if role!='leader':
            winnings=entity.get('WinningsTag',{})
            for key in list(winnings):
                if str(winnings[key].get('id','')).endswith('_badge'):del winnings[key]
        if not fresh and role == 'leader' and not entity.get('WinningsTag'):
            entity['WinningsTag'] = nbtlib.Compound({'item0': nbtlib.Compound({'id': nbtlib.String('pixelmon:' + NATIVE_BADGES[theme]), 'Count': nbtlib.Byte(1)})})
        if not fresh and role=='leader' and theme=='ground':
            for item in entity.get('WinningsTag',{}).values():
                if str(item.get('id',''))=='pixelmon:mine_badge':item['id']=nbtlib.String('pixelmon:'+NATIVE_BADGES[theme])
        if fresh:
            persistent['BackportGymNew'] = nbtlib.Byte(1)
            definition = THEMES[theme]
            entity['DefaultName'] = nbtlib.Byte(0)
            entity['Name'] = nbtlib.String(definition['city'] + ('馆主' if role == 'leader' else '训练家'))
            for key in ['DefaultGreet', 'DefaultWin', 'DefaultLose']:entity[key] = nbtlib.Byte(0)
            entity['Greeting'] = nbtlib.String(f"欢迎挑战{definition['city']}道馆，让战术决定胜负。")
            entity['WinMessage'] = nbtlib.String('战斗还有更多可能，再来挑战吧。')
            entity['LoseMessage'] = nbtlib.String('精彩的对战！期待我们的下一次交手。')
            if role == 'leader':
                entity['GymLeader'] = nbtlib.Byte(1)
                entity['WinningsTag'] = nbtlib.Compound({'item0': nbtlib.Compound({'id': nbtlib.String(f'pixelmon_backport:{theme}_gym_badge'), 'Count': nbtlib.Byte(1)})})

def decorate(data, theme):
    definition = THEMES[theme]
    for state in data.get('palette', []):
        name = str(state['Name'])
        match = re.fullmatch(r'minecraft:(?:white|orange|magenta|light_blue|yellow|lime|pink|gray|light_gray|cyan|purple|blue|brown|green|red|black)_(wool|concrete|terracotta|stained_glass|stained_glass_pane)', name)
        if match:
            accent=any(name.startswith('minecraft:'+color+'_') for color in ['yellow','lime','orange','white'])
            state['Name'] = nbtlib.String(f'minecraft:{definition["accent"] if accent else definition["color"]}_{match.group(1)}')
        if name == 'minecraft:stone_bricks':state['Name'] = nbtlib.String('minecraft:' + definition['trim'])
        # Decorative geology follows the destination, not the source city's dimension.
        family={'poison':'prismarine','dark':'polished_blackstone_brick','fighting':'andesite','flying':'quartz',
                'normal':'brick','bug':'mossy_stone_brick','psychic':'purpur','rock':'sandstone'}[theme]
        families=('sandstone','smooth_sandstone','red_sandstone','smooth_red_sandstone','nether_brick','red_nether_brick')
        raw=name.split(':',1)[1]
        if name.startswith('pixelmon:ultra_desert_'):raw=raw[len('ultra_desert_'):]
        for old in families:
            if raw in [old,old+'s','chiseled_'+old,'cracked_'+old,'cut_'+old] or raw.startswith(old+'_'):
                suffix=next((s for s in ['stairs','slab','wall','fence'] if raw.endswith('_'+s)),None)
                if suffix=='fence':mapped='iron_bars'
                elif suffix=='wall' and family in ['quartz','purpur','prismarine']:mapped='stone_brick_wall'
                elif suffix:mapped=family+'_'+suffix
                else:mapped={'prismarine':'dark_prismarine','polished_blackstone_brick':'polished_blackstone_bricks',
                             'andesite':'polished_andesite','quartz':'smooth_quartz','brick':'bricks',
                             'mossy_stone_brick':'mossy_stone_bricks','purpur':'purpur_block','sandstone':'cut_sandstone'}[family]
                state['Name']=nbtlib.String('minecraft:'+mapped)
                if not suffix or suffix=='fence':state.pop('Properties',None)
                break
        if name in ['minecraft:soul_sand','minecraft:soul_soil','minecraft:netherrack']:
            state['Name']=nbtlib.String('minecraft:'+('coarse_dirt' if theme in ['dark','bug'] else definition['trim']));state.pop('Properties',None)
        if name in ['minecraft:lava','minecraft:fire','minecraft:soul_fire']:
            state['Name']=nbtlib.String('minecraft:water' if name=='minecraft:lava' else 'minecraft:air');state.pop('Properties',None)
        if name in ['minecraft:nether_sprouts','minecraft:nether_wart','minecraft:potted_cactus']:
            state['Name']=nbtlib.String('minecraft:potted_fern' if 'potted' in name else 'minecraft:fern');state.pop('Properties',None)
        if name=='minecraft:nether_quartz_ore' or name=='minecraft:nether_gold_ore':
            state['Name']=nbtlib.String('minecraft:coal_ore' if theme=='dark' else 'minecraft:iron_ore')
        if name.startswith('minecraft:') and raw.endswith('_bed'):
            state['Name']=nbtlib.String('minecraft:'+definition['color']+'_bed')
        if name in ['minecraft:soul_lantern','minecraft:soul_campfire','minecraft:soul_torch','minecraft:soul_wall_torch'] and theme!='dark':
            state['Name']=nbtlib.String(name.replace('soul_',''))
    for index, entry in enumerate(data.get('entities',[])):
        entity=entry.get('nbt',{});kind=str(entity.get('id',''))
        if kind=='pixelmon:statue':
            entity['ndex']=nbtlib.Short(definition['roster'][index%len(definition['roster'])])
            entity['Variant']=nbtlib.String('');entity['form']=nbtlib.String('');entity['palette']=nbtlib.String('none')
        elif kind=='minecraft:item_frame' and str(entity.get('Item',{}).get('id',''))=='pixelmon:pixelmon_sprite':
            tag=entity['Item'].setdefault('tag',nbtlib.Compound())
            tag['ndex']=nbtlib.Short(definition['roster'][index%len(definition['roster'])]);tag['form']=nbtlib.String('');tag['palette']=nbtlib.String('none')
    for block in data.get('blocks',[]):
        nbt=block.get('nbt',{})
        if 'sign' not in str(nbt.get('id','')):continue
        for key in ['Text1','Text2','Text3','Text4']:
            if key not in nbt:continue
            text=str(nbt[key]);source=definition['source']
            if source.upper() in text or source.title() in text:
                nbt[key]=nbtlib.String(text.replace(source.upper(),theme.upper()).replace(source.title(),theme.title()))

def landmark(data, theme):
    """Add a distinct, solid roof landmark, preserving entrances and trainer connectors."""
    palette = data['palette']; blocks = data['blocks']; size = [int(x) for x in data['size']]
    cx, y, cz = size[0] // 2, size[1], size[2] // 2
    ids = {}
    def place(x, height, z, block):
        if not (0 <= x < size[0] and 0 <= z < size[2]):return
        name = 'minecraft:' + block
        if name not in ids:
            ids[name] = len(palette);palette.append(nbtlib.Compound({'Name': nbtlib.String(name)}))
        blocks.append(nbtlib.Compound({'pos': nbtlib.List[nbtlib.Int]([x, height, z]), 'state': nbtlib.Int(ids[name])}))
    color = THEMES[theme]['color']; accent = THEMES[theme]['accent']; trim = THEMES[theme]['trim']
    for dx in range(-6, 7):
        for dz in range(-6, 7):place(cx+dx, y, cz+dz, trim)
    if theme == 'poison':
        for dx in [-4, 4]:
            for dy in range(1, 10):
                for dz in [-1, 0, 1]:
                    for ox in [-1, 0, 1]:place(cx+dx+ox, y+dy, cz+dz, 'lime_stained_glass' if abs(ox)+abs(dz)>0 else 'lime_concrete')
    elif theme == 'flying':
        for dx in range(-11, 12):
            for dz in range(-2, 3):place(cx+dx, y+2+abs(dx)//3, cz+dz, 'quartz_block')
    elif theme == 'fighting':
        for dx in [-5, 5]:
            for dy in range(1, 9):place(cx+dx, y+dy, cz, 'red_concrete')
        for dx in range(-7, 8):place(cx+dx, y+8, cz, 'red_concrete');place(cx+dx, y+9, cz, 'black_concrete')
    elif theme == 'bug':
        for dx in range(-5, 6):
            for dz in range(-5, 6):
                h = 6-max(abs(dx), abs(dz))
                place(cx+dx, y+h+1, cz+dz, 'lime_stained_glass')
        for dx in [-3, 3]:place(cx+dx, y+2, cz, 'honeycomb_block')
    elif theme == 'psychic':
        for dx in range(-6, 7):
            for dy in range(1, 12):
                if 23 <= dx*dx+(dy-6)**2 <= 35:place(cx+dx, y+dy, cz, 'magenta_stained_glass')
        for dy in range(1, 9):place(cx, y+dy, cz+2, 'purpur_pillar')
    elif theme == 'rock':
        for dy in range(1, 10):
            width = max(1, 6-dy//2)
            for dx in range(-width, width+1):
                for dz in range(-width, width+1):place(cx+dx, y+dy, cz+dz, 'cut_sandstone' if dy%2 else 'brown_terracotta')
    else:
        for dy in range(1, 10):
            for dx in range(-2, 3):
                for dz in range(-2, 3):place(cx+dx, y+dy, cz+dz, trim)
        for dx in range(-3, 4):
            for dy in range(3, 10):place(cx+dx, y+dy, cz-3, color+'_concrete' if abs(dx)==3 or dy in [3,9] else accent+'_concrete')
        for dy in range(5, 9):place(cx, y+dy, cz-4, 'black_concrete' if theme=='normal' else 'sea_lantern')
    data['size'][1] = nbtlib.Int(size[1]+13)

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--pixelmon', type=pathlib.Path, required=True);args=parser.parse_args()
    signatures=[]; count=0;templates=[]
    with zipfile.ZipFile(args.pixelmon) as archive:
        names=archive.namelist()
        # Mark original gym templates without changing their authored teams or training values.
        for name in names:
            if not name.startswith('data/pixelmon/structures/gyms/') or not name.endswith('.nbt'):continue
            if not any(f'_{role}_' in name for role in ['leader','grunt','elite']):continue
            data=read_nbt(archive.read(name));theme=name.split('/')[4]
            role=next((r for r in ['leader','grunt','elite'] if f'_{r}_' in name),'')
            for entry in data.get('entities', []):
                npc=entry.get('nbt', {});first=npc.get('pokeStore', {}).get('party0', {})
                if str(npc.get('BossTier',''))=='equal' and 'ndex' in first:
                    signatures.append(dict(name=str(npc.get('Name','')), firstDex=int(first['ndex']), type=theme,role=role))
            mark_npcs(data, theme,role=role);save_nbt(RES/name, data)
        for theme, definition in THEMES.items():
            source=definition['source']
            generated=set()
            for name in names:
                if name.startswith(f'data/pixelmon/structures/gyms/{source}/') and name.endswith('.nbt'):
                    data=read_nbt(archive.read(name));strings(data, source, theme);decorate(data, theme)
                    filename=name.rsplit('/',1)[1].replace(f'gym_{source}',f'gym_{theme}')
                    role=next((r for r in ['leader','grunt','elite'] if f'_{r}_' in filename),'')
                    mark_npcs(data, theme, True, role)
                    try:rebuild(data,theme,filename)
                    except ValueError as error:raise ValueError((theme,filename,error.args)) from error
                    target=RES/'data/pixelmon_backport/structures/gyms'/theme/filename
                    save_nbt(target, data);generated.add(target);count+=1;templates.append(target.relative_to(RES).as_posix())
                elif name.startswith(f'data/pixelmon/worldgen/template_pool/gyms/{source}/') and name.endswith('.json'):
                    text=remap(archive.read(name).decode(), source, theme)
                    target=RES/'data/pixelmon_backport/worldgen/template_pool/gyms'/theme/name.rsplit('/',1)[1]
                    pool=json.loads(text)
                    # The supplied steel terminator pool references a nonexistent
                    # c template. Use its compatible authored b terminator.
                    if theme=='poison':
                        for entry in pool['elements']:
                            element=entry['element']
                            if element.get('location','').endswith('gym_poison_road_terminator_c'):element['location']=element['location'][:-1]+'b'
                    for entry in pool['elements']:
                        location=entry['element'].get('location','')
                        if not location and target.name in ['street.json','house.json']:entry['weight']=0
                        elif '_farm_' in location or '_berry_' in location:entry['weight']=1
                        elif '_house_' in location:entry['weight']*=3 if theme in ['normal','fighting','rock'] else 2
                        elif '_tree_' in location:entry['weight']=min(entry['weight'],4)
                    # Authored streets have their own supported themed trees.
                    # Native feature elements can legitimately decline placement.
                    pool['elements']=[e for e in pool['elements'] if e['weight']>0 and e['element'].get('element_type')!='minecraft:feature_pool_element']
                    write_json(target,pool);generated.add(target)
                elif name==f'data/pixelmon/worldgen/template_pool/npc/gyms/{source}.json':
                    write_json(RES/f'data/pixelmon_backport/worldgen/template_pool/npc/gyms/{theme}.json',json.loads(remap(archive.read(name).decode(),source,theme)))
            model=json.loads(archive.read(f'assets/pixelmon/models/item/{definition["icon"]}.json'))
            write_json(RES/f'assets/pixelmon_backport/models/item/{theme}_gym_badge.json',model)
            for folder,suffix in [('data/pixelmon_backport/structures/gyms','*.nbt'),('data/pixelmon_backport/worldgen/template_pool/gyms','*.json')]:
                for obsolete in (RES/folder/theme).glob(suffix):
                    if obsolete not in generated:obsolete.unlink()
        # A dark-forest city slot also retains the original grass city candidate.
        pool=json.loads(archive.read('data/pixelmon/worldgen/template_pool/gyms/grass/start_pool.json'))
        pool['name']='pixelmon_backport:gyms/natural/dark/start_pool'
        dark=json.loads((RES/'data/pixelmon_backport/worldgen/template_pool/gyms/dark/start_pool.json').read_text(encoding='utf-8'))
        pool['elements'].extend(dark['elements'])
        write_json(RES/'data/pixelmon_backport/worldgen/template_pool/gyms/natural/dark/start_pool.json',pool)
        for anchor in sorted({d['anchor'] for d in THEMES.values()}):
            obsolete=RES/f'data/pixelmon/worldgen/template_pool/gyms/{anchor}/start_pool.json'
            if obsolete.exists():obsolete.unlink()
    for definition in THEMES.values():definition['architectureRevision']=6
    write_json(RES/'backport/gym_cities.json',dict(nativeSignatures=signatures,nativeBadges=NATIVE_BADGES,newThemes=THEMES,megaAnchors=MEGA_ANCHORS))
    write_json(RES/'backport/gym_templates.json',dict(templates=sorted(templates)))
    help_path=RES/'backport/item_help.json';help_data=json.loads(help_path.read_text(encoding='utf-8'))
    for theme, definition in THEMES.items():
        from extend_version13 import LOCATIONS
        help_data[f'pixelmon_backport:{theme}_gym_badge']=[f'用途：{definition["city"]}道馆的挑战纪念徽章，可收藏或展示。',f'获取：击败{definition["city"]}馆主，作为道馆胜利奖励获得。',f'道馆地点：{LOCATIONS[theme]}。需探索启用结构生成的未生成区域。']
    write_json(help_path,help_data)
    for language in ['zh_cn','en_us']:
        path=RES/f'assets/pixelmon_backport/lang/{language}.json';data=json.loads(path.read_text(encoding='utf-8'))
        for theme, definition in THEMES.items():data[f'item.pixelmon_backport.{theme}_gym_badge']=definition['badge'] if language=='zh_cn' else theme.title()+' Gym Badge'
        write_json(path,data)
    from add_center_music_blocks import apply as restore_music
    restore_music(args.pixelmon)
    print(f'Generated {len(THEMES)} complete themed city libraries, {count} structure pieces, {len(signatures)} native gym NPC signatures and eight badge models.')

if __name__=='__main__':main()
