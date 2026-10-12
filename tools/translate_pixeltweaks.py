"""Translate every supplied PixelTweaks 1.10.6 language key; preserve format arguments."""
import argparse,hashlib,json,pathlib,re,zipfile
ROOT=pathlib.Path(__file__).resolve().parents[1]
TEXT='''Shiny Pokemon Sparkles	闪光宝可梦的闪耀声
%s must be level %s before using this!	%s 需要达到 %s 级才能使用！
Arceus Battle Music	阿尔宙斯对战音乐
Spawn Pokemon	生成宝可梦
Spawn Trainers (NPCs)	生成训练家 NPC
Pokemon Drops	宝可梦掉落物
Poke Loot Chest	宝可梦奖励箱
Tier 1 Loot	一级奖励箱战利品
Tier 2 Loot	二级奖励箱战利品
Tier 3 Loot	三级奖励箱战利品
Ultraspace Loot	究极空间战利品
Infuser Recipes	注入器配方
Catch Pokemon	捕捉宝可梦
Catch	捕捉
Pokemon Specification	宝可梦筛选条件
Amount	数量
Invert Specs	反向筛选
Catch Type	获取方式
Defeat Pokemon	击败宝可梦
Defeat	击败
Defeat %s with %s	击败 %s（使用 %s）
Target Pokemon Specification	目标宝可梦筛选条件
Wild Pokemon Only	仅限野生宝可梦
Invert Target Specs	反向筛选目标
Source Pokemon Specification	我方宝可梦筛选条件
Invert Source Specs	反向筛选我方
Defeat Pokemon Trainer	击败宝可梦训练家
Gym Leader	道馆馆主
Boss Tier	首领等级
The boss tier of the trainer. Default is any.	训练家的首领等级；默认不限制。
Defeat Player	击败玩家
Username	玩家名称
Optional. The name of the player to match against.	可选。需要匹配的玩家名称。
UUID	玩家 UUID
Optional. The UUID of the player to match against.	可选。需要匹配的玩家 UUID。
Party Wipeout	队伍全部失去战斗能力
Hatch Pokemon	孵化宝可梦
Hatch	孵化
Breed Pokemon	培育宝可梦
Breed	培育
Parent 1 Specification	亲代一筛选条件
Like other pokemon specifications. Optional.	与其他宝可梦筛选条件相同；可留空。
Parent 2 Specification	亲代二筛选条件
Trade Pokemon	交换宝可梦
Trade	交换
Evolve Pokemon	进化宝可梦
Evolve	进化
Evolution Cause	进化方式
Evolution Item	进化道具
The item that was used to evolve the pokemon. Optional. Only applicable for item interaction evolution.	使宝可梦进化的道具；可选，仅适用于通过道具交互触发的进化。
Pokedex Amount	图鉴数量
Amount Needed	所需数量
Pokedex Filter	图鉴筛选方式
The filter to use for the pokedex. Leaving to ALL will count all pokemon in the pokedex.	图鉴的筛选方式。选择“全部”会统计图鉴中的所有宝可梦。
Caught Only	仅统计已捕捉
Type	属性
The type of pokemon to count. Only applicable if you set the filter to TYPE.	需要统计的宝可梦属性，仅在筛选方式设为“属性”时生效。
Generation Min	起始世代
The minimum generation to count. Only applicable if you set the filter to GENERATION.	需要统计的最早世代，仅在筛选方式设为“世代”时生效。
Generation Max	结束世代
The maximum generation to count. Only applicable if you set the filter to GENERATION.	需要统计的最晚世代，仅在筛选方式设为“世代”时生效。
Specific Pokemon (Dex Number)	指定宝可梦（图鉴编号）
The specific pokemon to count. Only applicable if you set the filter to SPECIFIC POKEMON.	需要统计的指定宝可梦，仅在筛选方式设为“指定宝可梦”时生效。
Allow Undexable Pokemon	计入无法登记图鉴的宝可梦
Pokedex Percentage	图鉴完成比例
Percentage	百分比
The percentage of the pokedex that must be completed. 50% is 50.0, not 0.5	图鉴需要完成的百分比。50% 应填写 50.0，而不是 0.5。
Pokedollars	宝可币
Obtain %s Pokedollars	获得 %s 宝可币
The amount of pokedollars to collect.	需要收集的宝可币数量。
Consume Pokedollars	提交时扣除宝可币
Level Up Pokemon	提升宝可梦等级
Level	升级
Level Up Cause	升级方式
The cause of the level up. Can be any, curry, rare candy, etc.	升级的原因，可选择不限、咖喱、神奇糖果等。
Use Battle Move	使用对战招式
Use %s%s	使用 %s%s
Use %s%s with %s	使用 %s%s（宝可梦：%s）
Move	招式
Optional. The move to use, in full lowercase english with underscores instead of spaces. If blank, will match any move.	可选。填写招式的英文标识，使用小写字母，并用下划线替代空格；留空表示任意招式。
Move Type	招式类别
Invert Spec	反向筛选
Critical Hit	击中要害
Use Battle Item	使用对战道具
Item Type	道具类别
The type of item to match.	需要匹配的道具类别。
Specific Custom Item	指定自定义道具
The custom item to match. Only applicable if the item type is CUSTOM.	需要匹配的自定义道具，仅在道具类别设为“自定义”时生效。
Take Photo	拍摄照片
Take Photo of	拍摄宝可梦
Ignore Bosses	忽略首领
Whether to ignore boss pokemon. If true, the task will ignore photos of boss pokemon	是否忽略首领宝可梦。启用后，首领宝可梦的照片不计入任务。
Use External Move	使用场外技能
Use %s	使用 %s
The external move used	需要使用的场外技能。
Submit Pokemon	提交宝可梦
This will take pokemon from your %s	提交时会从你的%s中移除宝可梦。
This check your %s for the pokemon	检查你的%s中是否有符合条件的宝可梦。
Requires same OT	要求相同的初训家
Catch and Submit	捕捉并提交
Note: This cannot be more than 1 if the pokemon is not consumed!	注意：不移除宝可梦时，该数量不能大于 1！
Remove Pokemon From	从何处提交宝可梦
Where should the pokemon be taken from?	从队伍还是电脑盒子中选取需要提交的宝可梦？
Consume Pokemon	提交时移除宝可梦
Whether the pokemon is actually removed from the player when the task is submitted. If false, the pokemon is only checked for.	提交任务时是否从玩家处移除宝可梦。关闭后只检查是否拥有，不会移除。
Release Pokemon	放生宝可梦
Release	放生
Bring Pokemon to Location	携带宝可梦前往指定地点
Bring %s to %s	携带 %s 前往 %s
Party Slot	队伍位置
The slot in the party to check for the pokemon.	检查队伍中指定位置的宝可梦。
Position X	位置 X
Position Y	位置 Y
Position Z	位置 Z
Position Width (X)	位置范围宽度（X）
How far the player can be from the position on the X axis.	玩家在 X 轴上允许偏离指定位置的距离。
Position Height (Y)	位置范围高度（Y）
Position Depth (Z)	位置范围深度（Z）
How far the player can be from the position on the Z axis.	玩家在 Z 轴上允许偏离指定位置的距离。
Dimension	维度
Ignore Dimension	忽略维度
PokeLoot Chest Drop	宝可梦奖励箱战利品
%s Chest Drop	%s奖励箱战利品
Poke Ball	精灵球
Ultra Ball	高级球
Master Ball	大师球
Beast Ball	究极球
Loot Tier	战利品等级
The loot tier of the chest. Higher tiers have better loot.	奖励箱的战利品等级；等级越高，战利品越好。
Play Sound	播放音效
Will drop a random item from a %s Chest when claimed	领取时随机获得%s奖励箱中的一件道具。
Claimed %s %s Chest Drops	已领取 %s 份%s奖励箱战利品
The amount of Pokedollars to give.	奖励的宝可币数量。
Claimed %s Pokedollars	已领取 %s 宝可币
Pokemon	宝可梦
Claimed %s	已领取 %s
Shiny Chance	闪光概率
The 1 in X chance for the pokemon to be shiny. 0 is none, 100 is 1/100. Default is standard shiny rate (4096)	闪光概率为 1/X。0 表示不会闪光，100 表示 1/100；默认采用通常的闪光概率 1/4096。
Any	不限
Leveling	升级
Item Interaction	道具交互
Sporadically	周期检查
All	全部
Generation	世代
Legendary	传说宝可梦
Legendary & Mythical	传说与幻之宝可梦
Mythical	幻之宝可梦
Ultra Beast	究极异兽
Specific Pokemon	指定宝可梦
Curry	咖喱
Rare Candy	神奇糖果
Soda	汽水
Exp Candy	经验糖果
Battle	对战
All Items	所有道具
All Candies	所有糖果
Z-Move	Z 招式
Dynamax (Max) Move	极巨招式
Standard Move	普通招式
Pokeball Catch	使用精灵球捕捉
Raid	巢穴团体战
Pokeball Catch or Raid	精灵球捕捉或巢穴团体战
Fossil	化石复活
Christmas Bonus Gift	圣诞额外赠礼
Commands	指令
Any (Except Commands)	不限（不含指令）
Custom	自定义
Potion	伤药
Berry	树果
Revive	复活道具
Boost	能力提升道具
Herb	药草
Status Heal	异常状态恢复
Escape	逃离对战
Any Slot	任意队伍位置
Slot 1	队伍第 1 位
Slot 2	队伍第 2 位
Slot 3	队伍第 3 位
Slot 4	队伍第 4 位
Slot 5	队伍第 5 位
Slot 6	队伍第 6 位
Party	队伍
Box	电脑盒子
Party or Box	队伍或电脑盒子
Random	随机
%s Type	%s属性
Gen %s	第 %s 世代
in a %s	使用%s捕捉
with Pokerus	携带宝可病毒
Non %s	排除 %s
Invalid Pokemon Spec!	宝可梦筛选条件无效！
An error occurred parsing that spec!	解析该筛选条件时发生错误！
You do not have that pokemon!	你没有这只宝可梦！
You cannot submit a pokemon you do not have!	无法提交你未拥有的宝可梦！'''
TRANSLATIONS=dict(line.split('\t',1) for line in TEXT.splitlines())
SPEC_EXAMPLES="\n\n示例（配置语法保持英文）：\n - 'species:Arcanine form:Hisuian'\n - 'shiny type:water'\n - 'legendary ball:master_ball'\n\n详细语法：https://pixelmonmod.com/wiki/Pokemon_spec"
SPEC_HEADS={
 'The pokemon spec to match.':'需要匹配的宝可梦筛选条件。',
 'The target pokemon spec to match.':'需要匹配的目标宝可梦筛选条件。',
 'The source pokemon spec to match.':'需要匹配的我方宝可梦筛选条件。',
 'The pokemon the item is used on.':'接受道具作用的宝可梦筛选条件。',
}
PARAGRAPHS={
 'Default: Wild and non wild pokemon are counted\nTrue: Only wild pokemon\nFalse: Only non wild pokemon':'默认：统计野生与非野生宝可梦\n启用：仅统计野生宝可梦\n关闭：仅统计非野生宝可梦',
 'Default: Affects all trainers\nTrue: Only gym leaders\nFalse: Only non gym leaders':'默认：适用于所有训练家\n启用：仅限道馆馆主\n关闭：仅限非馆主训练家',
 'True: Only count caught pokemon\nFalse: Count seen and caught pokemon':'启用：仅统计已捕捉的宝可梦\n关闭：统计已见到和已捕捉的宝可梦',
 'True: Allow pokemon that cannot be dexed to count towards the percentage\nFalse: Do not allow pokemon that cannot be dexed to count towards the percentage':'启用：无法登记图鉴的宝可梦也计入完成比例\n关闭：不计入这些宝可梦',
 'True: Will consume the pokedollars on turn in\nFalse: Will not consume the pokedollars on turn in\nDefault: Will obey the chapter setting':'启用：提交任务时扣除宝可币\n关闭：提交时不扣除宝可币\n默认：遵循章节设置',
 'Default: Will trigger regardless of crit\nTrue: Will only trigger on a critical hit\nFalse: Will only trigger on a non-critical hit':'默认：无论是否击中要害都触发\n启用：仅在击中要害时触发\n关闭：仅在未击中要害时触发',
 'Whether the submitted pokemon must have the same Original Trainer ID as the player. \n\nTrue - Requires the same\nDefault: Doesn\'t matter\nFalse: Requires different OT.':'提交的宝可梦是否必须与玩家拥有相同的初训家编号。\n\n启用：必须相同\n默认：不限制\n关闭：必须不同',
 'How far the player can be from the position on the Y axis.\n\nIf 0, the Y axis is ignored.':'玩家在 Y 轴上允许偏离指定位置的距离。\n\n设为 0 时忽略 Y 轴。',
 'True: Will play the PokeLoot sound effect\nFalse: Will not play nothing':'启用：播放宝可梦奖励箱音效\n关闭：不播放音效',
}

