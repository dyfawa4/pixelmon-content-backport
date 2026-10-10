"""Convert a user-supplied Pixelmon 9.4.1 JAR for the 1.16.5 extension.

No game paths are embedded. Upstream assets retain their original ownership.
"""
import argparse, copy, io, json, pathlib, struct, zipfile, lzma
from PIL import Image

NS = 'pixelmon_backport'
ROOT = pathlib.Path(__file__).resolve().parents[1]
NEW_TMS={'Temper Flare':207,'Supercell Slam':210,'Hard Press':225,'Dragon Cheer':226,'Alluring Voice':227,'Psychic Noise':228,'Upper Hand':229}

def class_utf8(data):
    """Read native enum literals without requiring decompiled source files."""
    fp=io.BytesIO(data);assert fp.read(4)==b'\xca\xfe\xba\xbe';fp.read(4)
    count=struct.unpack('>H',fp.read(2))[0];index=1;strings=[]
    sizes={3:4,4:4,5:8,6:8,7:2,8:2,9:4,10:4,11:4,12:4,15:3,16:2,17:4,18:4,19:2,20:2}
    while index<count:
        tag=fp.read(1)[0]
        if tag==1:
            length=struct.unpack('>H',fp.read(2))[0];strings.append(fp.read(length).decode('utf-8',errors='replace'))
        else:
            fp.read(sizes[tag])
            if tag in (5,6):index+=1
        index+=1
    return strings
def legacy_bmd(data):
    if data[0]==2:
        # A number of upstream assets omit the XZ footer. The native reader
        # stops after the BMD payload. Validate that complete payload below.
        decoder=lzma.LZMADecompressor();data=b'\x01'+decoder.decompress(data[1:])
    assert data[0]==1
    # Legacy Bone.addVertex rejects this exporter helper by name even when
    # rigid geometry legitimately uses it as an anchor. Keep bone IDs and
    # transforms unchanged and rename the same header in models/animations.
    fp=io.BytesIO(data);fp.read(1);count=struct.unpack('>h',fp.read(2))[0]
    out=io.BytesIO();out.write(data[:3])
    for _ in range(count):
        out.write(fp.read(4));chars=[]
        while (char:=struct.unpack('>H',fp.read(2))[0]):chars.append(chr(char))
        name=''.join(chars)
        if name=='blender_implicit':name='backport_rigid_anchor'
        out.write((name+'\0').encode('utf-16-be'))
    out.write(fp.read());data=out.getvalue()
    return data
def dump(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2), encoding='utf-8')

class Bmd:
    """Losslessly read BMD v1; the legacy loader uses this same layout."""
    def __init__(self, data):
        data=legacy_bmd(data)
        self.fp = io.BytesIO(data)
        def read(fmt):
            s=struct.Struct('>'+fmt); return s.unpack(self.fp.read(s.size))
        self.read=read
        assert read('B')[0]==1, 'Unsupported BMD version'
        self.bones=[]
        for _ in range(read('h')[0]):
            idx,parent=read('hh'); chars=[]
            while (c:=read('H')[0]): chars.append(chr(c))
            assert idx==len(self.bones)
            self.bones.append((parent,''.join(chars)))
        self.rest={};self.frames=[]
        for _ in range(read('h')[0]):
            frame={}
            for _ in range(read('h')[0]):
                idx=read('h')[0]; frame[idx]=read('6f')
            self.frames.append(frame);self.rest.update(frame)
        for _ in range(read('h')[0]):
            while read('H')[0]: pass
        self.faces=[]
        for _ in range(read('h')[0]):
            read('B'); vs=[]
            for _ in range(3):
                rigid=read('h')[0]; coords=read('8f')
                weights=[read('hf') for _ in range(read('B')[0])]
                if not weights:weights=[(rigid if rigid>=0 else 0,1.0)]
                vs.append((coords,weights))
            self.faces.append(vs)
        assert not self.fp.read(), 'Unexpected trailing BMD data'

