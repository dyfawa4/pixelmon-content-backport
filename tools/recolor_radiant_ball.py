"""Exact palette swap requested by the user: never redraw, rescale or move a pixel."""
from pathlib import Path
import colorsys, json, hashlib
from PIL import Image

ROOT=Path(__file__).resolve().parents[1]
sources={
 'items/pokeballs':ROOT/'resources/assets/pixelmon_backport/textures/items/pokeballs/master_ball.png',
 'gui/overlay':ROOT/'.cache/radiant190/native-gui-overlay-master_ball.png',
 'pokeballs':ROOT/'.cache/radiant190/native-pokeballs-master_ball.png',
}
report={}
for folder,source in sources.items():
 original=Image.open(source).convert('RGBA');out=original.copy();changed=0
 for y in range(original.height):
  for x in range(original.width):
   r,g,b,a=original.getpixel((x,y));h,s,v=colorsys.rgb_to_hsv(r/255,g/255,b/255)
   # Purple/pink plastic only. Greys, white M/button, black seam and cyan UV parts stay byte-identical.
   target=a>0 and .69<=h<=.98 and s>=.25 and r>g*1.10 and b>g*1.20
   if folder=="pokeballs":target=a>0 and ((x<280 and y<215) or (x>430 and y>430)) and .60<=h<=.98 and s>=.12
   if not target:continue
   red,green,blue=colorsys.hsv_to_rgb(.115,max(.55,min(.85,s)),v if folder=="items/pokeballs" else .25+.75*v)
   out.putpixel((x,y),(round(red*255),round(green*255),round(blue*255),a));changed+=1
 dest=ROOT/'resources/assets/pixelmon_backport/textures'/folder/'radiant_ball.png'
 out.save(dest)
 assert original.getchannel('A').tobytes()==out.getchannel('A').tobytes()
 assert all(out.getpixel((x,y))==original.getpixel((x,y)) for y in range(original.height) for x in range(original.width) if original.getpixel((x,y))[:3][0]==original.getpixel((x,y))[:3][1]==original.getpixel((x,y))[:3][2])
 report[folder]={'source':str(source.relative_to(ROOT)),'size':original.size,'changed_pixels':changed,'alpha_exact':True,'source_sha256':hashlib.sha256(source.read_bytes()).hexdigest()}
 if folder=='items/pokeballs':
  preview=Image.new('RGBA',(1024,512),(64,64,64,255));preview.alpha_composite(original.resize((512,512),Image.Resampling.NEAREST),(0,0));preview.alpha_composite(out.resize((512,512),Image.Resampling.NEAREST),(512,0));preview.save(ROOT/'docs/previews/radiant-palette-comparison.png')
(ROOT/'.cache/resume190/palette-proof.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),'utf-8')
print(json.dumps(report,ensure_ascii=False))
