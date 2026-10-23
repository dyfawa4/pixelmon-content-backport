"""Reviewed travel/equipment recipes, folded help and native tooltip corrections."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'resources'
def write(path,value):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n','utf-8')
def apply():
 def shaped(name,pattern,key,result):
  write(RES/f'data/pixelmon_backport/recipes/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':key,'result':{'item':result,'count':1}})
 shaped('saddle',[' L ','LIL'],{'L':{'item':'minecraft:leather'},'I':{'item':'minecraft:iron_ingot'}},'minecraft:saddle')
 for material in ['gold','iron']:
  shaped('name_tag_'+material,[' N','P '],{'N':{'item':'minecraft:'+material+'_nugget'},'P':{'item':'minecraft:paper'}},'minecraft:name_tag')
 shaped('elytra',['MRM','MBM','M M'],{'M':{'item':'minecraft:phantom_membrane'},'R':{'item':'pixelmon:reins_of_unity'},'B':{'type':'pixelmon_backport:master_ball'}},'minecraft:elytra')
 for kind in ['smelting','smoking']:
  write(RES/f'data/pixelmon_backport/recipes/ultra_deep_kelp_{kind}.json',{'type':'minecraft:'+kind,'ingredient':{'item':'pixelmon:ultra_deep_kelp'},'result':'minecraft:dried_kelp_block','experience':0.1,'cookingtime':300 if kind=='smoking' else 600})
 write(RES/'data/pixelmon_backport/recipes/ultra_forest_flower_lime_dye.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'pixelmon:ultra_forest_flower'}],'result':{'item':'minecraft:lime_dye','count':2}})
 colors=['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']
 write(RES/'data/pixelmon/tags/items/day_cares.json',{'replace':False,'values':['pixelmon:'+color+'_day_care' for color in colors]})
 p=RES/'backport/item_help.json';data=json.loads(p.read_text('utf-8'))
 data['minecraft:saddle']=['获取：3个皮革与1个铁锭合成；皮革放顶中、中左、中右，铁锭放中心，产出1个马鞍。']
 data['minecraft:name_tag']=['获取：金粒或铁粒放右上，纸放左下，斜向摆放合成1个命名牌。']
 data['pixelmon:ultra_deep_kelp']=['加工：熔炉30秒，烟熏炉15秒；1个究极深海海带产出1个干海带块。']
 data['pixelmon:ultra_forest_flower']=['加工：1朵究极森林花无序合成2份黄绿色染料。']
 for color in colors:data['pixelmon:'+color+'_day_care']=['获取：可以在村庄房屋店员处使用宝可币购买获得']
 data['minecraft:elytra']=['获取：工作台两侧各3个幻翼膜，顶中牵绊缰绳，中心大师球，底中空，合成1个鞘翅。也可从5星极巨巢穴通用奖池获得，权重1。']
 extra='也可从5星极巨巢穴通用奖池随机获得，权重1。'
 rows=data['pixelmon_backport:eternatus_meteorite'];rows[:]=[row for row in rows if '5星极巨巢穴' not in row];rows.append(extra)
 if data.get('pixelmon:poke_ball')==[]:data.pop('pixelmon:poke_ball',None)
 write(p,data)
if __name__=='__main__':apply()
