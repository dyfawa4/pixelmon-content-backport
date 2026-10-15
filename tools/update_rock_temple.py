"""Replace only the rock city's two central landmarks, leaving v9 city assets intact."""
import copy,json,pathlib,hashlib
from extend_gym_cities import ROOT,read_nbt,save_nbt
from vanilla_rock_temple import install,basement,SURFACE


def semantic_blocks(data):
    return {tuple(map(int,b['pos'])):(copy.deepcopy(data['palette'][int(b['state'])]),copy.deepcopy(b.get('nbt'))) for b in data['blocks']}

def update(path):
    data=read_nbt(path.read_bytes());original=copy.deepcopy(data);before=semantic_blocks(data)
    data=install(data)
    after=semantic_blocks(data)
    assert data.get('entities')==original.get('entities'),'NPCs moved'
    old_tags=sorted(str(tag) for _,tag in before.values() if tag is not None and str(tag.get('name',''))!=SURFACE)
    new_tags=sorted(str(tag) for _,tag in after.values() if tag is not None and str(tag.get('name',''))!=SURFACE)
    assert old_tags==new_tags,'Existing service/connector NBT changed'
    save_nbt(path,data)
    return {'template':path.name,'size':list(map(int,data['size'])),'origin':list(map(int,data['BackportVanillaPyramidOrigin'])),'socketMoves':json.loads(str(data['BackportVanillaPyramidSocketMoves'])),'preserved_connectors_services':len(old_tags)}

def main():
    base=ROOT/'resources/data/pixelmon_backport/structures/gyms/rock'
    backup=ROOT/'.cache/rock-before-vanilla';backup.mkdir(exist_ok=True)
    for variant in ('a','b'):
        path=base/f'gym_rock_tc_{variant}.nbt';target=backup/path.name
        if not target.exists():target.write_bytes(path.read_bytes())
    baseline=backup/'city-structure-sha256.json'
    if not baseline.exists():baseline.write_text(json.dumps({str(p.relative_to(ROOT)):hashlib.sha256(p.read_bytes()).hexdigest() for p in (ROOT/'resources/data/pixelmon_backport/structures/gyms').rglob('*.nbt')},indent=2),encoding='utf8')
    basement()
    result=[update(base/f'gym_rock_tc_{variant}.nbt') for variant in ('a','b')]
    print(json.dumps({'passed':True,'landmarks':result},ensure_ascii=False,indent=2))

if __name__=='__main__':main()
