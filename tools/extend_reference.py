"""Extend the official conversion with the user's legacy ZA reference assets.

This is an offline build step, not a replacement of the installed Pixelmon.
All original assets retain their ownership. The supplied wrapper format is
decoded exactly as its own SmdModel/SmdAnimation readers do, into native BMD.
"""
import copy, io, json, pathlib, re, struct, zipfile
from convert_content import ROOT, NS, Bmd, align_animation, dump, asset

def asset_converter(reference, output, item_names=()):
    exported={}; warnings=[]
    def normalize_bone_names(blob,source):
        # The native 1.16 Bone.addVertex rejects the exporter's dummy name.
        # Rigid vertices carry a real parent ID, which can point at this static
        # anchor. Rename only node names, preserving IDs, hierarchy and poses.
        fp=io.BytesIO(blob);assert fp.read(1)==b'\x01'
        count=struct.unpack('>h',fp.read(2))[0];out=io.BytesIO();out.write(blob[:3])
        renamed=False
        for _ in range(count):
            out.write(fp.read(4));chars=[]
            while (char:=struct.unpack('>H',fp.read(2))[0]):chars.append(chr(char))
            name=''.join(chars)
            if name=='blender_implicit':name='backport_rigid_anchor';renamed=True
            out.write((name+'\0').encode('utf-16-be'))
        out.write(fp.read())
        if renamed:warnings.append({'renamed_export_anchor':source})
        return out.getvalue()
    def repair_skin(blob,source):
        fp=io.BytesIO(blob)
        def read(fmt):
            s=struct.Struct('>'+fmt);return s.unpack(fp.read(s.size))
        assert read('B')[0]==1
        bones=read('h')[0]
        for _ in range(bones):
            read('hh')
            while read('H')[0]:pass
        for _ in range(read('h')[0]):
            for _ in range(read('h')[0]):read('h6f')
        for _ in range(read('h')[0]):
            while read('H')[0]:pass
        count=read('h')[0];out=io.BytesIO();out.write(blob[:fp.tell()]);fixed=0
        for _ in range(count):
            out.write(fp.read(1))
            for _ in range(3):
                start=fp.tell();rigid=read('h')[0];fp.read(32)
                out.write(blob[start:fp.tell()]);links=read('B')[0]
                weights=[read('hf') for _ in range(links)]
                if not weights:
                    # Legacy exporters may omit links for rigid geometry. The
                    # encoded parent is authoritative; root only for -1.
                    rigid=rigid if rigid>=0 else 0
                    if not 0<=rigid<bones:raise ValueError('Invalid rigid bone '+source)
                    weights=[(rigid,1.0)];fixed+=1
                total=sum(w for _,w in weights)
                if total<=0:raise ValueError('Invalid weights '+source)
                if abs(total-1)>.002:weights=[(b,w/total) for b,w in weights];fixed+=1
                out.write(struct.pack('>B',len(weights)))
                for b,w in weights:out.write(struct.pack('>hf',b,w))
        assert not fp.read()
        if fixed:warnings.append({'repaired_skin':source,'vertices':fixed})
        return out.getvalue()
    def decode(blob):
        if blob.startswith(b'ZHIKEMENG'):
            key=b'ZhiKeMen';blob=bytes(x ^ key[i%len(key)] for i,x in enumerate(blob[9:]))
        return blob
    def write(path,blob):
        target=output/path;target.parent.mkdir(parents=True,exist_ok=True);target.write_bytes(blob)
    def copy_ref(ref):
        source=asset(ref)
        if source not in reference.namelist():raise ValueError('Missing custom resource '+source)
        result=NS+':reference/'+ref.split(':',1)[1].replace('.zhikemeng','.bmd')
        target=asset(result)
        if source in exported:return exported[source]
        exported[source]=result
        blob=decode(reference.read(source))
        if source.endswith('.pqc'):
            lines=[];base=ref.split(':',1)[1].rsplit('/',1)[0]
            body=None;animations=[]
            for line in blob.decode('utf-8-sig').splitlines():
                words=line.split()
                if not words:continue
                if words[0] in ('$body','$anim'):
                    index=1 if words[0]=='$body' else 2
                    source_ref=words[index] if ':' in words[index] else 'pixelmon:'+base+'/'+words[index]
                    words[index]=copy_ref(source_ref)
                    ns,path=words[index].split(':',1)
                    words[index]=ns+':'+(path if path.startswith('textures/') else 'textures/'+path)
                    ns,path=words[index].split(':',1)
                    resource=output/f'assets/{ns}/{path}'
                    if words[0]=='$body':body=resource
                    else:animations.append(resource)
                lines.append(' '.join(words))
            if body and body.suffix=='.bmd':
                Bmd(body.read_bytes())
                for anim in animations:
                    if anim.suffix=='.bmd':
                        aligned,changed=align_animation(body.read_bytes(),anim.read_bytes())
                        if changed:anim.write_bytes(aligned);warnings.append({'aligned':str(anim.relative_to(output))})
            blob=('\n'.join(lines)+'\n').encode('utf-8')
        elif source.endswith(('.bmd','.zhikemeng')):
            Bmd(blob);blob=normalize_bone_names(blob,source);blob=repair_skin(blob,source)
        write(target,blob)
        return result
    def convert(form):
        result=copy.deepcopy(form)
        for group in result.get('genderProperties',[]):
            for p in group.get('palettes',[]):
                for key in ['sprite','texture','emissive']:
                    if p.get(key):p[key]=copy_ref(p[key])
                if 'modelLocator' in p:
                    p['modelLocator']['pqc']=[copy_ref(x) for x in p['modelLocator']['pqc']]
        for key in ['megaItems']:
            if key in result:result[key]=[x.replace('pixelmon:',NS+':') for x in result[key]]
        for evo in result.get('evolutions',[]):
            item=evo.get('item',{}).get('itemID','')
            if item.split(':')[-1] in item_names:evo['item']['itemID']=item.replace('pixelmon:',NS+':')
        return result
    return convert, exported, warnings

