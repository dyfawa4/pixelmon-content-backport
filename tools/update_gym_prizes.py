"""Change only the marked leaders' cash, preserving accepted v9 geometry/NPC data."""
import copy,collections
import nbtlib
from extend_gym_cities import ROOT,read_nbt,save_nbt

def main():
    counts=collections.Counter()
    for namespace in ('pixelmon','pixelmon_backport'):
        for path in (ROOT/f'resources/data/{namespace}/structures/gyms').rglob('*.nbt'):
            data=read_nbt(path.read_bytes());before=copy.deepcopy(data);changed=False
            for index,entry in enumerate(data.get('entities',[])):
                npc=entry.get('nbt',{});marker=npc.get('ForgeData',{})
                if str(marker.get('BackportGymRole',''))!='leader':continue
                assert str(npc.get('id',''))=='pixelmon:npc_trainer',path
                npc['WinMoney']=nbtlib.Int(200000)
                before['entities'][index]['nbt']['WinMoney']=nbtlib.Int(200000)
                counts[str(marker['BackportGymType'])]+=1;changed=True
            # Both copies receive only the allowed field; equality proves that
            # palettes, blocks, doors, roofs, services and badge rewards survived.
            assert data==before,(path,'unexpected edit')
            if changed:save_nbt(path,data)
    assert len(counts)==18 and all(n==6 for n in counts.values()),counts
    print('PASS only 108 real leader cash fields changed across all 18 themes:',dict(sorted(counts.items())))

if __name__=='__main__':main()