def align_animation(model_data, animation_data):
    """The 1.16 renderer indexes bones; later exports may add helper bones.

    Reindex by name, preserve local transforms and verify parent relationships.
    Missing attachment-only bones retain their model rest transform. Unused
    animation helpers are removed only when not an ancestor of a used bone.
    """
    model=Bmd(model_data);animation=Bmd(animation_data)
    if model.bones==animation.bones:return legacy_bmd(animation_data),False
    sources={name:i for i,(_,name) in enumerate(animation.bones)}
    aliases={}
    # Upstream Starmie's right arm is called Root in model/walk but Arm_R
    # in idle. The hierarchy is identical; this is a name-only export change.
    if 'Root' in dict((n,p) for p,n in model.bones) and 'Root' not in sources and 'Arm_R' in sources:
        aliases['Root']='Arm_R'
    mapping={i:sources.get(aliases.get(name,name)) for i,(_,name) in enumerate(model.bones)}
    for i,(parent,name) in enumerate(model.bones):
        source=mapping[i]
        if source is None:
            if name!='Hat':raise ValueError('Missing animated bone: '+name)
            continue
        source_parent=animation.bones[source][0]
        actual_parent=animation.bones[source_parent][1] if source_parent>=0 else None
        expected_parent=aliases.get(model.bones[parent][1],model.bones[parent][1]) if parent>=0 else None
        if actual_parent!=expected_parent:raise ValueError('Retarget requires hierarchy conversion: '+name)
    out=io.BytesIO()
    def w(fmt,*values):out.write(struct.pack('>'+fmt,*values))
    w('Bh',1,len(model.bones))
    for i,(parent,name) in enumerate(model.bones):w('hh',i,parent);out.write((name+'\0').encode('utf-16-be'))
    w('h',len(animation.frames));poses={}
    for frame in animation.frames:
        poses.update(frame);w('h',len(model.bones))
        for i in range(len(model.bones)):
            source=mapping[i];pose=poses[source] if source is not None else model.rest[i]
            w('h6f',i,*pose)
    w('hh',0,0)
    return out.getvalue(),True

def combine_models(parts, archive):
    """Merge multipart models with a texture atlas and a shared named skeleton."""
    models=[Bmd(archive.read(asset(p['model']))) for p in parts]
    images=[Image.open(io.BytesIO(archive.read(asset(p['texture'])))).convert('RGBA') for p in parts]
    width=sum(i.width for i in images);height=max(i.height for i in images)
    atlas=Image.new('RGBA',(width,height));offsets=[];offset=0
    for im in images: atlas.paste(im,(offset,0));offsets.append(offset);offset+=im.width
    names={};bones=[];rests={};maps=[]
    for model in models:
        mapping={}
        for i,(parent,name) in enumerate(model.bones):
            if name not in names:
                names[name]=len(bones);bones.append((mapping.get(parent,-1),name));rests[names[name]]=model.rest.get(i,(0,)*6)
            mapping[i]=names[name]
        maps.append(mapping)
    out=io.BytesIO()
    def w(fmt,*values): out.write(struct.pack('>'+fmt,*values))
    w('Bh',1,len(bones))
    for i,(parent,name) in enumerate(bones): w('hh',i,parent);out.write((name+'\0').encode('utf-16-be'))
    w('hh',1,len(bones))
    for i in range(len(bones)):w('h6f',i,*rests[i])
    w('h',1);out.write('atlas\0'.encode('utf-16-be'))
    count=sum(len(m.faces) for m in models);assert count<32768
    w('h',count)
    for mi,m in enumerate(models):
        for face in m.faces:
            w('B',0)
            for coords,weights in face:
                c=list(coords);c[6]=(offsets[mi]+c[6]*images[mi].width)/width;c[7]=c[7]*images[mi].height/height
                w('h8fB',0,*c,len(weights))
                for bone,weight in weights:w('hf',maps[mi][bone],weight)
    im=io.BytesIO();atlas.save(im,format='PNG')
    return out.getvalue(),im.getvalue()

