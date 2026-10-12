"""Audit original city geometry and retained native gameplay components."""
import argparse, collections, hashlib, json, zipfile
from extend_gym_cities import THEMES, ROOT, read_nbt, strings, decorate
from city_architecture import kind,empty_lab_fixture
from verify_city_structure import structural_components,pane_errors

def name(d,b):return str(d['palette'][int(b['state'])]['Name'])
def native_entities(d):return collections.Counter(str(e['nbt'].get('id','')) for e in d.get('entities',[]) if str(e['nbt'].get('id','')).startswith('pixelmon:'))
def kept(d,b):
 s=name(d,b);tag=b.get('nbt',{})
 return s.startswith('pixelmon:') and (s.endswith(('_pc','_chest')) or any(w in s for w in ['healer','trade_machine','fossil_cleaner','fossil_machine','anvil','vending_machine'])) or bool(tag.get('Items',[])) or 'LootTable' in tag or s in ['minecraft:chest','minecraft:trapped_chest','minecraft:barrel','minecraft:furnace','minecraft:blast_furnace','minecraft:smoker','minecraft:brewing_stand']
def components(d):
 def properties(b):
  props=d['palette'][int(b['state'])].get('Properties',{}).unpack() if 'Properties' in d['palette'][int(b['state'])] else {}
  # Reorienting an intact service/container is part of the room repair.
  props.pop('facing',None)
  return props
 return collections.Counter((name(d,b),json.dumps(properties(b),sort_keys=True),json.dumps(b.get('nbt',{}).unpack() if 'nbt' in b else {},sort_keys=True,default=lambda v:v.tolist())) for b in d['blocks'] if kept(d,b))

def audit_interiors(d,theme,filename):
 category=kind(filename,theme)
 if category not in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:return {'fixtures':0,'nurses':0,'beds':0}
 raw=list(map(int,d.get('BackportInterior',[])));inside=set(zip(raw[::2],raw[1::2]))
 assert inside,(theme,filename,'missing real room mask')
 vox={tuple(map(int,b['pos'])):b for b in d['blocks']};w,_,depth=map(int,d['size'])
 def final(p):
  b=vox.get(p)
  return 'minecraft:air' if b is None else str(b.get('nbt',{}).get('final_state','minecraft:air')) if name(d,b)=='minecraft:jigsaw' else name(d,b)
 # Probe above furniture and door openings to reconstruct facade enclosure
 # independently of the serialized interior mask and the route-carving code.
 outside={(x,z) for x in range(w) for z in range(depth) if x in [0,w-1] or z in [0,depth-1]}
 outside={p for p in outside if final((p[0],4,p[1])) in ['minecraft:air','minecraft:structure_void']}
 queue=collections.deque(outside)
 while queue:
  x,z=queue.popleft()
  for p in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
   if 0<=p[0]<w and 0<=p[1]<depth and p not in outside and final((p[0],4,p[1])) in ['minecraft:air','minecraft:structure_void']:outside.add(p);queue.append(p)
 fixtures=beds=0;nurses=[];healers=[]
 directions={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}
 for (x,y,z),b in vox.items():
  s=name(d,b);props=d['palette'][int(b['state'])].get('Properties',{})
  fixture=kept(d,b) or any(t in s for t in ['_bed','bookshelf','lectern','crafting_table','cauldron','potted_'])
  if fixture:
   assert (x,z) in inside,(theme,filename,'fixture on facade or outside room',s,x,y,z)
   if category!='gym':assert (x,z) not in outside,(theme,filename,'fixture outside independently enclosed facade',s,x,y,z)
   fixtures+=1
  if s.endswith('_bed') and str(props.get('part'))=='foot':
   dx,dz=directions[str(props['facing'])];head=vox.get((x+dx,y,z+dz))
   assert head is not None and name(d,head)==s and str(d['palette'][int(head['state'])]['Properties'].get('part'))=='head',(theme,filename,'incomplete bed',x,y,z)
   beds+=1
  if 'healer' in s:healers.append((x,y,z))
  if s=='minecraft:jigsaw' and str(b.get('nbt',{}).get('pool','')).startswith(('pixelmon:npc/','pixelmon_backport:npc/','minecraft:village/')):
   assert (x,z) in inside,(theme,filename,'NPC outside actual room',x,y,z)
   if category!='gym':assert (x,z) not in outside,(theme,filename,'NPC outside independently enclosed facade',x,y,z)
   if 'nurse' in str(b['nbt'].get('pool','')):nurses.append((x,y+1,z))
 for nurse in nurses:
  assert healers and min(sum((a-b)**2 for a,b in zip(nurse,healer)) for healer in healers)<=16,(theme,filename,'nurse healer more than four blocks away',nurse,healers)
 if category=='house':assert beds>=1,(theme,filename,'missing furnished sleeping area')
 if category=='pokecenter':assert nurses and healers,(theme,filename,'missing nurse/healer service')
 return {'fixtures':fixtures,'nurses':len(nurses),'beds':beds}
