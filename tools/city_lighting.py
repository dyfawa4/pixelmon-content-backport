"""Supported interior lamps and visible gym portals, without changing gameplay NBT.

Night coverage uses conservative six-neighbour block-light attenuation. Skylight
is deliberately omitted, so windows and daytime do not mask a dark room.
"""
import collections,copy,re
import nbtlib as n

AIR={'minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'}
DIRECTIONS={'north':(0,0,-1),'south':(0,0,1),'west':(-1,0,0),'east':(1,0,0)}
ROOMS={'gym','house','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop'}
LIGHTS={'lantern':15,'soul_lantern':10,'glowstone':15,'sea_lantern':15,'shroomlight':15,'end_rod':14,'torch':14,'wall_torch':14,'soul_torch':10,'soul_wall_torch':10,'jack_o_lantern':15,'beacon':15,'lava':15,'fire':15}
COOL={'electric','steel','water','ice','flying','psychic','poison','fairy'}

class Voxels:
 def __init__(self,data):
  self.data=data;self.w,self.h,self.d=map(int,data['size']);self.vox={tuple(map(int,b['pos'])):b for b in data['blocks']};self.palette=data['palette'];self.ids={}
  for i,s in enumerate(self.palette):self.ids[str(s['Name']),tuple(sorted((str(k),str(v)) for k,v in s.get('Properties',{}).items()))]=i
  self.nonwalk_floor={tuple(map(int,p)) for p in data.get('BackportNonWalkFixtures',[])}
 def name(self,p):
  b=self.vox.get(p)
  if b is None:return 'minecraft:air'
  name=str(self.palette[int(b['state'])]['Name'])
  return str(b.get('nbt',{}).get('final_state','minecraft:air')).split('[')[0] if name=='minecraft:jigsaw' else name
 def props(self,p):
  b=self.vox.get(p);return {} if b is None else dict(self.palette[int(b['state'])].get('Properties',{}))
 def put(self,p,name,props=None):
  x,y,z=p
  if not(0<=x<self.w and 0<=z<self.d and y>=0):return False
  if p in self.vox and 'nbt' in self.vox[p]:return False
  name=name if ':' in name else 'minecraft:'+name;key=name,tuple(sorted((props or {}).items()))
  if key not in self.ids:
   state=n.Compound({'Name':n.String(name)})
   if props:state['Properties']=n.Compound({k:n.String(v) for k,v in props.items()})
   self.ids[key]=len(self.palette);self.palette.append(state)
  self.vox[p]=n.Compound({'pos':n.List[n.Int](p),'state':n.Int(self.ids[key])});self.h=max(self.h,y+1);return True
 def finish(self):
  # Newly framed entrances can alter adjacent pane/fence connections.
  from city_architecture import connect_neighbors
  self.blocks=self.vox
  self.state=lambda name,props=None:self._state(name,props)
  connect_neighbors(self)
  self.data['blocks']=n.List[n.Compound](self.vox[p] for p in sorted(self.vox,key=lambda p:(p[1],p[0],p[2])))
  self.data['size']=n.List[n.Int]([self.w,self.h,self.d])
  return self.data
 def _state(self,name,props=None):
  scratch=(-1,0,-1);name=name if ':' in name else 'minecraft:'+name;key=name,tuple(sorted((props or {}).items()))
  if key not in self.ids:
   state=n.Compound({'Name':n.String(name)})
   if props:state['Properties']=n.Compound({k:n.String(v) for k,v in props.items()})
   self.ids[key]=len(self.palette);self.palette.append(state)
  return self.ids[key]

def transmitting(name):
 return name in AIR or any(t in name for t in ['glass','lantern','torch','chain','end_rod','_carpet','potted_','_door','fence','_wall','_stairs','_slab','iron_bars','leaves','vine','ladder','banner','sapling','flower','lectern','_bed','rail','button','pressure_plate'])

def walk_air(name):
 return name in AIR or any(t in name for t in ['_carpet','potted_','_door','torch','vine','ladder','banner','flower','sapling','rail','button','pressure_plate'])

def floor(name):
 if name in ['minecraft:sea_lantern','minecraft:glowstone','minecraft:shroomlight']:return True
 if name.startswith('pixelmon:') and name.endswith(('_planks','_log','_wood')):return True
 return name.startswith(('pixelmon:temple_','pixelmon:ultra_')) and not transmitting(name) or ('_slab' in name or '_stairs' in name or 'glass' in name and 'pane' not in name) or name not in AIR and not transmitting(name) and not name.startswith('pixelmon:') and name not in ['minecraft:water','minecraft:lava']

