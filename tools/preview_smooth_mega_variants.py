"""Offline geometry/atlas diagnostic, no game or source-raster modification."""
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw,ImageFont
from convert_content import Bmd

ROOT=Path(__file__).resolve().parents[1];R=ROOT/'resources/assets/pixelmon_backport/textures'
ROWS=[('Curly: orange centre',R/'reference/pokemon/978_tatsugiri/all/mega/none/1.bmd',R/'reference/pokemon/978_tatsugiri/all/mega/none/texture.png'),('Droopy: red centre',R/'smooth_variants/978_tatsugiri/megadroopy/1.bmd',R/'reference/pokemon/978_tatsugiri/all/mega/none/texture.png'),('Stretchy: yellow centre',R/'smooth_variants/978_tatsugiri/megastretchy/1.bmd',R/'reference/pokemon/978_tatsugiri/all/mega/none/texture.png'),('Magearna: existing colors',R/'reference/pokemon/801_magearna/all/mega/none/1.bmd',R/'reference/pokemon/801_magearna/all/mega/none/texture.png'),('Original Color: silver gears',R/'smooth_variants/801_magearna/megaoriginal/none/model.bmd',R/'reference/pokemon/801_magearna/all/mega/none/texture.png')]

def run():
 canvas=Image.new('RGB',(1800,950),'#eeeeef');draw=ImageDraw.Draw(canvas)
 try:font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',25)
 except OSError:font=ImageFont.load_default()
 draw.text((25,12),'Smooth mesh candidates / bind pose + existing atlas / manual acceptance pending',fill='#222222',font=font)
 for k,(label,body,texture) in enumerate(ROWS):
  image=Image.open(texture).convert('RGB');model=Bmd(body.read_bytes());vertices=np.array([[v[0][:3] for v in f] for f in model.faces]);centre=(vertices.min((0,1))+vertices.max((0,1)))/2
  azimuth=np.radians(-80 if k<3 else -65);elevation=np.radians(30);camera=np.array([np.cos(elevation)*np.cos(azimuth),np.cos(elevation)*np.sin(azimuth),np.sin(elevation)])
  right=np.cross([0,0,1],camera);right/=np.linalg.norm(right);up=np.cross(camera,right);projection=np.stack((right,up,camera),axis=1);points=(vertices-centre)@projection
  scale=370/max(np.ptp(points[:,:,0]),np.ptp(points[:,:,1]));screen=points[:,:,:2]*[scale,-scale];offset=np.array([(k%3)*600+300,(k//3)*440+260]);screen+=offset
  tris=[]
  for idx,face in enumerate(model.faces):
   c=np.array([v[0] for v in face]);uv=c[:,6:8].mean(0);x=int(np.clip(uv[0]*image.width,0,image.width-1));y=int(np.clip((1-uv[1])*image.height,0,image.height-1));color=np.array(image.getpixel((x,y)),float)
   normal=c[:,3:6].mean(0);normal/=max(1e-9,np.linalg.norm(normal));shade=.7+.3*max(0,np.dot(normal,[.4,-.6,.7]));rgb=tuple(np.clip(color*shade,0,255).astype(int));tris.append((points[idx,:,2].mean(),screen[idx],rgb))
  for _,triangle,color in sorted(tris,key=lambda item:item[0]):draw.polygon([tuple(p) for p in triangle],fill=color)
  draw.text(((k%3)*600+25,(k//3)*440+58),label,fill='#222222',font=font)
 out=ROOT/'docs/evidence/smooth-mega-variants-2026-10-19.png';out.parent.mkdir(parents=True,exist_ok=True);canvas.save(out);print(out)

if __name__=='__main__':run()
