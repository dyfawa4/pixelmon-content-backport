"""Reviewed follow-up: exclusive Ash Mega stone and Lightning Oath descriptions/data."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1];RES=ROOT/'resources'
def write(path,data):path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf-8')
def apply():
 p=RES/'backport/cap_pikachu.json';data=json.loads(p.read_text('utf-8'))
 move=next(m for m in data['moves'] if m['attackName']=='Ten Million Volts');move.update(basePower=50,ppBase=10,ppMax=16,effects=[{'effectTypeID':'BackportLightningOath','persists':False,'modifiers':[]}]);write(p,data)
 write(RES/'assets/pixelmon_backport/models/item/ash_greninjite.json',{'parent':'minecraft:item/generated','textures':{'layer0':'pixelmon_backport:item/greninjite'}})
 p=RES/'backport/item_help.json';help=json.loads(p.read_text('utf-8'));help['pixelmon:pikashunium_z']=['获取：击败野生智皮卡必得1个；同时有50%概率获得1个大师球，另50%获得1个GS球。','也可使用4个普通皮卡丘Z无序合成1个。'];write(p,help)
 for lang,changes in {
  'zh_cn':{'item.pixelmon_backport.ash_greninjite':'小智版甲贺忍蛙进化石','pixelmon.backport.ash_mega.use':'仅未变身的普通甲贺忍蛙可用，包括羁绊变身个体。','pixelmon.backport.ash_mega.ability':'超进化后特性为降雨。','pixelmon.backport.ash_mega.source':'获取：击败作者后有1%概率额外获得；溺水Boss不掉落。','attack.ten_million_volts.description':'攻击并赋予雷之誓约，持续5回合。雷之誓约：消除电系抗性，赋予电系克制。','pixelmon.backport.oath.start':'%s受到了雷之誓约！','pixelmon.backport.oath.end':'%s的雷之誓约消失了！','pixelmon.backport.oath.name':'雷之誓约','pixelmon.backport.oath.description':'消除电系抗性，赋予电系克制。'},
  'en_us':{'item.pixelmon_backport.ash_greninjite':'Ash Greninjite','pixelmon.backport.ash_mega.use':'Only untransformed Greninja, including Battle Bond individuals, can use this stone.','pixelmon.backport.ash_mega.ability':'Mega Evolution grants Drizzle.','pixelmon.backport.ash_mega.source':'Source: independent 1% extra prize after defeating an author Gym Leader. Never dropped by Drowned Bosses.','attack.ten_million_volts.description':'Attacks and inflicts Lightning Oath for 5 turns. Lightning Oath removes Electric resistance and makes the target weak to Electric attacks.','pixelmon.backport.oath.start':'%s was afflicted with Lightning Oath!','pixelmon.backport.oath.end':"%s's Lightning Oath wore off!",'pixelmon.backport.oath.name':'Lightning Oath','pixelmon.backport.oath.description':'Removes Electric resistance and makes the target weak to Electric attacks.'}
 }.items():
  changes['pixelmon.backport.ash_mega.source']='获取：首次击败作者必得，之后有1%概率额外获得；溺水Boss不掉落。' if lang=='zh_cn' else 'Source: guaranteed extra prize on your first author victory, then 1% per victory. Never dropped by Drowned Bosses.'
  changes['pixelmon.backport.normal_greninja_mega.use']='仅普通、非羁绊变身特性的甲贺忍蛙可用；智蛙和羁绊忍蛙不能使用。' if lang=='zh_cn' else 'Only ordinary Greninja without Battle Bond can use this stone. Ash and Battle Bond Greninja cannot use it.'
  p=RES/f'assets/pixelmon_backport/lang/{lang}.json';data=json.loads(p.read_text('utf-8'));data.update(changes);write(p,data)
  p=RES/f'backport/description_fixes_{lang}.json';data=json.loads(p.read_text('utf-8'));data['attack.ten_million_volts.description']=changes['attack.ten_million_volts.description'];write(p,data)
 help['pixelmon_backport:greninjite'][0]='携带：仅普通、非羁绊变身特性的甲贺忍蛙可用；智蛙和羁绊忍蛙不能使用。训练家需要装备Mega手环。';write(RES/'backport/item_help.json',help)
if __name__=='__main__':apply()
