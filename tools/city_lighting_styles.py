"""Themed fixtures mounted inside the envelope; exterior roof materials remain intact."""
PROFILES={
 'normal':('timber','lantern','spruce_planks','brown'),
 'fighting':('timber','lantern','dark_oak_planks','red'),
 'fire':('timber','lantern','dark_oak_planks','orange'),
 'dragon':('timber','lantern','spruce_planks','red'),
 'grass':('garden','shroomlight','jungle_planks','green'),
 'bug':('garden','shroomlight','jungle_planks','lime'),
 'dark':('gallery','lantern','polished_blackstone_bricks','black'),
 'ghost':('gallery','lantern','polished_blackstone_bricks','purple'),
 'rock':('gallery','lantern','cut_sandstone','orange'),
 'ground':('gallery','lantern','sandstone','brown'),
 'poison':('panel','sea_lantern','dark_prismarine','lime'),
 'steel':('panel','sea_lantern','polished_andesite','gray'),
 'electric':('inlay','sea_lantern','polished_andesite','yellow'),
 'water':('inlay','sea_lantern','prismarine_bricks','cyan'),
 'ice':('inlay','sea_lantern','smooth_quartz','light_blue'),
 'flying':('rod','end_rod','smooth_quartz','light_blue'),
 'psychic':('rod','end_rod','purpur_block','magenta'),
 'fairy':('rod','end_rod','smooth_quartz','pink')}

def add_lamp(v,p,theme,force_floor=False):
 from city_lighting import AIR,ceiling,floor,walk_air,DIRECTIONS
 style,light,trim,color=PROFILES[theme];x,y,z=p;roof=ceiling(v,p)
 if roof is not None and roof<=y+3 and 'nbt' not in v.vox.get((x,roof,z),{}):
  # Low attic stairs have no room for a pendant or bracket. Recess the panel
  # into the ceiling and restore its original roof cladding above the source.
  source=(x,roof,z);cladding=v.name(source);properties={k:str(value) for k,value in v.props(source).items()}
  if cladding not in ['minecraft:sea_lantern','minecraft:glowstone','minecraft:shroomlight']:
   v.put(source,'sea_lantern' if style in ['panel','inlay','rod'] else 'shroomlight' if style=='garden' else 'glowstone')
   if (x,roof+1,z) not in getattr(v,'walk_feet',set()):v.nonwalk_floor.add(source)
   if v.name((x,roof+1,z)) in AIR:
    cap=(x,roof+1,z)
    if cap in getattr(v,'walk_feet',set()) or any(token in cladding for token in ['_trapdoor','_stairs','_slab']):v.put(cap,color+'_carpet')
    elif cap not in getattr(v,'walk_clearance',set()):
     v.put(cap,cladding,properties);v.nonwalk_floor.add(cap)
   return source,style+'_clad_attic_panel'
 # Short pendants hug the actual ceiling, with at most ONE chain block.
 # High halls receive wall lamps and recessed floor light instead of long chains.
 if not force_floor and roof is not None and y+4<=roof<=y+6:
  source=(x,roof-2,z) if style in ['timber','gallery'] else (x,roof-1,z)
  if source not in getattr(v,'walk_clearance',set()) and v.name(source) in AIR and v.name((x,source[1]+1,z)) in AIR|{v.name((x,roof,z))}:
   if style in ['timber','gallery']:
    v.put(source,'lantern',{'hanging':'true','waterlogged':'false'})
    v.put((x,roof-1,z),'chain',{'axis':'y','waterlogged':'false'})
   elif style=='rod':v.put(source,'end_rod',{'facing':'down'})
   else:v.put(source,light)
   v.nonwalk_floor.add(source)
   return source,style+'_ceiling'
 if not force_floor and style in ['timber','gallery','rod','garden']:
  # Find an inside wall bracket, leaving a full two-block circulation height.
  for distance in range(1,4):
   for facing,(dx,_,dz) in DIRECTIONS.items():
    source=(x+dx*distance,y+3,z+dz*distance);support=(source[0]+dx,source[1],source[2]+dz)
    if v.name(source) not in AIR or not floor(v.name(support)) or source in getattr(v,'walk_clearance',set()) or (source[0],source[1]-1,source[2]) in getattr(v,'walk_clearance',set()):continue
    if 'nbt' in v.vox.get(source,{}) or 'glass' in v.name(support):continue
    # The mount occupies the upper wall, rather than dangling into a walkway.
    if style=='rod':
     inward=next(k for k,(a,_,b) in DIRECTIONS.items() if (a,b)==(-dx,-dz))
     v.put(source,'end_rod',{'facing':inward})
    else:
     if v.name((source[0],source[1]-1,source[2])) not in AIR:continue
     bracket=trim.replace('_planks','_slab') if trim.endswith('_planks') else trim
     v.put((source[0],source[1]-1,source[2]),bracket,{'type':'top','waterlogged':'false'} if bracket.endswith('_slab') else None)
     v.put(source,'lantern',{'hanging':'false','waterlogged':'false'})
    return source,style+'_bracket'
 # A concealed light in the floor is covered with the actual court colour.
 # This also supplies high vaults without installing suspended chains.
 candidates=[(x,y-1,z)]
 candidates+=sorted(((x+dx,y+dy-1,z+dz) for dx in range(-2,3) for dz in range(-2,3) for dy in [-1,0,1]),key=lambda q:sum(abs(a-b) for a,b in zip(q,p)))
 for source in candidates:
  if not(0<=source[0]<v.w and 0<=source[2]<v.d and source[1]>=0):continue
  old=v.name(source);top=(source[0],source[1]+1,source[2])
  if not floor(old) or any(t in old for t in ['_slab','_stairs','glass']) or 'nbt' in v.vox.get(source,{}) or v.name(top) not in AIR or 'nbt' in v.vox.get(top,{}):continue
  import re
  match=re.match(r'minecraft:(\w+)_(?:wool|concrete|terracotta)$',old)
  actual=match[1] if match else color
  v.put(source,'shroomlight' if style=='garden' else 'sea_lantern' if style in ['panel','inlay','rod'] else 'glowstone')
  v.put(top,actual+'_carpet');return source,style+'_covered_inlay'
 # Stair shafts may have only slabs underfoot; a small wall sconce preserves them.
 for distance in range(1,4):
  for facing,(dx,_,dz) in DIRECTIONS.items():
   source=(x+dx*distance,y+2,z+dz*distance);support=(source[0]+dx,source[1],source[2]+dz)
   if not(0<=source[0]<v.w and 0<=source[2]<v.d) or source in getattr(v,'walk_clearance',set()) or v.name(source) not in AIR or 'nbt' in v.vox.get(source,{}) or not floor(v.name(support)):continue
   inward=next(k for k,(a,_,b) in DIRECTIONS.items() if (a,b)==(-dx,-dz))
   v.put(source,'end_rod' if style in ['panel','inlay','rod'] else 'wall_torch',{'facing':inward})
   return source,style+'_corner_sconce'
 return None,None

