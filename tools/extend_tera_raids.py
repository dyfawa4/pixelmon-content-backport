"""Extract enabled upstream Tera encounters; keep rewards separate from Max raids."""
import json,pathlib,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
(ROOT/'.cache').mkdir(parents=True,exist_ok=True)
def main():
    with zipfile.ZipFile(ROOT/'.cache/Pixelmon-1.21.1-9.4.1-universal.jar') as z:
        rows=[]
        for path in sorted(z.namelist()):
            if path.startswith('data/pixelmon/pixelmon/raid/spawn/tera/') and path.endswith('.json'):
                v=json.loads(z.read(path));rows.append(v)
        (ROOT/'.cache/tera-upstream-encounters.json').write_text(json.dumps(rows,indent=2),encoding='utf-8')
        encounters=[{'spec':r['spec'],'min':r['stars'].get('min_inclusive',r['stars'].get('value',1)),
                     'max':r['stars'].get('max_inclusive',r['stars'].get('value',1)),'weight':r.get('weight',1)}
                    for v in rows for r in v['spawns']]
        data=json.loads((ROOT/'resources/backport/content.json').read_text('utf-8'))
        data['referenceExtras'].pop('drops/raiddrops',None)
        (ROOT/'resources/backport/content.json').write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        rewards=[];substitutions=[]
        mochi=dict(zip(['health','muscle','resist','genius','clever','swift'],['hp_up','protein','iron','calcium','zinc','carbos']))
        def tag_items(name,seen=()):
            if name in seen:raise ValueError('Recursive item tag '+name)
            namespace,path=name.split(':',1)
            tag=json.loads(z.read('data/'+namespace+'/tags/item/'+path+'.json'))
            result=[]
            for value in tag['values']:
                value=value['id'] if isinstance(value,dict) else value
                result.extend(tag_items(value[1:],seen+(name,)) if value.startswith('#') else [value])
            return list(dict.fromkeys(result))
        def leaves(entry,weight):
            kind=entry['type']
            if kind=='minecraft:tag':
                assert entry.get('expand') is True,entry
                values=tag_items(entry['name'])
                return [(name,weight/len(values)) for name in values]
            if kind=='minecraft:item':
                name=entry['name']
                if name=='pixelmon:fresh_start_mochi':
                    substitutions.append({'source':name,'action':'excluded: legacy engine has no equivalent EV-reset medicine'});return []
                if name.endswith('_mochi'):
                    replacement='pixelmon:'+mochi[name.split(':')[1].removesuffix('_mochi')]
                    substitutions.append({'source':name,'replacement':replacement});name=replacement
                return [(name,weight)]
            if kind=='minecraft:loot_table':
                name=entry['value']
                if 'tera_shards_' in name:return [] # Guaranteed matching shards replace random shard rolls.
                nested=json.loads(z.read('data/pixelmon/loot_table/'+name.split(':',1)[1]+'.json'))
                assert len(nested['pools'])==1 and nested['pools'][0]['rolls']==1,nested
                entries=nested['pools'][0]['entries'];total=sum(e.get('weight',1) for e in entries)
                return [leaf for e in entries for leaf in leaves(e,weight*e.get('weight',1)/total)]
            raise ValueError(entry)
        upstream_loot=json.loads(z.read('data/pixelmon/loot_table/raid_den/tera.json'))
        for pool in upstream_loot['pools'][:6]:
            stars=pool['conditions'][0]['stars']
            for entry in pool['entries']:
                for item,weight in leaves(entry,entry.get('weight',1)):rewards.append({'item':item,'weight':weight,'min':stars,'max':stars})
            # Extra legacy pack routes for fully functional ability items.
            if stars>=4:rewards.append({'item':'pixelmon:ability_capsule','weight':5,'min':stars,'max':stars})
            if stars>=5:rewards.append({'item':'pixelmon:ability_patch','weight':3,'min':stars,'max':stars})
        compat={}
        for path in z.namelist():
            if path.startswith('data/pixelmon/species/') and path.endswith('.json'):
                species=json.loads(z.read(path));forms=[f['name'] for f in species['forms'] if 'Tera Blast' in f.get('moves',{}).get('tmMoves9',[])]
                if forms and species['dex']<=1025:compat[str(species['dex'])]=forms
        (ROOT/'resources/backport/tera_learning.json').write_text(json.dumps(compat,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        manifest={'source':'Pixelmon 9.4.1 Tera encounters; independent legacy pack rewards',
                  'encounters':encounters,'rewards':rewards,'substitutions':substitutions}
        (ROOT/'resources/backport/tera_raids.json').write_text(json.dumps(manifest,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
        print('Tera encounters',len(encounters),'separate reward entries',len(rewards),'Max raid overrides removed')
if __name__=='__main__':main()
