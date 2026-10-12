"""Offline review of all ten native gym libraries without loading a game world."""
import argparse,collections,json,pathlib,zipfile
from extend_gym_cities import NATIVE_BADGES,read_nbt
ROOT=pathlib.Path(__file__).resolve().parents[1]

def review(data,path):
 vox={tuple(map(int,b['pos'])):b for b in data['blocks']}
 def name(b):return str(data['palette'][int(b['state'])]['Name'])
 def final(p):
  b=vox.get(p)
  return 'minecraft:air' if b is None else str(b.get('nbt',{}).get('final_state','minecraft:air')).split('[')[0] if name(b)=='minecraft:jigsaw' else name(b)
 nurses=[];healers=[];beds=0;failures=[]
 directions={'north':(0,-1),'south':(0,1),'west':(-1,0),'east':(1,0)}
 for (x,y,z),b in vox.items():
  s=name(b);props=data['palette'][int(b['state'])].get('Properties',{})
  if 'healer' in s:healers.append((x,y,z))
  if s=='minecraft:jigsaw' and 'nurse' in str(b.get('nbt',{}).get('pool','')):
   nurse=(x,y+1,z);nurses.append(nurse)
   if any(final((x,y+dy,z)) not in ['minecraft:air','minecraft:cave_air','minecraft:void_air','minecraft:structure_void'] for dy in [1,2]):failures.append(['blocked_nurse_spawn',nurse])
  if s.endswith('_bed') and str(props.get('part'))=='foot':
   dx,dz=directions[str(props['facing'])];peer=vox.get((x+dx,y,z+dz));beds+=1
   if peer is None or name(peer)!=s or str(data['palette'][int(peer['state'])].get('Properties',{}).get('part'))!='head':failures.append(['incomplete_bed',[x,y,z],s])
 distances=[]
 for nurse in nurses:
  distance=min((sum((a-b)**2 for a,b in zip(nurse,healer)) for healer in healers),default=1000000)**.5;distances.append(round(distance,3))
  if distance>8:failures.append(['nurse_without_healer_in_native_search_radius',nurse,distance])
 return {'template':path,'complete_beds':beds,'nurses':len(nurses),'healers':len(healers),'nearest_healer_distances':distances,'failures':failures}

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--pixelmon',required=True);args=parser.parse_args()
 reports={theme:[] for theme in NATIVE_BADGES}
 with zipfile.ZipFile(args.pixelmon) as jar:
  for path in jar.namelist():
   if not path.startswith('data/pixelmon/structures/gyms/') or not path.endswith('.nbt'):continue
   theme=path.split('/')[4]
   if theme not in reports:continue
   if not any('_'+role in path.rsplit('/',1)[1] for role in ['house_','pokecenter','pokemart','daycare','blacksmith','church','tcg_shop']):continue
   reports[theme].append(review(read_nbt(jar.read(path)),path))
 result={'scope':'Ten native city libraries; original architecture retained. Native nurse radius is eight blocks. New eight city geometry is verified separately.','cities':reports}
 (ROOT/'docs/NATIVE_CITY_INTERIOR_REVIEW_1.3.0.json').write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 failures=[(theme,r['template'],f) for theme,rs in reports.items() for r in rs for f in r['failures']]
 print('Native interior templates',sum(map(len,reports.values())),'complete beds',sum(r['complete_beds'] for rs in reports.values() for r in rs),'nurses',sum(r['nurses'] for rs in reports.values() for r in rs),'failures',len(failures))
 for failure in failures:print(failure)
 if failures:raise SystemExit(1)
if __name__=='__main__':main()
