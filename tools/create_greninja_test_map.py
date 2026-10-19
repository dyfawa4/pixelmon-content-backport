"""Only clean metadata is exported. The optional helper prepares its named world."""
from pathlib import Path
import copy
import nbtlib as n
root=Path(__file__).resolve().parents[1]
world=copy.deepcopy(n.load(root/'test-maps/tera-visual-saves-v145/backport-tera-visuals-145/level.dat'))
data=world['Data'];data.pop('Player',None)
data['LevelName']=n.String('轻快之旅 1.6.0 忍蛙与配置测试')
data['SpawnX']=n.Int(0);data['SpawnY']=n.Int(65);data['SpawnZ']=n.Int(8)
data['WorldGenSettings']['seed']=n.Long(16020261018)
dest=root/'test-maps/greninja-saves-v160/backport-greninja-160/level.dat'
dest.parent.mkdir(parents=True,exist_ok=True)
world.save(dest,gzipped=True)
print('Prepared clean Greninja world metadata')