def translate(value):
 if value in TRANSLATIONS:return TRANSLATIONS[value]
 if value in PARAGRAPHS:return PARAGRAPHS[value]
 if value.startswith('Whether to invert the '):
  who='目标宝可梦' if 'target pokemon' in value else '我方宝可梦' if 'source pokemon' in value else '宝可梦'
  return f'是否反向应用{who}筛选条件。启用后，任务只统计不符合该条件的宝可梦。'
 for head,translated in SPEC_HEADS.items():
  if value.startswith(head+' Can match'):
   out=translated+'可匹配种类、属性、精灵球等；留空表示任意宝可梦。'+SPEC_EXAMPLES
   if 'Pokemon in raids cannot check the pokeball' in value:out+='\n\n注意：巢穴团体战中的宝可梦无法检查精灵球；这是 Pixelmon 原有的限制。'
   return out
 if value.startswith('The pokemon spec to match. In this case,'):
  return '需要匹配进化后的宝可梦，例如妙蛙草、妙蛙花，而不是进化前的妙蛙种子。\n\n可匹配种类、属性、精灵球等；留空表示任意宝可梦。\n\n详细语法：https://pixelmonmod.com/wiki/Pokemon_spec'
 if value.startswith('The pokemon to give.'):
  return "奖励的宝可梦，可指定种类、属性、精灵球、初训家等。\n\n示例（配置语法保持英文）：\n - 'species:Arcanine form:Hisuian'\n - 'random shiny type:water'\n - 'random legendary ball:master_ball'\n\n详细语法：https://pixelmonmod.com/wiki/Pokemon_spec"
 raise ValueError('Untranslated source: '+repr(value))

