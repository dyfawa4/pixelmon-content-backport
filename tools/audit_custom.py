"""Read-only comparison of the supplied 1.16.5 reference and official JARs."""
import argparse, json, pathlib, zipfile, hashlib, collections
ROOT=pathlib.Path(__file__).resolve().parents[1]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--reference',required=True,type=pathlib.Path)
p.add_argument('--old',required=True,type=pathlib.Path)
p.add_argument('--upstream',required=True,type=pathlib.Path)
a=p.parse_args()
CUSTOM,OLD,NEW=a.reference,a.old,a.upstream
(ROOT/'.cache').mkdir(parents=True,exist_ok=True)
def species(z):
    return {d['dex']:d for n in z.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('.json') for d in [json.loads(z.read(n))]}
def formnames(d):return [f.get('name','') for f in d.get('forms',[])]
with zipfile.ZipFile(OLD) as old, zipfile.ZipFile(CUSTOM) as custom, zipfile.ZipFile(NEW) as new:
    os,cs,ns=species(old),species(custom),species(new)
    on=set(old.namelist());cn=set(custom.namelist())
    added=sorted(cn-on);changed=[n for n in sorted(cn&on) if custom.getinfo(n).CRC!=old.getinfo(n).CRC]
    classes=[n for n in cn if n.endswith('.class')]
    majors=collections.defaultdict(list)
    for n in classes:majors[int.from_bytes(custom.read(n)[6:8],'big')].append(n)
    tera=[n for n in cn if any(s in n.lower() for s in ['tera','stellar','crystal']) and not any(s in n.lower() for s in ['lateral','matera','itera','literat'])]
    report={'custom':str(CUSTOM),'sha256':hashlib.file_digest(CUSTOM.open('rb'),'sha256').hexdigest(),
      'speciesCounts':{'old':len(os),'custom':len(cs),'upstream':len(ns)},
      'addedSpecies':[{'dex':i,'name':cs[i]['name'],'forms':formnames(cs[i])} for i in sorted(cs.keys()-os.keys())],
      'missingUpstreamSpecies':[{'dex':i,'name':ns[i]['name']} for i in sorted(ns.keys()-cs.keys())],
      'additionalForms':[{'dex':i,'name':cs[i]['name'],'forms':sorted(set(formnames(cs[i]))-set(formnames(os[i])))} for i in sorted(cs.keys()&os.keys()) if set(formnames(cs[i]))-set(formnames(os[i]))],
      'majorCounts':{str(k):len(v) for k,v in majors.items()},'aboveJava11':[n for k,v in majors.items() if k>55 for n in v],
      'addedClasses':[n for n in added if n.endswith('.class')], 'changedClasses':[n for n in changed if n.endswith('.class')],
      'teraEntries':sorted(tera),'addedEntries':added,'changedEntries':changed}
    (ROOT/'.cache/custom-audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf-8')
    for k in ['sha256','speciesCounts','addedSpecies','missingUpstreamSpecies','additionalForms','majorCounts','aboveJava11','addedClasses']:
        print(k,json.dumps(report[k],ensure_ascii=False))
    print('Changed classes',len(report['changedClasses']))
    print('Tera classes',json.dumps([n for n in tera if n.endswith('.class')],ensure_ascii=False))
