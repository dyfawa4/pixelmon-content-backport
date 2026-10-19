"""Derive missing Mega variants from the accepted smooth native models.

No raster artwork is edited or generated. Magearna material colors are sampled
from its existing atlas through per-palette UVs. Tatsugiri's three existing fish
and their animated attachment roots move together; no cube geometry is imported.
"""
import copy,io,json,pathlib,struct,hashlib
import numpy as np
from PIL import Image
from convert_content import Bmd,dump

ROOT=pathlib.Path(__file__).resolve().parents[1];R=ROOT/'resources'
PREFIX='pixelmon_backport:textures/smooth_variants/'
_SWATCH_CACHE={}

def layout(raw):
 stream=io.BytesIO(raw)
 def read(fmt):return struct.unpack('>'+fmt,stream.read(struct.calcsize('>'+fmt)))
 def string():
  while read('H')[0]:pass
 read('B')
 for _ in range(read('h')[0]):read('hh');string()
 poses=[]
 for _ in range(read('h')[0]):
  frame={}
  for _ in range(read('h')[0]):
   index=read('h')[0];offset=stream.tell();pose=read('6f');frame[index]=(offset,pose)
  poses.append(frame)
 for _ in range(read('h')[0]):string()
 faces=[]
 for _ in range(read('h')[0]):
  read('B');face=[]
  for _ in range(3):
   read('h');offset=stream.tell();coords=read('8f');weights=[read('hf') for _ in range(read('B')[0])];face.append((offset,coords,weights))
  faces.append(face)
 assert not stream.read()
 return poses,faces

def rotation(pose):
 x,y,z=pose[3:];cx,sx=np.cos(x),np.sin(x);cy,sy=np.cos(y),np.sin(y);cz,sz=np.cos(z),np.sin(z)
 return np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]])

def global_rotations(model):
 rotations=[]
 for i,(parent,_) in enumerate(model.bones):
  rotations.append((rotations[parent] if parent>=0 else np.eye(3))@rotation(model.rest[i]))
 return rotations

def atlas_pixel(image,uv):
 x=min(image.width-1,max(0,int(uv[0]*image.width)));y=min(image.height-1,max(0,int((1-uv[1])*image.height)))
 return image.getpixel((x,y))[:3]

def swatch(image,target,emissive):
 """Pick a stable existing neutral texel; no painting/resampling of PNGs."""
 if id(image) not in _SWATCH_CACHE:
  data=np.asarray(image.convert('RGB')).astype(np.int32);variation=np.zeros(data.shape[:2],dtype=np.int32)
  for axis in (0,1):
   for shift in (-1,1):variation+=np.sum(np.abs(data-np.roll(data,shift,axis=axis)),axis=2)
  unsafe=np.asarray(emissive.convert('RGBA'))[:,:,3]>0
  unsafe=np.logical_or.reduce([np.roll(np.roll(unsafe,x,axis=0),y,axis=1) for x in range(-2,3) for y in range(-2,3)])
  _SWATCH_CACHE[id(image)]=(image,data,variation,unsafe)
 _,data,variation,unsafe=_SWATCH_CACHE[id(image)];distance=np.sum((data-np.array(target))**2,axis=2)
 score=distance+variation*20
 score[np.ptp(data,axis=2)>5]=10**9 # silver, not a similarly bright blue/gold texel
 score[unsafe]=10**9 # never sample emissive eye/core texels for metal surfaces
 score[:2,:]=10**9;score[-2:,:]=10**9;score[:,:2]=10**9;score[:,-2:]=10**9
 y,x=np.unravel_index(np.argmin(score),score.shape)
 assert score[y,x]<10**9,'No non-emissive native silver material available'
 return ((x+.5)/image.width,1-(y+.5)/image.height),data[y,x].tolist()

