"""Replace only the rock city's two central landmarks, leaving v9 city assets intact."""
import copy,json
import nbtlib as n
from extend_gym_cities import ROOT,read_nbt,save_nbt
from city_architecture import Builder,rock_temple


def semantic_blocks(data):
    return {tuple(map(int,b['pos'])):(copy.deepcopy(data['palette'][int(b['state'])]),copy.deepcopy(b.get('nbt'))) for b in data['blocks']}

def update(path):
    data=read_nbt(path.read_bytes());original=copy.deepcopy(data);before=semantic_blocks(data)
    b=Builder(data,'rock');b.palette=copy.deepcopy(list(data['palette']))
    b.ids={(str(state['Name']),tuple(sorted((str(k),str(v)) for k,v in state.get('Properties',{}).items()))):i for i,state in enumerate(b.palette)}
    b.blocks={tuple(map(int,block['pos'])):copy.deepcopy(block) for block in data['blocks']}
    x0,x1,z0,z1=rock_temple(b)
    data['palette']=n.List[n.Compound](b.palette);data['blocks']=n.List[n.Compound](b.blocks[p] for p in sorted(b.blocks,key=lambda p:(p[1],p[0],p[2])))
    data['size'][1]=n.Int(max(int(data['size'][1]),max(p[1] for p in b.blocks)+1))
    after=semantic_blocks(data)
    for p in set(before)|set(after):
        if before.get(p)!=after.get(p):assert x0<=p[0]<=x1 and z0<=p[2]<=z1,('changed approved outside geometry',p)
    assert data.get('entities')==original.get('entities'),'NPCs moved'
    for p,(state,tag) in before.items():
        if tag is not None:assert after[p]==(state,tag),('service/connector changed',p)
    # A central three-wide route, sealed roof and grounded lighting.
    for z in range(z0,z0+6):
        for x in range(b.w//2-1,b.w//2+2):
            for y in (1,2,3):assert after[x,y,z][0]['Name']=='minecraft:air'
    for x in range(x0+1,x1):
        for z in range(z0+5,z1):assert after[x,6,z][0]['Name']=='minecraft:cut_sandstone'
    save_nbt(path,data)
    return {'template':path.name,'size':list(map(int,data['size'])),'changed_area':[x0,x1,z0,z1],'preserved_connectors_services':sum(tag is not None for _,tag in before.values())}

def main():
    base=ROOT/'resources/data/pixelmon_backport/structures/gyms/rock'
    result=[update(base/f'gym_rock_tc_{variant}.nbt') for variant in ('a','b')]
    print(json.dumps({'passed':True,'landmarks':result},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