def run(reference, old_path, output):
    data=json.loads((output/'backport/content.json').read_text(encoding='utf-8'))
    patches={p['species']['dex']:p for p in data['patches']}
    reference=zipfile.ZipFile(reference);old=zipfile.ZipFile(old_path)
    def species(z):return {v['dex']:v for n in z.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('.json') for v in [json.loads(z.read(n))]}
    rs,os=species(reference),species(old)
    item_names=[n.split('/')[-1][:-5] for n in reference.namelist() if n.startswith('assets/pixelmon/models/item/') and n not in old.namelist() and n.endswith('.json')]
    convert,exported,warnings=asset_converter(reference,output,item_names)
    extra_forms=[];connections=[]
    for dex,source in sorted(rs.items()):
        original=os.get(dex)
        if original is None:continue # already converted from the official version
        oldforms={f['name']:f for f in original['forms']}
        oldpatch=patches.get(dex)
        incoming={'name':source['name'],'dex':dex,'generation':source['generation'],'forms':[]}
        if oldpatch:incoming=oldpatch['species']
        for form in source['forms']:
            name=form['name'];native=oldforms.get(name)
            current=next((f for f in incoming['forms'] if f['name']==name),None)
            if native is None:
                if current is None:incoming['forms'].append(convert(form));extra_forms.append([dex,name])
                elif 'pixelmon_backport:reference/' in json.dumps(current):
                    incoming['forms'][incoming['forms'].index(current)]=convert(form);extra_forms.append([dex,name])
                continue
            # Preserve established base models and canonical stats. Import the
            # reference's Mega links and evolutionary/learnset additions only.
            additions={k:form[k] for k in ['megaItems','megas','evolutions','moves'] if form.get(k)!=native.get(k) and k in form}
            if 'megaItems' in additions or 'megas' in additions:
                additions.update({k:form.get(k,[]) for k in ['megaItems','megas']})
            if not additions:continue
            if current is None:current={'name':name};incoming['forms'].append(current)
            converted=convert(dict(additions,name=name))
            if 'megaItems' in converted and 'megas' in converted:
                target_items=current.setdefault('megaItems',[]);target_forms=current.setdefault('megas',[])
                assert len(target_items)==len(target_forms)
                assert len(converted['megaItems'])==len(converted['megas'])
                for item,mega in zip(converted['megaItems'],converted['megas']):
                    if item not in target_items:target_items.append(item);target_forms.append(mega)
            for key,value in converted.items():
                if key in ['name','megaItems','megas']:continue
                if key not in current:current[key]=value
                elif isinstance(value,list):
                    current[key]+=[x for x in value if x not in current[key]]
                elif key=='moves':
                    for move_key,values in value.items():current[key].setdefault(move_key,[]).extend(x for x in values if x not in current[key].get(move_key,[]))
            connections.append([dex,name,list(additions)])
        if incoming['forms']:patches[dex]={'mode':'merge','species':incoming}
    # Reference item registry determines whether an icon is a Mega stone or a
    # regular item. Runtime behavior is implemented by native subclasses.
    item_spec=[]
    mega_items={i.split(':')[-1]:f['name'] for p in patches.values() for f in p['species']['forms'] for i in f.get('megaItems',[]) }
    # MegaItems belong to base forms; obtain destination form from megas.
    for p in patches.values():
        for f in p['species']['forms']:
            for i,dest in zip(f.get('megaItems',[]),f.get('megas',[])):mega_items[i.split(':')[-1]]=dest
    for name in item_names:
        model=json.loads(reference.read(f'assets/pixelmon/models/item/{name}.json'))
        ref=next(iter(model.get('textures',{}).values()),None)
        if ref is None:raise ValueError('No item icon '+name)
        ns,path=ref.split(':',1);icon=f'assets/{ns}/textures/{path}.png'
        write(f'assets/{NS}/textures/item/{name}.png',reference.read(icon))
        dump(output/f'assets/{NS}/models/item/{name}.json',{'parent':'minecraft:item/generated','textures':{'layer0':NS+':item/'+name}})
        item_spec.append({'name':name,'kind':'mega' if name in mega_items else 'item','form':'mega' if name in mega_items else None})
    # Retain stable old attack IDs. Deduplicate the reference's duplicated
    # Ivy Cudgel file by actual attack name, and use the official DLC effects.
    existing_moves={m['attackName'] for m in data['moves']}
    existing_moves|={json.loads(old.read(n))['attackName'] for n in old.namelist() if n.startswith('data/pixelmon/moves/') and n.endswith('.json')}
    for n in reference.namelist():
        if n.startswith('data/pixelmon/moves/') and n.endswith('.json'):
            move=json.loads(reference.read(n))
            if move['attackName'] in existing_moves:continue
            if move['attackName']!='Nihil Light':raise ValueError('Unreviewed move '+move['attackName'])
            move['attackIndex']=920;data['moves'].append(move);existing_moves.add(move['attackName'])
    data['patches']=[patches[d] for d in sorted(patches)]
    data['referenceItems']=item_spec
    # Additional drop/raid entries are merged by the native data loader. Keep
    # legacy data, rather than bundling a replacement core configuration.
    def rewrite(value):
        if isinstance(value,str) and value.startswith('pixelmon:') and value.split(':',1)[1] in item_names:return NS+':'+value.split(':',1)[1]
        if isinstance(value,list):return [rewrite(v) for v in value]
        if isinstance(value,dict):return {k:rewrite(v) for k,v in value.items()}
        return value
    extras={}
    for filename in ['drops/pokedrops','drops/pokechestdrops','raids/raids']:
        path='data/pixelmon/'+filename+'.json';before=json.loads(old.read(path));after=json.loads(reference.read(path))
        if filename=='drops/pokedrops':
            extras[filename]=rewrite([v for v in after if v not in before])
        elif filename=='drops/pokechestdrops':
            extras[filename]=rewrite({k:[v for v in after[k] if v not in before[k]] for k in after})
        else:
            extras[filename]={k:{'species':[v for v in after[k]['species'] if v not in before[k]['species']]} for k in after if before[k]!=after[k]}
    for path in reference.namelist():
        if path.startswith('data/pixelmon/daycare/') and path.endswith('.json') and old.read(path)!=reference.read(path):extras[path[len('data/pixelmon/'):-5]]=json.loads(reference.read(path))
    data['referenceExtras']=extras
    dump(output/'backport/content.json',data)
    # Merge reference translations for reference-only content; never replace
    # already verified official/override translations with older text.
    for lang in ['en_us','zh_cn']:
        path=output/f'assets/{NS}/lang/{lang}.json';entries=json.loads(path.read_text(encoding='utf-8'))
        translated=json.loads(reference.read(f'assets/pixelmon/lang/{lang}.json'))
        dex_names={rs[d]['name'].lower() for d in patches if d in rs}
        for key,value in translated.items():
            if any(name in key.lower().replace('_','') for name in dex_names):entries.setdefault(key,value)
        for name in item_names:entries.setdefault(f'item.{NS}.{name}',translated.get('item.pixelmon.'+name,name))
        entries[f'item.{NS}.eternatus_meteorite']='无极陨星' if lang=='zh_cn' else 'Eternatus Meteorite'
        entries[f'item.{NS}.eternatus_meteorite.tooltip']=('本模组新增规则：无极汰那携带后变为无极巨化形态；取下恢复普通形态。\n工作台合成：6 个末地石砖＋天界之笛＋陨石＋大师球。' if lang=='zh_cn' else 'Mod-added rule: transforms Eternatus into Eternamax while held; removing it restores its ordinary form.\nCraft with 6 end stone bricks, an Azure Flute, a Meteorite and a Master Ball.')
        entries['attack.nihil_light']='无尽之光' if lang=='zh_cn' else 'Nihil Light'
        entries['attack.nihil_light.description']='以强烈的光束攻击对手。此招式仅用于本参考版本的基格尔德超级进化。' if lang=='zh_cn' else 'A powerful beam used by this reference version of Mega Zygarde.'
        entries['ability.TeraformZero']='归零化境' if lang=='zh_cn' else 'Teraform Zero'
        entries['ability.TeraformZero.description']='变为星晶形态时，使天气和场地的效果消失。' if lang=='zh_cn' else 'Eliminates weather and terrain on changing to Stellar Form.'
        entries['ability.EmbodyAspect']='面影辉映' if lang=='zh_cn' else 'Embody Aspect'
        entries['ability.EmbodyAspect.description']='太晶化后，依面具形态提高对应的能力。' if lang=='zh_cn' else 'Raises a stat based on its mask after Terastallizing.'
        dump(path,entries)
    # Mega boss sets use the existing interval, not an independent new timer.
    for n in reference.namelist():
        if n.startswith('data/pixelmon/spawning/megas/') and n not in old.namelist() and n.endswith('.json'):
            write(n,reference.read(n))
    summary={'referenceForms':extra_forms,'connections':connections,'items':item_spec,'assets':len(exported),'warnings':warnings}
    dump(ROOT/'.cache/reference-conversion.json',summary)
    print(json.dumps({k:v for k,v in summary.items() if k!='connections'},ensure_ascii=False,indent=2))

if __name__=='__main__':
    import argparse
    p=argparse.ArgumentParser();p.add_argument('--reference',required=True,type=pathlib.Path);p.add_argument('--old',required=True,type=pathlib.Path);p.add_argument('--output',default=ROOT/'resources',type=pathlib.Path);a=p.parse_args();run(a.reference,a.old,a.output)
