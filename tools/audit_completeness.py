"""Private audit: compare actual content names, not filename/version labels.

Presence is not proof of semantic equivalence. Keep source behavior differences
separate from missing registry entries; alias base/normal legacy spellings.
"""
import argparse,copy,json,pathlib,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
(ROOT/'.cache').mkdir(parents=True,exist_ok=True)
def load(z,kind):
    result={}
    for n in z.namelist():
        if n.startswith('data/pixelmon/'+kind+'/') and n.endswith('.json'):
            v=json.loads(z.read(n));result[v['dex'] if kind=='species' else v['attackName']]=v
    return result
def normalized(name):return '' if name in ('base','normal') else name
def audit(old_path,upstream_path,reference_path):
    old=zipfile.ZipFile(old_path);up=zipfile.ZipFile(upstream_path);ref=zipfile.ZipFile(reference_path)
    target=load(old,'species');moves=load(old,'moves');data=json.loads((ROOT/'resources/backport/content.json').read_text('utf-8'))
    for patch in data['patches']:
        s=patch['species'];current=target.setdefault(s['dex'],copy.deepcopy(s));forms={f['name']:f for f in current['forms']}
        for f in s['forms']:
            if f['name'] not in forms:current['forms'].append(copy.deepcopy(f));forms[f['name']]=current['forms'][-1]
    for m in data['moves']:moves.setdefault(m['attackName'],m)
    for name,m in json.loads((ROOT/'resources/backport/modern-moves.json').read_text('utf-8')).items():
        if name in moves and 'Splinters' in json.dumps(moves[name].get('effects')):
            moves[name]['effects']=copy.deepcopy(m['effects'])
    report={'disabled_incomplete_previews':[1026,1029,1032],'target_species':len(target),'added_moves':len(data['moves']),'sources':{}}
    for label,z in [('official_9.4.1',up),('provided_ZA',ref)]:
        species=load(z,'species');source_moves=load(z,'moves');missing_species=[];missing_forms=[];effect_differences=[]
        for dex,s in species.items():
            if dex in report['disabled_incomplete_previews']:continue
            if dex not in target:missing_species.append({'dex':dex,'name':s['name']});continue
            present={normalized(f['name']) for f in target[dex]['forms']}
            for f in s['forms']:
                if normalized(f['name']) not in present:missing_forms.append({'dex':dex,'species':s['name'],'form':f['name']})
        for name,m in source_moves.items():
            if name in moves and m.get('effects')!=moves[name].get('effects'):effect_differences.append(name)
        report['sources'][label]={'species':len(species),'missing_species':missing_species,'missing_forms':missing_forms,'missing_moves':sorted(set(source_moves)-set(moves)),'raw_effect_differences_requiring_semantic_review':effect_differences}
    report['limits']=['Form and move name coverage does not prove mechanics, acquisition or animations.','Legacy established mechanics are deliberately preserved unless a targeted backport replaces them.','Full Tera research, NPC auto activation, Tera raids and visual crystal/crowns remain incomplete.','Utility umbrella has targeted native weather hooks; Eternatus meteorite equip/save/remove is implemented.','Furfrou six native trims have an explicit shears survival adaptation.','Ceaseless Edge/Stone Axe use modern hazards; Ancient definitions retain Splinters.']
    path=ROOT/'.cache/completeness-audit.json';path.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({k:v for k,v in report.items() if k!='sources'},ensure_ascii=False))
    for label,s in report['sources'].items():print(label,'missing species',len(s['missing_species']),'forms',len(s['missing_forms']),'moves',len(s['missing_moves']),'raw effect diffs',len(s['raw_effect_differences_requiring_semantic_review']))
if __name__=='__main__':
    p=argparse.ArgumentParser();p.add_argument('--old',required=True);p.add_argument('--upstream',required=True);p.add_argument('--reference',required=True);a=p.parse_args();audit(a.old,a.upstream,a.reference)