def fill_shadow(v,p,theme):
 # Route corrective lighting through the same protected, covered mounting rules.
 from city_lighting import transmitting,floor,AIR
 import collections
 queue=collections.deque([(p,0)]);seen={p}
 while queue:
  q,distance=queue.popleft()
  # Choose a wall surface reached through this air component. A nearby floor
  # behind an opaque staircase cannot illuminate this shaft.
  x,y,z=q
  for dx,dz in [(1,0),(-1,0),(0,1),(0,-1)]:
   source=(x+dx,y,z+dz);name=v.name(source)
   if not(0<=source[0]<v.w and 0<=source[2]<v.d) or not floor(name) or transmitting(name) or 'nbt' in v.vox.get(source,{}) or not name.startswith('minecraft:'):continue
   style,light,trim,color=PROFILES[theme]
   v.put(source,'sea_lantern' if style in ['panel','inlay','rod'] else 'shroomlight' if style=='garden' else 'glowstone')
   return source
  if distance>=5:continue
  for dx,dy,dz in [(0,1,0),(1,0,0),(-1,0,0),(0,0,1),(0,0,-1),(0,-1,0)]:
   nxt=x+dx,y+dy,z+dz
   if 0<=nxt[0]<v.w and 0<=nxt[1]<v.h and 0<=nxt[2]<v.d and nxt not in seen and transmitting(v.name(nxt)):
    seen.add(nxt);queue.append((nxt,distance+1))
 return None
