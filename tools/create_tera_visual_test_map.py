"""Clean manual Tera world metadata only, without copying any played chunks."""
import copy
from pathlib import Path
import nbtlib as n
ROOT = Path(__file__).resolve().parents[1]


def main():
    world=copy.deepcopy(n.load(ROOT/'test-maps/npc-quest-saves-v145/backport-npc-quests-145/level.dat'))
    for key in ('fml','forge'): world.pop(key,None)
    data=world['Data'];data.pop('Player',None)
    data['LevelName']=n.String('回迁太晶外观 1.4.5 手动验收')
    data['GameType']=n.Int(1);data['SpawnX']=n.Int(0);data['SpawnY']=n.Int(65);data['SpawnZ']=n.Int(33)
    data['WorldGenSettings']['seed']=n.Long(1452026101702)
    target=ROOT/'test-maps/tera-visual-saves-v145/backport-tera-visuals-145/level.dat'
    if target.exists() and n.load(target)['Data']['LevelName']!=data['LevelName']: raise ValueError('Unexpected existing target world')
    target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True)
    print('Prepared clean Tera manual world:',target)


if __name__=='__main__':main()
