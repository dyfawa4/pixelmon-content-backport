"""Keep the reviewed exploration/crafting sources and folded item help together."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
NS='pixelmon_backport'
MASK_TABLES={
 'jungle_temple':'teal_mask',
 'shipwreck_map':'wellspring_mask','shipwreck_supply':'wellspring_mask','shipwreck_treasure':'wellspring_mask',
 'buried_treasure':'wellspring_mask','underwater_ruin_small':'wellspring_mask','underwater_ruin_big':'wellspring_mask',
 'nether_bridge':'hearthflame_mask','bastion_bridge':'hearthflame_mask','bastion_hoglin_stable':'hearthflame_mask','bastion_other':'hearthflame_mask','bastion_treasure':'hearthflame_mask',
 'desert_pyramid':'cornerstone_mask'
}
REMOVED={'teal_mask','wellspring_mask','hearthflame_mask','cornerstone_mask','syrupy_apple','gimmighoul_coin'}
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
def apply(output=None):
 output=Path(output or ROOT/'resources')
 def recipe(name,pattern,key,result):
  write(output/f'data/{NS}/recipes/{name}.json',{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{symbol:{'item':item} for symbol,item in key.items()},'result':{'item':result,'count':1}})
 recipe('purple_nectar',[' H ','HFH',' H '],{'H':'minecraft:honey_bottle','F':'pixelmon:ultra_desert_cactus'},NS+':purple_nectar')
 recipe('sweet_apple',[' S ','SAS',' S '],{'S':'minecraft:sugar','A':'minecraft:golden_apple'},'pixelmon:sweet_apple')
 recipe('tart_apple',[' W ','WAW',' W '],{'W':'minecraft:nether_wart','A':'minecraft:golden_apple'},'pixelmon:tart_apple')
 recipe('syrupy_apple',['H','A'],{'H':'minecraft:honey_block','A':'minecraft:golden_apple'},NS+':syrupy_apple')
 recipe('sachet',[' S ','LML','LLL'],{'S':'minecraft:string','L':'minecraft:leather','M':'pixelmon:balm_mushroom'},'pixelmon:sachet')
 recipe('whipped_dream',['SMS','WWW'],{'S':'minecraft:sugar','M':'pixelmon:lemonade','W':'minecraft:wheat'},'pixelmon:whipped_dream')
 recipe('linking_cord',['DLD','LBL',' L '],{'D':'minecraft:diamond','L':'minecraft:lead','B':'minecraft:black_dye'},'pixelmon:linking_cord')
 recipe('gimmighoul_coin',['GGG','GSG','GGG'],{'G':'minecraft:gold_block','S':'pixelmon:ghost_gem'},NS+':gimmighoul_coin')
 recipe('pixelmon_grass',['DAJ','VGU','BSO'],{'D':'minecraft:dark_oak_sapling','A':'minecraft:acacia_sapling','J':'minecraft:jungle_sapling','V':'minecraft:vine','G':'minecraft:grass','U':'pixelmon:ultra_jungle_vine','B':'minecraft:birch_sapling','S':'minecraft:spruce_sapling','O':'minecraft:oak_sapling'},'pixelmon:pixelmon_grass')
 for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
  path=output/f'data/{NS}/recipes/pc_{color}_from_parts.json'
  write(path,{'type':'minecraft:crafting_shaped','pattern':['AGA','ALA','ARA'],'key':{'A':{'tag':'forge:ingots/aluminium'},'G':{'item':'minecraft:'+color+'_stained_glass_pane'},'L':{'item':'minecraft:redstone_lamp'},'R':{'tag':'forge:dusts/redstone'}},'result':{'item':'pixelmon:'+color+'_pc','count':1}})
  write(output/f'data/{NS}/recipes/trade_machine_{color}_from_parts.json',{'type':'minecraft:crafting_shaped','pattern':[' M ','LPR',' D '],'key':{'M':{'item':'pixelmon:trade_monitor'},'L':{'item':'pixelmon:trade_holder_left'},'P':{'item':'pixelmon:trade_panel'},'R':{'item':'pixelmon:trade_holder_right'},'D':{'tag':'forge:dyes/'+color}},'result':{'item':'pixelmon:'+color+'_trade_machine','count':1}})
 for color in ['pink','purple','blue','green','red']:
  write(output/f'data/{NS}/recipes/berry_planks_{color}_from_logs.json',{'type':'minecraft:crafting_shapeless','ingredients':[{'item':'pixelmon:apricorn_log'},{'item':'pixelmon:apricorn_log'},{'tag':'forge:dyes/'+color}],'result':{'item':'pixelmon:'+color+'_berry_wood_planks','count':8}})
 masks={'chance':0.07,'chances':{NS+':teal_mask':0.20},'tables':{'minecraft:chests/'+name:NS+':'+mask for name,mask in MASK_TABLES.items()}}
 write(output/'backport/structure_mask_loot.json',masks)
 write(output/f'data/{NS}/loot_modifiers/exploration_masks.json',{'type':NS+':exploration_masks','conditions':[],**masks})
 write(output/'data/forge/loot_modifiers/global_loot_modifiers.json',{'replace':False,'entries':[NS+':exploration_masks']})
 p=output/'backport/content.json';data=json.loads(p.read_text('utf-8'))
 for pool in data.get('referenceExtras',{}).get('drops/pokechestdrops',{}).values():
  if isinstance(pool,list):pool[:]=[row for row in pool if (row.get('type') if isinstance(row,dict) else row) not in {NS+':'+name for name in REMOVED}]
 write(p,data)
 p=output/'backport/item_help.json';help=json.loads(p.read_text('utf-8'))
 for name,source in {
 'teal_mask':'获取：丛林神殿箱子，出现概率20%。',
 'wellspring_mask':'获取：沉船、埋藏的宝藏、海底废墟箱子，各有7%概率出现。',
 'hearthflame_mask':'获取：下界要塞、猪灵堡垒箱子，各有7%概率出现。',
 'cornerstone_mask':'获取：沙漠神殿箱子，出现概率7%。',
 'syrupy_apple':'获取：蜂蜜块紧邻放在金苹果正上方，合成1个；横放、倒放或隔开均不可合成。',
 'gimmighoul_coin':'获取：8个金块围绕1颗幽灵宝石，合成1枚。',
 'purple_nectar':'获取：工作台中心放究极沙漠仙人掌，上下左右各放1瓶蜂蜜，合成1个，返还4个玻璃瓶。'
 }.items():
  if name.endswith('_mask'):source+=' 也可从5星极巨巢穴通用奖池随机获得，单项权重5。'
  key=NS+':'+name;help[key]=[line for line in help.get(key,[]) if not line.startswith('获取：')]+[source]
 help['pixelmon:sweet_apple']=['啃果虫的进化道具，可使其进化为丰蜜龙。','获取：工作台中心金苹果，上下左右各1份糖，合成1个。']
 help['pixelmon:tart_apple']=['啃果虫的进化道具，可使其进化为苹裹龙。','获取：工作台中心金苹果，上下左右各1个地狱疣，合成1个。']
 help['pixelmon:sachet']=['粉香香的进化携带物。','获取：顶中1根线，中间芳香蘑菇，中排两侧及底排共5张皮革，合成1个。']
 help['pixelmon:whipped_dream']=['绵绵泡芙的进化携带物。','获取：果汁牛奶两侧各1份糖，正下方一排3份小麦，合成1个。']
 help['pixelmon:linking_cord']=['用于兼容连接交换进化的宝可梦。','获取：顶排为钻石、拴绳、钻石；中排为拴绳、黑色染料、拴绳；底中1根拴绳，合成1个。']
 for color in ['white','orange','magenta','light_blue','yellow','lime','pink','gray','light_gray','cyan','purple','blue','brown','green','red','black']:
  help['pixelmon:'+color+'_trade_machine']=['获取：顶中交换机显示器；中排左托架、交换机控制板、右托架；底中对应颜色染料，合成1个。已有交换机不能用染料换色。']
 for color in ['pink','purple','blue','green','red']:
  help['pixelmon:'+color+'_berry_wood_planks']=['获取：2个球果原木与1份对应颜色染料合成8块；不再使用成品木板换色。'+('绿色还可由树果原木直接加工获得。' if color=='green' else '')]
 help['pixelmon:pixelmon_grass']=['获取：工作台顶排为深色橡树树苗、金合欢树苗、丛林树苗；中排为原版藤蔓、草、究极丛林藤蔓；底排为白桦树苗、云杉树苗、橡树树苗，每格1个，合成1个宝可梦草丛。']
 help.pop(NS+':tera_raid_music_disc',None)
 write(p,help)
 for lang,changes in {
  'zh_cn':{
   'attack.origin_of_thunder.description':'威力185的专属电系 Z 招式，展开爆裂场地。保留电气场地原有效果；展开方电属性宝可梦的电系招式非中性倍率增加25%，对方攻击本方电属性宝可梦的非中性倍率降低25%。中性与免疫不变，捕捉倍率额外加2；禁止捕捉仍无效。持续至被其他场地覆盖或本场对战结束。',
   'attack.ninja_training.description':'大幅提高自身特攻。',
   'item.pixelmon.legend_plate.tooltip':'一块灌注了万物本源之力的石板。对特定宝可梦使用后，可使其获得所有属性的力量。',
   'item.pixelmon.boss_music_disc.desc':'剑盾-道馆馆主对战',
   'item.pixelmon.wild_music_disc.desc':"宝可梦 Let's Go! 皮卡丘／伊布-野生宝可梦对战",
   'item.pixelmon.raid_music_disc.desc':'宝可梦剑盾-极巨巢穴对战',
   'item.pixelmon_backport.tera_raid_music_disc':'音乐唱片',
   'item.pixelmon_backport.tera_raid_music_disc.desc':'宝可梦朱紫-太晶巢穴对战',
   'item.pixelmon.wishing_piece.tooltip':'投掷到宝可梦的巢穴里，就会有极巨化/太晶化宝可梦出现',
   'item.pixelmon.waterdude_wishing_piece.tooltip':'投掷到宝可梦的巢穴里，就会有传说中的极巨化/太晶化宝可梦出现',
   'pixelmon.backport.cap.capture_warning':'这只皮卡丘貌似更喜欢%s，该球捕捉倍率固定为2倍，其他可用球按原生倍率乘0.5；大师球、起源球、公园球无法捕捉，只能在战斗中捕捉！',
   'pixelmon.backport.cap.forbidden_ball':'大师球、起源球和公园球无法捕捉戴帽子的皮卡丘，请换一种精灵球。',
   'pixelmon.backport.cap.field_capture_blocked':'戴帽子的皮卡丘无法场外捕捉，请进入战斗后使用精灵球。',
   'pixelmon.generic.form.terateal':'太晶·碧草面具',
   'pixelmon.generic.form.terawellspring':'太晶·水井面具',
   'pixelmon.generic.form.terahearthflame':'太晶·火灶面具',
   'pixelmon.generic.form.teracornerstone':'太晶·础石面具'},
  'en_us':{
   'attack.origin_of_thunder.description':'A 185-power Electric Z-Move that creates Burst Terrain. Inherits Electric Terrain; boosts non-neutral Electric effectiveness for Electric allies by 25%, reduces non-neutral incoming effectiveness against Electric allies by 25%, and adds 2 to usable catch multipliers. Neutral hits, immunities and capture bans are unchanged. Lasts until replaced or this battle ends.',
   'attack.ninja_training.description':"Sharply raises the user's Sp. Atk.",
   'item.pixelmon.boss_music_disc.desc':'Sword and Shield Gym Leader Battle',
   'item.pixelmon.wild_music_disc.desc':'Wild Pokemon Battle',
   'item.pixelmon.raid_music_disc.desc':'Zeecount - Max Raid Battle',
   'item.pixelmon_backport.tera_raid_music_disc':'Music Disc',
   'item.pixelmon_backport.tera_raid_music_disc.desc':'Tera Raid Battle',
   'item.pixelmon.wishing_piece.tooltip':'Throw into an empty den to summon a Dynamax or Tera Raid Pokemon.',
   'item.pixelmon.waterdude_wishing_piece.tooltip':'Throw into an empty den to summon a Legendary Dynamax or Tera Raid Pokemon.',
   'pixelmon.backport.cap.capture_warning':'This Pikachu prefers %s: a fixed 2x catch multiplier. Other usable balls use 0.5x their native multiplier. Master, Origin and Park Balls cannot catch it. It can only be caught in battle!',
   'pixelmon.backport.cap.forbidden_ball':'Master, Origin and Park Balls cannot catch Cap Pikachu. Choose another ball.',
   'pixelmon.backport.cap.field_capture_blocked':'Cap Pikachu can only be caught in battle.',
   'pixelmon.generic.form.terateal':'Terastal Teal Mask',
   'pixelmon.generic.form.terawellspring':'Terastal Wellspring Mask',
   'pixelmon.generic.form.terahearthflame':'Terastal Hearthflame Mask',
   'pixelmon.generic.form.teracornerstone':'Terastal Cornerstone Mask'}
 }.items():
  path=output/f'assets/{NS}/lang/{lang}.json';data=json.loads(path.read_text('utf-8'));data.update(changes)
  if lang=='zh_cn':data.update(json.loads((output/'assets/comforts/lang/zh_cn.json').read_text('utf-8')))
  write(output/f'backport/description_fixes_{lang}.json',{key:value for key,value in changes.items() if key.endswith(('.tooltip','.desc','.description')) or key=='item.pixelmon_backport.tera_raid_music_disc'})
  write(path,data)
 from extend_oath_mega import apply as apply_oath_mega
 apply_oath_mega()
 from extend_boss_followup import apply as apply_boss_followup
 apply_boss_followup()
if __name__=='__main__':apply()
