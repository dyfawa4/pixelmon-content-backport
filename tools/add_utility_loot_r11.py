"""Dedicated Poke Mart chest tables retain their original loot plus two independent5% rolls."""
from pathlib import Path
from zipfile import ZipFile
import gzip,io,json,sys
ROOT=Path(__file__).resolve().parents[1]
sys.path.insert(0,str(ROOT/'.cache/python-nbt'))
import nbtlib
RES=ROOT/'resources'
ITEMS=['pixelmon_backport:moving_agreement','pixelmon_backport:gender_potion']
def write(path,data):
    path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf8')
report=[];wrappers={}
with ZipFile(ROOT/'.cache/map-search-full-1547/mods/Pixelmon-legacy.jar') as jar:
    native=set(n for n in jar.namelist() if n.endswith(('_pokemart.nbt','_mart.nbt')))
    local=set(p.relative_to(RES).as_posix() for p in RES.rglob('*.nbt') if p.name.endswith(('_pokemart.nbt','_mart.nbt')))
    for name in sorted(native|local):
        path=RES/name;blob=path.read_bytes() if path.exists() else jar.read(name);data=nbtlib.File.parse(io.BytesIO(gzip.decompress(blob)));count=0
        for block in data['blocks']:
            state=str(data['palette'][int(block['state'])]['Name']);tag=block.get('nbt',{})
            if state not in {'minecraft:chest','minecraft:trapped_chest'} or 'LootTable' not in tag:continue
            original=str(tag['LootTable'])
            if original.startswith('pixelmon_backport:chests/pokemart/'):continue
            namespace,table=original.split(':',1);suffix=namespace+'/'+table;key='pixelmon_backport:chests/pokemart/'+suffix
            pools=[{'name':'original_shop_loot','rolls':1,'entries':[{'type':'minecraft:loot_table','name':original}]}]
            for item in ITEMS:pools.append({'name':item.split(':')[1]+'_five_percent','rolls':1,'conditions':[{'condition':'minecraft:random_chance','chance':0.05}],'entries':[{'type':'minecraft:item','name':item}]})
            wrappers[key]={'type':'minecraft:chest','pools':pools};tag['LootTable']=nbtlib.String(key);count+=1
        if count:
            stream=io.BytesIO();data.write(stream);path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(stream.getvalue(),mtime=0));report.append({'template':name,'chests_changed':count})
    for key,table in wrappers.items():write(RES/'data/pixelmon_backport/loot_tables'/Path(key.split(':',1)[1]+'.json'),table)
    path=RES/'data/pixelmon/loot_tables/special/common_pokestop.json';stop=json.loads(path.read_text('utf-8-sig')) if path.exists() else json.loads(jar.read('data/pixelmon/loot_tables/special/common_pokestop.json'))
    pool=next(p for p in stop['pools'] if p.get('name')=='medicine');present={e.get('name') for e in pool['entries']};weight=next((e.get('weight',1) for e in pool['entries'] if e.get('weight',1)>0),1)
    for item in ITEMS:
        if item not in present:pool['entries'].append({'type':'minecraft:item','name':item,'weight':weight})
    write(path,stop)
write(ROOT/'.cache/daycare-r11/shop-template-changes.json',{'templates':report,'wrapper_tables':sorted(wrappers),'new_items':ITEMS,'independent_shop_chance_each':0.05,'common_pokestop_pool':'medicine'})
print('Updated',len(report),'shop templates;',sum(r['chests_changed'] for r in report),'chests;',len(wrappers),'dedicated tables; common PokeStop medicine pool weight',weight)
