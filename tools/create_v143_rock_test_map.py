"""Create one new empty inspection world; never read or replace player saves."""
import copy,pathlib,nbtlib as n
ROOT=pathlib.Path(__file__).resolve().parents[1]
def main():
    if (ROOT/'test-maps/gym-saves-v143/backport-gym-v143-rock-vanilla/APPROVED_LAYOUT.json').exists():
        raise SystemExit('Approved city snapshot exists; refusing to replace it with a blank world.')
    source=ROOT/'test-maps/gym-saves-v14/backport-gym-v14-rock/level.dat'
    world=copy.deepcopy(n.load(source));data=world['Data'];data.pop('Player',None)
    data['LevelName']=n.String('回迁道馆 1.4.3 岩石城原版神殿验收 [rock]')
    data['WorldGenSettings']['seed']=n.Long(143143)
    target=ROOT/'test-maps/gym-saves-v143/backport-gym-v143-rock-vanilla/level.dat'
    target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True);print(target)
if __name__=='__main__':main()
