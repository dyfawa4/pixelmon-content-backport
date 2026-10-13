"""Independent initial manual test world, never copies player data or live chunks."""
import copy
from pathlib import Path
import nbtlib as n

ROOT=Path(__file__).resolve().parents[1]

def main():
    world=copy.deepcopy(n.load(ROOT/'test-maps/saves/backport-qa-shop/level.dat'))
    data=world['Data'];data.pop('Player',None)
    data['LevelName']=n.String('回迁智皮卡 1.4.0 手动验收');data['GameType']=n.Int(0);data['allowCommands']=n.Byte(1)
    for key,value in [('SpawnX',0),('SpawnY',65),('SpawnZ',9)]:data[key]=n.Int(value)
    data['Time']=n.Long(6000);data['DayTime']=n.Long(6000);data['raining']=n.Byte(0);data['thundering']=n.Byte(0)
    generation=data['WorldGenSettings'];generation['seed']=n.Long(1401026);generation['generate_features']=n.Byte(0);generation['bonus_chest']=n.Byte(0)
    generator=generation['dimensions']['minecraft:overworld']['generator'];generator.clear()
    generator.update({'type':n.String('minecraft:flat'),'settings':n.Compound({
        'biome':n.String('minecraft:plains'),
        'layers':n.List[n.Compound]([n.Compound({'block':n.String('minecraft:'+block),'height':n.Int(height)}) for block,height in [('bedrock',1),('stone',59),('dirt',3),('grass_block',1)]]),
        'lakes':n.Byte(0),'features':n.Byte(0),'structures':n.Compound({'structures':n.Compound()})})})
    rules=data.setdefault('GameRules',n.Compound())
    for key,value in [('doDaylightCycle','false'),('doWeatherCycle','false'),('doFireTick','false'),('randomTickSpeed','0'),('doMobSpawning','false'),('keepInventory','true')]:rules[key]=n.String(value)
    target=ROOT/'test-maps/cap-saves-v14/backport-cap-v14/level.dat'
    if target.exists():
        existing=n.load(target)
        if existing['Data']['LevelName']!=data['LevelName']:raise ValueError('Unexpected existing world; refusing overwrite')
    target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True)
    print('Prepared independent initial world:',target)

if __name__=='__main__':main()
