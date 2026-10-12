"""Read final resources, check night estimates/portals and preserved native gameplay."""
import argparse,json,zipfile,collections
from extend_gym_cities import ROOT,RES,read_nbt,NATIVE_BADGES,THEMES
from city_architecture import kind
from city_lighting import Voxels,walking,propagate,luminosity,ROOMS,DIRECTIONS,walk_air,floor
from verify_gym_city_assets import components

def audit(data,category,path):
 assert int(data.get('BackportLightingRevision',0))==9,path
 v=Voxels(data);points=walking(v,category);levels=propagate(v,{},[p for p in v.vox if luminosity(v,p)>0])
 assert points,(path,'no room coverage')
 dark=[p for p in points if levels.get(p,0)<8];assert not dark,(path,'night shadow',dark[:6])
 protected={tuple(map(int,p)) for p in data.get('BackportLightingWalkFeet',[])}
 for x,y,z in protected:
  assert walk_air(v.name((x,y,z))) and walk_air(v.name((x,y+1,z))),(path,'fixture blocks original circulation',x,y,z)
  assert levels.get((x,y,z),0)>=8,(path,'original circulation left dark',x,y,z)
 for x,y,z in v.nonwalk_floor:
  assert (x,y+1,z) not in protected,(path,'real circulation hidden by fixture metadata')
  assert luminosity(v,(x,y,z))>0 or luminosity(v,(x,y-1,z))>0,(path,'non-fixture surface excluded',x,y,z)
 portals=data.get('BackportGymPortals',[])
 widths=list(map(int,data.get('BackportGymPortalWidths',[])))
 if category=='gym':
  assert portals and len(widths)==len(portals),(path,'no visible entrance/width metadata')
  for raw,width in zip(portals,widths):
   assert width in [1,2],(path,'unsupported entrance width')
   x,y,z=map(int,raw);facing=str(v.props((x,y,z))['facing']);dx,_,dz=DIRECTIONS[facing];sx,sz=-dz,dx
   for xx,zz in [(x+sx*i,z+sz*i) for i in range(width)]:
    assert v.name((xx,y,zz)).endswith('_door') and str(v.props((xx,y+1,zz)).get('half'))=='upper',(path,'incomplete portal')
    assert floor(v.name((xx,y-1,zz))),(path,'unsupported portal')
    assert all(walk_air(v.name((xx+dx*k,y+j,zz+dz*k))) for k in [-1,1] for j in [0,1]),(path,'blocked portal')
 return len(points),len(portals)

def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);a=p.parse_args();total=cells=portals=0
 with zipfile.ZipFile(a.pixelmon) as jar:
  for theme,ns in [(t,'pixelmon') for t in NATIVE_BADGES]+[(t,'pixelmon_backport') for t in THEMES]:
   for path in (RES/f'data/{ns}/structures/gyms/{theme}').glob('*.nbt'):
    category=kind(path.name,theme)
    if category not in ROOMS:continue
    data=read_nbt(path.read_bytes());count,gates=audit(data,category,str(path));cells+=count;portals+=gates;total+=1
    if ns=='pixelmon':
     source=path.relative_to(RES).as_posix();original=read_nbt(jar.read(source))
     canonical=lambda value:json.dumps(value.unpack() if hasattr(value,'unpack') else value,sort_keys=True,default=lambda v:v.tolist())
     tags=lambda d:{tuple(map(int,b['pos'])):canonical(b['nbt']) for b in d['blocks'] if 'nbt' in b}
     assert tags(original)==tags(data),(source,'native socket/tile NBT changed')
     from embedded_npc_zh_cn import translate
     translate(original)
     assert canonical(original.get('entities'))==canonical(data.get('entities')),(source,'native NPC/entity nontranslation field changed')
     assert components(original)==components(data),(source,'native functional device removed')
 print(f'PASS final 18-city resources: {total} buildings, {cells} inspected walking cells, {portals} authored entrances, night estimate >=8, native NPC/tile/jigsaw gameplay NBT retained except intentional custom-text translation.')
if __name__=='__main__':main()