def magearna():
 source=R/'assets/pixelmon_backport/textures/reference/pokemon/801_magearna/all/mega/none'
 raw=(source/'1.bmd').read_bytes();model=Bmd(raw);_,faces=layout(raw);ordinary=Image.open(source/'texture.png').convert('RGB')
 chosen=[]
 for face in faces:
  names=[model.bones[max(weights,key=lambda w:w[1])[0]][1] for _,_,weights in face]
  colors=[atlas_pixel(ordinary,c[6:8]) for _,c,_ in face]
  # Explicit gear / lower gown bones; do not recolor the red upper dress,
  # gold limbs, eyes, chest core, ornamental spokes or unrelated dark details.
  # The source exporter attaches the floating gears to the two horn_02 bones;
  # the generic 'skirt' and 'feeler_o' bones also contain the waist/core, which
  # must stay dark. Only side-gown descendants contain the lower white panels.
  if all(n in ('left_horn_02','right_horn_02') or (n.startswith(('left_','right_')) and 'skirt' in n) for n in names) and all(max(c)-min(c)<4 and max(c)<95 for c in colors):chosen.append(face)
 assert len(chosen)>900,('Missing original gear/lower-gown material regions',len(chosen))
 records=[]
 for palette in ('none','shiny'):
  image=Image.open(source.parent/palette/'texture.png').convert('RGB')
  emissive=Image.open(source.parent/palette/'emissive.png').convert('RGBA')
  out=bytearray(raw);samples={}
  for face in chosen:
   for offset,c,_ in face:
    # Keep baked light/dark contrast, using the corresponding native palette.
    level=max(atlas_pixel(ordinary,c[6:8]));t=(level-45)/16
    target=int((160 if palette=='none' else 116)+max(0,min(1,t))*(76 if palette=='none' else 120))
    if target not in samples:samples[target]=swatch(image,(target,)*3,emissive)
    uv,_=samples[target];struct.pack_into('>2f',out,offset+24,*uv)
  target=R/f'assets/pixelmon_backport/textures/smooth_variants/801_magearna/megaoriginal/{palette}'
  target.mkdir(parents=True,exist_ok=True);(target/'model.bmd').write_bytes(out)
  reference='pixelmon_backport:textures/reference/pokemon/801_magearna/all/mega/none/'
  (target/'model.pqc').write_text(f'$body {PREFIX}801_magearna/megaoriginal/{palette}/model.bmd\n$anim idle {reference}idle.bmd\n$anim walk {reference}walk.bmd\n$scale 0.3\n',encoding='utf8')
  after=Bmd(bytes(out));assert after.bones==model.bones and after.frames==model.frames
  for before,result in zip(model.faces,after.faces):
   for (a,w),(b,ww) in zip(before,result):assert a[:6]==b[:6] and w==ww
  records.append({'palette':palette,'edited_faces':len(chosen),'operation':'UV-only silver/white gear and lower gown; exact original positions, normals, skeleton, weights and animations','swatches':{str(k):{'uv':uv,'rgb':rgb} for k,(uv,rgb) in samples.items()},'folder':str(target.relative_to(R)).replace('\\','/')})
 return records