def ceiling(v,p):
 x,y,z=p
 return next((yy for yy in range(y+2,v.h) if floor(v.name((x,yy,z))) or 'glass' in v.name((x,yy,z))),None)

def walking(v,category):
 mask=v.data.get('BackportInterior');inside=None if mask is None else set(zip(map(int,mask[::2]),map(int,mask[1::2])))
 candidates=[]
 for (x,y,z) in v.vox:
  if (x,y,z) in v.nonwalk_floor:continue
  if 'BackportRoofBase' in v.data and y>=int(v.data['BackportRoofBase']):continue
  if not floor(v.name((x,y,z))) or not walk_air(v.name((x,y+1,z))) or not walk_air(v.name((x,y+2,z))):continue
  if inside is not None and (x,z) not in inside:continue
  p=(x,y+1,z)
  if ceiling(v,p) is not None:candidates.append(p)
  elif category=='gym':
   if any(t in v.name((x,y,z)) for t in ['_slab','_stairs']):continue
   # Open courts still need night lighting; exclude roofs and stacked balconies.
   if not any(floor(v.name((x,yy,z))) and walk_air(v.name((x,yy+1,z))) and walk_air(v.name((x,yy+2,z))) for yy in range(y)):
    if 1<=x<v.w-1 and 1<=z<v.d-1:candidates.append(p)
 return sorted(set(candidates))

def luminosity(v,p):
 name=v.name(p).split(':')[-1];props=v.props(p)
 if name in ['redstone_lamp','campfire','soul_campfire','furnace','smoker','blast_furnace'] and str(props.get('lit','false'))=='true':return 10 if name=='soul_campfire' else 15 if name in ['redstone_lamp','campfire'] else 13
 return LIGHTS.get(name,0)

def propagate(v,levels,sources):
 queue=collections.deque()
 for p in sources:
  level=luminosity(v,p)
  if level>levels.get(p,0):levels[p]=level;queue.append(p)
 while queue:
  x,y,z=queue.popleft();level=levels[x,y,z]-1
  if level<=0:continue
  for dx,dy,dz in [(1,0,0),(-1,0,0),(0,1,0),(0,-1,0),(0,0,1),(0,0,-1)]:
   p=x+dx,y+dy,z+dz
   if not(0<=p[0]<v.w and 0<=p[1]<v.h and 0<=p[2]<v.d):continue
   if levels.get(p,0)>=level or not transmitting(v.name(p)):continue
   levels[p]=level;queue.append(p)
 return levels

def add_lamp(v,p,theme):
 from city_lighting_styles import add_lamp as themed
 return themed(v,p,theme)

def fill_shadow(v,p,theme):
 from city_lighting_styles import fill_shadow as themed
 return themed(v,p,theme)

