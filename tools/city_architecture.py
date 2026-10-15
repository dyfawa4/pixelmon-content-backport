"""Original themed architecture; native templates provide only connectors and services.

Geometry is generated here, except the rock town's recorded vanilla pyramid.
Native NPCs, functional tile data and reward rules are retained, with positions
rebuilt to match the new accessible ground-floor rooms.
"""
import collections, copy, hashlib, heapq, math, re
import nbtlib as n

PALETTES={
 'poison':('polished_blackstone','dark_prismarine','purple_concrete','lime_stained_glass','sea_lantern'),
 'dark':('deepslate','polished_blackstone_bricks','crimson_planks','purple_stained_glass','soul_lantern'),
 'fighting':('stone_bricks','stripped_dark_oak_wood','dark_oak_planks','red_stained_glass','lantern'),
 'flying':('smooth_quartz','quartz_pillar','white_concrete','light_blue_stained_glass','sea_lantern'),
 'normal':('stone_bricks','bricks','spruce_planks','white_stained_glass','lantern'),
 'bug':('mossy_stone_bricks','stripped_jungle_wood','jungle_planks','lime_stained_glass','glowstone'),
 'psychic':('smooth_quartz','purpur_pillar','purpur_block','magenta_stained_glass','sea_lantern'),
 'rock':('polished_andesite','sandstone','cut_sandstone','orange_stained_glass','lantern'),
}
# 1.16 has no deepslate: use the available blackstone family.
PALETTES['dark']=('blackstone',)+PALETTES['dark'][1:]
LABELS={'poison':'净化实验城','dark':'暮影夜市','fighting':'演武坊','flying':'风穹观景城','normal':'市民商埠','bug':'生态花园','psychic':'星轨学城','rock':'化石矿业城'}

