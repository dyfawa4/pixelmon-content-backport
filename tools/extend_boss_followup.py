"""Generate reviewed 2026-10-23 Boss achievements and final music labels."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]; RES=ROOT/'resources'; NS='pixelmon_backport'
def write(path,data):
 path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf-8')
def apply():
 rows=[('common','普通','初遇强敌','task','poke_ball'),('uncommon','优秀','强手迭出','task','great_ball'),('rare','罕见','劲敌频现','task','ultra_ball'),('epic','史诗','接踵而来','goal','gs_ball'),('legendary','传说','强敌踵至','goal','master_ball'),('ultimate','终极','愈演愈烈','goal','park_ball'),('drowned','溺水','宿命之战','challenge','origin_ball')]
 for lang in ('zh_cn','en_us'):
  changes={}
  for tier,name,title,frame,ball in rows:
   key=f'advancement.{NS}.defeat_boss_{tier}'
   changes[key]=title if lang=='zh_cn' else f'Defeat a {tier.title()} Boss'
   changes[key+'.desc']=f'击败了{name}等级的boss' if lang=='zh_cn' else f'Defeat a {tier.title()} tier Boss.'
   if lang=='zh_cn':changes['enum.trainerBoss.'+tier]=name
   write(RES/f'data/{NS}/advancements/adventure/defeat_boss_{tier}.json',{'parent':'minecraft:adventure/root','display':{'icon':{'item':'pixelmon:poke_ball','nbt':'{PokeBallID:"'+ball+'"}'},'title':{'translate':key},'description':{'translate':key+'.desc'},'frame':frame,'show_toast':True,'announce_to_chat':True,'hidden':False},'criteria':{'earned':{'trigger':'minecraft:impossible'}}})
  changes.update({'item.pixelmon.wild_music_disc.desc':"宝可梦 Let's Go! 皮卡丘／伊布-野生宝可梦对战音乐（其一）" if lang=='zh_cn' else "Pokemon Let's Go! Pikachu / Eevee - Wild Battle Music (Part 1)",f'item.{NS}.wild_music_disc_two':'音乐唱片' if lang=='zh_cn' else 'Music Disc',f'item.{NS}.wild_music_disc_two.desc':"宝可梦 Let's Go! 皮卡丘／伊布-野生宝可梦对战音乐（其二）" if lang=='zh_cn' else "Pokemon Let's Go! Pikachu / Eevee - Wild Battle Music (Part 2)"})
  changes['attack.ninja_training.description']='大幅提高自身特攻，并使下一回合招式优先度提高1级。' if lang=='zh_cn' else 'Sharply raises the user’s Sp. Atk and raises its move priority by 1 for the next turn.'
  changes.update({'item.pixelmon.boss_music_disc.desc':'宝可梦剑盾-道馆馆主对战' if lang=='zh_cn' else 'Pokemon Sword and Shield - Gym Leader Battle','item.pixelmon.ultraspace_music_disc.desc':'究极空间/溺水世界' if lang=='zh_cn' else 'Ultra Space / Drowned World'})
  changes['pixelmon_backport.options.battle_music']='宝可梦音乐：%s%%' if lang=='zh_cn' else 'Pokemon Music: %s%%'
  changes.update({'pixelmon.generic.form.terastal':'太晶形态' if lang=='zh_cn' else 'Terastal Form','pixelmon.generic.form.stellar':'星晶形态' if lang=='zh_cn' else 'Stellar Form'})
  changes.update({'pixelmon_backport.field_wheel.form_cycle':'切换形态' if lang=='zh_cn' else 'Change Form','pixelmon_backport.field_wheel.tera':'太晶化' if lang=='zh_cn' else 'Terastallize','pixelmon_backport.field_mega.mask':'需要携带面具' if lang=='zh_cn' else 'A held mask is required','pixelmon_backport.field_mega.form_started':'开始变换形态' if lang=='zh_cn' else 'Changing form'})
  changes.update({'pixelmon.backport.author.protect_rate':'成功率%s%%' if lang=='zh_cn' else 'Success: %s%%','pixelmon.backport.author.buff_rate':'强化率×80%%' if lang=='zh_cn' else 'Buff: base chance x80%%','pixelmon.backport.author.challenge.handicap':'作者挑战：上场宝可梦附加常驻蚀。\n命中率降低20个百分点；守住和自身强化成功率各为原概率的80%，不重复判定。\nZ招式不受命中与成功率削弱。' if lang=='zh_cn' else 'Author challenge: sent-out Pokemon receive persistent Erosion.\nAccuracy is reduced by 20 percentage points. Protect and self buffs instead retain 80% of their original success chance.\nZ-Moves are exempt from these accuracy and success penalties.'})
  for slug,title,description,en in [('zygarde_power_construct','群聚变形','在对战中实际触发基格尔德的群聚变形特性。','Power Construct'),('zygarde_complete','闪亮登场','让基格尔德变身为完全体形态。','A Brilliant Entrance'),('zygarde_mega','最终兵器','让基格尔德进行Mega进化。','The Ultimate Weapon')]:
   key=f'advancement.{NS}.{slug}';changes[key]=title if lang=='zh_cn' else en;changes[key+'.desc']=description if lang=='zh_cn' else {'zygarde_power_construct':"Activate Zygarde's Power Construct in battle.",'zygarde_complete':'Transform Zygarde into its Complete Forme.','zygarde_mega':'Mega Evolve Zygarde.'}[slug]
  changes[f'advancement.{NS}.defeat_author_10']='大败而归' if lang=='zh_cn' else 'Tenfold Defeat'
  changes[f'advancement.{NS}.defeat_author_10.desc']='累计击败作者10次。' if lang=='zh_cn' else 'Defeat the author ten times.'
  for path in (RES/f'assets/{NS}/lang/{lang}.json',RES/f'backport/description_fixes_{lang}.json'):
   data=json.loads(path.read_text('utf-8'));data.update(changes);write(path,data)
 p=RES/'backport/item_help.json';help=json.loads(p.read_text('utf-8'))
 for key in list(help):
  if key in ('pixelmon:wild_music_disc',NS+':wild_music_disc_two','pixelmon:wishing_piece','pixelmon:waterdude_wishing_piece') or (key.startswith('pixelmon:') and key.endswith(('_pc','_trade_machine','_berry_wood_planks'))):help.pop(key)
 write(p,help)
 for color in 'white orange magenta light_blue yellow lime pink gray light_gray cyan purple blue brown green red black'.split():help['pixelmon:'+color+'_day_care']=['获取：可以在村庄房屋店员处使用宝可币购买获得']
 help['pixelmon:pixelmon_grass']=['在不同群系进入其中时，会有不同的宝可梦出现，有较长冷却时间。']
 write(p,help)
 write(RES/f'assets/{NS}/models/item/wild_music_disc_two.json',{'parent':'pixelmon:item/pixelmon_item','textures':{'layer0':'pixelmon_backport:item/wild_music_disc_two'}})
 write(RES/'data/minecraft/tags/items/music_discs.json',{'replace':False,'values':[NS+':wild_music_disc_two']})
 write(RES/f'data/{NS}/recipes/berry_juice.json',{'type':'minecraft:crafting_shaped','pattern':['M','B','W'],'key':{'M':{'item':'minecraft:milk_bucket'},'B':{'tag':'pixelmon:berries'},'W':{'item':'minecraft:bowl'}},'result':{'item':'pixelmon:berry_juice','count':1}})
 p=RES/'backport/tera_raids.json';raid=json.loads(p.read_text('utf-8'))
 berries=sum(row['weight'] for row in raid['rewards'] if row['min']<=1<=row['max'] and row['item'].endswith('_berry'))
 for ore in ('pixelmon:dynite_ore','pixelmon:armorite_ore'):
  raid['rewards']=[row for row in raid['rewards'] if not(row['item']==ore and row['min']==row['max']==1)]
  raid['rewards'].append({'item':ore,'weight':berries,'min':1,'max':1})
 write(p,raid)
 for slug,form in [('zygarde_power_construct','fifty_percent'),('zygarde_complete','complete'),('zygarde_mega','mega')]:
  key=f'advancement.{NS}.{slug}'
  write(RES/f'data/{NS}/advancements/legends/{slug}.json',{'parent':f'{NS}:legends/forms','display':{'icon':{'item':'pixelmon:pixelmon_sprite','nbt':'{ndex:718s,palette:"none",form:"'+form+'",gender:0b}'},'title':{'translate':key},'description':{'translate':key+'.desc'},'frame':'goal' if slug!='zygarde_mega' else 'challenge','show_toast':True,'announce_to_chat':True,'hidden':True},'criteria':{'earned':{'trigger':'minecraft:impossible'}}})
 a=json.loads((RES/f'data/{NS}/advancements/adventure/defeat_author.json').read_text('utf8'))
 a['parent']=f'{NS}:adventure/defeat_author'
 a['display']['title']={'translate':f'advancement.{NS}.defeat_author_10'}
 a['display']['description']={'translate':f'advancement.{NS}.defeat_author_10.desc'}
 write(RES/f'data/{NS}/advancements/adventure/defeat_author_10.json',a)
if __name__=='__main__':apply()
