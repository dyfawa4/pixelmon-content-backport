"""Offline inspection of final city voxels, without running Minecraft."""
import collections

DIRECTIONS={'north':(0,0,-1),'east':(1,0,0),'south':(0,0,1),'west':(-1,0,0)}

def structural_components(d):
 vox={}
 for b in d['blocks']:
  s=str(d['palette'][int(b['state'])]['Name'])
  if s=='minecraft:jigsaw':s=str(b.get('nbt',{}).get('final_state','minecraft:air'))
  if s not in ['minecraft:air','minecraft:structure_void','minecraft:water']:
   vox[tuple(map(int,b['pos']))]=s
 # Corner-touching diagonal roof/arch pixels count as continuous geometry.
 remaining=set(vox);unsupported=[]
 while remaining:
  first=remaining.pop();component={first};queue=[first];grounded=first[1]==0
  while queue:
   x,y,z=queue.pop()
   for dx in [-1,0,1]:
    for dy in [-1,0,1]:
     for dz in [-1,0,1]:
      p=(x+dx,y+dy,z+dz)
      if p in remaining:
       remaining.remove(p);component.add(p);queue.append(p);grounded|=p[1]==0
  if not grounded:unsupported.append({'size':len(component),'min':tuple(map(min,zip(*component))),'max':tuple(map(max,zip(*component))),'materials':dict(collections.Counter(vox[p] for p in component))})
 return unsupported

def pane_errors(d):
 vox={tuple(map(int,b['pos'])):d['palette'][int(b['state'])] for b in d['blocks']}
 errors=[];count=0
 for p,s in vox.items():
  name=str(s['Name'])
  if not name.endswith('_pane') and name!='minecraft:iron_bars':continue
  count+=1;props=s.get('Properties',{})
  for direction,(dx,dy,dz) in DIRECTIONS.items():
   neighbor=vox.get((p[0]+dx,p[1]+dy,p[2]+dz),{});other=str(neighbor.get('Name','minecraft:air'))
   # An adjacent pane/bars segment must connect, irrespective of wall type.
   if other.endswith('_pane') or other=='minecraft:iron_bars':
    if str(props.get(direction,'false'))!='true':errors.append((p,direction,other))
 return count,errors

def regression_cases():
 """Ensure the audit rejects both defects shown in the player's screenshots."""
 import copy
 from city_architecture import Builder
 import nbtlib as n
 def fixture():return n.Compound({'size':n.List[n.Int]([5,8,5])})
 tree=Builder(fixture(),'bug');tree.box(0,0,0,4,0,4,'grass_block')
 tree.box(2,1,2,2,5,2,'jungle_log');tree.box(1,5,1,3,6,3,'jungle_leaves')
 good=tree.finish();assert not structural_components(good)
 amputated=copy.deepcopy(good)
 amputated['blocks']=n.List[n.Compound](b for b in amputated['blocks'] if not (int(b['pos'][0])==2 and int(b['pos'][2])==2 and 1<=int(b['pos'][1])<=3))
 assert structural_components(amputated),'Audit failed to reject a floating tree canopy'
 panes=Builder(fixture(),'flying');panes.box(0,0,0,4,0,4,'smooth_quartz')
 panes.box(1,1,2,3,2,2,'light_blue_stained_glass_pane')
 good=panes.finish();count,errors=pane_errors(good);assert count==6 and not errors
 broken=copy.deepcopy(good)
 for state in broken['palette']:
  if str(state['Name']).endswith('_pane'):
   state.setdefault('Properties',n.Compound())
   for direction in DIRECTIONS:state['Properties'][direction]=n.String('false')
 assert pane_errors(broken)[1],'Audit failed to reject default disconnected panes'
 print('PASS offline regression fixtures: missing trunk and disconnected glass panes rejected; intact structures accepted.')

if __name__=='__main__':regression_cases()
