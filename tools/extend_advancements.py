"""Create capture/fusion advancements, using quest titles as naming references."""
import argparse, json, pathlib, re, zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
GROUPS={
 'gen4':('神奥的传说',list(range(480,494))),
 'gen5':('合众的传说',[494]+list(range(638,650))),
 'gen6':('卡洛斯的传说',list(range(716,722))),
 'gen7':('阿罗拉的传说',[772,773,785,786,787,788,789,790,791,792,800,801,802,807,808,809]),
 'gen8':('伽勒尔与洗翠的传说',list(range(888,899))+[905]),
 'gen9':('帕底亚的传说',list(range(1001,1009))+list(range(1014,1018))+[1024,1025]),
 'ultra':('究极之门',list(range(793,800))+list(range(803,807))),
 'paradox':('时空之外',list(range(984,996))+[1009,1010,1020,1021,1022,1023])}
FUSIONS=[(646,'black','玄雷冰魄'),(646,'white','焰白冰心'),(800,'dusk','噬日之辉'),(800,'dawn','邀月之翼'),(898,'icerider','雪原御骑'),(898,'shadowrider','幽夜御骑')]
FUSION_DESCRIPTIONS={
 (646,'black'):'使用基因之楔，让酋雷姆与捷克罗姆合体，成为暗黑酋雷姆。解除合体不会撤销成就。',
 (646,'white'):'使用基因之楔，让酋雷姆与莱希拉姆合体，成为焰白酋雷姆。解除合体不会撤销成就。',
 (800,'dusk'):'使用奈克洛索尔合体器，让奈克洛兹玛与索尔迦雷欧合体，成为黄昏之鬃奈克洛兹玛。解除合体不会撤销成就。',
 (800,'dawn'):'使用奈克洛露奈合体器，让奈克洛兹玛与露奈雅拉合体，成为拂晓之翼奈克洛兹玛。解除合体不会撤销成就。',
 (898,'icerider'):'使用牵绊缰绳，让蕾冠王与雪暴马合体，成为白马蕾冠王。解除合体不会撤销成就。',
 (898,'shadowrider'):'使用牵绊缰绳，让蕾冠王与灵幽马合体，成为黑马蕾冠王。解除合体不会撤销成就。'}
NEW={1014:'碧面猛犬',1015:'迷心智猿',1016:'绯羽锦雉',1017:'面具下的微笑',1020:'燎原古焰',1021:'长颈雷云',1022:'破岩铁锋',1023:'铁冠之剑',1024:'万象晶辉',1025:'桃梦幽契'}
USER_TITLES={719:'窈窕淑女',1014:'垂涎三尺',1015:'痴心妄想',1016:'含灵蠢动',1017:'钟灵毓秀'}

def sprite_icon(species,dex,assets,form_name=None):
 s=species[dex];forms=s['forms'];base=forms[0]
 f=next((f for f in forms if f['name']==form_name),None) if form_name is not None else base
 if f is None:raise ValueError((dex,form_name,'missing form'))
 gender=(f.get('possibleGenders',base.get('possibleGenders')) or ['NONE'])[0]
 properties=f.get('genderProperties',base.get('genderProperties',[]))
 g=next((g for g in properties if g['gender']==gender),None) or next((g for g in properties if g['gender']=='ALL'),None)
 palette=next((p for p in (g or {}).get('palettes',[]) if p['name']=='none'),None)
 if palette is None:raise ValueError((dex,f['name'],gender,'missing palette'))
 ns,path=palette['sprite'].split(':',1);asset=f'assets/{ns}/textures/{path}'
 if asset not in assets and not (ROOT/'resources'/asset).is_file():raise ValueError((dex,asset,'missing sprite'))
 return {'item':'pixelmon:pixelmon_sprite','nbt':f'{{ndex:{dex}s,palette:"none",form:"{f["name"]}",gender:{["MALE","FEMALE","NONE"].index(gender)}b}}'}

def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def advancement(title,description,icon,parent=None,challenge=False):
 obj={'display':{'icon':icon,'title':{'text':title},'description':{'text':description},'frame':'challenge' if challenge else 'task','show_toast':True,'announce_to_chat':True,'hidden':True},'criteria':{'earned':{'trigger':'minecraft:impossible'}}}
 if parent:obj['parent']='pixelmon_backport:legends/'+parent
 else:obj['display']['background']='minecraft:textures/block/obsidian.png'
 return obj
