"""Audit actual loaded forms against pinned primary learnset data; never execute it.

Run after exporting tera110-forms.json from the native test. --update writes
the full actual-form mapping, preserving explicit custom/event exclusions.
"""
from pathlib import Path
import argparse,hashlib,json
p=argparse.ArgumentParser();p.add_argument('forms',type=Path);p.add_argument('--update',action='store_true');a=p.parse_args()
root=Path(__file__).resolve().parents[1];ref=root/'.cache/110/tera-reference'
hashes={'learnsets.json':'cedbe661968e2d69abe9f317e9a3f5d4f2dcf989e1f937cb0cdd72e7af29ab8f','pokedex.json':'e74d0364b230001fc086cefb4259e051f1d16a4730eb89f7d6336a88a0d9a47e'}
for name,digest in hashes.items():assert hashlib.sha256((ref/name).read_bytes()).hexdigest()==digest,name
learn=json.loads((ref/'learnsets.json').read_text('utf-8'));dex=json.loads((ref/'pokedex.json').read_text('utf-8'))
eligible={dex[k].get('num',0) for k,v in learn.items() if k in dex and any(m.startswith('9M') for m in v.get('learnset',{}).get('terablast',[]))};eligible={n for n in eligible if 1<=n<=1025}
forms=json.loads(a.forms.read_text('utf-8'));file=root/'resources/backport/tera_learning.json';table=json.loads(file.read_text('utf-8'))
# Explicit non-SV event learnsets and one native custom moveset are not omissions.
excluded={(25,f) for f in ['cosplay','rockstar','belle','popstar','libre','phd']}|{(172,'spiky'),(670,'az'),(386,'sus')}
if a.update:
 for n in sorted(eligible|{1026}):
  table[str(n)]=['base' if not r['form'] else r['form'] for r in forms if r['dex']==n and (n,r['form']) not in excluded]
  assert table[str(n)],n
 file.write_text(json.dumps(dict(sorted(table.items(),key=lambda x:int(x[0]))),ensure_ascii=False,indent=2)+'\n','utf-8')
expected=[dict(r,expected=(r['dex'] in eligible or r['dex']==1026) and (r['dex'],r['form']) not in excluded) for r in forms]
errors=[r for r in expected if r['learns']!=r['expected']]
report={'sources':{'learnsets':'https://play.pokemonshowdown.com/data/learnsets.json','pokedex':'https://play.pokemonshowdown.com/data/pokedex.json'},'sha256':hashes,'officialSpecies':len(eligible),'customSpecies':[1026],'forms':len(forms),'compatible':sum(r['expected'] for r in expected),'exceptions':[list(r) for r in sorted(excluded)],'mismatches':errors,'audit':expected}
(ref/'audit.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n','utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in ['audit','sources','sha256']},ensure_ascii=False))
if not a.update and errors:raise SystemExit(1)
