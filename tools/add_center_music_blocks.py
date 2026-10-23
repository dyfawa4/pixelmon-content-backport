"""Restore native invisible ambient-music blocks in reviewed center/shop templates."""
from pathlib import Path
from zipfile import ZipFile
import argparse,gzip,io,json
import nbtlib
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'resources'
def wanted(name):
 return name.endswith('.nbt') and (name.endswith(('poke_center.nbt','_pokecenter.nbt','_pokemart.nbt','_mart.nbt')) or '_tcg_shop' in name)
def patch(blob):
 data=nbtlib.File.parse(io.BytesIO(gzip.decompress(blob)));palette=data['palette'];names=[str(p['Name']) for p in palette];size=[int(v) for v in data['size']]
 music=[b for b in data['blocks'] if names[int(b['state'])]=='pixelmon:music_block']
 if music:block=music[0]
 else:
  candidates=[b for b in data['blocks'] if names[int(b['state'])] in {'minecraft:air','minecraft:cave_air','minecraft:void_air'} and 1<=int(b['pos'][1])<=max(2,size[1]-3)]
  if not candidates:raise ValueError('No safe air cell in center/shop')
  block=min(candidates,key=lambda b:(int(b['pos'][0])-size[0]/2)**2+(int(b['pos'][2])-size[2]/2)**2+.5*(int(b['pos'][1])-size[1]/3)**2)
  palette.append(nbtlib.Compound({'Name':nbtlib.String('pixelmon:music_block'),'Properties':nbtlib.Compound({'facing':nbtlib.String('north')})}));block['state']=nbtlib.Int(len(palette)-1)
 # A symmetric horizontal footprint remains correct through every jigsaw rotation.
 radius=min(6,max(size[0],size[2])//2);height=min(8,max(3,size[1]//2));pos=[int(v) for v in block['pos']]
 tag=nbtlib.Compound({'id':nbtlib.String('pixelmon:music_block'),'track':nbtlib.String('pixelmon:pixelmon.music.pokecenter'),'BackportAmbient':nbtlib.Byte(1),
  'offsetX':nbtlib.Int(0),'offsetY':nbtlib.Int(0),'offsetZ':nbtlib.Int(0),'sizeX':nbtlib.Int(radius),'sizeY':nbtlib.Int(height),'sizeZ':nbtlib.Int(radius)})
 block['nbt']=tag
 stream=io.BytesIO();data.write(stream)
 return gzip.compress(stream.getvalue(),mtime=0),{'position':pos,'size':[radius,height,radius]}
def apply(pixelmon):
 sources={}
 with ZipFile(pixelmon) as z:
  sources.update({name:z.read(name) for name in z.namelist() if name.startswith('data/pixelmon/structures/') and wanted(name)})
 for path in (RES/'data').rglob('*.nbt'):
  relative=path.relative_to(RES).as_posix()
  if wanted(relative):sources[relative]=path.read_bytes()
 manifest={}
 for name,blob in sorted(sources.items()):
  updated,info=patch(blob);out=RES/name;out.parent.mkdir(parents=True,exist_ok=True);out.write_bytes(updated)
  parts=name.split('/');ident=parts[1]+':'+name.split('/structures/',1)[1][:-4];manifest[ident]=info
 target=RES/'backport/center_music_templates.json';target.write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n','utf-8')
 print(f'Restored center music in {len(manifest)} native/themed templates; air cells only, no solid blocks removed.')
 return manifest
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);apply(p.parse_args().pixelmon)