class Builder:
 def __init__(self,data,theme):
  self.data=data;self.theme=theme;self.w,self.oldh,self.d=map(int,data['size']);self.blocks={};self.palette=[];self.ids={};self.supports=set()
  self.floor,self.wall,self.roof,self.glass,self.light=PALETTES[theme]
  self.interior=None;self.wall_columns=set();self.nurse_positions=[]
 def state(self,name,props=None):
  if ':' not in name:name='minecraft:'+name
  key=(name,tuple(sorted((props or {}).items())))
  if key not in self.ids:
   self.ids[key]=len(self.palette);s=n.Compound({'Name':n.String(name)})
   if props:s['Properties']=n.Compound({k:n.String(v) for k,v in props.items()})
   self.palette.append(s)
  return self.ids[key]
 def put(self,x,y,z,name,props=None,nbt=None):
  x,y,z=int(x),int(y),int(z)
  if not (0<=x<self.w and y>=0 and 0<=z<self.d):return
  if getattr(self,'decorating_room',False) and 1<=y<=3 and self.interior is not None and (x,z) not in self.interior:return
  # Room fixtures cannot replace a facade or sit on an exterior terrace.
  fixture=any(s in name for s in ['_bed','bookshelf','crafting_table','cauldron','lectern','potted_','furnace','barrel',':chest',':trapped_chest','_chest','healer','vending_machine','trade_machine','_pc'])
  if fixture and self.interior is not None and (x,z) not in self.interior:return
  b=n.Compound({'pos':n.List[n.Int]([x,y,z]),'state':n.Int(self.state(name,props))})
  if nbt is not None:b['nbt']=copy.deepcopy(nbt)
  self.blocks[x,y,z]=b
 def box(self,x0,y0,z0,x1,y1,z1,name,props=None):
  for x in range(max(0,x0),min(self.w-1,x1)+1):
   for y in range(max(0,y0),y1+1):
    for z in range(max(0,z0),min(self.d-1,z1)+1):self.put(x,y,z,name,props)
 def line(self,x0,z0,x1,z1,y,name,width=1):
  steps=max(abs(x1-x0),abs(z1-z0),1)
  for step in range(steps+1):
   x=round(x0+(x1-x0)*step/steps);z=round(z0+(z1-z0)*step/steps)
   self.box(x-width//2,y,z-width//2,x+width//2,y,z+width//2,name)
 def path(self,x0,z0,x1,z1,y,width=3):
  steps=max(abs(x1-x0),abs(z1-z0),1)
  for step in range(steps+1):
   x=round(x0+(x1-x0)*step/steps);z=round(z0+(z1-z0)*step/steps)
   self.box(x-width//2,y,z-width//2,x+width//2,y,z+width//2,self.floor)
   self.box(x-width//2,y+1,z-width//2,x+width//2,y+3,z+width//2,'air')
 def circle(self,cx,cz,r,y,name,fill=False):
  for x in range(cx-r,cx+r+1):
   for z in range(cz-r,cz+r+1):
    q=(x-cx)**2+(z-cz)**2
    if q<=r*r and (fill or q>=(r-1.4)**2):self.put(x,y,z,name)
 def shell(self,x0,z0,x1,z1,y,height,wall=None,roof=None):
  wall=wall or self.wall;roof=roof or self.roof
  self.interior={(x,z) for x in range(x0+1,x1) for z in range(z0+1,z1)}
  self.wall_columns.update((x,z) for x in range(x0,x1+1) for z in [z0,z1])
  self.wall_columns.update((x,z) for z in range(z0,z1+1) for x in [x0,x1])
  self.box(x0,y,z0,x1,y,z1,self.floor)
  for h in range(1,height+1):
   for x in range(x0,x1+1):
    for z in [z0,z1]:self.put(x,y+h,z,self.glass if 2<=h<=3 and (x-x0)%4 in [1,2] else wall)
   for z in range(z0,z1+1):
    for x in [x0,x1]:self.put(x,y+h,z,self.glass if 2<=h<=3 and (z-z0)%4 in [1,2] else wall)
  self.box(x0,y+height+1,z0,x1,y+height+1,z1,roof)
 def arch(self,cx,z,y,width=4,height=5):
  for x in [cx-width,cx+width]:self.box(x,y,z,x,y+height,z,self.wall);self.supports.add((x,z))
  self.box(cx-width,y+height,z,cx+width,y+height,z,self.roof)
 def dome(self,cx,cz,r,y):
  for dy in range(r+1):
   rr=round(math.sqrt(max(0,r*r-dy*dy)))
   self.circle(cx,cz,rr,y+dy,self.wall if dy%4==0 else self.glass)
 def pillar(self,x,z,y,height=8):
  self.box(x-1,y,z-1,x+1,y+height,z+1,self.wall)
  if y<=1:self.supports.update((xx,zz) for xx in range(x-1,x+2) for zz in range(z-1,z+2))
  self.box(x-2,y+height,z-2,x+2,y+height,z+2,self.roof)
  self.put(x,y+height+1,z,self.light)
 def gable(self,x0,z0,x1,z1,y,material=None):
  material=material or self.roof
  for rise in range((x1-x0)//2+1):
   for x in [x0+rise,x1-rise]:self.box(x,y+rise,z0,x,y+rise,z1,material)
   for z in [z0,z1]:self.box(x0+rise,y,z,x1-rise,y+rise,z,self.wall)
 def trim(self,x0,z0,x1,z1,y,height):
  for x in range(x0,x1+1,5):
   for z in [z0,z1]:
    self.box(x,y,z,x,y+height,z,self.wall)
    self.put(x,y+height+1,z,self.light)
  for z in range(z0,z1+1,5):
   for x in [x0,x1]:self.box(x,y,z,x,y+height,z,self.wall)
 def reward(self,x,y,z,tier='poke',hidden=False):
  tag=n.Compound({'id':n.String('pixelmon:poke_chest'),'chestOneTime':n.Byte(1),'Owner':n.IntArray([0,0,0,0]),'customDrop':n.Byte(0),'specialEvent':n.Byte(0),'visibility':n.Byte(int(hidden)),'dropOneTime':n.Byte(1),'timeEnabled':n.Byte(0),'grotto':n.Byte(0),'customDrops':n.List[n.Compound]()})
  self.put(x,y,z,'pixelmon:'+tier+'_chest',{'facing':'north'},tag)
 def finish(self):
  connect_neighbors(self)
  self.data['palette']=n.List[n.Compound](self.palette)
  self.data['blocks']=n.List[n.Compound](self.blocks[k] for k in sorted(self.blocks,key=lambda p:(p[1],p[0],p[2])))
  height=max((p[1] for p in self.blocks),default=0)+1
  self.data['size']=n.List[n.Int]([self.w,max(height,4),self.d])
  self.data['BackportArchitecture']=n.String('original-city-v6:'+self.theme)
  if self.interior is not None:self.data['BackportInterior']=n.IntArray([v for p in sorted(self.interior) for v in p])
  return self.data

def kind(filename,theme):
 stem=filename.removesuffix('.nbt').replace('gym_'+theme,'').strip('_')
 if not stem:return 'gym'
 if any(role in stem for role in ['leader_','grunt_','elite_']):return 'npc'
 if stem.startswith('roadside'):return 'street_art'
 if stem.startswith('road') or stem=='connector':return 'road'
 if 'tree_' in stem:return 'tree'
 if stem.startswith('tc_'):return 'plaza'
 # A lighthouse shaft/cap is not a residential room template.
 if re.search(r'(^|_)house(?:_|$)',stem):return 'house'
 if stem in ['pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:return stem
 if 'farm' in stem or 'berry' in stem:return 'garden'
 return 'street_art'

def connectors(data):
 return [copy.deepcopy(b) for b in data['blocks'] if str(data['palette'][int(b['state'])]['Name'])=='minecraft:jigsaw']

def walk_socket(tag):
 entrances=['pixelmon:street','pixelmon:house','pixelmon:gym','pixelmon:mart','pixelmon:pokecenter','pixelmon:corner_l','pixelmon:corner_r']
 return str(tag.get('name','')) in entrances or str(tag.get('target','')) in entrances

def connect_neighbors(b):
 """Serialize panes/fences using their actual final neighbors, not defaults."""
 directions={'north':(0,-1),'east':(1,0),'south':(0,1),'west':(-1,0)}
 def name(p):
  old=b.blocks.get(p)
  if old is None:return 'minecraft:air'
  state=str(b.palette[int(old['state'])]['Name'])
  return str(old.get('nbt',{}).get('final_state','minecraft:air')) if state=='minecraft:jigsaw' else state
 def full(s):
  return s!='minecraft:air' and not any(token in s for token in ['structure_void','jigsaw','water','lantern','torch','fence','_wall','slab','stairs','leaves','potted_','_bed','_door','banner','sapling','flower','vine','chain','end_rod','lectern'])
 for p,old in list(b.blocks.items()):
  state=b.palette[int(old['state'])];s=str(state['Name']);props={k:str(v) for k,v in state.get('Properties',{}).items()}
  if s.endswith('_pane') or s=='minecraft:iron_bars' or s.endswith('_fence'):
   for direction,(dx,dz) in directions.items():
    neighbor=name((p[0]+dx,p[1],p[2]+dz))
    same=neighbor.endswith('_fence') if s.endswith('_fence') else neighbor.endswith('_pane') or neighbor=='minecraft:iron_bars'
    props[direction]=str(same or full(neighbor)).lower()
   props['waterlogged']=props.get('waterlogged','false');old['state']=n.Int(b.state(s,props))
  elif s.endswith('_leaves'):
   props.update(persistent='true',distance='1');old['state']=n.Int(b.state(s,props))

def empty_lab_fixture(name,tag):
 """Only redundant, completely unused copies; real stock/loot and machine history stay."""
 if name not in ['minecraft:barrel','minecraft:blast_furnace']:return False
 if set(tag)-{'id','Items','BurnTime','CookTime','CookTimeTotal','RecipesUsed'}:return False
 return not tag.get('Items') and not tag.get('RecipesUsed') and all(int(tag.get(k,0))==0 for k in ['BurnTime','CookTime','CookTimeTotal'])

def retain(b,data,joints,ground,category):
 """Put native services in usable rooms; re-site challenge connectors on accessible routes."""
 pending=[]
 b.entrance_aisles=getattr(b,'entrance_aisles',set())
 external=[j for j in joints if not str(j.get('nbt',{}).get('pool','')).startswith(('pixelmon_backport:npc/','pixelmon:npc/','minecraft:village/'))]
 for j in external:
  if str(j['nbt'].get('pool',''))=='minecraft:empty' and str(j['nbt'].get('name',''))=='minecraft:empty':continue
  x,_,z=map(int,j['pos']);s=data['palette'][int(j['state'])]
  orientation=str(s.get('Properties',{}).get('orientation','north_up'))
  horizontal=not orientation.startswith(('up_','down_'))
  y=ground+1 if horizontal else ground
  tag=copy.deepcopy(j['nbt'])
  decoration=category in ['tree','garden','street_art']
  underlying=b.blocks.get((x,y,z))
  final=str(b.palette[int(underlying['state'])]['Name']) if decoration and underlying else 'minecraft:air' if horizontal else 'minecraft:'+b.floor
  tag['final_state']=n.String(final)
  b.put(x,y,z,str(s['Name']),{k:str(v) for k,v in s.get('Properties',{}).items()},tag)
  pending.append(copy.deepcopy(b.blocks[x,y,z]))
  # Match the authored street elevation, with a three-wide clear entrance.
  cx,cz=b.w//2,b.d//2
  # Orthogonal approaches do not carve diagonal notches into street blocks.
  # A decoration's incoming anchor is NOT a doorway. Clearing a 3x3 region
  # around every anchor amputated tree trunks, lamp posts and colonnades.
  if horizontal and walk_socket(tag) and not decoration:
   b.box(x,ground+1,z,x,ground+2,z,'air')
   # Keep appliances back from the actual street-to-door approach.
   direction=orientation.split('_')[0];dx,dz={'east':(-1,0),'west':(1,0),'north':(0,1),'south':(0,-1)}[direction]
   for step in range(5):
    for side in [-1,0,1]:b.entrance_aisles.add((x+dx*step+dz*side,z+dz*step+dx*side))
  b.put(x,y,z,str(s['Name']),{k:str(v) for k,v in s.get('Properties',{}).items()},tag)
 npcs=[j for j in joints if j not in external]
 points=[]
 for z in range(5,max(6,b.d-4),6):
  for x in [max(3,b.w//3),min(b.w-4,2*b.w//3)]:points.append((x,z))
 reserved={(int(j['pos'][0]),int(j['pos'][2])) for j in pending}
 def free(p):
  if b.interior is not None and p not in b.interior:return False
  if p in reserved or p in b.supports or p in b.entrance_aisles:return False
  if all(q in b.supports for q in [(p[0]-1,p[1]),(p[0]+1,p[1]),(p[0],p[1]-1),(p[0],p[1]+1)]):return False
  return all((p[0],ground+h,p[1]) not in b.blocks or str(b.palette[int(b.blocks[p[0],ground+h,p[1]]['state'])]['Name'])=='minecraft:air' for h in [1,2])
 points=list(dict.fromkeys(p for p in points if 0<=p[0]<b.w and 0<=p[1]<b.d and free(p)))
 if len(points)<len(npcs):
  points=list(dict.fromkeys(points+[(x,z) for z in range(1,b.d-1) for x in range(1,b.w-1) if free((x,z))]))
 if len(points)<len(npcs):raise ValueError(('NPC room too small',b.theme,category,len(npcs),b.w,b.d))
 for j in sorted(npcs,key=lambda j:str(j['nbt'].get('target',''))!='pixelmon:gym_leader'):
  leader=str(j['nbt'].get('target',''))=='pixelmon:gym_leader' and category=='gym'
  if leader:
   x,z=b.leader_position
   if not free((x,z)):raise ValueError(('Leader stage obstructed',b.theme,x,z))
  else:
   available=[p for p in points if free(p) and p not in getattr(b,'leader_reserved',set())|getattr(b,'leader_aisles',set())]
   if not available:raise ValueError(('No side position for gym trainer',b.theme,category))
   x,z=available[0]
  reserved.add((x,z))
  if 'nurse' in str(j['nbt'].get('pool','')):b.nurse_positions.append((x,z))
  tag=copy.deepcopy(j['nbt']);tag['final_state']=n.String('minecraft:'+b.floor)
  b.put(x,ground,z,'jigsaw',{'orientation':'up_'+b.front_direction if leader else 'up_north'},tag)
  # Route to the spawn later using the actual free floor; never cut through a
  # building or landmark just to draw a straight line to the template center.
  pending.append(copy.deepcopy(b.blocks[x,ground,z]))
 # Native tile data includes PCs/healers, shop storage, beds and real loot.
 services=[]
 laboratory=category=='gym' and b.theme=='poison'
 empty_counts=collections.Counter()
 for old in data['blocks']:
  state=data['palette'][int(old['state'])];name=str(state['Name'])
  if name=='minecraft:jigsaw':continue
  tag=old.get('nbt',{})
  functional=name.startswith('pixelmon:') and (name.endswith(('_pc','_chest')) or any(word in name for word in ['healer','trade_machine','fossil_cleaner','fossil_machine','anvil','vending_machine']))
  inventory=bool(tag.get('Items',[])) or 'LootTable' in tag or name in ['minecraft:chest','minecraft:trapped_chest','minecraft:barrel','minecraft:furnace','minecraft:blast_furnace','minecraft:smoker','minecraft:brewing_stand']
  if functional or inventory:
   if laboratory and empty_lab_fixture(name,tag):
    empty_counts[name]+=1
    if empty_counts[name]>(6 if name=='minecraft:barrel' else 2):continue
   services.append(old)
 if laboratory:
  # Retain every stocked/loot container, but organize storage as two-high racks.
  barrels=[old for old in services if str(data['palette'][int(old['state'])]['Name'])=='minecraft:barrel']
  services=[old for old in services if old not in barrels]
  for index,old in enumerate(barrels):
   old=copy.deepcopy(old);old['pos']=n.List[n.Int]([index//2*3,index%2,0]);services.append(old)
 # Pack connected appliances as complete clusters: splitting top/bottom PCs or
 # trade-machine arms would leave a visually present but unusable service.
 remaining={tuple(map(int,old['pos'])):old for old in services};groups=[]
 while remaining:
  first=next(iter(remaining));todo=[first];group=[]
  while todo:
   pos=todo.pop()
   if pos not in remaining:continue
   item=remaining.pop(pos);group.append(item)
   x,y,z=pos
   state=data['palette'][int(item['state'])];machine=str(state['Name'])
   for p in [(x-1,y,z),(x+1,y,z),(x,y-1,z),(x,y+1,z),(x,y,z-1),(x,y,z+1)]:
    if p not in remaining:continue
    other=data['palette'][int(remaining[p]['state'])];othername=str(other['Name'])
    connected_machine=machine.startswith('pixelmon:') and othername.startswith('pixelmon:') and not machine.endswith('_chest') and not othername.endswith('_chest')
    double_chest=machine==othername=='minecraft:chest' and str(state.get('Properties',{}).get('type','single'))!='single' and str(other.get('Properties',{}).get('type','single'))!='single'
    barrel_rack=laboratory and machine==othername=='minecraft:barrel' and p[0]==x and p[2]==z
    if connected_machine or double_chest or barrel_rack:todo.append(p)
  groups.append(group)
 occupied={(int(j['pos'][0]),int(j['pos'][2])) for j in pending}
 covered_nurses=set()
 for i,group in enumerate(sorted(groups,key=lambda a:(not any('healer' in str(data['palette'][int(old['state'])]['Name']) for old in a),-len(a)))):
  positions=[tuple(map(int,old['pos'])) for old in group]
  xmin,ymin,zmin=map(min,zip(*positions));xmax,ymax,zmax=map(max,zip(*positions))
  width=xmax-xmin+1;depth=zmax-zmin+1
  if category in ['road','street_art','garden','tree']:
   for old,(x,y,z) in zip(group,positions):
    state=data['palette'][int(old['state'])]
    b.put(x,ground+1+y-ymin,z,str(state['Name']),{k:str(v) for k,v in state.get('Properties',{}).items()},old.get('nbt'))
   continue
  candidates=[]
  for ox in range(2,b.w-1-width):
   for oz in range(2,b.d-1-depth):
    cells={(x,z) for x in range(ox,ox+width) for z in range(oz,oz+depth)}
    if laboratory and any(abs(x-b.w//2)<=8 and abs(z-b.d//2)<=11 for x,z in cells):continue
    if b.interior is not None and not cells<=b.interior:continue
    if cells & (occupied|b.supports|b.entrance_aisles|getattr(b,'leader_reserved',set())|getattr(b,'leader_aisles',set())):continue
    # Appliances belong on clear room floors. Never make room for a machine by
    # chopping a facade, greenhouse shell, tree trunk or structural column.
    if any((x,ground+y,z) in b.blocks and str(b.palette[int(b.blocks[x,ground+y,z]['state'])]['Name']) not in ['minecraft:air'] for x,z in cells for y in [1,2]):continue
    neighbors={(x+dx,z+dz) for x,z in cells for dx,dz in [(-1,0),(1,0),(0,-1),(0,1)]}-cells
    if not any(p not in occupied|b.supports and 0<p[0]<b.w-1 and 0<p[1]<b.d-1 for p in neighbors):continue
    healers=[(ox+x-xmin,oz+z-zmin) for old,(x,y,z) in zip(group,positions) if 'healer' in str(data['palette'][int(old['state'])]['Name'])]
    needed_nurses=[p for p in b.nurse_positions if p not in covered_nurses] or b.nurse_positions
    nurse_distance=min(((hx-nx)**2+(hz-nz)**2 for hx,hz in healers for nx,nz in needed_nurses),default=0)
    # The real nurse searches within eight blocks; target at most four.
    if healers and b.nurse_positions and nurse_distance>16:continue
    rank=nurse_distance if healers and b.nurse_positions else min(ox,b.w-ox-width)+min(oz,b.d-oz-depth)
    candidates.append((rank,ox,oz,cells))
  if not candidates:raise ValueError(('Service room too small',b.theme,category,len(services),b.w,b.d,width,depth))
  _,ox,oz,cells=min(candidates,key=lambda q:q[:3]);occupied.update(cells)
  placed_healers=[(ox+x-xmin,oz+z-zmin) for old,(x,y,z) in zip(group,positions) if 'healer' in str(data['palette'][int(old['state'])]['Name'])]
  covered_nurses.update((nx,nz) for nx,nz in b.nurse_positions if any((hx-nx)**2+(hz-nz)**2<=16 for hx,hz in placed_healers))
  # Independent containers must not pack into a solid grid with inaccessible
  # barrels in the middle. Leave a one-block interaction aisle around fixtures.
  occupied.update((x+dx,z+dz) for x,z in cells for dx in [-1,0,1] for dz in [-1,0,1])
  for x,z in cells:
   b.box(x,ground+1,z,x,ground+max(4,ymax-ymin+2),z,'air')
   b.put(x,ground,z,b.floor)
  for old,(x,y,z) in zip(group,positions):
   state=data['palette'][int(old['state'])];name=str(state['Name']);props={k:str(v) for k,v in state.get('Properties',{}).items()}
   px,pz=ox+x-xmin,oz+z-zmin
   if width==depth==1 and 'facing' in props and props['facing'] in ['north','south','west','east'] and not (name=='minecraft:chest' and props.get('type','single')!='single'):
    # Face the open interaction aisle, rather than reusing the old wall direction.
    directions={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}
    choices=[direction for direction,(dx,dz) in directions.items() if (px+dx,pz+dz) not in cells|b.supports and (b.interior is None or (px+dx,pz+dz) in b.interior) and all((px+dx,ground+h,pz+dz) not in b.blocks or str(b.palette[int(b.blocks[px+dx,ground+h,pz+dz]['state'])]['Name'])=='minecraft:air' for h in [1,2])]
    if choices:props['facing']=min(choices,key=lambda direction:abs(px+directions[direction][0]-b.w//2)+abs(pz+directions[direction][1]-b.d//2))
   b.put(px,ground+1+y-ymin,pz,name,props,old.get('nbt'))
 for i,e in enumerate(data.get('entities',[])):
  # Service/trainer/statue positions follow the new floor; remove wall art and carts
  # which reference the old building's absent walls and rails.
  if not str(e['nbt'].get('id','')).startswith('pixelmon:'):continue
  candidates=[(x,z) for z in range(b.d) for x in range(b.w) if free((x,z)) and (x,z) not in occupied|getattr(b,'leader_reserved',set())|getattr(b,'leader_aisles',set())]
  if not candidates:raise ValueError(('No clear room for retained entity',b.theme,category,e['nbt'].get('id')))
  target=(b.w//2-11,b.d//2) if laboratory else (b.w//2,b.d//2)
  x,z=min(candidates,key=lambda p:abs(p[0]-target[0])+abs(p[1]-target[1]));reserved.add((x,z));occupied.add((x,z))
  e['pos']=n.List[n.Double]([x+.5,ground+1.,z+.5]);e['blockPos']=n.List[n.Int]([x,ground+1,z]);e['nbt']['Pos']=copy.deepcopy(e['pos'])
 data['entities']=n.List[n.Compound](e for e in data.get('entities',[]) if str(e['nbt'].get('id','')).startswith('pixelmon:'))
 # Later clear entrance paths may cross earlier connector tiles. Restore every
 # connector last, or a city can silently lose its mart/gym/branch expansion.
 for joint in pending:b.blocks[tuple(map(int,joint['pos']))]=joint
 b.service_aisles=occupied

def gym(b,g):
 """Eight original silhouettes surrounding distinct routes and a clear arena."""
 w,d=b.w,b.d;cx,cz=w//2,d//2;r=min(w,d)//2-5;t=b.theme
 b.interior={(x,z) for x in range(2,w-2) for z in range(2,d-2)}
 if t in ['poison','bug','psychic']:
  radius=r if t!='psychic' else r-1
  b.interior={(x,z) for x in range(w) for z in range(d) if (x-cx)**2+(z-cz)**2<(radius-1.5)**2}
  b.wall_columns.update((x,z) for x in range(w) for z in range(d) if (radius-1.5)**2<=(x-cx)**2+(z-cz)**2<=radius**2)
 b.box(0,g,0,w-1,g,d-1,b.floor)
 # An open central battle court and perimeter challenge promenade.
 for z in range(3,d-3):
  for x in range(3,w-3):
   if x in [3,w-4] or z in [3,d-4]:b.put(x,g,z,b.roof)
 b.box(cx-6,g,cz-9,cx+6,g,cz+9,'white_concrete')
 b.circle(cx,cz,4,g,b.roof);b.line(cx-6,cz,cx+6,cz,g,b.roof)
 if t=='poison':
  for y in range(1,13):b.circle(cx,cz,r,g+y,b.wall if y in [1,5,12] else b.glass)
  for dx in [-r+4,r-4]:
   for dz in [-r+4,r-4]:
    for h in range(1,19):b.circle(cx+dx,cz+dz,3,g+h,'lime_stained_glass' if h%5 else 'dark_prismarine')
    b.circle(cx+dx,cz+dz,3,g+19,'sea_lantern',True)
  b.box(cx-8,g+13,cz-8,cx+8,g+13,cz+8,b.glass)
  for x in [cx-8,cx+8]:
   for z in [cz-8,cz+8]:b.pillar(x,z,g,13)
 elif t=='fighting':
  # A complete roofed dojo with a garden court and east-facing entry gate.
  b.shell(8,8,w-9,d-9,g,8)
  b.gable(7,7,w-8,d-8,g+9,'dark_oak_planks')
  for z in [cz-5,cz+5]:b.box(w-5,g+1,z,w-5,g+7,z,b.wall);b.supports.add((w-5,z))
  b.box(w-6,g+7,cz-6,w-4,g+7,cz+6,b.roof)
  b.box(w-6,g+8,cz-7,w-4,g+8,cz+7,'red_concrete')
  for x in [3,w-4]:
   for z in [4,d-5]:b.pillar(x,z,g,6)
 elif t=='flying':
  # Four viewing towers, open-air arena, wing canopy and balustrades.
  for x in [4,w-5]:
   for z in [5,d-6]:b.pillar(x,z,g,20 if z>d//2 else 12)
  for dx in range(-r,r+1):
   for dz in range(-3,4):b.put(cx+dx,g+14+abs(dx)//3,cz+dz,b.floor)
  for z in range(1,d-1):
   for x in [1,w-2]:b.put(x,g+1,z,'white_concrete');b.put(x,g+2,z,'glass_pane')
  for z in [5,d-6]:b.arch(cx,z,g+1,8,10)
  for dx in range(-r,r+1):
   for dz in range(-max(1,7-abs(dx)//3),max(1,7-abs(dx)//3)+1):b.put(cx+dx,g+14+abs(dx)//3,cz+dz,b.floor)
  for dx in [-r,-r//2,r//2,r]:
   for z in [cz-3,cz+3]:b.pillar(cx+dx,z,g,14+abs(dx)//3)
  for x in [4,w-5]:
   for z in [5,d-6]:
    b.box(x-3,g+10,z-3,x+3,g+10,z+3,'smooth_quartz')
    for yy in range(g+1,g+11):b.put(x,yy,z-2,'ladder',{'facing':'north'})
 elif t=='normal':
  # Civic sports hall, arcade frontage and asymmetric city clock tower.
  b.shell(2,3,w-3,d-4,g,8)
  for x in range(4,w-3,5):b.arch(x,2,g+1,2,5)
  b.pillar(w-8,7,g,24)
  for dy in range(-2,3):
   for dx in range(-2,3):b.put(w-8+dx,g+20+dy,4,'white_concrete' if abs(dx)==2 or abs(dy)==2 else 'black_concrete')
  b.put(w-8,g+22,3,'gold_block')
  b.gable(2,3,w-3,d-4,g+9)
  b.trim(2,3,w-3,d-4,g,8)
  b.arch(cx,2,g+1,5,6)
  b.box(cx-4,g+7,1,cx+4,g+7,1,'white_concrete')
  b.put(cx,g+8,1,'gold_block')
 elif t=='bug':
  # Greenhouse campus: central dome, two smaller honeycomb labs and leafy arcade.
  b.dome(cx,cz,r,g+1)
  for x in [5,w-6]:b.dome(x,7,4,g+1);b.dome(x,d-8,4,g+1)
  for x in [2,w-3]:
   for z in range(4,d-4,5):b.pillar(x,z,g,5);b.put(x,g+6,z,'honeycomb_block')
 elif t=='psychic':
  b.dome(cx,cz,r-1,g+1)
  for x in [3,w-4]:
   for z in [4,d-5]:b.pillar(x,z,g,22)
  for dx in range(-10,11):
   for dy in range(-10,11):
    if 80<=dx*dx+dy*dy<=104:b.put(cx+dx,g+25+dy,cz,b.roof)
  for x in [cx-10,cx+10]:
   for z in [cz-5,cz+5]:b.pillar(x,z,g,25)
   b.box(x,g+25,cz-5,x,g+25,cz+5,b.roof)
  for x,z in [(cx-5,cz-5),(cx+5,cz+5)]:b.put(x,g+1,z,'end_rod')
 elif t=='rock':
  # A finished fossil museum around an open arena, with supported galleries.
  b.shell(2,2,w-3,d-3,g,11,'smooth_sandstone','cut_sandstone')
  b.box(8,g+12,8,w-9,g+12,d-9,'air')
  for x in [4,w-5]:
   b.box(x,g+6,4,x+3 if x==4 else x,g+6,d-5,'cut_sandstone')
   for z in range(4,d-4,6):
    if abs(z-cz)>2:b.pillar(x,z,g,10)
  for z in [4,d-5]:b.box(4,g+6,z,w-5,g+6,z+2,'cut_sandstone')
  b.trim(2,2,w-3,d-3,g,11)
  for z in [5,d-6]:b.arch(cx,z,g+1,7,7)
  for x in [6,w-7]:
   for z in [7,d-8]:
    b.box(x-1,g+1,z-1,x+1,g+1,z+1,'polished_andesite')
    b.put(x,g+2,z,'bone_block');b.put(x,g+3,z,'bone_block')
    b.box(x-1,g+4,z,x+1,g+4,z,'bone_block')
  b.box(cx-4,g+13,2,cx+4,g+15,2,'orange_terracotta')
  b.put(cx,g+14,1,'gold_block')
 elif t=='dark':
  # Midnight cathedral: split nave, flying buttresses, rose window and side spires.
  b.shell(3,3,w-4,d-4,g,12,'polished_blackstone_bricks','blackstone')
  for x in [4,w-5]:
   for z in [5,d-6]:b.pillar(x,z,g,29 if z<d//2 else 21)
  for z in range(8,d-5,6):
   for x in [8,w-9]:b.pillar(x,z,g,10)
  b.gable(3,3,w-4,d-4,g+13,'blackstone')
  b.trim(3,3,w-4,d-4,g,12)
  for x in [4,w-5]:
   b.pillar(x,5,g,35)
   for h in range(6):b.box(x-2+(h//2),g+36+h,3+(h//2),x+2-(h//2),g+36+h,7-(h//2),'blackstone')
  for dx in range(-5,6):
   for dy in range(-5,6):
    if dx*dx+dy*dy<=25:b.put(cx+dx,g+9+dy,3,b.glass if dx*dx+dy*dy<20 else 'blackstone')
  b.arch(cx,2,g+1,4,6)
 # Continuous, accessible paths across the court; no fake locked maze.
 b.line(cx,1,cx,d-2,g,b.floor,3);b.line(1,cz,w-2,cz,g,b.floor,3)
 gym_interior(b,g)
 b.reward(2,g+1,d-3,'ultra',True);b.reward(w-3,g+1,2,'poke')

def gym_interior(b,g):
 """Readable courts and distinct occupied side areas, with ground-supported light."""
 w,d=b.w,b.d;cx,cz=w//2,d//2;t=b.theme
 accents={'normal':'red','fighting':'red','poison':'lime','flying':'light_blue','bug':'yellow','psychic':'magenta','rock':'orange','dark':'purple'}
 color=accents[t]
 # Full court markings survive the crossing approach paths.
 for x in range(cx-7,cx+8):
  for z in range(cz-10,cz+11):
   edge=x in [cx-7,cx+7] or z in [cz-10,cz+10]
   b.put(x,g,z,color+'_concrete' if edge else 'white_concrete' if t not in ['fighting','dark'] else 'smooth_sandstone' if t=='fighting' else 'polished_blackstone')
 b.circle(cx,cz,3,g,color+'_concrete');b.line(cx-7,cz,cx+7,cz,g,color+'_concrete')
 # Four seating galleries. Side aisles and the central arena remain open.
 bench='dark_oak' if t=='fighting' else 'crimson' if t=='dark' else 'jungle' if t=='bug' else 'birch' if t=='flying' else 'oak'
 for x in [max(6,cx-13),min(w-7,cx+13)]:
  for z in [cz-8,cz-2,cz+4,cz+10]:
   if any((x,yy,z) in b.blocks and str(b.palette[int(b.blocks[x,yy,z]['state'])]['Name'])!='minecraft:air' for yy in [g+1,g+2]):continue
   b.put(x,g+1,z,bench+'_stairs',{'facing':'east' if x<cx else 'west','half':'bottom','shape':'straight','waterlogged':'false'})
   b.put(x,g+1,z+1,bench+'_stairs',{'facing':'east' if x<cx else 'west','half':'bottom','shape':'straight','waterlogged':'false'})
 for x in [cx-11,cx+11]:
  for z in range(6,d-5,6):
   if (x,z) in b.supports:continue
   b.box(x,g+1,z,x,g+2,z,b.wall);b.put(x,g+3,z,b.light)
 # The actual fixtures are different for each theme, rather than recolored copies.
 for x in [cx-15,cx+15]:
  for z in [cz-7,cz+7]:
   if not (2<x<w-3 and 2<z<d-3) or (x,z) in b.supports:continue
   if t=='normal':
    b.put(x,g+1,z,'bookshelf');b.put(x,g+2,z,'gold_block');b.put(x+1,g+1,z,'note_block')
   elif t=='fighting':
    b.box(x,g+1,z,x,g+2,z,'stripped_oak_log');b.put(x,g+3,z,'red_wool');b.put(x+1,g+2,z,'dark_oak_fence');b.put(x-1,g+2,z,'dark_oak_fence')
   elif t=='flying':
    b.box(x,g+1,z,x,g+3,z,'quartz_pillar');b.put(x,g+4,z,'white_banner',{'rotation':'4' if x<cx else '12'});b.put(x+1,g+1,z,'potted_white_tulip')
   elif t=='poison':
    b.put(x,g+1,z,'cauldron',{'level':'3'});b.put(x+1,g+1,z,'dark_prismarine');b.box(x+1,g+2,z,x+1,g+4,z,'lime_stained_glass')
   elif t=='bug':
    b.put(x,g+1,z,'jungle_log');b.put(x,g+2,z,'bee_nest',{'facing':'east' if x<cx else 'west'});b.put(x+1,g+1,z,'grass_block');b.put(x+1,g+2,z,'potted_fern')
   elif t=='psychic':
    b.box(x,g+1,z,x,g+3,z,'bookshelf');b.put(x+1,g+1,z,'lectern',{'facing':'east' if x<cx else 'west','has_book':'false','powered':'false'});b.put(x,g+4,z,'purple_stained_glass')
   elif t=='rock':
    b.put(x,g+1,z,'polished_andesite');b.box(x,g+2,z,x,g+3,z,'bone_block');b.box(x-1,g+4,z,x+1,g+4,z,'bone_block');b.put(x+1,g+1,z,'gold_ore')
   else:
    b.box(x,g+1,z,x,g+3,z,'chiseled_polished_blackstone');b.put(x,g+4,z,'soul_lantern');b.put(x+1,g+1,z,'potted_wither_rose')
 if t=='poison':
  for side in [-1,1]:
   x=cx+side*11
   for z in [cz-6,cz+6]:
    b.box(x-1,g+1,z,x+1,g+1,z,'smooth_quartz')
    b.put(x,g+2,z,'brewing_stand',{'has_bottle_0':'false','has_bottle_1':'false','has_bottle_2':'false'})
    b.put(x-side,g+2,z,'potted_bamboo')
 leader_stage(b,g,color)

def leader_stage(b,g,color):
 """Reserve the challenge end before trainers, services and retained entities are placed."""
 cx,cz=b.w//2,b.d//2
 fx,fz={'east':(1,0),'west':(-1,0),'north':(0,-1),'south':(0,1)}[b.front_direction]
 sx,sz=-fz,fx
 candidates=[]
 for depth in sorted(range(8,14),key=lambda d:abs(d-10)):
  x,z=cx-fx*depth,cz-fz*depth
  pad={(x+dx,z+dz) for dx in [-1,0,1] for dz in [-1,0,1]}
  back={(x-fx*3+sx*side,z-fz*3+sz*side) for side in range(-2,3)}
  if pad<=b.interior and not (pad|back)&b.supports:
   candidates.append((x,z,pad,back));break
 if not candidates:raise ValueError(('No clear axial leader stage',b.theme))
 x,z,pad,back=candidates[0];b.leader_position=(x,z)
 b.leader_reserved={(x+dx,z+dz) for dx in range(-3,4) for dz in range(-3,4)}|back
 b.leader_aisles=set()
 front_distance=(b.w-1-cx if fx>0 else cx) if fx else (b.d-1-cz if fz>0 else cz)
 for step in range(-front_distance,abs(x-cx)+abs(z-cz)+1):
  xx,zz=cx-fx*step,cz-fz*step
  for side in [-1,0,1]:
   at=xx+sx*side,zz+sz*side;b.leader_aisles.add(at)
   if at in b.interior and at not in b.wall_columns:
    if at in b.supports:raise ValueError(('Column crosses leader approach',b.theme,at))
    b.box(at[0],g+1,at[1],at[0],g+3,at[1],'air')
 for xx,zz in pad:
  b.put(xx,g,zz,color+'_concrete' if (xx,zz)!=(x,z) else 'gold_block')
  b.box(xx,g+1,zz,xx,g+3,zz,'air')
 panel={'poison':'purple_concrete','fighting':'white_terracotta','normal':'black_concrete','flying':'light_blue_stained_glass','bug':'honeycomb_block','psychic':'magenta_stained_glass','rock':'chiseled_sandstone','dark':'purple_stained_glass'}[b.theme]
 for side in range(-2,3):
  bx,bz=x-fx*3+sx*side,z-fz*3+sz*side
  for height in range(1,5):b.put(bx,g+height,bz,b.wall if abs(side)==2 else panel)
  b.put(bx,g+5,bz,b.roof)
  if abs(side)==2:b.put(bx,g+6,bz,b.light)
 b.put(x-fx*3,g+3,z-fz*3,'gold_block')
 b.data['BackportLeaderStage']=n.IntArray([x,g,z])
 b.data['BackportLeaderFront']=n.String(b.front_direction)
 b.data['BackportLeaderReserved']=n.IntArray([v for p in sorted(b.leader_reserved|b.leader_aisles) for v in p])

def road_landscaping(b,g,joints):
 """Wide native road footprints become boulevards with usable pocket gardens."""
 if min(b.w,b.d)<7:return
 protected=set();cx,cz=b.w//2,b.d//2
 for j in joints:
  x,_,z=map(int,j['pos']);protected.update((x+dx,z+dz) for dx in range(-2,3) for dz in range(-2,3))
  if not walk_socket(j.get('nbt',{})):continue
  for xx in range(min(x,cx),max(x,cx)+1):protected.update((xx,z+dz) for dz in [-1,0,1])
  for zz in range(min(z,cz),max(z,cz)+1):protected.update((cx+dx,zz) for dx in [-1,0,1])
 garden_count=0
 for x in range(3,b.w-3,10):
  for z in range(3,b.d-3,12):
   cells={(xx,zz) for xx in range(x-2,x+3) for zz in range(z-2,z+3)}
   if cells & protected:continue
   protected.update(cells)
   garden_count+=1
   b.box(x-2,g,z-2,x+2,g,z+2,'grass_block' if b.theme not in ['rock','dark'] else b.wall)
   for xx in range(x-2,x+3):b.put(xx,g,z-2,b.roof);b.put(xx,g,z+2,b.roof)
   for zz in range(z-2,z+3):b.put(x-2,g,zz,b.roof);b.put(x+2,g,zz,b.roof)
   wood='jungle' if b.theme=='bug' else 'dark_oak' if b.theme=='fighting' else 'crimson' if b.theme=='dark' else 'oak'
   for xx in range(x-1,x+2):b.put(xx,g+1,z+1,wood+'_stairs',{'facing':'south','half':'bottom','shape':'straight','waterlogged':'false'})
   plant='potted_fern' if b.theme in ['bug','poison'] else 'potted_allium' if b.theme=='psychic' else 'potted_cactus' if b.theme=='rock' else 'potted_poppy'
   b.put(x-1,g+1,z-1,plant);b.put(x+1,g+1,z-1,plant)
   # Lighting belongs at a garden edge; no forest of posts on a large slab.
   if garden_count%3==1:
    b.put(x+2,g,z-2,b.floor)
    b.box(x+2,g+1,z-2,x+2,g+2,z-2,b.wall);b.put(x+2,g+3,z-2,b.light)

def prepare_doorways(b,g,category,joints):
 """Plan a real entrance before putting appliances or trainers in the room."""
 b.planned_doors={};b.entrance_aisles=set()
 b.joint_columns={(int(j['pos'][0]),int(j['pos'][2])) for j in joints}
 if category not in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:return
 def name(x,y,z):
  old=b.blocks.get((x,y,z));return str(b.palette[int(old['state'])]['Name']) if old else 'minecraft:air'
 choices={}
 for joint in joints:
  tag=joint.get('nbt',{});ori=str(b.data['palette'][int(joint['state'])].get('Properties',{}).get('orientation',''))
  if not walk_socket(tag) or ori.startswith(('up_','down_')):continue
  face=ori.split('_')[0];dx,dz={'east':(-1,0),'west':(1,0),'north':(0,1),'south':(0,-1)}[face]
  sx,_,sz=map(int,joint['pos'])
  for offset in range(-3,4):
   for step in range(1,max(b.w,b.d)//2):
    x,z=sx+dx*step+dz*offset,sz+dz*step+dx*offset
    if not (2<=x<b.w-2 and 2<=z<b.d-2):
     if not (1<=x<b.w-1 and 1<=z<b.d-1):continue
    if (x,z) not in b.wall_columns:continue
    if b.interior is not None and ((x+dx*2,z+dz*2) not in b.interior or (x-dx,z-dz) in b.interior):continue
    if name(x,g+4,z)=='minecraft:air' or all(name(x,g+y,z)=='minecraft:air' for y in [1,2]):continue
    footprint={(x+dx*i,z+dz*i) for i in range(-2,3)}|{(x+dz,z+dx),(x-dz,z-dx)}
    footprint={(xx,zz) for xx,zz in footprint if 0<=xx<b.w and 0<=zz<b.d}
    if any((xx,zz) in b.supports for xx,zz in footprint):continue
    if {(x,z),(x+dz,z+dx),(x-dz,z-dx)}&b.joint_columns:continue
    if any('nbt' in b.blocks.get((xx,g+y,zz),{}) or name(xx,g+y,zz).endswith('_bed') for xx,zz in footprint for y in [1,2,3]):continue
    candidate=(abs(offset)*3+step,x,z,dx,dz)
    if face not in choices or candidate<choices[face]:choices[face]=candidate
    break
 for _,x,z,dx,dz in ([min(choices.values())] if choices else []):
  b.planned_doors[x,z]=(dx,dz)
  for step in range(-2,3):
   xx,zz=x+dx*step,z+dz*step
   b.box(xx,g+1,zz,xx,g+2,zz,'air');b.entrance_aisles.add((xx,zz))
  b.entrance_aisles.update([(x+dz,z+dx),(x-dz,z-dx)])
  b.put(x,g+3,z,b.roof)

def finish_doorways(b,g,category):
 """Dress actual facade crossings after routing; both halves are real wooden doors."""
 if category not in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:return
 def name(x,y,z):
  block=b.blocks.get((x,y,z));return str(b.palette[int(block['state'])]['Name']) if block else 'minecraft:air'
 def clear(x,z):
  return all(name(x,g+y,z)=='minecraft:air' or name(x,g+y,z)=='minecraft:jigsaw' and str(b.blocks[x,g+y,z].get('nbt',{}).get('final_state','minecraft:air')) in ['minecraft:air','minecraft:structure_void'] for y in [1,2])
 def wall(x,z):
  return all(name(x,g+y,z) not in ['minecraft:air','minecraft:jigsaw'] for y in [1,2])
 door={'normal':'spruce','fighting':'dark_oak','poison':'birch','flying':'birch','bug':'jungle','psychic':'dark_oak','rock':'acacia','dark':'crimson'}[b.theme]+'_door'
 count=0;selected=set()
 def blocks_existing_door(cells):
  if set(cells)&getattr(b,'joint_columns',set()):return True
  protected=set()
  for xx,zz in selected:
   facing=str(b.palette[int(b.blocks[xx,g+1,zz]['state'])]['Properties']['facing'])
   dx,dz=(0,1) if facing in ['north','south'] else (1,0)
   protected.update([(xx,zz),(xx-dx,zz-dz),(xx+dx,zz+dz)])
  return bool(set(cells)&protected)
 for (x,z),(dx,dz) in getattr(b,'planned_doors',{}).items():
  if not clear(x,z) or not clear(x-dx,z-dz) or not clear(x+dx,z+dz):raise ValueError(('Planned entrance obstructed',b.theme,category,x,z))
  for half,y in [('lower',g+1),('upper',g+2)]:b.put(x,y,z,door,{'half':half,'facing':'north' if dz else 'west','hinge':'left','open':'false','powered':'false'})
  for xx,zz in [(x+dz,z+dx),(x-dz,z-dx)]:b.box(xx,g+1,zz,xx,g+3,zz,b.wall)
  selected.add((x,z));count+=1
 for x in range(1,b.w-1):
  for z in range(1,b.d-1):
   if not clear(x,z) or (x,z) in selected or (x,g,z) not in b.blocks:continue
   # Only replace a genuine wall crossing, never an arena aisle or gate space.
   along_x=wall(x-1,z) and wall(x+1,z) and clear(x,z-1) and clear(x,z+1) and name(x,g+4,z)!='minecraft:air'
   along_z=wall(x,z-1) and wall(x,z+1) and clear(x-1,z) and clear(x+1,z) and name(x,g+4,z)!='minecraft:air'
   if not along_x and not along_z:continue
   if blocks_existing_door([(x,z)]+([(x-1,z),(x+1,z)] if along_x else [(x,z-1),(x,z+1)])):continue
   facing='north' if along_x else 'west'
   for half,y in [('lower',g+1),('upper',g+2)]:b.put(x,y,z,door,{'half':half,'facing':facing,'hinge':'left','open':'false','powered':'false'})
   b.put(x,g+3,z,b.roof)
   for xx,zz in [(x-1,z),(x+1,z)] if along_x else [(x,z-1),(x,z+1)]:
    b.box(xx,g+1,zz,xx,g+3,zz,b.wall)
   selected.add((x,z));count+=1
 # A two-wide native connector needs a real double door instead of a bare gap.
 for axis in ['x','z']:
  dx,dz=(1,0) if axis=='x' else (0,1)
  for x in range(1,b.w-2):
   for z in range(1,b.d-2):
    if not all(clear(x+dx*i,z+dz*i) and name(x+dx*i,g+4,z+dz*i)!='minecraft:air' for i in [0,1]):continue
    if not wall(x-dx,z-dz) or not wall(x+2*dx,z+2*dz):continue
    if not all(clear(x+dx*i+dz*side,z+dz*i+dx*side) for i in [0,1] for side in [-1,1]):continue
    if blocks_existing_door([(x+dx*i,z+dz*i) for i in [-1,0,1,2]]):continue
    for i in [0,1]:
     xx,zz=x+dx*i,z+dz*i
     for half,y in [('lower',g+1),('upper',g+2)]:b.put(xx,y,zz,door,{'half':half,'facing':'north' if axis=='x' else 'west','hinge':'left' if i==0 else 'right','open':'false','powered':'false'})
     b.put(xx,g+3,zz,b.roof);count+=1;selected.add((xx,zz))
 # Wide/asymmetric routed entrances receive a framed vestibule on the actual
 # connector axis. Keep appliances and spawn sockets out of its two jambs.
 if not selected and not (category=='gym' and b.theme=='flying'):
  candidates=[]
  for p,block in b.blocks.items():
   if name(*p)!='minecraft:jigsaw':continue
   tag=block.get('nbt',{});ori=str(b.palette[int(block['state'])].get('Properties',{}).get('orientation',''))
   if not walk_socket(tag) or ori.startswith(('up_','down_')):continue
   dx,dz={'east':(-1,0),'west':(1,0),'north':(0,1),'south':(0,-1)}[ori.split('_')[0]]
   for step in range(1,max(b.w,b.d)//2):
    x,z=p[0]+dx*step,p[2]+dz*step
    if not (1<=x<b.w-1 and 1<=z<b.d-1):continue
    if not clear(x,z) or not clear(x-dx,z-dz) or not clear(x+dx,z+dz) or name(x,g+4,z)=='minecraft:air':continue
    sides=[(x+dz,z+dx),(x-dz,z-dx)]
    if {(x,z),*sides}&getattr(b,'joint_columns',set()):continue
    if any((xx,zz) in b.supports or any('nbt' in b.blocks.get((xx,g+y,zz),{}) for y in [1,2,3]) for xx,zz in sides):continue
    candidates.append((step,x,z,dx,dz,sides));break
  if candidates:
   _,x,z,dx,dz,sides=min(candidates)
   for half,y in [('lower',g+1),('upper',g+2)]:b.put(x,y,z,door,{'half':half,'facing':'north' if dz else 'west','hinge':'left','open':'false','powered':'false'})
   b.put(x,g+3,z,b.roof)
   for xx,zz in sides:b.box(xx,g+1,zz,xx,g+3,zz,b.wall)
   selected.add((x,z));count+=1
 if not selected and not (category=='gym' and b.theme=='flying'):
  # A three-wide opening or a bend around interior furniture can put the
  # usable crossing off the incoming connector axis. Frame its clear lane.
  candidates=[]
  for x in range(1,b.w-1):
   for z in range(1,b.d-1):
    if not clear(x,z) or name(x,g+4,z)=='minecraft:air':continue
    for dx,dz in [(1,0),(0,1)]:
     if not clear(x-dx,z-dz) or not clear(x+dx,z+dz):continue
     sides=[(x+dz,z+dx),(x-dz,z-dx)]
     if {(x,z),*sides}&getattr(b,'joint_columns',set()):continue
     if not any(wall(x+dz*distance,z+dx*distance) for distance in [-2,-1,1,2]):continue
     if any((xx,zz) in b.supports or any('nbt' in b.blocks.get((xx,g+y,zz),{}) for y in [1,2,3]) for xx,zz in sides):continue
     candidates.append((min(x,z,b.w-1-x,b.d-1-z),x,z,dx,dz,sides))
  if candidates:
   _,x,z,dx,dz,sides=min(candidates)
   for half,y in [('lower',g+1),('upper',g+2)]:b.put(x,y,z,door,{'half':half,'facing':'north' if dz else 'west','hinge':'left','open':'false','powered':'false'})
   b.put(x,g+3,z,b.roof)
   for xx,zz in sides:b.box(xx,g+1,zz,xx,g+3,zz,b.wall)
   selected.add((x,z));count+=1
 # Thresholds meet the street on both sides of every doorway.
 for x,z in selected:
  block=b.blocks[x,g+1,z];facing=str(b.palette[int(block['state'])]['Properties']['facing'])
  dx,dz=(0,1) if facing=='north' else (1,0)
  for side in [-1,1]:
   if name(x+dx*side,g,z+dz*side)!='minecraft:jigsaw':b.put(x+dx*side,g,z+dz*side,b.floor)
 b.data['BackportDoorCount']=n.Int(count)

def building(b,g,category,variant):
 w,d=b.w,b.d;cx,cz=w//2,d//2;t=b.theme
 large=category!='house';height=(7 if large else 5)+(variant%3)*2
 b.roof_base=g+height
 b.box(0,g,0,w-1,g,d-1,b.floor)
 if t=='poison':
  # Offset clean-room laboratories and glass reactor conservatories.
  b.shell(1,1,w-2,d-2,g,height,'smooth_quartz','dark_prismarine')
  for z in range(2,d-2):b.put(1,g+height,z,b.glass)
  if variant%3==0:b.dome(cx,cz,max(2,min(w,d)//3),g+height+1)
  elif variant%3==1:
   for x in [3,w-4]:b.pillar(x,d-4,g+height+1,5)
  else:
   b.box(2,g+height+2,2,w-3,g+height+2,d//2,b.glass)
 elif t=='fighting':
  b.shell(1,2,w-2,d-3,g,height)
  for h in range(3):b.box(h,g+height+1+h,1+h,w-1-h,g+height+1+h,d-2-h,b.roof)
  for x in [1,w-2]:b.box(x,g,2,x,g+height,d-3,b.wall)
  b.arch(cx,0,g+1,max(2,w//4),4)
  if variant%2:b.circle(cx,5,2,g,'red_concrete')
 elif t=='flying':
  # White terrace villas, projecting glass balconies and light column arcades.
  b.shell(2,2,w-3,d-3,g,height)
  b.box(0,g+height+1,0,w-1,g+height+1,d-1,'smooth_quartz')
  for z in range(1,d-1):b.put(w-2,g+height+2,z,'glass_pane')
  for x in [1,w-2]:b.pillar(x,1,g,max(4,height-1))
  if variant%2:b.box(3,g+height+1,d//2,w-4,g+height+3,d-4,'air');b.box(3,g+height+1,d//2,w-4,g+height+1,d-4,'light_blue_stained_glass')
 elif t=='normal':
  # Townhouses vary between brick arcade, gabled timber home and flat market loft.
  b.shell(1,1,w-2,d-2,g,height,'bricks' if variant%2 else 'white_terracotta')
  for h in range(max(2,w//3)):
   if variant%3:b.box(h,g+height+1+h,0,w-1-h,g+height+1+h,d-1,b.roof)
  if variant%3==0: b.box(1,g+height+2,1,w-2,g+height+2,d-2,'oak_fence')
 elif t=='bug':
  # An elliptical pod uses the full native lot instead of stranding fixtures
  # outside a small circle in a long rectangular footprint.
  rx=max(2,w/2-1.25);rz=max(2,d/2-1.25);r=int(min(rx,rz))
  b.interior={(x,z) for x in range(w) for z in range(d) if ((x-cx)/max(1,rx-.85))**2+((z-cz)/max(1,rz-.85))**2<1}
  # Dilate the discrete room footprint; a continuous ellipse equation alone
  # leaves one-block gaps where the inner and outer curves round differently.
  rim={(x+dx,z+dz) for x,z in b.interior for dx in [-1,0,1] for dz in [-1,0,1] if 0<=x+dx<w and 0<=z+dz<d}-b.interior
  b.wall_columns.update(rim)
  for h in range(1,height+1):
   for x,z in rim:b.put(x,g+h,z,b.glass if h%3 else b.wall)
  for h in range(r+1):
   scale=math.sqrt(max(0,1-(h/max(1,r))**2));rrx=max(0,round(rx*scale));rrz=max(0,round(rz*scale))
   for x in range(cx-rrx,cx+rrx+1):
    for z in range(cz-rrz,cz+rrz+1):
     q=((x-cx)/max(1,rrx))**2+((z-cz)/max(1,rrz))**2
     inner=((x-cx)/max(.5,rrx-1.5))**2+((z-cz)/max(.5,rrz-1.5))**2
     if q<=1 and (inner>=1 or h>=r-1):b.put(x,g+height+h,z,b.wall if h%3==0 else b.glass)
  if variant%2:
   b.circle(cx,cz,r,g+height+1,'honeycomb_block')
   for x in [cx-r,cx+r]:b.put(x,g+height+2,cz,'yellow_wool')
  for x in [1,w-2]:
   for z in [1,d-2]:
    b.box(x,g+1,z,x,g+height,z,'jungle_log');b.circle(x,z,2,g+height,'jungle_leaves',True);b.supports.add((x,z))
 elif t=='psychic':
  # Observatory pods alternate with tall astronomical libraries.
  b.shell(1,1,w-2,d-2,g,height,'smooth_quartz','purpur_block')
  if variant%2:b.dome(cx,cz,max(2,min(w,d)//2-2),g+height+1)
  else:b.pillar(cx,cz,g+height+1,6)
  for x in range(2,w-1,4):b.put(x,g+height+2,1,'end_rod')
 elif t=='rock':
  # Stepped adobe homes and stone-cut workshop terraces.
  b.shell(1,1,w-2,d-2,g,height,'smooth_sandstone','cut_sandstone')
  for h in range(3):b.box(1+h,g+height+1+h,1+h,w-2-h,g+height+1+h,d-2-h,'orange_terracotta' if h%2 else 'cut_sandstone')
  b.trim(1,1,w-2,d-2,g,height)
  # Window bands and a shaded arcade make terraces read as inhabited buildings.
  for z in range(3,d-2,4):
   b.box(1,g+2,z,1,g+3,min(d-3,z+1),b.glass)
   b.box(w-2,g+2,z,w-2,g+3,min(d-3,z+1),b.glass)
  b.box(0,g+4,0,w-1,g+4,2,'cut_sandstone')
  for x in range(1,w-1,4):b.box(x,g+1,0,x,g+3,0,'sandstone')
  if variant%2:
   for x in range(3,w-2,4):b.pillar(x,d-3,g+height+1,3)
 elif t=='dark':
  b.shell(1,1,w-2,d-2,g,height,'polished_blackstone_bricks','crimson_planks')
  for h in range(max(3,w//3)):b.box(h,g+height+1+h,0,w-1-h,g+height+1+h,d-1,'blackstone')
  if variant%2:b.pillar(2,d-3,g,height+6)
 # Furnish only after actual entrance, NPC and service positions are known.
 # Rectangular corner coordinates are not room coordinates in a greenhouse.
 if category in ['pokecenter','pokemart']:
  block='red_concrete' if category=='pokecenter' else 'blue_concrete'
  sign_z=max(1,cz-max(2,min(w,d)//2-2)) if t=='bug' else 0
  b.box(cx-2,g+height+3,sign_z,cx+2,g+height+3,sign_z,block)
  b.box(cx,g+height+1,sign_z,cx,g+height+5,sign_z,block)

def furnish_rooms(b,g,category):
 """Place complete furniture on clear indoor footprints after real services."""
 if category not in ['house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop'] or not b.interior:return
 reserved=set(b.service_aisles)|b.supports|b.entrance_aisles
 middle=(sum(p[0] for p in b.interior)/len(b.interior),sum(p[1] for p in b.interior)/len(b.interior))
 entry=next(iter(b.planned_doors.values()),(0,1));ex,ez=entry;sx,sz=ez,-ex
 span=max(1,min(b.w,b.d)//4)
 def zone(side,depth):return (middle[0]+span*(sx*side+ex*depth),middle[1]+span*(sz*side+ez*depth))
 def clear(p):
  return p in b.interior and p not in reserved and all((p[0],g+h,p[1]) not in b.blocks or str(b.palette[int(b.blocks[p[0],g+h,p[1]]['state'])]['Name'])=='minecraft:air' for h in [1,2,3])
 def place(parts,near=None,preferred=None,turns=range(4)):
  candidates=[]
  for x,z in sorted(b.interior):
   for turn in turns:
    rotated=[]
    for dx,dy,dz,s,props in parts:
     props=dict(props) if props else None
     for i in range(turn):
      dx,dz=-dz,dx
      if props and props.get('facing') in ['north','east','south','west']:
       props['facing']={'north':'east','east':'south','south':'west','west':'north'}[props['facing']]
     rotated.append((dx,dy,dz,s,props))
    cells={(x+dx,z+dz) for dx,dy,dz,s,props in rotated}
    if not all(clear(p) for p in cells):continue
    neighbors={(xx+dx,zz+dz) for xx,zz in cells for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]}-cells
    if not any(clear(p) for p in neighbors):continue
    edge=min(abs(x-wx)+abs(z-wz) for wx,wz in b.wall_columns) if b.wall_columns else 0
    distance=abs(x-near[0])+abs(z-near[1]) if near else 0
    if near and distance>4:continue
    preference=abs(x-preferred[0])+abs(z-preferred[1]) if preferred else 0
    candidates.append((distance,preference,edge,x,z,turn,cells,rotated))
  if not candidates:return False
  _,_,_,x,z,turn,cells,rotated=min(candidates,key=lambda c:c[:6])
  for dx,dy,dz,s,props in rotated:b.put(x+dx,g+dy,z+dz,s,props)
  reserved.update(cells)
  # Keep an interaction/seating aisle available for later furniture clusters.
  reserved.update((xx+dx,zz+dz) for xx,zz in cells for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)])
  return True
 wood={'bug':'jungle','dark':'crimson','fighting':'dark_oak','flying':'birch'}.get(b.theme,'oak')
 stair={'facing':'south','half':'bottom','shape':'straight','waterlogged':'false'}
 plant={'bug':'potted_fern','dark':'potted_wither_rose','psychic':'potted_allium','rock':'potted_cactus','poison':'potted_bamboo'}.get(b.theme,'potted_white_tulip')
 if category=='house':
  bed=[(0,1,0,'white_bed',{'facing':'south','part':'foot','occupied':'false'}),(0,1,1,'white_bed',{'facing':'south','part':'head','occupied':'false'})]
  if not place(bed,preferred=zone(-1,1)):raise ValueError(('No complete indoor bed footprint',b.theme,b.w,b.d))
 if category=='pokecenter':
  # A low reception desk beside the nurse, plus several waiting seats.
  for nurse in b.nurse_positions:
   preferred=(nurse[0]-ex*2,nurse[1]-ez*2)
   counter=[(i,1,0,wood+'_slab',{'type':'bottom','waterlogged':'false'}) for i in range(3)]
   place(counter,nurse,preferred,turns=[0,2] if ez else [1,3])
   for x,z in b.interior:
    if abs(x-nurse[0])<=3 and abs(z-nurse[1])<=3 and str(b.palette[int(b.blocks[x,g,z]['state'])]['Name'])!='minecraft:jigsaw':b.put(x,g,z,'white_concrete')
  # Two coherent rows flank the approach aisle instead of scattered seats.
  for side in [-1,1]:
   waiting=zone(side,-1)
   row=[(i,1,0,wood+'_stairs',stair) for i in range(3)]
   if not place(row,preferred=waiting,turns=[0,2] if ez else [1,3]):place(row[:2],preferred=waiting)
   for x,z in b.interior:
    if abs(x-waiting[0])<=2 and abs(z-waiting[1])<=2 and str(b.palette[int(b.blocks[x,g,z]['state'])]['Name'])!='minecraft:jigsaw':b.put(x,g,z,'light_gray_concrete')
 else:
  # Compact sitting group: chairs flank a low table in one complete footprint.
  group=[(0,1,0,wood+'_stairs',stair),(1,1,0,wood+'_slab',{'type':'bottom','waterlogged':'false'}),(2,1,0,wood+'_stairs',stair)]
  if not place(group,preferred=zone(1,-1)):place([(0,1,0,wood+'_stairs',stair)],preferred=zone(1,-1))
 reading=[(0,1,0,'bookshelf',None),(0,2,0,plant,None),(1,1,0,'lectern',{'facing':'south','has_book':'false','powered':'false'})]
 if not place(reading,preferred=zone(1,1)):place(reading[:2],preferred=zone(1,1))
 if category in ['house','blacksmith']:place([(0,1,0,'crafting_table',None)],preferred=zone(-1,-1))
 # Every service building has an indoor light on a supported reading stand.
 place([(0,1,0,wood+'_planks',None),(0,2,0,b.light,None)],preferred=zone(1,1))
 b.data['BackportFurnitureCells']=n.Int(len(reserved-set(b.service_aisles)-b.supports-b.entrance_aisles))

def rock_temple(b,g=0):
    from vanilla_rock_temple import install_into_builder
    return install_into_builder(b,g)


def outdoors(b,g,category,variant,joints):
 cx,cz=b.w//2,b.d//2;t=b.theme
 if category=='tree':
  b.box(0,g,0,b.w-1,g,b.d-1,'grass_block' if t not in ['rock','dark'] else b.floor)
  height=4+variant%4;r=max(1,min(b.w,b.d)//2-1)
  wood='dark_oak_log' if t in ['dark','poison'] else 'jungle_log' if t=='bug' else 'birch_log' if t=='flying' else 'oak_log'
  if t=='psychic':
   b.pillar(cx,cz,g,height);b.circle(cx,cz,r,g+height+1,b.glass);return
  b.box(cx,g+1,cz,cx,g+height,cz,wood)
  for y in range(height-2,height+2):b.circle(cx,cz,max(1,r-abs(y-height)//2),g+y,'jungle_leaves' if t=='bug' else 'dark_oak_leaves' if t in ['dark','poison'] else 'oak_leaves',True)
  b.circle(cx,cz,max(1,r-1),g+height+2,'oak_leaves',True)
  if t=='bug':b.put(cx,g+height+1,cz,'bee_nest',{'facing':'north'})
  return
 if category=='road':
  # Legacy footprints include side sockets. A narrow centerline with grass
  # between sockets left jagged holes and gaps at the paired one-block joints.
  # Pave the complete street module; all sockets share a supported elevation.
  b.box(0,g,0,b.w-1,g,b.d-1,b.floor)
  for x in range(b.w):
   for z in range(b.d):
    if x%6==0 and z%6==0:b.put(x,g,z,b.roof)
  road_landscaping(b,g,joints)
  return
 b.box(0,g,0,b.w-1,g,b.d-1,b.floor if category in ['plaza','street_art'] else 'grass_block')
 if category=='plaza':
  for j in joints:
   x,y,z=map(int,j['pos']);b.line(x,z,cx,cz,g,b.floor,5)
  r=max(2,min(b.w,b.d)//4)
  b.circle(cx,cz,r,g+1,b.wall)
  if t in ['poison','bug']:b.dome(cx,cz,r,g+2)
  elif t=='flying':
   for dx in range(-r,r+1):b.put(cx+dx,g+8+abs(dx)//2,cz,b.roof)
   for x in [cx-r,cx+r]:b.pillar(x,cz,g,8+r//2)
  elif t=='psychic':
   b.pillar(cx,cz,g,14);b.circle(cx,cz,r,g+10,b.glass)
   b.line(cx-r,cz,cx+r,cz,g+10,b.roof);b.line(cx,cz-r,cx,cz+r,g+10,b.roof)
  elif t=='rock':
   # Install the complete native landmark after services/connectors have been
   # retained, so they can be moved clear of the larger original footprint.
   pass
  else:b.pillar(cx,cz,g,12 if t=='dark' else 8)
  b.reward(2,g+1,max(2,b.d-3),'ultra',True)
 elif category=='garden' and b.w>=5 and b.d>=5:
  # Different small public spaces replace the repeated wheat-field module.
  if t=='poison':b.dome(cx,cz,max(2,min(b.w,b.d)//2-1),g+1)
  elif t=='fighting':b.arch(cx,cz,g+1,max(1,b.w//3),4)
  elif t=='flying':b.pillar(cx,cz,g,5);b.put(cx,g+6,cz,'white_banner',{'rotation':'0'})
  elif t=='normal':
   b.box(1,g+3,1,b.w-2,g+3,b.d-2,'white_wool');b.box(1,g+1,1,1,g+2,b.d-2,'oak_fence')
  elif t=='bug':
   for x in range(1,b.w-1,2):
    for z in range(1,b.d-1,2):b.put(x,g+1,z,'sunflower',{'half':'lower'});b.put(x,g+2,z,'sunflower',{'half':'upper'})
  elif t=='psychic':b.pillar(cx,cz,g,5);b.circle(cx,cz,2,g+6,b.glass)
  elif t=='rock':
   # Complete geological display rather than a low, unfinished rectangular wall.
   b.box(1,g,1,b.w-2,g,b.d-2,'polished_andesite')
   for x,z,h in [(cx,cz,3),(max(1,cx-2),cz,2),(cx,min(b.d-2,cz+2),1)]:
    b.box(x,g+1,z,x,g+h,z,'granite');b.put(x,g+h+1,z,'gold_ore')
   for x in [1,b.w-2]:b.put(x,g+1,1,b.light)
  else:b.arch(cx,cz,g+1,max(1,b.w//3),4)
 else:
  # Small native clutter lots become finished street furniture, not repeated
  # oversized posts on almost every road frontage.
  if min(b.w,b.d)>=5:
   b.box(1,g,1,b.w-2,g,b.d-2,b.floor)
   for x in range(1,b.w-1):b.put(x,g+1,b.d-2,b.roof)
   for x in [1,b.w-2]:b.put(x,g+2,b.d-2,b.wall)
   b.put(cx,g+1,1,'potted_fern' if t in ['bug','poison'] else 'potted_cactus' if t=='rock' else 'potted_poppy')
  else:
   b.box(cx,g+1,cz,cx,g+3,cz,b.wall);b.put(cx,g+4,cz,b.light)

def rebuild(data,theme,filename):
 category=kind(filename,theme)
 if category=='npc':return data
 original=copy.deepcopy(data);joints=connectors(original);b=Builder(data,theme)
 b.category=category
 external=[j for j in joints if str(j['nbt'].get('name',''))!='minecraft:empty' and not str(j['nbt'].get('pool','')).startswith(('pixelmon_backport:npc/','minecraft:village/'))]
 # Native decoration sockets sometimes sit 7-9 blocks above their foundation.
 # Those heights belonged to the removed native geometry. Reusing them lifted
 # replacement lots above roads; use one authored ground plane for new pieces.
 ground=0
 variant=int(hashlib.sha256(filename.encode()).hexdigest()[:6],16)
 if category=='gym':
  approaches=[]
  for joint in joints:
   orientation=str(original['palette'][int(joint['state'])].get('Properties',{}).get('orientation','')).split('_')[0]
   if orientation in ['east','west','north','south'] and walk_socket(joint.get('nbt',{})):approaches.append((tuple(map(int,joint['pos'])),orientation))
  if not approaches:raise ValueError(('Gym lacks authored horizontal street approach',theme))
  b.front_direction=min(approaches)[1]
  gym(b,ground)
 elif category in ['road','plaza','garden','street_art','tree']:outdoors(b,ground,category,variant,joints)
 else:building(b,ground,category,variant)
 b.decorating_room=category in ['house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']
 themed_details(b,ground,category,variant)
 b.decorating_room=False
 prepare_doorways(b,ground,category,joints)
 retain(b,original,joints,ground,category)
 furnish_rooms(b,ground,category)
 # Service placement can occupy a decorative reward corner. Add guaranteed
 # exploration rewards only after all fixtures, on free supported floor tiles.
 needed=2 if category=='gym' else 1 if category in ['plaza','pokecenter','pokemart','church','blacksmith'] else 0
 existing=sum(str(b.palette[int(block['state'])]['Name']).startswith('pixelmon:') and str(b.palette[int(block['state'])]['Name']).endswith('_chest') for block in b.blocks.values())
 candidates=[(x,z) for x in range(2,b.w-2) for z in range(2,b.d-2) if abs(x-b.w//2)>2 and abs(z-b.d//2)>2]
 for x,z in candidates:
  if existing>=needed:break
  floor=b.blocks.get((x,ground,z));air=b.blocks.get((x,ground+1,z))
  if (b.interior is None or (x,z) in b.interior) and (x,z) not in b.service_aisles|b.supports|b.entrance_aisles and floor and (air is None or str(b.palette[int(air['state'])]['Name'])=='minecraft:air'):
   b.reward(x,ground+1,z,'ultra' if category=='gym' else 'poke',existing>0);existing+=1
 # Restore the modified/repositioned service entities into the generated template.
 data['entities']=original.get('entities',n.List[n.Compound]())
 ensure_service_routes(b,ground)
 finish_doorways(b,ground,category)
 if category in ['house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop'] and b.interior:
  # Verify the whole indoor footprint independently of furniture and walkability.
  # Low furniture/beams cannot masquerade as the final enclosing roof.
  roof_base=b.roof_base
  if theme=='bug':
   # One connected glass ceiling closes the discrete dome's inner edge gaps;
   # isolated patch squares otherwise float below its curved shell.
   for x,z in b.interior:b.put(x,roof_base,z,b.glass)
  roof_columns={(xx,zz) for (xx,yy,zz),block in b.blocks.items() if yy>=roof_base and str(b.palette[int(block['state'])]['Name']) not in ['minecraft:air','minecraft:structure_void'] and not any(token in str(b.palette[int(block['state'])]['Name']) for token in ['lantern','torch','chain','end_rod','_fence','_pane','leaves'])}
  for x,z in b.interior-roof_columns:b.put(x,roof_base+1,z,b.glass if theme=='bug' else b.roof)
  data['BackportRoofBase']=n.Int(roof_base)
  data['BackportRoofFootprint']=n.IntArray([v for p in sorted(b.interior) for v in p])
 result=b.finish()
 if theme=='rock' and category=='plaza':
  from vanilla_rock_temple import install
  return install(result)
 return result

def themed_details(b,g,category,variant):
 """Theme-specific facades, rooms and small public spaces for all eight cities.

 Add before native services and final entrance carving so decorations cannot
 replace a healer, chest, trainer socket or the required approach corridor.
 """
 if category in ['road','tree']:return
 w,d=b.w,b.d;cx,cz=w//2,d//2;t=b.theme
 indoors=category in ['house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']
 if indoors and min(w,d)>=9:
  # Furnished side rooms leave the main cross-corridor clear.
  if t=='poison':
   for x in range(3,w-3,5):
    b.put(x,g+1,d-3,'cauldron',{'level':'0'})
    b.put(x,g+2,d-2,'lime_stained_glass')
   for z in range(4,d-3,6):
    b.box(1,g+4,z,1,g+6,z,'dark_prismarine');b.put(1,g+7,z,'sea_lantern')
   b.box(2,g+4,1,w-3,g+4,1,'dark_prismarine')
  elif t=='fighting':
   # Practice stations and red-white timber facade bands.
   for x in range(3,w-3,6):
    b.box(x,g+1,d-3,x,g+2,d-3,'red_wool');b.put(x,g+3,d-3,'black_wool')
   for z in [2,d-3]:
    for x in range(2,w-2,4):b.put(x,g+5,z,'white_terracotta')
   b.box(2,g+1,2,4,g+1,2,'dark_oak_slab',{'type':'bottom','waterlogged':'false'})
  elif t=='flying':
   # Open colonnade balconies with a continuous parapet, rather than bare slabs.
   for x in [1,w-2]:
    for z in range(3,d-3,5):
     b.box(x,g+1,z,x,g+5,z,'quartz_pillar');b.put(x,g+6,z,'sea_lantern')
   b.box(2,g+5,1,w-3,g+5,1,'smooth_quartz')
   for x in range(2,w-2):b.put(x,g+6,1,'light_blue_stained_glass_pane')
   for x in [3,w-4]:b.put(x,g+1,d-3,'potted_white_tulip')
  elif t=='normal':
   # Alternating shopfront awnings and domestic reading/seating corners.
   for x in range(2,w-2):b.put(x,g+4,0,'red_wool' if (x//2+variant)%2 else 'white_wool')
   for x in range(2,w-2,5):
    b.put(x,g+1,d-3,'bookshelf');b.put(x,g+2,d-3,'potted_poppy')
   if category!='house':
    b.box(cx-2,g+7,1,cx+2,g+11,1,'white_concrete')
    b.box(cx,g+8,0,cx,g+10,0,'black_concrete');b.put(cx+1,g+9,0,'black_concrete')
  elif t=='bug':
   # Living timber supports, hanging foliage and honeycomb work benches.
   for x in [1,w-2]:
    for z in range(2,d-2,5):
     if (x,z) not in b.interior:continue
     b.box(x,g+1,z,x,g+3,z,'jungle_log');b.supports.add((x,z));b.put(x,g+4,z,'jungle_leaves',{'persistent':'true','distance':'1'})
   for x in range(3,w-3,5):
    b.put(x,g+1,d-3,'honeycomb_block');b.put(x,g+2,d-3,'potted_fern')
  elif t=='psychic':
   # Observatory libraries and an explicit constellation pattern on the facade.
   for x in range(3,w-3,5):b.box(x,g+1,d-3,x,g+2,d-3,'bookshelf')
   for x in range(2,w-2,4):
    b.put(x,g+5,1,'sea_lantern');b.put(x,g+6,1,'purple_stained_glass')
   for z in [3,d-4]:b.put(2,g+1,z,'lectern',{'facing':'east','has_book':'false','powered':'false'})
  elif t=='rock':
   for x in range(3,w-3,6):
    b.put(x,g+1,d-3,'polished_andesite');b.put(x,g+2,d-3,'bone_block')
   for z in range(3,d-3,5):b.put(w-2,g+5,z,'chiseled_sandstone')
  elif t=='dark':
   # Lantern galleries and a repeating recessed crimson facade motif.
   for x in range(2,w-2,4):
    b.box(x,g+4,1,x,g+7,1,'chiseled_polished_blackstone');b.put(x,g+8,1,'soul_lantern')
   for x in range(3,w-3,5):
    b.put(x,g+1,d-3,'crimson_slab',{'type':'bottom','waterlogged':'false'})
    b.put(x,g+2,d-2,'purple_stained_glass')
 elif category in ['plaza','garden','street_art'] and min(w,d)>=7:
  # Completed lot edges, seats and lighting around each original central feature.
  for x in range(1,w-1):b.put(x,g,1,b.roof);b.put(x,g,d-2,b.roof)
  for z in range(1,d-1):b.put(1,g,z,b.roof);b.put(w-2,g,z,b.roof)
  for x in [1,w-2]:
   b.put(x,g+1,d-2,b.wall);b.put(x,g+2,d-2,b.light)
  b.box(2,g+1,d-2,min(w-3,4),g+1,d-2,b.roof)
  plants={'poison':'potted_bamboo','fighting':'potted_oak_sapling','flying':'potted_white_tulip','normal':'potted_poppy','bug':'potted_fern','psychic':'potted_allium','rock':'potted_cactus','dark':'potted_wither_rose'}
  plant=plants[t]
  b.put(2,g+1,2,plant);b.put(w-3,g+1,2,plant)

def ensure_service_routes(b,g):
 """Open a real approach to preserved appliances without removing any fixture.

 The old service cluster coordinates may face a newly authored interior wall.
 Find an inexpensive corridor through decoration, keeping tagged appliances,
 reward boxes, beds and connector positions intact.
 """
 protected=set();targets=[]
 for p,block in b.blocks.items():
  s=str(b.palette[int(block['state'])]['Name'])
  functional=s.startswith('pixelmon:') and (s.endswith(('_pc','_chest')) or any(word in s for word in ['healer','trade_machine','fossil_cleaner','fossil_machine','anvil','vending_machine']))
  if functional or 'nbt' in block and s!='minecraft:jigsaw' or s.endswith(('_bed','_log')):
   if p[1] in [g+1,g+2]:protected.add((p[0],p[2]))
   if p[1]==g+1 and (functional or 'nbt' in block):targets.append((p[0],p[2]))
  if s=='minecraft:jigsaw':
   tag=block['nbt'];ori=str(b.palette[int(block['state'])]['Properties']['orientation'])
   if not ori.startswith(('up_','down_')) and b.category not in ['tree','garden','street_art'] and walk_socket(tag) and str(tag.get('final_state'))=='minecraft:air' or str(tag.get('pool','')).startswith(('pixelmon:npc/','pixelmon_backport:npc/','minecraft:village/')):
    targets.append((p[0],p[2]))
 protected.update(b.supports)
 protected.update(b.wall_columns-b.entrance_aisles)
 def clear(x,z):
  for y in [g+1,g+2]:
   old=b.blocks.get((x,y,z))
   if old:
    s=str(b.palette[int(old['state'])]['Name'])
    if s!='minecraft:air' and not (s=='minecraft:jigsaw' and str(old['nbt'].get('final_state'))=='minecraft:air'):return False
  return True
 if b.category in ['gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']:
  # Incoming sockets can have an empty child pool. They still need an entrance
  # into the room; merely connecting NPCs inside a sealed shell is insufficient.
  room=[(x,z) for x in range(2,b.w-2) for z in range(2,b.d-2) if (b.interior is None or (x,z) in b.interior) and (x,z) not in protected and clear(x,z)]
  if not room:raise ValueError(('No interior circulation space',b.theme,b.category))
  # Connect the actual room first. Doing appliances first cuts separate holes
  # through the facade right in front of PCs and vending machines.
  targets.insert(0,min(room,key=lambda p:abs(p[0]-b.w//2)+abs(p[1]-b.d//2)))
 def component():
  candidates=[(x,z) for x in range(b.w) for z in range(b.d) if (x,z) not in protected and clear(x,z) and (x,g,z) in b.blocks]
  if not candidates:return set()
  entries=[p for p in targets if p in candidates and p not in protected]
  start=min(entries or candidates,key=lambda p:(min(p[0],p[1],b.w-1-p[0],b.d-1-p[1]),abs(p[0]-b.w//2)+abs(p[1]-b.d//2)))
  seen={start};q=collections.deque([start])
  while q:
   x,z=q.popleft()
   for p in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
    if 0<=p[0]<b.w and 0<=p[1]<b.d and p not in seen and p not in protected and clear(*p):seen.add(p);q.append(p)
  return seen
 for tx,tz in targets:
  reachable=component();ends=({(tx-1,tz),(tx+1,tz),(tx,tz-1),(tx,tz+1)} if (tx,tz) in protected else {(tx,tz)})-protected
  ends={p for p in ends if 0<=p[0]<b.w and 0<=p[1]<b.d}
  if ends & reachable:continue
  if not reachable or not ends:raise ValueError(('No service approach',b.theme,tx,tz))
  q=[(0,p) for p in reachable];heapq.heapify(q);cost={p:0 for p in reachable};previous={};found=None
  while q:
   distance,p=heapq.heappop(q)
   if distance!=cost[p]:continue
   if p in ends:found=p;break
   x,z=p
   for nxt in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
    if not (0<=nxt[0]<b.w and 0<=nxt[1]<b.d) or nxt in protected:continue
    price=distance+(1 if clear(*nxt) else 5)
    if price<cost.get(nxt,10**9):cost[nxt]=price;previous[nxt]=p;heapq.heappush(q,(price,nxt))
  if found is None:raise ValueError(('Enclosed service',b.theme,tx,tz))
  while found not in reachable:
   x,z=found
   for y in [g+1,g+2,g+3]:
    old=b.blocks.get((x,y,z))
    if old is None or str(b.palette[int(old['state'])]['Name'])!='minecraft:jigsaw':b.put(x,y,z,'air')
   old=b.blocks.get((x,g,z))
   if old is None or str(b.palette[int(old['state'])]['Name'])!='minecraft:jigsaw':b.put(x,g,z,b.floor)
   found=previous[found]
