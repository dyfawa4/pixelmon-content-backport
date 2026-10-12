"""Offline validation of Chinese NPC resources and untouched gameplay fields."""
import argparse,json,pathlib,re,zipfile
from npc_zh_cn import leaves
from extend_gym_cities import read_nbt

def audit_embedded(data,path):
 count=0
 for row in data.get('entities',[]):
  e=row.get('nbt',{})
  if str(e.get('id',''))!='pixelmon:npc_chatting':continue
  for key,value in e.items():
   if key=='Name' or re.fullmatch(r'chat\d+',key):
    assert not re.search('[A-Za-z]',str(value)),(path,'embedded English NPC text',key,str(value))
  count+=1
 return count

def skeleton(v):
 if isinstance(v,str):return '<text>'
 if isinstance(v,list):return [skeleton(x) for x in v]
 if isinstance(v,dict):return {k:skeleton(x) for k,x in v.items()}
 return v

def main():
 p=argparse.ArgumentParser();p.add_argument('--pixelmon',required=True);a=p.parse_args()
 root=pathlib.Path(__file__).resolve().parents[1];overlay=lines=available=0
 with zipfile.ZipFile(a.pixelmon) as native:
  for path in native.namelist():
   if not path.startswith('data/pixelmon/npcs/') or not path.endswith('_en_us.json'):continue
   name=path.replace('_en_us.json','_zh_cn.json');target=root/'resources'/name
   if target.is_file():
    en=json.loads(native.read(path));zh=json.loads(target.read_text(encoding='utf8'))
    assert skeleton(en['chat'])==skeleton(zh['chat']),(name,'changed chat indices')
    assert {k:v for k,v in en.items() if k!='chat'}=={k:v for k,v in zh.items() if k!='chat'},(name,'gameplay/name/skin data changed')
    for text in leaves(zh['chat']):
     assert re.search('[\u4e00-\u9fff]',text) or not re.search('[A-Za-z]',text),(name,'English fallback',text)
    overlay+=1;lines+=len(list(leaves(zh['chat'])))
   else:assert name in native.namelist(),(name,'missing Chinese locale')
   available+=1
  pip=json.loads((root/'resources/data/pixelmon/npcs/villagers/littleboy_zh_cn.json').read_text(encoding='utf8'))
  assert '捕捉高手' in pip['chat'][2]['lines'][0]['text']
 assert overlay==162 and available==178,(overlay,available)
 embedded=0
 for ns in ['pixelmon','pixelmon_backport']:
  for path in (root/f'resources/data/{ns}/structures/gyms').rglob('*.nbt'):embedded+=audit_embedded(read_nbt(path.read_bytes()),str(path))
 assert embedded>=31,('Missing serialized NPC coverage',embedded)
 print(f'PASS Chinese NPC dialogue: {overlay} overlays / {lines} lines; all {available} stock NPC definitions have zh_cn; unchanged gameplay and chat indices; Pip regression')
 print(f'PASS {embedded} serialized custom chatting NPCs: Chinese names and chat fields; includes template-only Barley/daycare text.')

if __name__=='__main__':main()