def asset(ref):
    ns,p=ref.split(':',1);return f'assets/{ns}/textures/{p}'
def ref_to_local(ref):return ref.replace('pixelmon:',NS+':',1)
def old_name(name):return '' if name=='base' else name

def run(old_path,new_path,out,reference_path=None):
    old=zipfile.ZipFile(old_path);new=zipfile.ZipFile(new_path)
    reference_archive=zipfile.ZipFile(reference_path) if reference_path else None
    def read_species(z):return {x['dex']:x for n in z.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('.json') for x in [json.loads(z.read(n))] if 'dex' in x}
    old_species=read_species(old);new_species=read_species(new)
    old_attacks={m['attackName'].lower():m['attackName'] for p in old.namelist() if p.startswith('data/pixelmon/moves/') and p.endswith('.json') for m in [json.loads(old.read(p))]}
    native_tm9={name.lower():name for name in class_utf8(old.read('com/pixelmonmod/pixelmon/enums/technicalmoves/Gen9TechnicalMachines.class')) if name.lower() in old_attacks}
    written=set();warnings=[]
    def write_bytes(path,data):
        p=out/path;p.parent.mkdir(parents=True,exist_ok=True);p.write_bytes(data);written.add(path)
    def copy_asset(ref):
        source=asset(ref)
        archive=new if source in new.namelist() else reference_archive
        if archive is None or source not in archive.namelist():raise ValueError('Missing upstream resource: '+source)
        target=source.replace('assets/pixelmon/','assets/'+NS+'/',1)
        if target not in written:
            data=archive.read(source)
            if source.endswith('.bmd'):data=legacy_bmd(data)
            write_bytes(target,data)
        return ref_to_local(ref)
    def palette(p, dex, form, gender, state=None):
        p=copy.deepcopy(p);groups=p.get('models',[])
        if len(groups)>1:
            states={q['model_predicate']['type'].split(':')[1]:q for q in groups}
            assert set(states)<= {'always','walk','flying'} and 'always' in states
            base=dict(p);base['models']=[states['always']]
            result=palette(base,dex,form,gender,'always')
            switches={}
            for key,group in states.items():
                if key=='always':continue
                variant=dict(p);variant['models']=[dict(group,model_predicate={'type':'pixelmon:always'})]
                switches[key]=palette(variant,dex,form,gender,key)['modelLocator']['pqc'][0]
            main=result['modelLocator']['pqc'][0].split(':')[1]
            dump(out/f'assets/{NS}/textures/{main}.states.json',switches)
            return result
        assert len(groups)==1 and groups[0]['model_predicate']['type']=='pixelmon:always'
        parts=groups[0]['models'];scales={q.get('scale',1) for q in parts}
        if len(scales)!=1:raise ValueError('Mixed model scales require conversion')
        stem=f'pokemon/{dex}/{form or "base"}/{gender}/{p["name"]}'
        if state:stem+='/'+state
        pqc=f'{stem}/model.pqc'
        embedded_animations={}
        if len(parts)==1 and parts[0]['model'].endswith('.glb'):
            from glb_to_legacy import convert_glb
            generated=convert_glb(new.read(asset(parts[0]['model'])))
            for filename,data in generated.items():write_bytes(f'assets/{NS}/textures/{stem}/{filename}',data)
            texture_ref=copy_asset(parts[0]['texture']) if parts[0].get('texture')!='minecraft:textures/none' else f'{NS}:{stem}/texture.png'
            body_ref=f'{NS}:{stem}/model.smd'
            embedded_animations={name[:-4]:f'{NS}:{stem}/{name}' for name in generated if name.endswith('.smd') and name!='model.smd'}
        elif len(parts)>1:
            body,texture=combine_models(parts,new)
            write_bytes(f'assets/{NS}/textures/{stem}/combined.bmd',body)
            write_bytes(f'assets/{NS}/textures/{stem}/texture.png',texture)
            texture_ref=f'{NS}:{stem}/texture.png';body_ref=f'{NS}:{stem}/combined.bmd'
        else:body_ref=copy_asset(parts[0]['model']);texture_ref=copy_asset(parts[0]['texture'])
        # Legacy ValveStudioModel.getResource hardcodes the pixelmon namespace
        # for relative paths, even for a descriptor owned by another namespace.
        # Use explicit locations, including textures/, for every referenced file.
        def resource(ref):
            ns,path=ref.split(':',1)
            return ns+':'+(path if path.startswith('textures/') else 'textures/'+path)
        lines=['$body '+resource(body_ref)]
        for name,ref in embedded_animations.items():lines.append('$anim '+name+' '+resource(ref))
        for anim in parts[0].get('animations',[]):
            reference=anim['animation']
            if body_ref.endswith('.bmd') and reference.endswith('.bmd'):
                body_data=(out/asset(body_ref)).read_bytes()
                animation_data,changed=align_animation(body_data,new.read(asset(reference)))
                if changed:
                    reference=f'{NS}:{stem}/{anim["type"]}.bmd'
                    write_bytes(asset(reference),animation_data)
                    warnings.append({'aligned_animation':anim['animation'],'model':body_ref})
                else:reference=copy_asset(reference)
            else:reference=copy_asset(reference)
            lines.append('$anim '+anim['type']+' '+resource(reference))
        lines.append('$scale '+str(next(iter(scales))))
        write_bytes(f'assets/{NS}/textures/{pqc}',('\n'.join(lines)+'\n').encode())
        sprite=p['sprite']
        if asset(sprite) not in new.namelist() and not (reference_archive and asset(sprite) in reference_archive.namelist()):
            fallback=sprite.replace('/'+p['name']+'/', '/none/')
            assert asset(fallback) in new.namelist()
            warnings.append({'missing_upstream_sprite':sprite,'fallback':fallback});sprite=fallback
        sounds=[s['sound_id'].removeprefix('pixelmon:') if isinstance(s,dict) else s for s in p.get('sounds',[])]
        result={'name':p['name'],'sprite':copy_asset(sprite),'texture':texture_ref,'modelLocator':{'factoryType':'NORMAL','pqc':[f'{NS}:{pqc}']},'sounds':sounds}
        if parts[0].get('emissive'):result['emissive']=copy_asset(parts[0]['emissive'])
        if p['name']=='shiny':result['particle']='arcanery:shiny'
        return result
    def convert_form(f,dex):
        f=copy.deepcopy(f);f['name']=old_name(f['name'])
        if 'defaultBaseForm' in f:f['defaultBaseForm']=old_name(f['defaultBaseForm'])
        f.pop('growth_data',None)
        # Gen9TechnicalMachines is an enum in 1.16.5; unresolved DLC entries
        # would become null. Keep native attacks in the generic TM learning set
        # and resolve their item definitions through ITechnicalMove instead.
        moves=f.get('moves',{});dlc=[a for a in moves.get('tmMoves9',[]) if a in NEW_TMS]
        moves['tmMoves9']=[native_tm9.get(a.lower(),a) for a in moves.get('tmMoves9',[]) if a not in NEW_TMS]
        moves.setdefault('tmMoves',[]).extend(a for a in dlc if a not in moves.get('tmMoves',[]))
        if 'genderProperties' in f:
            for g in f['genderProperties']:
                g['palettes']=[palette(p,dex,f['name'],g['gender'].lower()) for p in g['palettes']]
        # Tera transformation is separate from these three content milestones.
        mask_abilities={'TealMask':'Defiant','WellspringMask':'WaterAbsorb','HearthflameMask':'MoldBreaker','CornerstoneMask':'Sturdy'}
        if 'abilities' in f:
            for key in f['abilities']:f['abilities'][key]=[mask_abilities.get(a,a) for a in f['abilities'][key]]
        for e in f.get('evolutions',[]):
            if 'item' in e:e['item']['itemID']=e['item']['itemID'].replace('pixelmon:',NS+':')
        return f
    # Source only contains preview base forms, without complete evolution
    # chains. Keep their assets offline; do not register them in this addon.
    disabled_previews={1026,1029,1032}
    patches=[];new_ids=sorted(set(new_species)-set(old_species)-disabled_previews);placeholders=[];megaf=[]
    for dex,x in old_species.items():
        for f in x['forms']:
            if any(p.get('modelLocator',{}).get('pqc')==['none'] for g in f.get('genderProperties',[]) for p in g['palettes']):placeholders.append((dex,f['name']))
    # Base has been renamed upstream. Compare normalized form names.
    for dex,x in new_species.items():
        if dex>1025 or dex not in old_species:continue
        names={f['name'] for f in old_species[dex]['forms']}
        for f in x['forms']:
            if 'mega' in f['name'] and old_name(f['name']) not in names:megaf.append((dex,f['name']))
    assert len(placeholders)==13 and len(megaf)==6
    targets=sorted(set(new_ids+[d for d,_ in placeholders]+[d for d,_ in megaf]+[884,676]))
    needed_moves=set();needed_abilities=set()
    for dex in targets:
        source=copy.deepcopy(new_species[dex]);result={'name':source['name'],'dex':dex,'generation':source['generation'],'defaultForms':[old_name(n) for n in source['defaultForms']],'forms':[]}
        if dex in new_ids or dex==676:
            selected=source['forms']
        else:
            requested={old_name(n) for d,n in placeholders+megaf if d==dex}
            if dex in [884,1011] or any(d==dex for d,_ in megaf):requested.add('')
            selected=[f for f in source['forms'] if old_name(f['name']) in requested]
        for f in selected:
            if dex==884 or (dex in [26,121,149,491,609] and f['name']=='base'):
                # Connection fields only; do not replace the old base model.
                c={'name':old_name(f['name']),'megaItems':[s.replace('pixelmon:',NS+':') for s in f.get('megaItems',[])],'megas':f.get('megas',[]),'evolutions':copy.deepcopy(f.get('evolutions',[]))}
                for e in c['evolutions']:
                    if 'item' in e:e['item']['itemID']=e['item']['itemID'].replace('pixelmon:',NS+':')
            else:c=convert_form(f,dex)
            result['forms'].append(c)
            for key,ls in c.get('moves',{}).items():
                needed_moves.update(a for row in ls for a in row['attacks']) if key=='levelUpMoves' else needed_moves.update(ls)
            for ls in c.get('abilities',{}).values():needed_abilities.update(ls)
        patches.append({'mode':'new' if dex in new_ids else 'merge','species':result})
    om={json.loads(old.read(k))['attackName'] for k in old.namelist() if k.startswith('data/pixelmon/moves/') and k.endswith('.json')}
    moves=[];old_effects=set()
    for k in new.namelist():
        if k.startswith('data/pixelmon/moves/') and k.endswith('.json'):
            m=json.loads(new.read(k))
            if m['attackName'] in needed_moves-om or m['attackName'] in {'Ancient Ceaseless Edge','Ancient Stone Axe'}:
                m['animations']=['leapForward'];moves.append(m)
    move_ids=json.loads((ROOT/'tools/move_ids.json').read_text(encoding='utf-8'))
    for m in moves:
        m['attackIndex']=move_ids[m['attackName']]
        # Legacy MoveFlags.Adapter does not skip unknown keys; preserve the
        # native flags and implement slicing/bullet with targeted hooks.
        if 'flags' in m:m['flags']={k:v for k,v in m['flags'].items() if k in ['authentic','nontarget','sound']}
        for zmove in m.get('z',[]):zmove['crystal']=zmove['crystal'].removeprefix('pixelmon:')
        for e in m['effects']:
            if e['effectTypeID'] in {'AlluringVoice','BurningBulwark','DragonCheer','ElectroShot','FickleBeam','HardPress','IvyCudgel','PsychicNoise','SupercellSlam','SyrupBomb','TeraStarstorm','UpperHand'}:
                e['effectTypeID']='Backport'+e['effectTypeID'];e.pop('type',None)
        # Attack flags that only exist in the new engine get targeted legacy hooks.
        if m['attackName']=='Mighty Cleave':m['effects'].append({'effectTypeID':'BackportMightyCleave','modifiers':[],'persists':False})
    dump(out/'backport/content.json',{'upstream':'9.4.1','patches':patches,'moves':moves})
    # Independent item ids; original item ids and registries remain untouched.
    item_names=['dragoninite','starminite','chandelurite','darkranite','raichunite_x','raichunite_y','metal_alloy','unremarkable_teacup','masterpiece_teacup','teal_mask','wellspring_mask','hearthflame_mask','cornerstone_mask']
    for item in item_names:
        model_path=f'assets/pixelmon/models/item/{item}.json'
        model=json.loads(new.read(model_path))
        # Inventory icons are a stable 1.16.5 generated item representation.
        tex=next(iter(model.get('textures',{}).values()),None)
        if tex is None:raise ValueError('No item texture: '+item)
        ns,path=tex.split(':',1);source=f'assets/{ns}/textures/{path}.png'
        write_bytes(f'assets/{NS}/textures/item/{item}.png',new.read(source))
        dump(out/f'assets/{NS}/models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'{NS}:item/{item}'}})
    for lang in ['en_us','zh_cn']:
        upstream=json.loads(new.read(f'assets/pixelmon/lang/{lang}.json'))
        entries={k:v for k,v in upstream.items() if k.startswith(('pixelmon.', 'ability.', 'attack.', 'pokemon.', 'form.', 'item.pixelmon.'))}
        # Copy only content-related keys, avoiding unrelated UI text changes.
        tokens=[new_species[d]['name'].lower() for d in targets]+[m['attackName'].lower().replace(' ','') for m in moves]+[a.lower().replace(' ','') for a in needed_abilities]+['terashift','terashell','poisonpuppeteer']
        entries={k:v for k,v in entries.items() if any(t in k.lower().replace('_','').replace(' ','') for t in tokens)}
        entries={k:v for k,v in entries.items() if not k.startswith('pixelmon.research.')}
        for item in item_names:
            key='item.pixelmon.'+item
            entries['item.'+NS+'.'+item]=upstream.get(key,item)
        for tm_id in NEW_TMS.values():
            key='item.pixelmon.tm_gen9.'+str(tm_id)
            if key in upstream:entries[key]=upstream[key]
        if lang=='zh_cn':entries.update(json.loads((ROOT/'tools/zh_cn_overrides.json').read_text(encoding='utf-8')))
        else:entries['pixelmon.greninja.form.ash_permanent']='Ash-Greninja (Permanent)'
        dump(out/f'assets/{NS}/lang/{lang}.json',entries)
    summary={'new_species':[new_species[d]['name'] for d in new_ids],'placeholder_forms':placeholders,'mega_forms':megaf,'new_moves':[m['attackName'] for m in moves],'items':item_names,'upstream_assets':len(written),'upstream_issues':warnings}
    dump(ROOT/'.cache/conversion-summary.json',summary)
    print(json.dumps(summary,ensure_ascii=False,indent=2))

if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--old',required=True,type=pathlib.Path);p.add_argument('--upstream',required=True,type=pathlib.Path);p.add_argument('--reference',type=pathlib.Path);p.add_argument('--output',type=pathlib.Path,default=ROOT/'resources');a=p.parse_args();run(a.old,a.upstream,a.output,a.reference)
