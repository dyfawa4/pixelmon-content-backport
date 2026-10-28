"""Original pixel atlas, drawn directly at the user's explicit request; no borrowed textures."""
from pathlib import Path
import json, random
from PIL import Image, ImageDraw
ROOT=Path(__file__).resolve().parents[1]
atlas=Image.new('RGBA',(64,64));rng=random.Random(20261028)
colours=[(57,66,77),(17,23,29),(190,140,43),(8,59,40),(68,226,107),(201,255,231),(208,39,48),(44,57,75)]
for slot,colour in enumerate(colours):
 tile=Image.new('RGBA',(16,16),colour+(255,));d=ImageDraw.Draw(tile)
 for y in range(16):
  for x in range(16):
   shade=rng.choice([-6,-3,0,3,6]) if slot not in [3,4,5,6] else rng.choice([-2,0,2])
   tile.putpixel((x,y),tuple(max(0,min(255,v+shade)) for v in colour)+(255,))
 if slot in [0,7]:
  for y in [2,6,10,14]:d.line((0,y,15,y),fill=tuple(v+8 for v in colour)+(255,))
  for x,y in [(2,2),(13,2),(2,13),(13,13)]:d.point((x,y),fill=(102,117,130,255))
 if slot==1:d.line((0,0,15,0),fill=(39,49,58,255));d.line((0,1,0,15),fill=(31,39,46,255))
 if slot==2:
  for y,c in [(0,(247,205,89,255)),(1,(232,183,62,255)),(8,(211,163,54,255)),(15,(133,85,22,255))]:d.line((0,y,15,y),fill=c)
 if slot==3:
  for p in [3,7,11,15]:d.line((p,0,p,15),fill=(15,77,50,255));d.line((0,p,15,p),fill=(15,77,50,255))
  d.ellipse((2,2,13,13),outline=(30,120,72,255));d.line((8,8,12,4),fill=(44,160,91,255));d.point((5,6),fill=(134,247,158,255))
 if slot==6:d.rectangle((2,2,13,13),outline=(236,75,80,255));d.line((4,3,11,3),fill=(255,136,136,255))
 for repeat in [0,2]:atlas.alpha_composite(tile,((slot%4)*16,(slot//4+repeat)*16))
target=ROOT/'resources/assets/pixelmon_backport/textures/item/boss_spawn_checker.png';target.parent.mkdir(parents=True,exist_ok=True);atlas.save(target)
p=ROOT/'resources/assets/pixelmon_backport/models/item/boss_spawn_checker.json';model=json.loads(p.read_text('utf-8'))
slots={'case':0,'black':1,'gold':2,'green':3,'lime':4,'white':5,'red':6}
for element in model['elements']:
 for face in element['faces'].values():
  material=face['texture'].lstrip('#');slot=slots[material];u=(slot%4)*4;v=(slot//4)*4
  face['uv']=[u,v,u+4,v+4]
model['textures']={key:'pixelmon_backport:item/boss_spawn_checker' for key in slots};model['textures']['particle']='pixelmon_backport:item/boss_spawn_checker';model['texture_size']=[64,64]
p.write_text(json.dumps(model,indent=2)+'\n','utf-8')
preview=ROOT/'docs/previews/boss-checker-pixel-atlas.png';atlas.resize((512,512),Image.Resampling.NEAREST).save(preview)
print(target)