def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);p.add_argument('--quests',required=True);args=p.parse_args()
 names={}
 for path in pathlib.Path(args.quests).glob('*.snbt'):
  text=path.read_text(encoding='utf-8');starts=list(re.finditer(r'^\t\t\{\s*$',text,re.M))
  for i,start in enumerate(starts):
   block=text[start.start():starts[i+1].start() if i+1<len(starts) else len(text)]
   if 'tasks:' not in block:continue
   title=re.search(r'^\t\t\ttitle: "((?:\\.|[^"\\])*)"',block,re.M)
   dex=re.search(r'ndex:\s*(\d+)',block)
   if title and dex:names.setdefault(int(dex[1]),json.loads('"'+title[1]+'"'))
 with zipfile.ZipFile(args.pixelmon) as jar:
  species={json.loads(jar.read(n))['dex']:json.loads(jar.read(n)) for n in jar.namelist() if n.startswith('data/pixelmon/species/') and n.endswith('.json')}
  chinese=json.loads(jar.read('assets/pixelmon/lang/zh_cn.json'))
  assets=set(jar.namelist())
 content=json.loads((ROOT/'resources/backport/content.json').read_text(encoding='utf-8'))
 chinese.update(json.loads((ROOT/'resources/assets/pixelmon_backport/lang/zh_cn.json').read_text(encoding='utf-8')))
 for patch in content['patches']:
  incoming=patch['species'];dex=incoming['dex']
  if dex not in species:species[dex]=incoming;continue
  if patch['mode']=='new':continue
  for f in incoming['forms']:
   present=next((v for v in species[dex]['forms'] if v['name']==f['name']),None)
   if present is None:species[dex]['forms'].append(f);continue
   placeholder='"none"]' in json.dumps(present,separators=(',',':'))
   for key,value in f.items():
    if key not in ('name','moves','evolutions','megaItems','megas') and (placeholder or key not in present):present[key]=value
 target=ROOT/'resources/data/pixelmon_backport/advancements/legends';catalog={'captures':{},'fusions':[]}
 master={'item':'pixelmon:master_ball'}
 icon=lambda dex,form=None:sprite_icon(species,dex,assets,form)
 write(target/'root.json',advancement('传说续章','追寻新的传说、究极异兽与跨越时空的宝可梦。',master))
 for category,(label,dexes) in GROUPS.items():
  write(target/(category+'.json'),advancement(label,'完成这一篇章中的首次捕捉。',master,'root'))
  for dex in dexes:
   if dex not in species:raise ValueError(f'Missing species {dex}')
   s=species[dex];title=USER_TITLES.get(dex,names.get(dex,NEW.get(dex,'与'+chinese.get('pixelmon.'+s['name'].lower(),s['name'])+'相遇')))
   ident='capture_'+str(dex);description='成功捕捉'+chinese.get('pixelmon.'+s['name'].lower(),s['name'])+'。'
   write(target/(ident+'.json'),advancement(title,description,icon(dex),category,dex==493))
   catalog['captures'][str(dex)]={'id':ident,'category':category,'title':title,'species':s['name']}
 write(target/'fusion.json',advancement('交织的传说','使用对应道具，亲手完成传说宝可梦的合体。',master,'root'))
 for dex,form,title in FUSIONS:
  # Native form spelling is verified against the supported species data.
  forms=[f['name'] for f in species[dex]['forms']]
  if form not in forms:raise ValueError((dex,form,forms))
  ident=f'fusion_{dex}_{form}';obj=advancement(title,FUSION_DESCRIPTIONS[(dex,form)],icon(dex,form),'fusion');write(target/(ident+'.json'),obj)
  catalog['fusions'].append({'dex':dex,'form':form,'id':ident,'title':title})
 obj=advancement('无极之巅','让自己拥有的无极汰那携带无极陨星，转变为无极巨化形态。',icon(890,'eternamax'),'gen8')
 write(target/'form_890_eternamax.json',obj)
 catalog['transformations']=[{'dex':890,'form':'eternamax','id':'form_890_eternamax','title':'无极之巅'}]
 write(ROOT/'resources/backport/advancements.json',catalog)
 write(ROOT/'docs/ADVANCEMENT_CATALOGUE_1.3.0.json',catalog)
 print(f'Generated {len(catalog["captures"])} capture and {len(FUSIONS)} fusion achievements; quest names matched: {sum(int(d) in names for d in catalog["captures"])}.')
if __name__=='__main__':main()