def geometry(d):
 return sorted((tuple(map(int,b['pos'])),name(d,b)) for b in d['blocks'] if name(d,b) not in ['minecraft:air','minecraft:structure_void','minecraft:jigsaw'])

def audit_doors(d,theme,filename):
 vox={tuple(map(int,b['pos'])):b for b in d['blocks']};count=0
 def final(p):
  b=vox.get(p)
  return 'minecraft:air' if b is None else str(b.get('nbt',{}).get('final_state','minecraft:air')) if name(d,b)=='minecraft:jigsaw' else name(d,b)
 for (x,y,z),block in vox.items():
  if not name(d,block).endswith('_door'):continue
  props=d['palette'][int(block['state'])]['Properties'];lower=str(props['half'])=='lower'
  other=vox.get((x,y+(1 if lower else -1),z))
  assert other is not None and name(d,other)==name(d,block),(theme,filename,'missing door half',x,y,z)
  peer=d['palette'][int(other['state'])]['Properties']
  assert str(peer['half'])!=str(props['half']) and all(str(peer[k])==str(props[k]) for k in ['facing','hinge','open','powered']),(theme,filename,'mismatched door halves',x,y,z)
  if not lower:continue
  count+=1;dx,dz=(0,1) if str(props['facing']) in ['north','south'] else (1,0)
  assert final((x,y-1,z)) not in ['minecraft:air','minecraft:structure_void'],(theme,filename,'unsupported door')
  from city_lighting import walk_air
  assert all(walk_air(final((x+dx*side,y+h,z+dz*side))) for side in [-1,1] for h in [0,1]),(theme,filename,'object blocks door approach',x,y,z)
 if kind(filename,theme) in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop'] and not (kind(filename,theme)=='gym' and theme=='flying'):
  assert count>0,(theme,filename,'missing functional doorway')
 return count

