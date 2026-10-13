"""Audit serialized leader rewards without starting a game or battle."""
import collections,json,pathlib
from extend_gym_cities import ROOT,read_nbt,THEMES,NATIVE_BADGES

def main():
 counts=collections.Counter()
 for namespace in ['pixelmon','pixelmon_backport']:
  for path in (ROOT/f'resources/data/{namespace}/structures/gyms').rglob('*.nbt'):
   data=read_nbt(path.read_bytes())
   for entry in data.get('entities',[]):
    entity=entry.get('nbt',{});marker=entity.get('ForgeData',{})
    if str(marker.get('BackportGymRole',''))!='leader':
     assert not any(str(item.get('id','')).endswith('_badge') for item in entity.get('WinningsTag',{}).values()),(path,'non-leader awards badge')
     continue
    assert int(entity.get('WinMoney',0))==200000,(path,'leader money',entity.get('WinMoney'))
    theme=str(marker['BackportGymType']);fresh=bool(marker.get('BackportGymNew',0))
    # Some native leaders are identified by template role, rather than this
    # flag. Their native loseBattle reward path does not require the flag.
    if fresh:assert bool(entity.get('GymLeader',0)),(path,'not flagged as leader')
    expected=f'pixelmon_backport:{theme}_gym_badge' if fresh else 'pixelmon:'+NATIVE_BADGES[theme]
    items=entity.get('WinningsTag',{})
    assert any(str(item.get('id',''))==expected and int(item.get('Count',0))==1 for item in items.values()),(path,'missing correct badge',expected,items)
    if fresh:assert (ROOT/f'resources/assets/pixelmon_backport/models/item/{theme}_gym_badge.json').exists(),expected
    counts[theme]+=1
 assert set(counts)==set(THEMES)|set(NATIVE_BADGES),counts
 print('PASS serialized badge rewards for all 18 gym types:',dict(sorted(counts.items())))

if __name__=='__main__':main()
