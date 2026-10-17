"""Create only clean level metadata; played chunks are never packaged."""
from pathlib import Path
import copy
import nbtlib as n
root=Path(__file__).resolve().parents[1]
world=copy.deepcopy(n.load(root/'test-maps/tera-visual-saves-v145/backport-tera-visuals-145/level.dat'))
data=world['Data'];data.pop('Player',None)
data['LevelName']=n.String('轻快之旅 1.5.0 乘骑测试')
data['SpawnX']=n.Int(0);data['SpawnY']=n.Int(65);data['SpawnZ']=n.Int(22)
data['WorldGenSettings']['seed']=n.Long(1502026101705)
dest=root/'test-maps/riding-saves-v150/backport-riding-150/level.dat'
dest.parent.mkdir(parents=True,exist_ok=True)
if dest.exists() and n.load(dest)['Data']['LevelName']!=data['LevelName']:raise ValueError('Unexpected world target')
world.save(dest,gzipped=True)
print('Prepared clean riding test world')
