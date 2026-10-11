"""Create clean, independent inspection worlds; no existing player save is read."""
import copy, pathlib
import nbtlib

ROOT = pathlib.Path(__file__).resolve().parents[1]
TYPES = ['normal','fire','water','electric','grass','ice','fighting','poison','ground','flying','psychic','bug','rock','ghost','dragon','dark','steel','fairy']
LABELS = ['一般','火','水','电','草','冰','格斗','毒','地面','飞行','超能力','虫','岩石','幽灵','龙','恶','钢','妖精']
NEW_TYPES={'normal','fighting','poison','flying','psychic','bug','rock','dark'}

def main():
    base = nbtlib.load(ROOT/'test-maps/saves/backport-qa-shop/level.dat')
    for index, (theme, label) in enumerate(zip(TYPES, LABELS), 1):
        world = copy.deepcopy(base)
        data = world['Data']
        data.pop('Player', None)
        revision='细节修复 ' if theme in NEW_TYPES else ''
        data['LevelName'] = nbtlib.String(f'回迁道馆 {revision}{index:02d} {label}属性 [{theme}]')
        data['GameType'] = nbtlib.Int(1)
        data['allowCommands'] = nbtlib.Byte(1)
        for key,value in [('SpawnX',0),('SpawnY',150),('SpawnZ',120)]:data[key]=nbtlib.Int(value)
        data['Time'] = nbtlib.Long(1000);data['DayTime']=nbtlib.Long(1000)
        gen = data['WorldGenSettings'];gen['seed']=nbtlib.Long(120000+index)
        gen['generate_features']=nbtlib.Byte(0);gen['bonus_chest']=nbtlib.Byte(0)
        generator = gen['dimensions']['minecraft:overworld']['generator']
        layers = [('bedrock',1),('stone',59),('dirt',3),('grass_block',1)]
        if theme=='water':layers=[('bedrock',1),('stone',47),('dirt',3),('water',13)]
        settings=nbtlib.Compound({
            'biome':nbtlib.String('minecraft:deep_ocean' if theme=='water' else 'minecraft:plains'),
            'layers':nbtlib.List[nbtlib.Compound]([nbtlib.Compound({'block':nbtlib.String('minecraft:'+block),'height':nbtlib.Int(height)}) for block,height in layers]),
            'lakes':nbtlib.Byte(0),'features':nbtlib.Byte(0),
            'structures':nbtlib.Compound({'structures':nbtlib.Compound()})})
        generator.clear();generator.update({'type':nbtlib.String('minecraft:flat'),'settings':settings})
        rules=data.setdefault('GameRules',nbtlib.Compound())
        for key,value in [('doDaylightCycle','false'),('doWeatherCycle','false'),('doFireTick','false'),('randomTickSpeed','0'),('doMobSpawning','false')]:rules[key]=nbtlib.String(value)
        folder=f'backport-gym-v5-{theme}' if theme in NEW_TYPES else f'backport-gym-{theme}'
        target=ROOT/f'test-maps/gym-saves/{folder}/level.dat'
        target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True)
    data['LevelName']=nbtlib.String('回迁徽章 18 属性徽章展示')
    for key,value in [('SpawnX',0),('SpawnY',65),('SpawnZ',0)]:data[key]=nbtlib.Int(value)
    target=ROOT/'test-maps/gym-saves/backport-gym-badges/level.dat'
    target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True)
    for suffix,title in [('riding','回迁专项 08 新宝可梦骑乘测试'),('achievements','回迁专项 09 传说成就与合体测试')]:
        data['LevelName']=nbtlib.String(title)
        target=ROOT/f'test-maps/saves/backport-qa-{suffix}/level.dat'
        target.parent.mkdir(parents=True,exist_ok=True);world.save(target,gzipped=True)
    print('Created 18 independent gym inspection worlds, one badge gallery and two feature test worlds.')

if __name__=='__main__':main()