def portal(v,theme,category,points):
 if category!='gym':return []
 doors=[p for p in v.vox if v.name(p).endswith('_door') and not 'trapdoor' in v.name(p) and str(v.props(p).get('half'))=='lower']
 candidates=[];fallback=False;authored=str(v.data.get('BackportArchitecture','')).startswith('original-city-')
 if doors:
  # Dress the existing actual facade doors, not a decorative socket.
  for p in doors:
   facing=str(v.props(p).get('facing','north'));dx,_,dz=DIRECTIONS[facing];candidates.append((p,(dx,dz)))
 else:
  sockets=[b for b in v.vox.values() if str(b.get('nbt',{}).get('name',''))=='pixelmon:gym']
  for b in sockets:
   facing=str(v.props(tuple(map(int,b['pos']))).get('orientation','')).split('_')[0]
   if facing not in DIRECTIONS:continue
   dx,_,dz=DIRECTIONS[facing];x,y,z=map(int,b['pos'])
   # Keep boundary jigsaws intact. A gate sits two blocks inside its street approach.
   for step in range(2,7):
    q=(x-dx*step,y,z-dz*step)
    for yy in range(max(1,y-1),min(v.h-2,y+8)):
     p=q[0],yy,q[2]
     if any(floor(v.name((p[0],yy-drop,p[2]))) for drop in [1,2]) and all(walk_air(v.name((p[0]+dx*k,yy+j,p[2]+dz*k))) for k in [-1,0,1] for j in [0,1]):
      candidates.append((p,(dx,dz)));break
  if not candidates and points:
   fallback=True
   for p in sorted(points,key=lambda p:(min(p[0],v.w-1-p[0],p[2],v.d-1-p[2]),-p[1],p[0],p[2]))[:150]:
    if 2<=p[0]<v.w-2 and 2<=p[2]<v.d-2:
     for _,(dx,_,dz) in DIRECTIONS.items():candidates.append((p,(dx,dz)))
 created=[];used=set();door='dark_oak_door' if theme in ['dark','ghost','fighting'] else 'spruce_door' if theme in ['normal','grass','fire','dragon'] else 'birch_door'
 if authored:
  # Authored halls have a symmetric main room. Centre on that facade, then
  # retain the supported exterior apron linking it to the unchanged street socket.
  for p,(dx,dz) in list(candidates):
   target=(v.w-1)/2 if dz else (v.d-1)/2
   for start in {int(target),int(target+.999)}:
    candidates.append(((start,p[1],p[2]) if dz else (p[0],p[1],start),(dx,dz)))
 # Centre authored entrances; keep native openings at their original coordinates.
 candidates.sort(key=lambda row:(abs(row[0][0]-(v.w-1)/2) if row[1][1] else abs(row[0][2]-(v.d-1)/2),abs(row[0][0]-v.w//2) if row[1][1] else abs(row[0][2]-v.d//2),row[0]))
 for p,(dx,dz) in candidates:
  if (fallback or authored) and created:break
  x,y,z=p;sx,sz=-dz,dx
  if any(abs(x-a)+abs(z-c)<12 and abs(y-b)<8 for a,b,c in used):continue
  # Preserve native paired openings. A single authored crossing stays a single door.
  peer=(x+sx,y,z+sz)
  paired=not authored and peer in doors and v.props(peer).get('facing')==v.props(p).get('facing')
  pair=[p,peer] if paired else [p]
  valid=True
  for xx,yy,zz in pair:
   if not(2<=xx<v.w-2 and 2<=zz<v.d-2):valid=False;break
   for k in [-1,1]:
    if not any(floor(v.name((xx+dx*k,yy-drop,zz+dz*k))) for drop in [1,2]):valid=False
    for j in [0,1]:
     at=(xx+dx*k,yy+j,zz+dz*k);name=v.name(at)
     structural=any(t in name for t in ['brick','stone','quartz','pillar','concrete','terracotta','prismarine','glass','planks','_log','_wood'])
     if 'nbt' in v.vox.get(at,{}) or not walk_air(name) and not structural:valid=False
    if 'nbt' in v.vox.get((xx+dx*k,yy-1,zz+dz*k),{}):valid=False
   if any('nbt' in v.vox.get((xx,yy+j,zz),{}) for j in range(4)):valid=False
  if not valid:continue
  # Preserve wall fixtures and native junctions on the two frame columns.
  cols=[(x-sx,z-sz),(x+sx*len(pair),z+sz*len(pair))]
  if any('nbt' in v.vox.get((xx,y+j,zz),{}) for xx,zz in cols for j in range(-1,4)):continue
  from city_lighting_styles import PROFILES
  frame=PROFILES[theme][2]
  facing=next(k for k,(a,_,b) in DIRECTIONS.items() if (a,b)==(dx,dz))
  if authored:
   from city_architecture import PALETTES
   facade=PALETTES[theme][1]
   # Close all abandoned crossings in this facade, including the routed old opening.
   # Tagged assembly tiles are immutable; exterior circulation still reaches the main door.
   along=range(1,v.w-1) if dz else range(1,v.d-1)
   for at in along:
    old=(at,y,z) if dz else (x,y,at)
    if old in pair:continue
    if not walk_air(v.name(old)) or not walk_air(v.name((old[0],y+1,old[2]))):continue
    if v.name((old[0],y+4,old[2])) in AIR:continue
    if any('nbt' in v.vox.get((old[0],y+j,old[2]),{}) for j in range(4)):continue
    for j in range(4):v.put((old[0],y+j,old[2]),facade)
  for old in doors:
   if authored and old not in pair and old[1]==y and (old[0]-x)*dx+(old[2]-z)*dz==0:
    for j in range(4):v.put((old[0],old[1]+j,old[2]),facade)
  for i,(xx,yy,zz) in enumerate(pair):
   for k in [-1,1]:
    for j in [0,1]:v.put((xx+dx*k,yy+j,zz+dz*k),'air')
   for k in [-1,0,1]:v.put((xx+dx*k,yy-1,zz+dz*k),frame)
   # A lowered native street receives a half-step ramp, not a one-block jump.
   ramp=(xx+dx*2,yy-1,zz+dz*2)
   if v.name(ramp) in AIR and floor(v.name((ramp[0],yy-2,ramp[2]))):
    inward=next(key for key,(a,_,b) in DIRECTIONS.items() if (a,b)==(-dx,-dz))
    v.put(ramp,'stone_brick_stairs',{'facing':inward,'half':'bottom','shape':'straight','waterlogged':'false'})
   for half,j in [('lower',0),('upper',1)]:v.put((xx,yy+j,zz),door,{'half':half,'facing':facing,'hinge':'left' if i==0 else 'right','open':'false','powered':'false'})
   v.put((xx,y+2,zz),'glass');v.put((xx,y+3,zz),frame)
  for xx,zz in cols:
   for j in range(-1,4):v.put((xx,y+j,zz),frame)
  # Door framing uses the building palette; lighting is separate from its structural posts.
  used.add(p);created.append({'position':list(p),'facing':facing,'width':len(pair)})
 v.data['BackportGymPortals']=n.List[n.IntArray](n.IntArray(row['position']) for row in created)
 v.data['BackportGymPortalWidths']=n.IntArray([row['width'] for row in created])
 return created

def upgrade(data,theme,category):
 if category not in ROOMS:return data,None
 v=Voxels(data)
 if category!='gym' and 'BackportRoofBase' not in data:
  from verify_city_envelopes import native_room_projection,roof_cover
  repairs=[]
  for x,base,z in sorted(native_room_projection(v)):
   if roof_cover(v,x,z,base):continue
   neighbours=[]
   for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
    for y in range(base,v.h):
     at=(x+dx,y,z+dz);name=v.name(at)
     if floor(name) and not any(t in name for t in ['_trapdoor','leaves']) and name.startswith('minecraft:'):
      neighbours.append((y-base,at,name));break
   if not neighbours:raise ValueError(('Enclosed native room has no neighbouring roof material',theme,category,x,base,z))
   _,at,name=min(neighbours);target=(x,at[1],z)
   if v.put(target,name,{k:str(value) for k,value in v.props(at).items()}):repairs.append(target)
  data['BackportNativeRoofRepairs']=n.List[n.IntArray](n.IntArray(p) for p in repairs)
 points=walking(v,category);gates=portal(v,theme,category,points);points=walking(v,category)
 v.walk_feet=set(points);v.walk_clearance={q for x,y,z in points for q in [(x,y,z),(x,y+1,z)]}
 levels=propagate(v,{},[p for p in v.vox if luminosity(v,p)>0]);added=[]
 # A regular ceiling layout first; sparse corrective fixtures only for occluded corners.
 for p in points:
  if p[0]%8==4 and p[2]%8==4 and levels.get(p,0)<10:
   source,style=add_lamp(v,p,theme)
   if source:added.append((source,style));propagate(v,levels,[source])
 failures=[]
 for p in sorted(points,key=lambda p:(levels.get(p,0),p)):
  if levels.get(p,0)>=8:continue
  source,style=add_lamp(v,p,theme)
  if source:added.append((source,style));propagate(v,levels,[source])
  if levels.get(p,0)<8:failures.append(p)
 actual=propagate(v,{},[p for p in v.vox if luminosity(v,p)>0])
 for p in points:
  if actual.get(p,0)<8:
   source=fill_shadow(v,p,theme)
   if source:added.append((source,'recessed_sconce'));propagate(v,actual,[source])
 dark=[p for p in points if actual.get(p,0)<8]
 # Cladding a lamp can create a small supported upper surface. Inspect the
 # final geometry again, rather than trusting the original walking-cell list.
 for _ in range(3):
  final_points=walking(v,category)
  v.walk_feet.update(final_points);v.walk_clearance.update(q for x,y,z in final_points for q in [(x,y,z),(x,y+1,z)])
  dark=[p for p in final_points if actual.get(p,0)<8]
  if not dark:break
  for p in dark:
   source=fill_shadow(v,p,theme)
   if source:added.append((source,'final_surface_panel'));propagate(v,actual,[source])
 points=walking(v,category);dark=[p for p in points if actual.get(p,0)<8]
 if dark:raise ValueError(('Unlit indoor walking cells',theme,category,len(dark),[(p,v.name(p),ceiling(v,p)) for p in dark[:6]]))
 data['BackportLightingRevision']=n.Int(9);data['BackportLightFixtures']=n.List[n.IntArray](n.IntArray(p) for p,_ in added)
 data['BackportNonWalkFixtures']=n.List[n.IntArray](n.IntArray(p) for p in sorted(v.nonwalk_floor))
 data['BackportLightingWalkFeet']=n.List[n.IntArray](n.IntArray(p) for p in sorted(v.walk_feet))
 marker=str(data.get('BackportArchitecture',''))
 if marker.startswith('original-city-v6:'):data['BackportArchitecture']=n.String(marker.replace('v6:','v9:'))
 report={'category':category,'walking_cells':len(points),'minimum_night_block_light':min((actual.get(p,0) for p in points),default=None),'added_lamps':len(added),'styles':dict(collections.Counter(style for _,style in added)),'portals':gates}
 return v.finish(),report
