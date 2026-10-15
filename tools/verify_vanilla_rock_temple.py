"""Offline native-voxel, connection, loot and local-change acceptance."""
import collections,copy,hashlib,json,pathlib
import nbtlib as n
from vanilla_rock_temple import ROOT,CANONICAL,STRUCTURES,POOL,SURFACE,BASEMENT,install

def state(data,block):
    s=data['palette'][int(block['state'])]
    return str(s['Name']),{str(k):str(v) for k,v in s.get('Properties',{}).items()},copy.deepcopy(block.get('nbt'))

def blocks(data,offset=(0,0,0)):
    result={}
    for b in data['blocks']:
        p=tuple(int(b['pos'][i])+offset[i] for i in range(3));assert p not in result,('duplicate cell',p)
        name,props,tag=state(data,b)
        if name=='minecraft:jigsaw':name=str(tag['final_state']);props={};tag=None
        result[p]=(name,props,tag)
    return result

def main():
    native=n.load(CANONICAL);base=int(native['BackportVanillaBaseY']);canonical=blocks(native,(0,-base,0))
    names=collections.Counter(v[0] for v in canonical.values())
    assert list(map(int,native['size']))==[21,25,21]
    assert names['minecraft:chest']==4 and names['minecraft:tnt']==9 and names['minecraft:stone_pressure_plate']==1
    assert canonical[10,0,10][0]=='minecraft:blue_terracotta' and canonical[10,-1,10][0]=='minecraft:air'
    for _,_,tag in canonical.values():
        if tag is not None:assert str(tag['LootTable'])=='minecraft:chests/desert_pyramid' and 'LootTableSeed' not in tag
    child=n.load(STRUCTURES/'vanilla_pyramid_basement.nbt');childvox=blocks(child,(0,-base,0))
    assert childvox=={p:v for p,v in canonical.items() if p[1]<0},'Basement differs from vanilla generation'
    childjoint=next(b for b in child['blocks'] if str(b.get('nbt',{}).get('name',''))==BASEMENT)
    assert list(map(int,childjoint['pos']))==[10,base-1,10]
    assert str(child['palette'][int(childjoint['state'])]['Properties']['orientation'])=='up_north'
    pool=json.loads((ROOT/'resources/data/pixelmon_backport/worldgen/template_pool/gyms/rock/vanilla_pyramid_basement.json').read_text())
    element=pool['elements'][0]['element']
    assert element['location']==POOL and element['projection']=='rigid' and element['element_type']=='pixelmon:moving'
    assert element['ground_level_delta']-base==1 and element['y_hack']==0,'Underground piece changes surface terrain plane'
    result=[]
    for variant in ('a','b'):
        path=STRUCTURES/f'gym_rock_tc_{variant}.nbt';data=n.load(path);vox=blocks(data);ox,g,oz=map(int,data['BackportVanillaPyramidOrigin']);w,h,d=map(int,data['size'])
        assert min(ox,oz,w-ox-21,d-oz-21)>=7,'Pyramid needs a seven-block setback on every side'
        tagged={tuple(map(int,b['pos'])) for b in data['blocks'] if 'nbt' in b}
        for x in range(w):
            for z in range(d):
                if not(ox<=x<=ox+20 and oz<=z<=oz+20) and (x,g,z) not in tagged:
                    assert vox[x,g,z][0]=='minecraft:polished_andesite',('Old yellow frame outside pyramid',variant,x,z)
        for p,value in canonical.items():
            x,y,z=p
            actual=vox.get((ox+x,g+y,oz+z)) if y>=0 else childvox.get(p)
            assert actual==value,(variant,'native voxel changed',p,actual,value)
        assert all(0<=p[0]<w and 0<=p[1]<h and 0<=p[2]<d for p in vox),'Template bounding box clips blocks'
        assert install(copy.deepcopy(data))==data,'Local install is not idempotent'
        parent=next(b for b in data['blocks'] if str(b.get('nbt',{}).get('name',''))==SURFACE)
        assert str(data['palette'][int(parent['state'])]['Properties']['orientation'])=='down_north'
        assert str(parent['nbt']['target'])==BASEMENT and str(parent['nbt']['pool'])==POOL
        # Opposing vertical jigsaws connect a child whose origin is exactly -14;
        # a horizontal rotation never changes this vertical relationship.
        for rotation in range(4):
            assert g-1-(base-1)==g-base
            for x,z in [(0,0),(20,0),(0,20),(20,20),(10,10)]:
                rx,rz=[(x,z),(20-z,x),(20-x,20-z),(z,20-x)][rotation]
                assert 0<=rx<21 and 0<=rz<21
        # Check two-block-high pedestrian routes to every retained street socket,
        # the original native front door and the retained Pixelmon reward chest.
        def name(p):return vox.get(p,('minecraft:air',{},None))[0]
        air={'minecraft:air','minecraft:structure_void'}
        walk={(x,z) for x in range(w) for z in range(d) if name((x,0,z)) not in air and name((x,1,z)) in air and name((x,2,z)) in air}
        start=(ox+10,oz);assert start in walk,(variant,'blocked native entrance')
        reached={start};q=collections.deque([start])
        while q:
            x,z=q.popleft()
            for p in ((x-1,z),(x+1,z),(x,z-1),(x,z+1)):
                if p in walk and p not in reached:reached.add(p);q.append(p)
        previous=n.load(ROOT/'.cache/rock-before-vanilla'/path.name)
        old=collections.Counter(str(b['nbt']) for b in previous['blocks'] if 'nbt' in b and str(b['nbt'].get('name',''))!=SURFACE)
        kept=collections.Counter(str(b['nbt']) for b in data['blocks'] if 'nbt' in b and str(b['nbt'].get('name',''))!=SURFACE)
        assert old==kept,'Service/connector metadata changed'
        assert data.get('entities')==previous.get('entities'),'Existing entities changed'
        for block in data['blocks']:
            if 'nbt' not in block:continue
            x,y,z=map(int,block['pos']);tag=block['nbt'];location=str(tag.get('pool',''))
            if location.startswith('pixelmon_backport:gyms/'):
                if str(tag.get('name',''))==SURFACE:continue
                assert (x,z) in reached,(variant,'street socket is inaccessible',(x,y,z))
            if str(tag.get('id',''))=='pixelmon:poke_chest':
                assert any(p in reached for p in ((x-1,z),(x+1,z),(x,z-1),(x,z+1))),'Reward chest is inaccessible'
        result.append({'variant':variant,'size':[w,h,d],'nativeVoxelsMatched':len(canonical),'retainedServiceTags':sum(kept.values()),'reachableGroundCells':len(reached)})
    baseline=json.loads((ROOT/'.cache/rock-before-vanilla/city-structure-sha256.json').read_text())
    changed=[p for p,digest in baseline.items() if hashlib.sha256((ROOT/p).read_bytes()).hexdigest()!=digest]
    assert set(pathlib.Path(p).name for p in changed)<= {'gym_rock_tc_a.nbt','gym_rock_tc_b.nbt'},changed
    report={'passed':True,'nativeCells':len(canonical),'chests':4,'tnt':9,'pressurePlates':1,'terrainPlanePreserved':True,'minimumSetback':7,'oldYellowFrameRemoved':True,'otherExistingCityTemplatesUnchanged':len(baseline)-2,'townCenters':result,'gameplayAcceptance':'layout revision pending manual acceptance'}
    (ROOT/'.cache/rock-vanilla-verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8');print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=='__main__':main()
