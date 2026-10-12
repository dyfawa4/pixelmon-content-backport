"""Fourth-round layout regressions using final templates; no game initialization."""
import argparse,copy,json,re,zipfile,collections
from extend_gym_cities import ROOT,RES,THEMES,NATIVE_BADGES,read_nbt
from city_lighting import Voxels,walk_air,DIRECTIONS
from verify_npc_dialogue import audit_embedded

def audit_main(data,theme):
 v=Voxels(data);cx,cz=v.w//2,v.d//2
 doors=[p for p in v.vox if v.name(p).endswith('_door') and not 'trapdoor' in v.name(p) and v.props(p).get('half')=='lower']
 assert len(doors)==1,(theme,'duplicate main facade entrance',doors)
 x,y,z=map(int,data['BackportLeaderStage']);front=str(data['BackportLeaderFront'])
 fx,fz={'east':(1,0),'west':(-1,0),'north':(0,-1),'south':(0,1)}[front]
 leaders=[p for p,b in v.vox.items() if str(b.get('nbt',{}).get('target',''))=='pixelmon:gym_leader']
 assert leaders==[(x,y,z)],(theme,'leader outside marked axial challenge end',leaders)
 assert (z==cz if fx else x==cx) and 8<=(cx-x)*fx+(cz-z)*fz<=13,(theme,'leader off axis/at entrance')
 for xx in range(x-1,x+2):
  for zz in range(z-1,z+2):assert all(walk_air(v.name((xx,y+j,zz))) for j in [1,2]),(theme,'leader surrounded by equipment',xx,zz)
 for step in range(abs(x-cx)+abs(z-cz)+1):
  assert all(walk_air(v.name((cx-fx*step,y+j,cz-fz*step))) for j in [1,2]),(theme,'leader approach blocked',step)
 assert v.name((x-fx*3,y+3,z-fz*3))=='minecraft:gold_block',(theme,'leader backdrop missing')
 gate=list(map(int,data['BackportGymPortals'][0]));gx,gy,gz=gate
 for at in range(min(gx,x),max(gx,x)+1) if fx else range(min(gz,z),max(gz,z)+1):
  px,pz=(at,cz) if fx else (cx,at)
  assert all(walk_air(v.name((px,y+j,pz))) for j in [1,2]),(theme,'entrance-to-leader sight line blocked',px,pz)
 for p,b in v.vox.items():
  target=str(b.get('nbt',{}).get('target',''))
  if target.startswith('pixelmon:trainer_'):assert abs(p[0]-x)+abs(p[2]-z)>3,(theme,'other boss occupies leader stage')
 for e in data.get('entities',[]):
  px,py,pz=map(float,e['pos']);assert abs(px-.5-x)+abs(pz-.5-z)>3,(theme,'retained entity occupies leader stage')
 if theme=='poison':
  equipment=['minecraft:barrel','minecraft:blast_furnace','pixelmon:anvil']
  for p in v.vox:
   if p[1] in [1,2] and abs(p[0]-cx)<=7 and abs(p[2]-cz)<=10:assert v.name(p) not in equipment,(theme,'warehouse in battle court',p)
  empty=collections.Counter()
  for block in data['blocks']:
   name=v.name(tuple(map(int,block['pos'])));tag=block.get('nbt',{})
   if name in equipment and not tag.get('Items') and not tag.get('RecipesUsed') and not tag.get('LootTable'):empty[name]+=1
  assert empty['minecraft:barrel']==6 and empty['minecraft:blast_furnace']==2,('unused fixture count',empty)
 return {'single_main_door':list(doors[0]),'leader_stage':[x,y,z],'leader_faces_entrance':front,'central_approach_clear':True}

def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);a=p.parse_args();rows={}
 for theme in THEMES:
  data=read_nbt((RES/f'data/pixelmon_backport/structures/gyms/{theme}/gym_{theme}.nbt').read_bytes());rows[theme]=audit_main(data,theme)
  # Moving the real leader while retaining metadata must fail independently.
  broken=copy.deepcopy(data)
  joint=next(b for b in broken['blocks'] if str(b.get('nbt',{}).get('target',''))=='pixelmon:gym_leader')
  joint['pos'][0]=__import__('nbtlib').Int(3)
  try:audit_main(broken,theme)
  except AssertionError:pass
  else:raise AssertionError(('leader-in-corner negative fixture accepted',theme))
  duplicate=copy.deepcopy(data);v=Voxels(duplicate);gx,gy,gz=map(int,duplicate['BackportGymPortals'][0]);facing=str(v.props((gx,gy,gz))['facing']);dx,_,dz=DIRECTIONS[facing]
  for half,offset in [('lower',0),('upper',1)]:v.put((gx-dz*3,gy+offset,gz+dx*3),'dark_oak_door',{'half':half,'facing':facing,'hinge':'left','open':'false','powered':'false'})
  v.finish()
  try:audit_main(duplicate,theme)
  except AssertionError:pass
  else:raise AssertionError(('old entrance reintroduced negative fixture accepted',theme))
 # An English custom dialogue regression must fail even when stock zh_cn exists.
 poison=read_nbt((RES/'data/pixelmon_backport/structures/gyms/poison/gym_poison.nbt').read_bytes())
 e=next(e['nbt'] for e in poison['entities'] if str(e['nbt'].get('id'))=='pixelmon:npc_chatting');e['chat1']=__import__('nbtlib').String('It is a Blast Furnace!')
 try:audit_embedded(poison,'negative Barley fixture')
 except AssertionError:pass
 else:raise AssertionError('embedded English negative fixture accepted')
 native={}
 with zipfile.ZipFile(a.pixelmon) as jar:
  for theme in NATIVE_BADGES:
   name=f'data/pixelmon/structures/gyms/{theme}/gym_{theme}.nbt';old=Voxels(read_nbt(jar.read(name)));new=Voxels(read_nbt((RES/name).read_bytes()))
   before=[p for p,b in old.vox.items() if str(b.get('nbt',{}).get('target'))=='pixelmon:gym_leader']
   after=[p for p,b in new.vox.items() if str(b.get('nbt',{}).get('target'))=='pixelmon:gym_leader']
   assert before==after,(theme,'native leader platform moved')
   original_doors={p for p in old.vox if old.name(p).endswith('_door') and not 'trapdoor' in old.name(p) and old.props(p).get('half')=='lower'}
   gates=list(new.data.get('BackportGymPortals',[]))
   if original_doors:assert all(tuple(map(int,g)) in original_doors for g in gates),(theme,'new entrance added next to existing native doors')
   else:
    planes=[(str(new.props(tuple(map(int,g))).get('facing')),int(g[1]),int(g[0]) if str(new.props(tuple(map(int,g))).get('facing')) in ['east','west'] else int(g[2])) for g in gates]
    assert len(planes)==len(set(planes)),(theme,'multiple invented entrances in same facade',planes)
   native[theme]={'leader_socket_positions_preserved':True,'existing_door_crossings_preserved':True}
 result={'scope':'fourth-round final-resource and negative-fixture checks; assembled appearance remains manual','new_main_gyms':rows,'native_main_gyms':native,'negative_fixtures':['leader moved into corner despite unchanged stage metadata','old entrance reintroduced beside new door','English embedded Barley dialogue despite stock localization']}
 (ROOT/'docs/GYM_LAYOUT_REVIEW_1.3.0.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('PASS 8 single entrances/axial leader platforms, 10 original leader layouts and native door crossings; poison court clear; displaced-leader and embedded-English negative fixtures rejected.')
if __name__=='__main__':main()