def tatsugiri():
 source=R/'assets/pixelmon_backport/textures/reference/pokemon/978_tatsugiri/all/mega/none'
 raw=(source/'1.bmd').read_bytes();model=Bmd(raw);poses,faces=layout(raw);rotations=global_rotations(model)
 roots={name:next(i for i,(_,n) in enumerate(model.bones) if n=='root_waist_'+name) for name in ('a','b','c')}
 def group(index):
  while index>=0:
   for name,root in roots.items():
    if index==root:return name
   index=model.bones[index][0]
  return None
 groups={i:group(i) for i in range(len(model.bones))};centers={}
 for name in roots:
  coords=[c[0] for face in faces for _,c,w in face if groups[max(w,key=lambda x:x[1])[0]]==name]
  centers[name]=(min(coords)+max(coords))/2
 # Source group a=yellow, b=red, c=orange. Select which fish leads at the
 # middle of the existing platter. Fish meshes and individual poses stay exact.
 slots={'a':centers['a'],'b':centers['b'],'c':centers['c']}
 arrangements={'megadroopy':{'a':'b','b':'c','c':'a'},'megastretchy':{'a':'c','b':'a','c':'b'}}
 records=[]
 for form,destinations in arrangements.items():
  deltas={name:np.array([slots[destinations[name]]-centers[name],0.,0.]) for name in roots}
  local={roots[name]:rotations[model.bones[roots[name]][0]].T@delta for name,delta in deltas.items()}
  target=R/f'assets/pixelmon_backport/textures/smooth_variants/978_tatsugiri/{form}';target.mkdir(parents=True,exist_ok=True)
  for filename in ('1.bmd','idle.bmd','walk.bmd'):
   original=(source/filename).read_bytes();out=bytearray(original);frames,triangles=layout(original)
   for frame in frames:
    for root,delta in local.items():
     if root not in frame:continue # sparse frames inherit the already shifted root
     offset,pose=frame[root];struct.pack_into('>3f',out,offset,*(np.array(pose[:3])+delta))
   for face in triangles:
    for offset,c,weights in face:
     memberships={groups[i] for i,w in weights if w>1e-6}
     assert len(memberships)==1,'Fish/platform blend requires a separate rig edit'
     name=next(iter(memberships))
     if name is not None:struct.pack_into('>3f',out,offset,*(np.array(c[:3])+deltas[name]))
   (target/filename).write_bytes(out)
   before=Bmd(original);after=Bmd(bytes(out));assert before.bones==after.bones
   for a,b in zip(before.faces,after.faces):
    for (c,w),(d,ww) in zip(a,b):assert c[3:]==d[3:] and w==ww
  prefix=PREFIX+'978_tatsugiri/'+form+'/'
  (target/'model.pqc').write_text(f'$body {prefix}1.bmd\n$anim idle {prefix}idle.bmd\n$anim walk {prefix}walk.bmd\n$scale 20\n',encoding='utf8')
  records.append({'form':form,'lead_fish':'red' if form=='megadroopy' else 'yellow','group_translation':{k:v.tolist() for k,v in deltas.items()},'operation':'Translate existing smooth fish and matching model/animation attachment roots together; keep platform, topology, texture, normals, UV and weights','folder':str(target.relative_to(R)).replace('\\','/')})
 return records

def run():
 variants={'magearna':magearna(),'tatsugiri':tatsugiri()}
 path=R/'backport/content.json';data=json.loads(path.read_text(encoding='utf8'))
 manifest_path=R/'backport/za-variant-assets.json';manifest=json.loads(manifest_path.read_text(encoding='utf8'))
 for patch in data['patches']:
  species=patch['species'];dex=species['dex']
  for form in species['forms']:
   name=form['name']
   if not(dex==801 and name=='megaoriginal' or dex==978 and name in ('megadroopy','megastretchy')):continue
   entry=next(m for m in manifest['models'] if m['dex']==dex and m['form']==name);entry['shared_original_visual']=False
   for gender in form['genderProperties']:
    for palette in gender['palettes']:
     folder=f'smooth_variants/{dex}_{"magearna" if dex==801 else "tatsugiri"}/{name}'+('/'+palette['name'] if dex==801 else '')
     palette['modelLocator']={'factoryType':'NORMAL','pqc':['pixelmon_backport:'+folder+'/model.pqc']}
   root=R/f'assets/pixelmon_backport/textures/smooth_variants/{dex}_{"magearna" if dex==801 else "tatsugiri"}/{name}'
   entry['files']=[f for f in entry['files'] if 'smooth_variants/' not in f['resource']]
   entry['files'] += [{'resource':str(p.relative_to(R)).replace('\\','/'),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in sorted(root.rglob('*')) if p.is_file()]
 manifest.update({'operation':'Preserve accepted smooth source visuals; derive missing variants using UV material selection and fish/rig arrangement edits only','visual_policy':'Distinct smooth variant candidates implemented; original PNG bytes preserved; manual appearance acceptance pending','derived_variants':variants})
 dump(path,data);dump(manifest_path,manifest)
 print('Smooth candidates: original-color Magearna silver gears/lower gown; red-leading Droopy and yellow-leading Stretchy Tatsugiri. Original raster assets unchanged.')

if __name__=='__main__':run()
