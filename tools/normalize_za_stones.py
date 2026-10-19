"""Reuse native 16px stone artwork and its exact inventory/hand proportions."""
import argparse,hashlib,json,pathlib,zipfile
from convert_content import dump,NS
ROOT=pathlib.Path(__file__).resolve().parents[1]
# Existing native palette variants; no resampling, smoothing or new raster art.
NATIVE={
 'chimechite':'medichamite','absolite_z':'absolite','staraptite':'pidgeotite',
 'garchompite_z':'garchompite','lucarionite_z':'lucarionite','heatranite':'cameruptite',
 'golurkite':'steelixite','meowsticite':'gardevoirite','crabominite':'abomasite',
 'golisopodite':'scizorite','magearnite':'diancite','zeraorite':'manectite',
 'scovillainite':'sceptilite','glimmorite':'tyranitarite','tatsugirinite':'swampertite',
 'baxcaliburite':'glalitite'}
def run(native,output):
 rows=[]
 with zipfile.ZipFile(native) as z:
  for name,source in NATIVE.items():
   path=f'assets/pixelmon/textures/items/helditems/{source}.png';blob=z.read(path)
   target=output/f'assets/{NS}/textures/item/{name}.png';target.write_bytes(blob)
   dump(output/f'assets/{NS}/models/item/{name}.json',{'parent':'pixelmon:item/pixelmon_item','textures':{'layer0':f'{NS}:item/{name}'}})
   rows.append({'item':name,'source':path,'sha256':hashlib.sha256(blob).hexdigest(),'operation':'reuse native PNG bytes unchanged; 16x16 canvas, native 12x12 visible stone; shared native palette artwork'})
 dump(output/'backport/za-mega-stone-sources.json',{'source':'Installed native Pixelmon stone palette variants; upstream rights retained. Distinct new registry names and evolution rules are unchanged.','items':rows})
 p=output/'backport/za-mega-import.json';manifest=json.loads(p.read_text(encoding='utf-8'));manifest['stone_style']='Native Pixelmon 16x16 stone icons with native transparent margin; reuse original palette artwork without editing or scaling.';dump(p,manifest)
 print('Reused 16 native stone icons; no image resampling or mechanics changes.')
if __name__=='__main__':
 p=argparse.ArgumentParser();p.add_argument('--native',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,default=ROOT/'resources');a=p.parse_args();run(a.native,a.output)
