"""Add missing ZA Mega forms only; never import the reference's global rules/learnsets."""
import argparse,copy,hashlib,json,pathlib,zipfile
from extend_reference import asset_converter
from convert_content import NS,dump
ROOT=pathlib.Path(__file__).resolve().parents[1]
STONE_NAMES={358:'chimechite',359:'absolite_z',398:'staraptite',445:'garchompite_z',448:'lucarionite_z',485:'heatranite',623:'golurkite',678:'meowsticite',740:'crabominite',768:'golisopodite',801:'magearnite',807:'zeraorite',952:'scovillainite',970:'glimmorite',978:'tatsugirinite',998:'baxcaliburite'}
DISPLAY_NAMES={'golisopodite':'Golisopite','glimmorite':'Glimmoranite','baxcaliburite':'Baxcalibrite'}
def read_species(z):return {s['dex']:s for n in z.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('.json') for s in [json.loads(z.read(n))]}
def run(reference,native,output):
    d=json.loads((output/'backport/content.json').read_text(encoding='utf-8'))
    patches={p['species']['dex']:p for p in d['patches']}
    manifest_path=output/'backport/za-mega-import.json'
    previous=json.loads(manifest_path.read_text(encoding='utf-8')) if manifest_path.exists() else {}
    with zipfile.ZipFile(reference) as source,zipfile.ZipFile(native) as original:
        incoming,old=read_species(source),read_species(original)
        effective={dex:{f['name']:f for f in s['forms']} for dex,s in old.items()}
        for dex,p in patches.items():effective.setdefault(dex,{}).update({f['name']:f for f in p['species']['forms']})
        selected=[]
        for dex,s in incoming.items():
            for f in s['forms']:
                if dex in STONE_NAMES and 'mega' in f['name'] and f['name'] not in effective.get(dex,{}):selected.append((dex,f))
        convert,exported,warnings=asset_converter(source,output)
        imported=[];items=[]
        for dex,f in selected:
            if dex not in STONE_NAMES:raise ValueError('Unreviewed source mega #'+str(dex))
            s=incoming[dex];name=f['name'];stone=STONE_NAMES[dex]
            if dex not in patches:patches[dex]={'mode':'merge','species':{'name':s['name'],'dex':dex,'generation':s['generation'],'forms':[]}}
            target=patches[dex]['species']['forms'];converted=convert(f)
            # A Mega must inherit an existing base form, never a source's unrelated rewrites.
            target.append(converted)
            linked=[]
            for base in s['forms']:
                pairs=[(i,dest) for i,dest in zip(base.get('megaItems',[]),base.get('megas',[])) if dest==name]
                if not pairs:continue
                ours=next((x for x in target if x['name']==base['name']),None)
                if ours is None:ours={'name':base['name']};target.append(ours)
                ours.setdefault('megaItems',[]);ours.setdefault('megas',[])
                item=NS+':'+stone
                if item not in ours['megaItems']:ours['megaItems'].append(item);ours['megas'].append(name)
                linked.append(base['name'])
            if not linked:raise ValueError('Unreachable Mega '+s['name']+'/'+name)
            if not any(i['name']==stone for i in d['referenceItems']):d['referenceItems'].append({'name':stone,'kind':'mega','form':name})
            # The icon is supplied separately in native pixel-art style; do not copy smooth source stones.
            dump(output/f'assets/{NS}/models/item/{stone}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+stone}})
            source_lang=json.loads(source.read('assets/pixelmon/lang/zh_cn.json'))
            chinese=source_lang.get('pixelmon.'+s['name'].lower(),s['name'])
            for language in ['zh_cn','en_us']:
                path=output/f'assets/{NS}/lang/{language}.json';lang=json.loads(path.read_text(encoding='utf-8'))
                lang[f'item.{NS}.{stone}']=(chinese+'进化石'+(' Z' if name=='megaz' else '')) if language=='zh_cn' else DISPLAY_NAMES.get(stone,stone.replace('_',' ').title())
                lang[f'item.{NS}.{stone}.tooltip']=('让对应的宝可梦携带后，使用超级进化。' if language=='zh_cn' else 'Allows its compatible Pokemon to Mega Evolve while held.')
                dump(path,lang)
            # Use a dedicated spawn-set name, leaving old Mega boss tables intact.
            spawn='data/pixelmon/spawning/megas/mega_'+s['name'].lower()+'.set.json'
            if spawn in source.namelist():
                spawns=json.loads(source.read(spawn));
                spawns['id']='Backport Mega '+s['name']+(' Z' if name=='megaz' else '')
                for row in spawns.get('spawnInfos',[]):
                    if 'spec' in row and isinstance(row['spec'],dict):row['spec']['form']=name
                    elif 'spec' in row and isinstance(row['spec'],str):
                        import re
                        row['spec']=re.sub(r'(?i)form:[^ ]+', 'form:'+name,row['spec']) if re.search(r'(?i)form:',row['spec']) else row['spec']+' form:'+name
                dump(output/f'data/pixelmon/spawning/megas/backport_{s["name"].lower()}_{name}.set.json',spawns)
            imported.append({'dex':dex,'species':s['name'],'form':name,'base_forms':linked,'stone':stone,'source_abilities':f.get('abilities',{})});items.append(stone)
        # The reference has seven incorrect Mega stat rows. Keep the reviewed
        # corrections reproducible, rather than restoring them on reimport.
        corrections=ROOT/'docs/ZA_MEGA_STAT_CORRECTIONS_1.6.0.json'
        if corrections.exists():
            for row in json.loads(corrections.read_text(encoding='utf-8'))['corrections']:
                for form in patches[row['dex']]['species']['forms']:
                    if form['name']==row['form']:form['battleStats']=copy.deepcopy(row['after'])
        d['patches']=sorted(patches.values(),key=lambda p:p['species']['dex']);dump(output/'backport/content.json',d)
        combined={(f['dex'],f['form']):f for f in previous.get('forms',[])}
        combined.update({(f['dex'],f['form']):f for f in imported})
        manifest={'source':reference.name,'sha256':hashlib.sha256(reference.read_bytes()).hexdigest(),'forms':list(combined.values()),'assets':previous.get('assets',0)+len(exported),'warnings':previous.get('warnings',[])+warnings,'stone_style':previous.get('stone_style','Source smooth icons excluded; native pixel style icons required before packaging.')}
        dump(output/'backport/za-mega-import.json',manifest)
        print(json.dumps({'forms':imported,'assets':len(exported),'warnings':len(warnings)},ensure_ascii=False,indent=2))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--reference',type=pathlib.Path,required=True);p.add_argument('--native',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,default=ROOT/'resources');a=p.parse_args();run(a.reference,a.native,a.output)
