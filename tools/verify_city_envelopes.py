"""Whole authored roof footprints plus native bed shelter and final entrance alignment."""
import collections,copy,json
from extend_gym_cities import ROOT,RES,THEMES,NATIVE_BADGES,read_nbt
from city_architecture import kind
from city_lighting import Voxels,ROOMS,AIR,DIRECTIONS,floor,walk_air

def roof_cover(v,x,z,base):
 return any(floor(v.name((x,y,z))) and not any(token in v.name((x,y,z)) for token in ['_trapdoor','leaves']) for y in range(base,v.h))

def native_room_projection(v):
 """Infer enclosed floors using a wall/window plane above the doorway."""
 candidates=collections.defaultdict(list)
 for x,y,z in v.vox:
  if floor(v.name((x,y,z))) and walk_air(v.name((x,y+1,z))) and walk_air(v.name((x,y+2,z))):candidates[y].append((x,z))
 expected=set()
 # The lowest circulation floor is the room foundation. Higher roof terraces
 # are deliberately outdoors; upstairs sleeping spaces are checked separately.
 lowest=min((y for y,points in candidates.items() if len(points)>=4),default=None)
 for y,points in candidates.items():
  if y!=lowest:continue
  if len(points)<4 or y+3>=v.h:continue
  barrier={(x,z) for x in range(v.w) for z in range(v.d) if floor(v.name((x,y+3,z))) or 'glass' in v.name((x,y+3,z)) or v.name((x,y+3,z)).endswith('_door')}
  outside={(x,z) for x in range(v.w) for z in [0,v.d-1]}|{(x,z) for z in range(v.d) for x in [0,v.w-1]}
  outside-=barrier;queue=collections.deque(outside)
  while queue:
   x,z=queue.popleft()
   for p in [(x-1,z),(x+1,z),(x,z-1),(x,z+1)]:
    if 0<=p[0]<v.w and 0<=p[1]<v.d and p not in barrier and p not in outside:outside.add(p);queue.append(p)
  expected.update((x,y+3,z) for x,z in points if (x,z) not in outside)
 return expected

def main():
 roofs=beds=columns=lamps=native_columns=native_rooms=0;centres={};counts=collections.Counter();styles=collections.Counter()
 for ns,themes in [('pixelmon',NATIVE_BADGES),('pixelmon_backport',THEMES)]:
  for theme in themes:
   for path in (RES/f'data/{ns}/structures/gyms/{theme}').glob('*.nbt'):
    category=kind(path.name,theme)
    if category not in ROOMS:continue
    d=read_nbt(path.read_bytes());v=Voxels(d)
    if 'BackportRoofFootprint' in d:
     base=int(d['BackportRoofBase']);mask=list(map(int,d['BackportRoofFootprint']));points=list(zip(mask[::2],mask[1::2]))
     missing=[p for p in points if not roof_cover(v,*p,base)]
     assert not missing,(path,'missing roof columns',missing[:8]);roofs+=1;columns+=len(points);counts[theme]+=1
     # A real removed roof column must be detected, irrespective of furniture.
     if points:
      broken=copy.deepcopy(d);x,z=points[len(points)//2]
      for block in broken['blocks']:
       xx,y,zz=map(int,block['pos'])
       if (xx,zz)==(x,z) and y>=base:block['state']=__import__('nbtlib').Int(0) if str(broken['palette'][0]['Name'])=='minecraft:air' else block['state']
      probe=Voxels(broken)
      for y in range(base,probe.h):probe.put((x,y,z),'air')
      assert not roof_cover(probe,x,z,base),'Roof audit ignored a missing column'
    elif ns=='pixelmon':
     if category!='gym':
      projection=native_room_projection(v);missing=[p for p in projection if not roof_cover(v,p[0],p[2],p[1])]
      assert not missing,(path,'native enclosed room roof gap',missing[:8])
      native_columns+=len(projection);native_rooms+=1
     for pos in v.vox:
      if v.name(pos).endswith('_bed') and str(v.props(pos).get('part'))=='foot':
       assert roof_cover(v,pos[0],pos[2],pos[1]+2),(path,'native bed exposed');beds+=1
    for raw in d.get('BackportLightFixtures',[]):
     x,y,z=map(int,raw);lamps+=1
     if v.name((x,y,z)).endswith('lantern'):
      chain=0
      for yy in range(y+1,v.h):
       if v.name((x,yy,z))!='minecraft:chain':break
       chain+=1
      assert chain<=1,(path,'long suspended fixture',x,y,z,chain)
    if category=='gym' and ns=='pixelmon_backport':
     offsets=[]
     for raw,width in zip(d.get('BackportGymPortals',[]),d.get('BackportGymPortalWidths',[])):
      x,y,z=map(int,raw);facing=str(v.props((x,y,z))['facing']);dx,_,dz=DIRECTIONS[facing]
      offset=abs(x-dz*(int(width)-1)*.5-(v.w-1)/2) if dz else abs(z+dx*(int(width)-1)*.5-(v.d-1)/2)
      offsets.append(offset)
      # Paired door hinges face opposite sides; both halves agree.
      if int(width)==2:
       peer=(x-dz,y,z+dx)
       assert str(v.props((x,y,z))['hinge'])!=str(v.props(peer)['hinge'])
     assert offsets and min(offsets)<=.5,(path,'main gate not centred',offsets)
     centres[theme]=offsets
 assert set(counts)==set(THEMES),counts
 result={'scope':'whole authored enclosed-building roof projection; native bed roof columns; final main gym entrance alignment and fixture chain limits',
         'authored_enclosed_roofs':roofs,'roof_columns':columns,'native_sheltered_beds':beds,'added_fixture_positions':lamps,
         'native_room_templates':native_rooms,'native_enclosed_roof_columns':native_columns,
         'main_gate_axis_offsets':centres,'limits':['Intentional open gym courts retain open sky.','Half-block maximum gate offset follows the two-door width on an odd-width facade.','Aesthetic quality and assembled-world roof/door appearance require manual acceptance.']}
 (ROOT/'docs/CITY_ENVELOPE_REVIEW_1.3.0.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('PASS',roofs,'complete authored roofs,',columns,'roof columns,',beds,'native sheltered beds; all eight new main gates centred; short fixture chains')
if __name__=='__main__':main()