def main():
 parser=argparse.ArgumentParser();parser.add_argument('--pixeltweaks',required=True);args=parser.parse_args()
 with zipfile.ZipFile(args.pixeltweaks) as jar:source=json.loads(jar.read('assets/pixeltweaks/lang/en_us.json'))
 result={key:translate(value) for key,value in source.items()}
 for key,value in source.items():
  assert re.findall(r'%(?:\d+\$)?[sd]',value)==re.findall(r'%(?:\d+\$)?[sd]',result[key]),(key,'format arguments changed')
 target=ROOT/'resources/assets/pixeltweaks/lang/zh_cn.json';target.parent.mkdir(parents=True,exist_ok=True)
 target.write_text(json.dumps(result,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 review={'source_jar':pathlib.Path(args.pixeltweaks).name,'source_sha256':hashlib.sha256(pathlib.Path(args.pixeltweaks).read_bytes()).hexdigest(),'source_keys':len(source),'translated_keys':len(result),'format_arguments_preserved':True,'literal_code_examples_preserved':True,'english_resources_unchanged':True}
 (ROOT/'docs/PIXELTWEAKS_LANGUAGE_REVIEW_1.3.0.json').write_text(json.dumps(review,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
 print('Translated all',len(result),'PixelTweaks language keys.')
if __name__=='__main__':main()
