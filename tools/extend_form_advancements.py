"""Add requested native Sky, Origin and Therian advancements using exact native form identifiers."""
import argparse,json,zipfile
from extend_advancements import ROOT,sprite_icon,advancement,write
FORMS=[
 (492,'sky','桃红柳绿','白天，对自己拥有的谢米使用葛拉西蒂亚花，完成天空形态变化。'),
 (483,'origin','时间溯源','手持大金刚宝玉与自己的帝牙卢卡交互，完成起源形态变化。'),
 (484,'origin','空间溯源','手持大白宝玉与自己的帕路奇亚交互，完成起源形态变化。'),
 (487,'origin','反转溯源','手持大白金宝玉与自己的骑拉帝纳交互，完成起源形态变化。'),
 (641,'therian','龙卷之灵','对自己拥有的龙卷云使用现形镜，完成灵兽形态变化。'),
 (642,'therian','雷电之灵','对自己拥有的雷电云使用现形镜，完成灵兽形态变化。'),
 (645,'therian','土地之灵','对自己拥有的土地云使用现形镜，完成灵兽形态变化。'),
 (905,'therian','眷恋之灵','对自己拥有的眷恋云使用现形镜，完成灵兽形态变化。'),
 (720,'unbound','解放之环','对自己拥有的胡帕使用惩戒之壶，完成解放形态变化。'),
 (888,'crowned','剑之王','让自己拥有的苍响携带腐朽的剑，通过场外形态变化或实际出战，完成变形成为剑之王形态。'),
 (889,'crowned','盾之王','让自己拥有的藏玛然特携带腐朽的盾，通过场外形态变化或实际出战，完成变形成为盾之王形态。'),
]

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--pixelmon',required=True);args=parser.parse_args()
 with zipfile.ZipFile(args.pixelmon) as jar:
  assets=set(jar.namelist());species={}
  for path in assets:
   if path.startswith('data/pixelmon/species/') and path.endswith('.json'):
    data=json.loads(jar.read(path));species[data['dex']]=data
 target=ROOT/'resources/data/pixelmon_backport/advancements/legends'
 write(target/'forms.json',advancement('传说异姿','让自己拥有的传说与幻之宝可梦，实际完成特殊形态变化。',{'item':'pixelmon:master_ball'},'root'))
 rows=[]
 for dex,form,title,description in FORMS:
  ident=f'form_{dex}_{form}'
  write(target/(ident+'.json'),advancement(title,description+'还原形态后保留成就；仅生成或获赠成品形态不视为完成转换。',sprite_icon(species,dex,assets,form),'forms'))
  rows.append({'dex':dex,'form':form,'id':ident,'title':title,'category':'forms','source':'form-change'})
 path=ROOT/'resources/backport/advancements.json';catalog=json.loads(path.read_text(encoding='utf-8'))
 added={row['id'] for row in rows};catalog['transformations']=[row for row in catalog['transformations'] if row['id'] not in added]+rows
 zygarde={}
 for form,ident,title,percent in [('ten_percent','capture_718_ten_percent','十方初聚','10%'),('fifty_percent','capture_718','浑然一体','50%')]:
  description=f'成功捕捉，或通过原生重组装置实际完成组装，或使用多面体让群聚变形个体实际切换为基格尔德 {percent} 形态。两个形态分别记录；仅获得细胞、核心或查看形态不会完成。'
  write(target/(ident+'.json'),advancement(title,description,sprite_icon(species,718,assets,form),'gen6'))
  zygarde[form]={'id':ident,'category':'gen6','title':title,'form':form}
 catalog['capturesByForm']={'718':zygarde}
 urshifu={}
 for form,ident,title,label in [('singlestrike','capture_892','招招致命','一击流'),('rapidstrike','capture_892_rapidstrike','流水连击','连击流')]:
  description=f'让自己拥有的熊徒弟按原生卷轴进化流程，实际进化为武道熊师{label}，或成功捕捉这一形态。两种流派分别记录；生成或获赠成品不会补发。'
  write(target/(ident+'.json'),advancement(title,description,sprite_icon(species,892,assets,form),'gen8'))
  urshifu[form]={'id':ident,'category':'gen8','title':title,'form':form}
 catalog['capturesByForm']['892']=urshifu
 # The native first-generation birds remain in the native advancement tab.
 # Only their Galarian forms belong to this addon's legend chapter.
 for dex,title,name in [(144,'冰羽心眼','伽勒尔急冻鸟'),(145,'雷足疾驰','伽勒尔闪电鸟'),(146,'烈焰黑翼','伽勒尔火焰鸟')]:
  ident=f'capture_{dex}_galarian'
  write(target/(ident+'.json'),advancement(title,f'成功捕捉{name}。普通地区形态的捕捉不计入，生成或赠送也不视为捕捉完成。',sprite_icon(species,dex,assets,'galarian'),'gen8'))
  catalog['capturesByForm'][str(dex)]={'galarian':{'id':ident,'category':'gen8','title':title,'form':'galarian'}}
 write(path,catalog);write(ROOT/'docs/ADVANCEMENT_CATALOGUE_1.3.0.json',catalog)
 print('Generated',len(rows),'special-form, two Zygarde, two Urshifu and three Galarian-bird achievements with verified native sprites.')
if __name__=='__main__':main()
