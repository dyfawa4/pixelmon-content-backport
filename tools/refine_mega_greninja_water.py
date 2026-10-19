"""Refine only the supplied Mega Greninja water mesh; body/rig/animation stay exact.

Uses existing atlas colors through UV coordinates, without editing raster assets.
"""
import argparse,hashlib,io,json,math,pathlib,struct,tempfile,zipfile
from collections import defaultdict
from PIL import Image
from convert_content import Bmd,dump,asset
from extend_reference import asset_converter
ROOT=pathlib.Path(__file__).resolve().parents[1]
def run(reference,output):
 with tempfile.TemporaryDirectory() as tmp,zipfile.ZipFile(reference) as source:
  dex=next(json.loads(source.read(n)) for n in source.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('greninja.json'))
  form=next(f for f in dex['forms'] if f['name']=='mega')
  convert,_,_=asset_converter(source,pathlib.Path(tmp));converted=convert(form)
  pqc=pathlib.Path(tmp)/asset(converted['genderProperties'][0]['palettes'][0]['modelLocator']['pqc'][0])
  body=next(line.split()[1] for line in pqc.read_text().splitlines() if line.startswith('$body '))
  raw=(pathlib.Path(tmp)/('assets/'+body.replace(':','/',1))).read_bytes()
 m=Bmd(raw);bone=next(i for i,(_,name) in enumerate(m.bones) if name=='feeler_a_01')
 folder=output/'assets/pixelmon_backport/textures/reference/pokemon/658_greninja/all/mega/none'
 atlas=Image.open(folder/'texture.png').convert('RGBA');width,height=atlas.size
 wanted={(95,171,215,255):None,(83,141,176,255):None,(41,70,88,255):None}
 for y in range(height):
  for x in range(width):
   color=atlas.getpixel((x,y))
   if color in wanted and wanted[color] is None:wanted[color]=((x+.5)/width,1-(y+.5)/height)
  if all(v is not None for v in wanted.values()):break
 assert all(v is not None for v in wanted.values()),'Existing water palette changed'
 selected=[i for i,f in enumerate(m.faces) if all(len(v[1])==1 and v[1][0][0]==bone for v in f)]
 assert len(selected)==336,('Unexpected source water topology',len(selected))
 vertices=[v[0] for i in selected for v in m.faces[i]];center=.669471
 assert max(v[0] for v in vertices)>1.9 and min(v[2] for v in vertices)>.6
 stream=io.BytesIO(raw)
 def read(fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize('>'+fmt)))
 def string():
  while read('H')[0]:pass
 read('B')
 for _ in range(read('h')[0]):read('hh');string()
 for _ in range(read('h')[0]):
  for _ in range(read('h')[0]):read('h6f')
 for _ in range(read('h')[0]):string()
 count=read('h')[0];out=bytearray(raw);chosen=set(selected);edited=[]
 for face in range(count):
  read('B');triangle=[]
  for _ in range(3):
   read('h');offset=stream.tell();v=list(read('8f'))
   for _ in range(read('B')[0]):read('hf')
   if face not in chosen:continue
   old_z=v[2];radius=math.hypot(v[0],v[1]);side=1 if old_z>=center else -1
   # Keep the supplied curved six-blade outline. A continuous lens profile
   # replaces the old flat hub and hard top/edge color bands.
   v[0]*=.72;v[1]*=.72
   bulge=.075*math.exp(-((radius/.65)**2))
   v[2]=center+(old_z-center)*5+side*bulge
   v[6:8]=wanted[(95,171,215,255)]
   triangle.append((offset,v,side))
  if triangle:edited.append(triangle)
 # Area-weighted smooth normals. Front and back remain separate at the lip,
 # preventing a zero normal where a thin blade has coincident seam vertices.
 normals=defaultdict(lambda:[0.,0.,0.])
 def key(v,side):return tuple(round(n,5) for n in v[:3])+(side,)
 for triangle in edited:
  a,b,c=[v for _,v,_ in triangle];u=[b[i]-a[i] for i in range(3)];w=[c[i]-a[i] for i in range(3)]
  n=[u[1]*w[2]-u[2]*w[1],u[2]*w[0]-u[0]*w[2],u[0]*w[1]-u[1]*w[0]]
  # Retain the upstream outward winding / normal convention.
  if sum(n[i]*a[i+3] for i in range(3))<0:n=[-x for x in n]
  for _,v,side in triangle:
   total=normals[key(v,side)]
   for axis in range(3):total[axis]+=n[axis]
 for triangle in edited:
  for offset,v,side in triangle:
   n=normals[key(v,side)];length=math.sqrt(sum(x*x for x in n))
   if length>1e-10:v[3:6]=[x/length for x in n]
   else:
    length=math.sqrt(sum(x*x for x in v[3:6]));v[3:6]=[x/length for x in v[3:6]]
   struct.pack_into('>8f',out,offset,*v)
 target=folder/'water-refined.bmd';target.write_bytes(out);after=Bmd(bytes(out))
 assert after.bones==m.bones and after.frames==m.frames
 for i,(before,result) in enumerate(zip(m.faces,after.faces)):
  if i not in chosen:assert before==result,'Body geometry was changed'
  else:assert [v[1] for v in before]==[v[1] for v in result],'Water skinning changed'
 desc=folder/'model.pqc';lines=desc.read_text().splitlines();desc.write_text('\n'.join('$body pixelmon_backport:textures/reference/pokemon/658_greninja/all/mega/none/water-refined.bmd' if l.startswith('$body ') else l for l in lines)+'\n',encoding='utf-8')
 content=output/'backport/content.json';d=json.loads(content.read_text(encoding='utf-8'))
 for patch in d['patches']:
  if patch['species']['dex']==658:
   for f in patch['species']['forms']:
    if f['name']=='mega':
     for gender in f['genderProperties']:
      for palette in gender['palettes']:palette['emissive']='pixelmon:empty.png'
 dump(content,d)
 dump(output/'backport/mega-greninja-water.json',{'source':reference.name,'source_model_sha256':hashlib.sha256(raw).hexdigest(),'model_sha256':hashlib.sha256(out).hexdigest(),'water_faces':len(selected),'unchanged_body_faces':len(m.faces)-len(selected),'bone':bone,'operation':'water outline x0.72, thickness x5 plus continuous center lens; area-weighted smooth normals and uniform existing atlas water blue; remove dark bands and flat hub; valid transparent emissive; no raster edits','unchanged':['body geometry','rig and skin weights','animations','battle moves and abilities']})
 print('Refined 336 water faces; 5630 body faces, skeleton and animation exact. No raster edits.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--reference',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,default=ROOT/'resources');a=p.parse_args();run(a.reference,a.output)
