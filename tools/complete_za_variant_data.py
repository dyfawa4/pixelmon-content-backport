"""Restore accepted Pixelmon visuals/routes, then derive smooth variants.

Always start from the untouched source, so repeated generation cannot accumulate
UV/rig edits or replace accepted smooth models with the retired cube assets.
"""
import copy,hashlib,io,json,pathlib,urllib.request
from PIL import Image
from convert_content import dump
ROOT=pathlib.Path(__file__).resolve().parents[1];R=ROOT/'resources'

def run():
 content=json.loads((R/'backport/content.json').read_text(encoding='utf8'))
 manifest={'source':'Pixelmon-9.13-1.9.jar','source_sha256':json.loads((R/'backport/za-mega-import.json').read_text(encoding='utf8'))['sha256'],
  'operation':'Restore existing smooth Pixelmon model, skeleton, animations and ordinary/shiny textures unchanged; retain separate origin routes and portraits.',
  'visual_policy':'Shared original visuals, not newly completed distinct variant artwork. The source supplies one Mega model and ordinary/shiny pair per species; original-color-specific and Tatsugiri-pose-specific mesh/material differences are not claimed.', 'models':[]}
 avatars=json.loads((R/'backport/za-mega-avatar-sources.json').read_text(encoding='utf8'))
 def sprite(dex,form,index,shiny):
  old=next(x for x in avatars['targets'] if x['dex']==dex and x['form']=='mega' and x['palette']==('shiny' if shiny else 'none'))
  url=old['url'].replace(f'{dex:04d}_30.png',f'{dex:04d}_{index}.png');cache=ROOT/f'.cache/za-mega-160/variant-avatar-{dex}-{index}-{shiny}.png'
  if not cache.exists():cache.write_bytes(urllib.request.urlopen(url,timeout=25).read())
  raw=cache.read_bytes();image=Image.open(io.BytesIO(raw)).convert('RGBA');assert image.size in ((64,32),(32,32))
  relative=f'assets/pixelmon_backport/textures/za_variants/{dex}_{"magearna" if dex==801 else "tatsugiri"}/{form}/sprite_{"shiny" if shiny else "none"}.png';target=R/relative;target.parent.mkdir(parents=True,exist_ok=True)
  # Exact first native-size animation frame, no resampling or recoloring.
  frame=image.crop((0,0,32,32));frame.save(target)
  avatars['targets']=[a for a in avatars['targets'] if not(a['dex']==dex and a['form']==form and a['palette']==old['palette'])]
  avatars['targets'].append({'dex':dex,'form':form,'palette':old['palette'],'resource':relative,'url':url,'source_sha256':hashlib.sha256(raw).hexdigest(),'resource_sha256':hashlib.sha256(target.read_bytes()).hexdigest(),'operation':'first 32x32 frame, exact RGBA pixels, no resizing or redraw'})
  return 'pixelmon_backport:'+relative.split('textures/',1)[1]
 for dex in (801,978):
  species=next(p['species'] for p in content['patches'] if p['species']['dex']==dex);forms=species['forms'];base=copy.deepcopy(next(f for f in forms if f['name']=='mega'))
  wanted=[('mega','',30),('megaoriginal','original_color',31)] if dex==801 else [('mega','curly',30),('megadroopy','droopy',31),('megastretchy','stretchy',32)]
  for name,origin,index in wanted:
   current=next((f for f in forms if f['name']==name),None)
   if current is None:current=copy.deepcopy(base);forms.append(current)
   current['name']=name;current['defaultBaseForm']=origin;current['megas']=[name]
   # Restore exact pre-Bedrock resource references; ordinary/shiny never stand
   # in for the original_color form. Form and shiny palette are separate axes.
   species_folder=f'reference/pokemon/{dex}_{"magearna" if dex==801 else "tatsugiri"}/all/mega'
   folder='pixelmon_backport:'+species_folder
   for gender in current['genderProperties']:
    for palette in gender['palettes']:
     shiny=palette['name']=='shiny'
     color='shiny' if shiny else 'none'
     palette['texture']=folder+'/'+color+'/texture.png';palette['emissive']=folder+'/'+color+'/emissive.png';palette['modelLocator']={'factoryType':'NORMAL','pqc':[folder+'/none/model.pqc']};palette['sprite']=sprite(dex,name,index,shiny)
   files=[R/f'assets/pixelmon_backport/textures/{species_folder}/{file}' for file in ('none/1.bmd','none/idle.bmd','none/walk.bmd','none/model.pqc','none/texture.png','none/emissive.png','shiny/texture.png','shiny/emissive.png')]
   manifest['models'].append({'dex':dex,'form':name,'origin':origin,'shared_original_visual':True,'files':[{'resource':str(p.relative_to(R)).replace('\\','/'),'sha256':hashlib.sha256(p.read_bytes()).hexdigest()} for p in files]})
   source=next((f for f in forms if f['name']==origin),None)
   if source is None:source={'name':origin};forms.append(source)
   source['megaItems']=copy.deepcopy(base['megaItems']);source['megas']=[name]
   if name!='mega':
    spawn=json.loads((R/f'data/pixelmon/spawning/megas/backport_{species["name"].lower()}_mega.set.json').read_text(encoding='utf8'))
    spawn['id']='Backport Mega '+species['name']+' '+origin
    for info in spawn['spawnInfos']:info['spec']=info['spec'].replace('form:mega','form:'+name)
    dump(R/f'data/pixelmon/spawning/megas/backport_{species["name"].lower()}_{name}.set.json',spawn)
 dump(R/'backport/content.json',content);dump(R/'backport/za-mega-avatar-sources.json',avatars);dump(R/'backport/za-variant-assets.json',manifest)
 # Only remove the retired converter's known outputs; keep variant portraits.
 obsolete_root=(R/'assets/pixelmon_backport/textures/za_variants').resolve()
 for path in list(obsolete_root.rglob('*')):
  if path.is_file() and not path.name.startswith('sprite_'):
   assert path.resolve().is_relative_to(obsolete_root)
   path.unlink()
 for name in ('MEGA_VARIANT_CREDITS.txt','MEGA_VARIANT_LICENSE.md'):
  (R/'META-INF'/name).unlink(missing_ok=True)
 for language in ('zh_cn','en_us'):
  path=R/f'assets/pixelmon_backport/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf8'))
  labels={'megadroopy':('超级进化（下垂姿势）','Mega Evolution (Droopy)'), 'megastretchy':('超级进化（平挺姿势）','Mega Evolution (Stretchy)'), 'megaoriginal':('超级进化（500年前的颜色）','Mega Evolution (Original Color)')}
  for name,values in labels.items():lang['pixelmon.generic.form.'+name]=values[0 if language=='zh_cn' else 1]
  dump(path,lang)
 # Build derived smooth candidates after the accepted source is restored, so
 # either historical entry point reproduces the current complete candidates.
 from complete_smooth_mega_variants import run as complete_visuals
 complete_visuals()

if __name__=='__main__':run()
