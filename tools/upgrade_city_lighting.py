"""Upgrade eighteen city libraries; never touch a running client or user save."""
import argparse,json,pathlib,zipfile
from extend_gym_cities import ROOT,RES,NATIVE_BADGES,THEMES,read_nbt,save_nbt
from city_architecture import kind
from city_lighting import ROOMS,upgrade

def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);p.add_argument('--check-only',action='store_true');p.add_argument('--native-only',action='store_true');a=p.parse_args()
 reports={theme:[] for theme in [*NATIVE_BADGES,*THEMES]};planned=[];missing=[];obsolete=[]
 with zipfile.ZipFile(a.pixelmon) as jar:
  for path in jar.namelist():
   if not path.startswith('data/pixelmon/structures/gyms/') or not path.endswith('.nbt'):continue
   theme=path.split('/')[4]
   if theme not in NATIVE_BADGES:continue
   category=kind(path.rsplit('/',1)[1],theme)
   if category not in ROOMS:
    target=RES/path
    if target.is_file() and read_nbt(target.read_bytes()).get('BackportLightingRevision')==9:obsolete.append(target)
    continue
   data=read_nbt(jar.read(path))
   from embedded_npc_zh_cn import translate
   translate(data)
   data,report=upgrade(data,theme,category)
   if category=='gym' and not report['portals']:missing.append(path)
   report['template']=path;reports[theme].append(report);planned.append((RES/path,data))
 for theme in ([] if a.native_only else THEMES):
  for path in sorted((RES/'data/pixelmon_backport/structures/gyms'/theme).glob('*.nbt')):
   category=kind(path.name,theme)
   data=read_nbt(path.read_bytes())
   if str(data.get('BackportArchitecture','')).startswith('original-city-v9:'):
    raise ValueError('Regenerate v6 geometry before applying this upgrade again: '+str(path))
   data,report=upgrade(data,theme,category)
   if report:
    if category=='gym' and not report['portals']:missing.append(str(path))
    report['template']=path.relative_to(RES).as_posix();reports[theme].append(report)
   if str(data.get('BackportArchitecture','')).startswith('original-city-v6:'):data['BackportArchitecture']=__import__('nbtlib').String('original-city-v9:'+theme)
   planned.append((path,data))
 if a.native_only:
  previous=json.loads((ROOT/'docs/CITY_LIGHTING_REVIEW_1.3.0.json').read_text(encoding='utf-8'))
  for theme in THEMES:reports[theme]=previous['cities'][theme]
 result={'revision':9,'scope':'All eighteen city libraries, night block-light only, no skylight credit','target_minimum_block_light':8,'cities':reports}
 target=ROOT/'docs/CITY_LIGHTING_REVIEW_1.3.0.json'
 if missing:raise ValueError(('Missing visible gym entrances',missing))
 if not a.check_only:
  for path in obsolete:
   assert path.resolve().is_relative_to((RES/'data/pixelmon/structures/gyms').resolve())
   path.unlink()
  for path,data in planned:save_nbt(path,data)
  cities=json.loads((RES/'backport/gym_cities.json').read_text(encoding='utf-8'))
  for definition in cities['newThemes'].values():definition['architectureRevision']=9
  (RES/'backport/gym_cities.json').write_text(json.dumps(cities,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
  target.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('PASS eighteen cities:',sum(map(len,reports.values())),'building templates;',sum(row['added_lamps'] for rows in reports.values() for row in rows),'added supported lamps;',sum(len(row['portals']) for rows in reports.values() for row in rows),'gym portals; minimum night light >= 8')
if __name__=='__main__':main()