def audit_routes(d,theme,filename):
 """Inspect final voxel geometry, independently of the builder's path calls.

 Require a continuous supported walking surface linking actual horizontal
 sockets, trainer/service spawn sockets, and appliance interaction positions.
 This is a template audit; terrain and assembled-world appearance remain manual.
 """
 category=kind(filename,theme)
 if category=='npc':return {'entrances':0,'appliances':0}
 vox={tuple(map(int,b['pos'])):b for b in d['blocks']}
 def final(p):
  b=vox.get(p)
  if b is None:return 'minecraft:air'
  return str(b.get('nbt',{}).get('final_state','minecraft:air')) if name(d,b)=='minecraft:jigsaw' else name(d,b)
 def passable(p):
  s=final(p)
  return s in ['minecraft:air','minecraft:structure_void'] or any(s.endswith(x) for x in ['_torch','_lantern','_banner','_door','_carpet']) or s.startswith('minecraft:potted_')
 w,_,h=map(int,d['size'])
 walk={(x,z) for x in range(w) for z in range(h) if final((x,0,z)) not in ['minecraft:air','minecraft:structure_void'] and passable((x,1,z)) and passable((x,2,z))}
 sockets=[]
 for b in d['blocks']:
  if name(d,b)!='minecraft:jigsaw':continue
  tag=b['nbt'];pool=str(tag.get('pool',''));target=str(tag.get('target',''))
  x,y,z=map(int,b['pos']);ori=str(d['palette'][int(b['state'])]['Properties']['orientation'])
  walkable=str(tag.get('name','')) in ['pixelmon:street','pixelmon:house','pixelmon:gym','pixelmon:mart','pixelmon:pokecenter','pixelmon:corner_l','pixelmon:corner_r'] or target in ['pixelmon:street','pixelmon:house','pixelmon:gym','pixelmon:mart','pixelmon:pokecenter','pixelmon:corner_l','pixelmon:corner_r']
  if not ori.startswith(('up_','down_')) and walkable and category not in ['tree','garden','street_art']:
   assert y==1,(theme,filename,'street socket is not at walking elevation',x,y,z)
   sockets.append((x,z))
  elif pool.startswith(('pixelmon:npc/','pixelmon_backport:npc/','minecraft:village/')):
   assert y==0,(theme,filename,'service/trainer spawn is above floor',x,y,z)
   sockets.append((x,z))
 if category=='road':
  assert all(final((x,0,z)) not in ['minecraft:air','minecraft:structure_void'] for x in range(w) for z in range(h)),(theme,filename,'unsupported street floor')
 if not sockets:return {'entrances':0,'appliances':0}
 assert all(p in walk for p in sockets),(theme,filename,'blocked entrance/spawn',[p for p in sockets if p not in walk])
 seen={sockets[0]};queue=collections.deque(seen)
 while queue:
  x,z=queue.popleft()
  for p in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
   if p in walk and p not in seen:seen.add(p);queue.append(p)
 assert all(p in seen for p in sockets),(theme,filename,'disconnected entrances/spawns',[p for p in sockets if p not in seen])
 if category in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:
  inside=[p for p in walk if 2<=p[0]<w-2 and 2<=p[1]<h-2]
  assert inside and min(inside,key=lambda p:abs(p[0]-w//2)+abs(p[1]-h//2)) in seen,(theme,filename,'entrance does not reach room interior')
 appliances=0
 for b in d['blocks']:
  if not kept(d,b) or int(b['pos'][1])!=1:continue
  x,_,z=map(int,b['pos'])
  assert any(p in seen for p in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]),(theme,filename,'unreachable ground-floor appliance',name(d,b),x,z)
  appliances+=1
 return {'entrances':len(sockets),'appliances':appliances}
def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);args=p.parse_args()
 shapes=set();report={};total=0
 with zipfile.ZipFile(args.pixelmon) as jar:
  for theme,definition in THEMES.items():
   source=definition['source'];pieces=entities=loot=entrances=appliances=panes=doors=0
   for path in jar.namelist():
    if not path.startswith(f'data/pixelmon/structures/gyms/{source}/') or not path.endswith('.nbt'):continue
    original=read_nbt(jar.read(path));strings(original,source,theme);decorate(original,theme)
    filename=path.rsplit('/',1)[1].replace('gym_'+source,'gym_'+theme)
    generated=read_nbt((ROOT/f'resources/data/pixelmon_backport/structures/gyms/{theme}/{filename}').read_bytes())
    assert native_entities(original)==native_entities(generated),(theme,filename,'native service/NPC removed')
    if theme=='poison' and filename=='gym_poison.nbt':
     # The redesign explicitly removes redundant unused barrels/furnaces only.
     import copy
     required=copy.deepcopy(original)
     required['blocks']=type(required['blocks'])(b for b in required['blocks'] if not empty_lab_fixture(name(required,b),b.get('nbt',{})))
    else:required=original
    assert not (components(required)-components(generated)),(theme,filename,'functional block/inventory lost',components(required)-components(generated))
    width,height,depth=map(int,generated['size']);positions=[tuple(map(int,b['pos'])) for b in generated['blocks']]
    assert len(positions)==len(set(positions)),(theme,filename,'overlapping blocks')
    assert all(0<=x<width and 0<=y<height and 0<=z<depth for x,y,z in positions),(theme,filename,'out of bounds')
    assert all(not name(generated,b).startswith('minecraft:deepslate') for b in generated['blocks'])
    npc=any(k in filename for k in ['leader_','grunt_','elite_'])
    if not npc:assert str(generated.get('BackportArchitecture',''))=='original-city-v9:'+theme
    if not npc:
     doors+=audit_doors(generated,theme,filename)
     audit_interiors(generated,theme,filename)
     assert not structural_components(generated),(theme,filename,'unsupported disconnected structure',structural_components(generated))
     count,errors=pane_errors(generated);panes+=count
     assert not errors,(theme,filename,'disconnected pane/bar states',errors[:12])
    routes=audit_routes(generated,theme,filename);entrances+=routes['entrances'];appliances+=routes['appliances']
    def joints(d):
     return collections.Counter((str(b['nbt']['pool']),str(b['nbt']['name']),str(b['nbt']['target'])) for b in d['blocks'] if name(d,b)=='minecraft:jigsaw' and not (str(b['nbt']['pool'])=='minecraft:empty' and str(b['nbt']['name'])=='minecraft:empty'))
    assert joints(original)==joints(generated),(theme,filename,'lost assembly connector',joints(original)-joints(generated))
    loot+=sum(name(generated,b).endswith('_chest') and name(generated,b).startswith('pixelmon:') for b in generated['blocks'])
    pieces+=1;entities+=sum(native_entities(generated).values())
    if filename==f'gym_{theme}.nbt':
     digest=hashlib.sha256(repr(geometry(generated)).encode()).hexdigest()
     assert digest not in shapes and geometry(generated)!=geometry(original),(theme,'copied main gym')
     shapes.add(digest)
     assert sum(name(generated,b).endswith('_chest') and name(generated,b).startswith('pixelmon:') for b in generated['blocks'])>=2
     materials=collections.Counter(name(generated,b) for b in generated['blocks'])
     assert sum(count for material,count in materials.items() if material.split(':')[-1] in ['lantern','sea_lantern','soul_lantern','glowstone'])>=10,(theme,'insufficient gym lighting')
     assert sum(count for material,count in materials.items() if material.endswith('_stairs'))>=8,(theme,'missing spectator seating')
   assert loot>=6,(theme,'missing exploration rewards',loot)
   report[theme]={'source_connector_reference':source,'pieces':pieces,'native_service_entities':entities,'native_reward_blocks':loot,'connected_entrances_and_spawns':entrances,'reachable_appliances':appliances,'checked_pane_and_bar_blocks':panes,'unobstructed_functional_doors':doors,'unsupported_components':0,'missing_pane_connections':0,'architecture_revision':9};total+=pieces
  for pool in (ROOT/'resources/data/pixelmon_backport/worldgen/template_pool').rglob('*.json'):
   for entry in json.loads(pool.read_text())['elements']:
    assert entry['element'].get('element_type')!='minecraft:feature_pool_element',(pool,'native optional tree retained')
    loc=entry['element'].get('location','')
    if loc.startswith('pixelmon_backport:'):
     assert (ROOT/'resources/data/pixelmon_backport/structures'/ (loc.split(':',1)[1]+'.nbt')).exists(),(pool,loc)
 (ROOT/'docs/GYM_ASSET_VERIFICATION_1.3.0.json').write_text(json.dumps({'cities':report,'distinct_original_main_gyms':len(shapes),'total_pieces':total},ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print(f'PASS {total} templates across eight new cities; connected supported entrances/spawns, reachable appliances, continuous streets, retained services/rewards and resolved pools. Assembled-world appearance requires manual acceptance.')
if __name__=='__main__':main()
