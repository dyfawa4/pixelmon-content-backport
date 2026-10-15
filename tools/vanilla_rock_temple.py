"""Install the recorded vanilla pyramid, with a rigid underground child piece.

Both pieces keep normal town ground at y=0; underground blocks never expand the
town's above-ground collision box or pull the terrain blending plane downward.
"""
import copy,io,gzip,json,pathlib
import nbtlib as n

ROOT=pathlib.Path(__file__).resolve().parents[1]
CANONICAL=ROOT/'resources/backport/structures/vanilla_desert_pyramid.nbt'
STRUCTURES=ROOT/'resources/data/pixelmon_backport/structures/gyms/rock'
POOL='pixelmon_backport:gyms/rock/vanilla_pyramid_basement'
SURFACE='pixelmon_backport:desert_pyramid_surface'
BASEMENT='pixelmon_backport:desert_pyramid_basement'

def save(path,data):
    stream=io.BytesIO();data.write(stream);path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(stream.getvalue(),mtime=0))

def joint(name,target,pool,final):
    return n.Compound({k:n.String(v) for k,v in {'id':'minecraft:jigsaw','joint':'aligned','name':name,'target':target,'pool':pool,'final_state':final}.items()})

def install_into_builder(b,g=0):
    native=n.load(CANONICAL);base=int(native['BackportVanillaBaseY']);oldw,oldd=b.w,b.d
    # Seven blocks of forecourt/setback on each side: the former narrow center
    # squeezed a full vanilla pyramid between the surrounding city buildings.
    b.w=max(35,b.w);b.d=max(35,b.d);x0=(b.w-21)//2;z0=(b.d-21)//2;x1=x0+20;z1=z0+20
    assert z0>=3,(b.w,b.d)
    protected=[copy.deepcopy(block) for block in b.blocks.values() if 'nbt' in block and str(block['nbt'].get('name',''))!=SURFACE]
    moves=[];occupied=set()
    for block in protected:
        before=tuple(map(int,block['pos']));x,y,z=before
        if b.w!=oldw and x==oldw-1:x=b.w-1
        if b.d!=oldd and z==oldd-1:z=b.d-1
        if x0<=x<=x1 and z0<=z<=z1:
            # Native interior decor sockets become perimeter sockets, with the
            # same NBT/name/pool/facing and elevation. Do not put NPCs in walls.
            choices=[(x0-1,y,z),(x1+1,y,z),(x,y,z0-1),(x,y,z1+1)]
            choices=sorted(choices,key=lambda p:(abs(p[0]-x)+abs(p[2]-z),p))
            x,y,z=next(p for p in choices if p not in occupied and 0<=p[0]<b.w and 0<=p[2]<b.d)
        after=(x,y,z);assert after not in occupied;occupied.add(after)
        if before!=after:
            b.blocks.pop(before,None);b.put(*before,b.floor if before[1]==g else 'air');moves.append({'from':list(before),'to':list(after)})
        block['pos']=n.List[n.Int](after)
    # Remove the old landmark's yellow sandstone outline, including the offset
    # rectangle left behind by the previous width expansion. Restore tagged
    # service/road sockets after laying the continuous city paving below.
    b.box(0,g,0,b.w-1,g,b.d-1,b.floor)
    if 'BackportVanillaPyramidOrigin' in b.data:
        px,py,pz=map(int,b.data['BackportVanillaPyramidOrigin'])
        b.box(px,py+1,pz,px+20,max(b.oldh-1,py+10),pz+20,'air')
    b.box(x0,g+1,z0,x1,max(b.oldh-1,g+10),z1,'air')
    for block in native['blocks']:
        x,y,z=map(int,block['pos']);y-=base
        if y<0:continue
        state=native['palette'][int(block['state'])]
        b.put(x0+x,g+y,z0+z,str(state['Name']),{str(k):str(v) for k,v in state.get('Properties',{}).items()},block.get('nbt'))
    b.put(x0+10,g,z0+10,'minecraft:jigsaw',{'orientation':'down_north'},joint(SURFACE,BASEMENT,POOL,'minecraft:blue_terracotta'))
    for block in protected:b.blocks[tuple(map(int,block['pos']))]=block
    b.data['BackportVanillaPyramid']=n.String('minecraft:desert_pyramid@1.16.5')
    b.data['BackportVanillaPyramidOrigin']=n.IntArray([x0,g,z0])
    if moves or 'BackportVanillaPyramidSocketMoves' not in b.data:
        b.data['BackportVanillaPyramidSocketMoves']=n.String(json.dumps(moves,separators=(',',':')))
    return x0,x1,z0,z1

def install(data):
    from city_architecture import Builder
    b=Builder(data,'rock');b.palette=copy.deepcopy(list(data['palette']))
    b.ids={(str(state['Name']),tuple(sorted((str(k),str(v)) for k,v in state.get('Properties',{}).items()))):i for i,state in enumerate(b.palette)}
    b.blocks={tuple(map(int,block['pos'])):copy.deepcopy(block) for block in data['blocks']}
    install_into_builder(b)
    data['palette']=n.List[n.Compound](b.palette)
    data['blocks']=n.List[n.Compound](b.blocks[p] for p in sorted(b.blocks,key=lambda p:(p[1],p[0],p[2])))
    data['size']=n.List[n.Int]([b.w,max(b.oldh,max(p[1] for p in b.blocks)+1),b.d])
    return data

def basement():
    from city_architecture import Builder
    native=n.load(CANONICAL);base=int(native['BackportVanillaBaseY'])
    data=n.File({'DataVersion':n.Int(2586),'size':n.List[n.Int]([21,base,21]),'entities':n.List[n.Compound]()})
    b=Builder(data,'rock')
    for block in native['blocks']:
        x,y,z=map(int,block['pos'])
        if y>=base:continue
        state=native['palette'][int(block['state'])]
        b.put(x,y,z,str(state['Name']),{str(k):str(v) for k,v in state.get('Properties',{}).items()},block.get('nbt'))
    b.put(10,base-1,10,'minecraft:jigsaw',{'orientation':'up_north'},joint(BASEMENT,SURFACE,'minecraft:empty','minecraft:air'))
    data['palette']=n.List[n.Compound](b.palette);data['blocks']=n.List[n.Compound](b.blocks[p] for p in sorted(b.blocks,key=lambda p:(p[1],p[0],p[2])))
    save(STRUCTURES/'vanilla_pyramid_basement.nbt',data)
    pool={'name':POOL,'fallback':'minecraft:empty','elements':[{'weight':1,'element':{
        'location':POOL,'processors':'minecraft:empty','projection':'rigid','element_type':'pixelmon:moving',
        # -base + (base+1) == the surface piece's ground level delta of 1.
        'ground_level_delta':base+1,'y_hack':0,'keep_liquids':False}}]}
    target=ROOT/'resources/data/pixelmon_backport/worldgen/template_pool/gyms/rock/vanilla_pyramid_basement.json'
    target.write_text(json.dumps(pool,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
