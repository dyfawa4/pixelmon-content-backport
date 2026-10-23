"""Generate legacy-schema spawn sets, Tera assets and explicit survival routes.

Official spawn rules are translated where present. Rules for species that the
new game obtains through research are this pack's documented adaptations.
"""
import argparse,json,pathlib,zipfile,copy
ROOT=pathlib.Path(__file__).resolve().parents[1]
NS='pixelmon_backport'
NAMES=['normal','fire','water','grass','electric','ice','fighting','poison','ground','flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy','stellar']
CHINESE=['一般','火','水','草','电','冰','格斗','毒','地面','飞行','超能力','虫','岩石','幽灵','龙','恶','钢','妖精','星晶']
DISABLED_PREVIEWS={1026:'Browt',1029:'Pombon',1032:'Gecqua'}
def dump(path,x):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(x,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def run(old_path,upstream_path,output):
    old=zipfile.ZipFile(old_path);new=zipfile.ZipFile(upstream_path)
    data=json.loads((output/'backport/content.json').read_text('utf-8'))
    # User requested that incomplete preview starters stay disabled until
    # the full evolution/interaction data is available. Remove generated
    # registration and spawn entry points, while retaining offline artwork.
    data['patches']=[p for p in data['patches'] if p['species']['dex'] not in DISABLED_PREVIEWS]
    data['disabledPreviewSpecies']=list(DISABLED_PREVIEWS.values())
    # Import the pre-evolution connection as well as Dipplin itself. Otherwise
    # Syrupy Apple exists in loot but never evolves an owned Applin.
    applin=json.loads(new.read('data/pixelmon/species/840_applin.json'))
    evolution=copy.deepcopy(next(e for f in applin['forms'] for e in f.get('evolutions',[]) if e['to']=='Dipplin'))
    evolution['item']['itemID']=f'{NS}:syrupy_apple'
    patch=next((p for p in data['patches'] if p['species']['dex']==840),None)
    if patch is None:
        patch={'mode':'merge','species':{'dex':840,'name':'Applin','generation':8,'forms':[{'name':'','evolutions':[]}]}}
        data['patches'].append(patch)
    target=next(f for f in patch['species']['forms'] if f['name']=='')
    if evolution not in target.setdefault('evolutions',[]):target['evolutions'].append(evolution)
    for name in DISABLED_PREVIEWS.values():
        stale=(output/f'data/pixelmon/spawning/standard/{name.lower()}.set.json').resolve()
        assert stale.is_relative_to(output.resolve())
        if stale.is_file():
            value=json.loads(stale.read_text('utf-8'))
            assert name.lower() in json.dumps(value).lower(),'Unexpected generated spawn file'
            stale.unlink()
    items={v['name'] for v in data['referenceItems']}|{'dragoninite','darkranite','raichunite_x','raichunite_y','teal_mask'}
    mappings=[];sets=[]
    config=json.loads(old.read('data/pixelmon/config/betterspawnerconfig.json'))
    categories=set(config['biomeCategories'])
    registered_biomes={b for values in config['biomeCategories'].values() for b in values}
    for n in old.namelist():
        if n.startswith('data/pixelmon/spawning/') and n.endswith('.json'):
            def collect(v):
                if isinstance(v,dict):
                    categories.update(v.get('stringBiomes',[]))
                    for a in v.values():collect(a)
                elif isinstance(v,list):
                    for a in v:collect(a)
            collect(json.loads(old.read(n)))
    def translate(v):
        if isinstance(v,list):return [translate(a) for a in v]
        if not isinstance(v,dict):return v
        r={k:translate(a) for k,a in v.items() if k!='biomes'}
        if 'biomes' in v:
            tags=[];registry=[]
            for b in v['biomes']:
                if b.startswith('#pixelmon:spawning/'):
                    category=b.split('/')[-1].replace('_',' ')
                    if category=='rivers':
                        registry+=['minecraft:river','minecraft:frozen_river'];mappings.append([b,registry[-2:]])
                    else:
                        if category not in categories:raise ValueError('Unsupported spawn category '+b)
                        tags.append(category)
                else:registry.append(b)
            # Legacy SpawnCondition.biomes is transient; only stringBiomes
            # is deserialized and resolves both categories and registry IDs.
            if tags or registry:r['stringBiomes']=tags+registry
        if 'itemID' in r and r['itemID'].startswith('pixelmon:') and r['itemID'].split(':')[1] in items:r['itemID']=NS+':'+r['itemID'].split(':')[1]
        return r
    for name in ['dipplin','poltchageist','sinistcha','archaludon','hydrapple','ogerpon']:
        path=f'data/pixelmon/spawning/{"legendaries" if name=="ogerpon" else "standard"}/{name}.set.json'
        if path in old.namelist():continue
        value=translate(json.loads(new.read(path)))
        dump(output/path,value);sets.append({'name':name,'source':'official translated','data':value})
    # Explicit pack adaptations for research/raid-only source species. Keep
    # actual Legendary/Mythical tags; Paradox species remain ordinary rare.
    adaptations={
        'Okidogi':(['roofed','mountainous forests'],['DAY'],True,{}),
        'Munkidori':(['swamps','roofed'],['DAWN','DUSK'],True,{}),
        'Fezandipiti':(['jungles','mountainous'],['DAY'],True,{}),
        'Terapagos':(['mountainous'],['NIGHT'],True,{'maxY':40}),
        'Pecharunt':(['roofed','swamps'],['NIGHT'],True,{}),
        'GougingFire':(['pixelmon:ultra_desert'],['DAY'],False,{}),
        'RagingBolt':(['ultra space'],['DAY'],False,{}),
        'IronBoulder':(['ultra space'],['NIGHT'],False,{}),
        'IronCrown':(['ultra space'],['DAWN','DUSK'],False,{})}
    for name,(biomes,times,legend,extra) in adaptations.items():
        assert set(biomes)<=categories|registered_biomes
        info={'spec':'species:'+name,'stringLocationTypes':['Land'],'minLevel':70,'maxLevel':70,'typeID':'pokemon','condition':{'stringBiomes':biomes,'times':times,**extra},'rarity':.5 if legend else .1}
        if name=='Terapagos':info['stringLocationTypes']=['Land','Underground']
        value={'id':'Backport_'+name,'spawnInfos':[info]}
        if legend:info['tags']=['mythical' if name=='Pecharunt' else 'legendary']
        path=f'data/pixelmon/spawning/{"legendaries" if legend else "standard"}/backport_{name.lower()}.set.json'
        dump(output/path,value);sets.append({'name':name,'source':'pack adaptation of research-only source','data':value})
    # Only complete, enabled species become visible to the Pokédex.
    for patch in data['patches']:
        for f in patch['species']['forms']:
            if patch['species']['dex']>=1020 and f.get('tags') and 'undexable' in f['tags']:f['tags'].remove('undexable')
    data['starterAdditions']=[]
    # Max raids retain their exact native loot. Tera raids own their rewards.
    data.setdefault('referenceExtras',{}).pop('drops/raiddrops',None)
    chest=data['referenceExtras'].setdefault('drops/pokechestdrops',{}).setdefault('tier3',[])
    # Eternatus Meteorite uses the owner's expensive crafting route, not loot.
    craft_only={'eternatus_meteorite','metal_alloy','pink_nectar','purple_nectar','red_nectar','yellow_nectar'}
    chest[:]=[row for row in chest if (row.get('type') if isinstance(row,dict) else row) not in {NS+':'+name for name in craft_only}]
    obtainable=['clear_amulet','covert_cloak','fairy_feather','masterpiece_teacup','unremarkable_teacup','syrupy_apple','gimmighoul_coin','teal_mask','wellspring_mask','hearthflame_mask','cornerstone_mask','utility_umbrella']
    for name in obtainable:
        row={'type':f'{NS}:{name}'}
        if row not in chest:chest.append(row)
    # Native chests do not roll the added Gen9 enum values. Preserve their
    # native TM item and NBT, and give every missing TM an explicit route.
    for tm in [207,210,225,226,227,228,229]:
        row={'type':'pixelmon:tm_gen9','nbt':{'tm':tm}}
        if row not in chest:chest.append(row)
    dump(output/'backport/content.json',data)
    for nectar,flower in {'pink_nectar':'minecraft:pink_tulip','red_nectar':'minecraft:red_tulip','purple_nectar':'pixelmon:ultra_desert_cactus','yellow_nectar':'minecraft:dandelion'}.items():
        dump(output/f'data/{NS}/recipes/{nectar}.json',{'type':'minecraft:crafting_shaped','pattern':[' H ','HFH',' H '],'key':{'H':{'item':'minecraft:honey_bottle'},'F':{'item':flower}},'result':{'item':NS+':'+nectar,'count':1}})
    for name in NAMES:
        asset=f'assets/pixelmon/textures/item/tera_shards/{name}_tera_shard.png'
        dest=output/f'assets/{NS}/textures/item/{name}_tera_shard.png';dest.parent.mkdir(parents=True,exist_ok=True);dest.write_bytes(new.read(asset))
        dump(output/f'assets/{NS}/models/item/{name}_tera_shard.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name+'_tera_shard'}})
    # Recipe is an explicit legacy-pack route, not claimed as upstream's
    # research unlock. Recharge uses existing healer; shards cost 50 each.
    dump(output/f'data/{NS}/recipes/tera_orb.json',{'type':'minecraft:crafting_shaped','pattern':['ASA','SD S'.replace(' ',''),'ASA'],'key':{'A':{'item':'minecraft:amethyst_shard'} if 'minecraft:amethyst_shard' in items else {'item':'pixelmon:crystal'},'S':{'item':'minecraft:diamond'},'D':{'item':'minecraft:nether_star'}},'result':{'item':NS+':tera_orb'}})
    dump(output/f'data/{NS}/recipes/eternatus_meteorite.json',{'type':'minecraft:crafting_shaped','pattern':['BFB','BMB','BPB'],'key':{'B':{'item':'minecraft:end_stone_bricks'},'F':{'item':'pixelmon:azure_flute'},'M':{'item':'pixelmon:meteorite'},'P':{'type':NS+':master_ball'}},'result':{'item':NS+':eternatus_meteorite','count':1}})
    for lang in ['en_us','zh_cn']:
        p=output/f'assets/{NS}/lang/{lang}.json';x=json.loads(p.read_text('utf-8'));zh=lang=='zh_cn'
        x['itemGroup.pixelmon_backport']='重铸内容回迁' if zh else 'Pixelmon Content Backport'
        strings={
            'activated':('%s 太晶化为%s属性！','%s Terastallized into the %s type!'),
            'button':('太晶化','Terastallize'),'selected':('已选择太晶化','Tera selected'),'active':('已太晶化','Terastallized'),
            'settings':('太晶属性','Tera Types'),'current':('当前太晶属性：%s','Current Tera type: %s'),
            'orb.charged':('已充能：每场战斗可使用一次','Charged: usable once per battle'),
            'orb.spent':('未充能：使用治疗机充能','Empty: use a healer to recharge'),
            'orb.help':('背包中携带可在对战中太晶化；右键查看和更改属性','Carry in your inventory to Terastallize; use to inspect types'),
            'shard.help':('更改属性需要太晶珠和 50 枚对应碎片','Changing type requires a Tera Orb and 50 matching shards'),
            'recharged':('太晶珠已重新充能','Tera Orb recharged'),
            'ui.subtitle':('选择宝可梦与目标属性，再确认更改', 'Select a Pokemon and type, then confirm'),
            'ui.party':('我的队伍', 'My party'),
            'ui.types':('目标属性 · 碎片数量', 'Target type / shards'),
            'ui.charged':('太晶珠 · 已充能', 'Tera Orb / charged'),
            'ui.spent':('太晶珠 · 待充能', 'Tera Orb / recharge'),
            'ui.confirm':('确认更改为%s', 'Change to %s'),
            'ui.pending':('正在同步…', 'Syncing...'),
            'ui.empty':('队伍中没有可设置的宝可梦', 'No eligible Pokemon in your party'),
            'ui.ogerpon':('厄诡椪的太晶属性由携带面具决定', 'Ogerpon: Tera type is determined by its mask'),
            'ui.no_orb':('需要在背包中携带太晶珠', 'Carry a Tera Orb in your inventory'),
            'ui.same':('当前已经是%s属性，选择其他属性以更改', 'Already %s; select another type'),
            'ui.shortage':('还缺少 %s 太晶碎片 ×%s', 'Need %s Tera Shards x%s more'),
            'ui.cost':('确认后消耗 %s 太晶碎片 ×50', 'Consumes 50 %s Tera Shards on confirmation'),
            'ui.creative':('创造模式：更改为%s不消耗碎片', 'Creative: change to %s without consuming shards'),
            'ui.free':('免消耗', 'Free'),
            'ui.current':('当前太晶属性：%s', 'Current Tera type: %s'),
            'ui.success':('更改成功', 'Type changed'),
            'ui.rejected':('更改未成功，请检查道具和队伍状态', 'Change rejected; check your items and party'),
            'ui.terapagos':('太乐巴戈斯固定为星晶属性', 'Terapagos: locked to Stellar'),
            }
        for k,pair in strings.items():x['pixelmon.backport.tera.'+k]=pair[0 if zh else 1]
        x['pixelmon.backport.groom.changed']='%s 的造型已调整为%s。' if zh else '%s was groomed into the %s trim.'
        for name,label in zip(['natural','dandy','debutante','heart','matron','star'],['自然','绅士','淑女','心形','贵妇','星形']):
            x['pixelmon.backport.groom.'+name]=label if zh else name.title()
        for i,name in enumerate(NAMES):
            x['pixelmon.backport.tera.type.'+name]=CHINESE[i] if zh else name.title()
            x[f'item.{NS}.{name}_tera_shard']=CHINESE[i]+'太晶碎片' if zh else name.title()+' Tera Shard'
        dump(p,x)
    from extend_exploration_crafting import apply as exploration_crafting
    exploration_crafting(output)
    dump(ROOT/'.cache/gameplay-conversion.json',{'spawns':sets,'biomeSubstitutions':mappings,'legacySurvivalOrbRecipe':True})
    print('Generated',len(sets),'spawn sets,',len(NAMES),'shards, starter additions and Tera translations')
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--old',required=True,type=pathlib.Path);p.add_argument('--upstream',required=True,type=pathlib.Path);p.add_argument('--output',default=ROOT/'resources',type=pathlib.Path);a=p.parse_args();run(a.old,a.upstream,a.output)
