"""Verify visual-scope invariants independently of the generation selection."""
import pathlib,collections,json
from PIL import Image
from convert_content import Bmd

ROOT=pathlib.Path(__file__).resolve().parents[1];R=ROOT/'resources/assets/pixelmon_backport/textures'

def run():
 source=R/'reference/pokemon/801_magearna/all/mega/none';old=Bmd((source/'1.bmd').read_bytes())
 for palette in ('none','shiny'):
  new=Bmd((R/f'smooth_variants/801_magearna/megaoriginal/{palette}/model.bmd').read_bytes());image=Image.open(source.parent/palette/'texture.png').convert('RGB');changed=collections.Counter()
  emissive=Image.open(source.parent/palette/'emissive.png').convert('RGBA')
  assert old.bones==new.bones and old.frames==new.frames
  for a,b in zip(old.faces,new.faces):
   for (c,w),(d,ww) in zip(a,b):
    assert c[:6]==d[:6] and w==ww,'Magearna geometry/skin changed'
    if c[6:]!=d[6:]:
     name=old.bones[max(w,key=lambda t:t[1])[0]][1];changed[name]+=1
     assert name in ('left_horn_02','right_horn_02') or (name.startswith(('left_','right_')) and 'skirt' in name),'Unrelated material recolored'
     col=image.getpixel((max(0,min(image.width-1,int(d[6]*image.width))),max(0,min(image.height-1,int((1-d[7])*image.height)))))
     assert max(col)-min(col)<8 and min(col)>(145 if palette=='none' else 100),'Non-silver selected atlas material'
     x=max(0,min(emissive.width-1,int(d[6]*emissive.width)));y=max(0,min(emissive.height-1,int((1-d[7])*emissive.height)))
     assert emissive.getpixel((x,y))[3]==0,'Metal surface inherited eye/core glow'
  assert changed['left_horn_02']==600 and changed['right_horn_02']==600,'Floating gears not fully recolored'
  assert not changed['skirt'] and not changed['feeler_o'],'Waist or core recolored'
 source=R/'reference/pokemon/978_tatsugiri/all/mega/none';old=Bmd((source/'1.bmd').read_bytes())
 names={i:name for i,(_,name) in enumerate(old.bones)}
 def group(i):
  while i>=0:
   if names[i].startswith('root_waist_'):return names[i][-1]
   i=old.bones[i][0]
  return None
 def centre(model,which):
  coords=[c[0] for face in model.faces for c,w in face if group(max(w,key=lambda t:t[1])[0])==which];return (min(coords)+max(coords))/2
 for form,lead in (('megadroopy','b'),('megastretchy','a')):
  new=Bmd((R/f'smooth_variants/978_tatsugiri/{form}/1.bmd').read_bytes());assert old.bones==new.bones and len(old.faces)==len(new.faces)
  assert abs(centre(new,lead)-centre(old,'c'))<1e-6,'Wrong leading fish'
  for a,b in zip(old.faces,new.faces):
   for (c,w),(d,ww) in zip(a,b):
    assert c[3:]==d[3:] and w==ww,'Fish normals/UV/skin changed'
    if group(max(w,key=lambda t:t[1])[0]) is None:assert c==d,'Platter changed'
  for animation in ('idle.bmd','walk.bmd'):
   before=Bmd((source/animation).read_bytes());after=Bmd((R/f'smooth_variants/978_tatsugiri/{form}/{animation}').read_bytes())
   assert before.bones==after.bones and len(before.frames)==len(after.frames)
   for frame,result in zip(before.frames,after.frames):
    assert frame.keys()==result.keys()
    for i,pose in frame.items():
     if names[i].startswith('root_waist_'):assert pose[3:]==result[i][3:]
     else:assert pose==result[i],'Unrelated animation changed'
 print('PASS smooth variants: silver gears/lower gown, exact Magearna geometry/rig, protected waist/core; correct red/yellow leading fish, retained topology/UV/skin/platform and animation timing.')

if __name__=='__main__':run()
