"""Translations for serialized custom NPC text, which bypasses stock locale resources."""
import re
import nbtlib as n
NAMES={'Mr. Barley':'巴利先生','Daycare Man':'寄放屋先生','Daycare Lady':'寄放屋女士','TCG Trader':'集换卡商人','Geoffrey Twirp':'杰弗里·特威普'}
NAMES.update({
 'Pirate Captain':'海盗船长','Caleb':'凯莱布','Jeremy':'杰里米','Jessie':'杰西','Max':'马克斯','Terry':'特里',
 'Santa':'圣诞老人','Witch':'女巫','Tower of Darkness Master':'恶之塔师傅','Tower of Waters Master':'水之塔师傅',
})
TEXTS={
 'Feel free to use the Smeltery from the shack here.':'可以使用这里工坊的冶炼设备。',
 "But make sure to ONLY give it metal ores! It's a Blast Furnace, not an oven!":'记得只放入金属矿石！这是高炉，可不是烤箱！',
 "I'm not going to climb in and unclog it for you!":'要是堵住了，我可不会钻进去替你疏通！',
 "Ah, it's good to see you! Have you gotten a chance to use the Daycare Blocks yet?":'见到你真高兴！你试过寄放屋方块了吗？',
 'You may have noticed that your Pokemon may occasionally request items when in a the daycare.':'你可能已经发现，寄放屋里的宝可梦偶尔会提出物品需求。',
 "In order for them to feel more comfortable, I'd recommend gathering the items to fulfill these requests.":'为了让它们住得更舒服，建议收集所需物品，满足这些需求。',
 'Once you have them in your inventory, just press the green tick. If you get all of them, you may eventually get yourself an egg!':'物品放进背包后，按下绿色勾号即可。满足全部需求后，就有机会获得宝可梦蛋！',
 'Have you ever used a daycare before, dearie? Once you have one, you can just right click it to see a menu.':'你用过寄放屋吗？放好寄放屋方块后，右键就能打开菜单。',
 'Pop two of your Pokemon into the slots, and then press the green tick!':'把两只宝可梦放进槽位，再按下绿色勾号！',
 'If your Pokemon have alternate forms, like from Galar or Alola, use the yellow arrows to select them.':'如果宝可梦有伽勒尔、阿罗拉等不同形态，可以用黄色箭头选择。',
 'You can have up to 3 ranches in operation at a time, and you can check on them from the tab on top of your inventory!':'最多可以同时使用三个牧场，在背包上方的标签页查看它们！',
 'Sometimes I have to hide for weeks in the forest just to spot one Noctowl.':'有时我得在森林里躲上好几周，才能观察到一只猫头夜鹰。',
 'It gets a bit uncomfortable, sleeping on the ground. Oh well, someone has to do it!':'睡在地上确实不太舒服。不过，总得有人来做这件事！',
 'Aaaarrrrgggghhhh!':'啊——呀——！',
 'This be the day you shall always remember as the day you almost caught Captain Ruford Tauros.':'记住今天吧！因为今天，你差一点就抓住了鲁福德·肯泰罗船长！',
 "A Pokemon's Nature can have a big effect on that Pokemon's performance, but there are 25 different types.":'宝可梦的性格会显著影响它的表现，性格共有25种。',
 'Finding the right Nature can be difficult, but can prove to be useful in turning the tide of a battle in your favor.':'找到合适的性格并不容易，但它能帮助你扭转战局。',
 'Just like us, Pokemon can have a unique Nature that makes them different than another of the same species.':'就像我们一样，宝可梦也有独特的性格，同一种宝可梦也会因此有所不同。',
 'These Natures affect your Pokemon by increasing one of their stats, but lowering another. ':'这些性格通常会提高一项能力，同时降低另一项能力。',
 'Do you know about Pokemon Individual Values? The higher the IV value, the higher that stat will be for the Pokemon!':'你知道宝可梦的个体值吗？个体值越高，对应的能力也就越高！',
 'I always try to get my Pokemon to have as high an IV as possible to make my Pokemon even stronger! ':'我总会尽量寻找个体值高的宝可梦，让它们变得更强！',
 "I heard the best way to increase the IV's of your Pokemon is breeding. The parent Pokemon can pass their IV stats onto the egg!":'听说想获得高个体值宝可梦，培育是个好办法。亲代可以把自己的个体值遗传给宝可梦蛋！',
 "There's also an item you can have your pokemon hold while breeding that can increase the amount of IV's passed through genetics!":'培育时让宝可梦携带某些道具，还能增加遗传给后代的个体值数量！',
 "When pokemon gain experience after a battle, they'll also gain some Effort Values in their stats.":'宝可梦在战斗后获得经验时，也会获得相应的努力值。',
 "The Effort Values they gain will depend on the type of Pokemon they defeated. They can only gain so many, so plan carefully!":'获得哪项努力值取决于击败的宝可梦。努力值有上限，培养前记得做好规划！',
 'Want a tip? If you have a Pokemon hold a Power item, like a Power Band, it increases a specific EV they receive from battle!':'给你个建议：让宝可梦携带力量道具，例如力量束带，就能增加战斗中获得的特定努力值！',
 "It will increase an EV even if the defeated Pokemon doesn't normally provide it, it's really useful for training your Pokemon!":'即使击败的宝可梦通常不提供那项努力值，也能获得额外加成。这对培养宝可梦很有帮助！',
 'Welcome to the Tower of Darkness.':'欢迎来到恶之塔。',
 'At the top of this tower you will find the covetted Scroll of Darkness..':'在塔顶，你会找到令人向往的恶之挂轴。',
 'This scroll can only be found in this tower and is used to evolve Kubufu into a Single Strike Style Urishufu.':'这份挂轴只能在这座塔中找到，能让熊徒弟进化为一击流武道熊师。',
 'Welcome to the Tower of Waters.':'欢迎来到水之塔。',
 'At the top of this tower you will find the covetted Scroll of Waters.':'在塔顶，你会找到令人向往的水之挂轴。',
 'This scroll can only be found in this tower and is used to evolve Kubufu into a Rapid Strike Style Urishufu.':'这份挂轴只能在这座塔中找到，能让熊徒弟进化为连击流武道熊师。',
 'Good luck in getting to the top!':'祝你顺利登上塔顶！',
 "Don't pay attention to the mess, we're working hard here!":'别介意这里有些乱，我们正在努力赶工呢！',
 "Hi there trainer. It seems you've found my workshop.":'你好，训练家。看来你找到我的工坊了。',
 'Ho Ho Ho!':'呵呵呵！',
 'MERRY CHRISTMAS from the Pixelmon Team!':'宝可梦重铸团队祝你圣诞快乐！',
 'Happy Hallow... Oh wait, wrong holiday...':'万圣节快……等等，说错节日了……',
 'Double, double toil and trouble; Fire burn and caldron bubble.':'忙忙碌碌，烦恼加倍；炉火熊熊，锅中翻沸。',
 'One Scorebunny foot, three Fearow feathers and a pinch of crushed Water gem dust!':'一只炎兔儿的脚、三根大嘴雀的羽毛，再加一小撮磨碎的水之宝石粉末！',
 "That'll get rid of those pesky trainers that keep eating my house!":'这样就能赶走那些老是偷吃我房子的讨厌训练家了！',
 'It appears the wedding party is fashionably late.':'看来参加婚礼的宾客们姗姗来迟了。',
 'I do hope they enjoy their trip here, carried by our flying Pok��mon.':'希望他们乘坐我们的飞行宝可梦前来，一路都能玩得开心。',
 'I do hope they enjoy their trip here, carried by our flying Pokémon.':'希望他们乘坐我们的飞行宝可梦前来，一路都能玩得开心。',
}
def translate(data):
 count=0
 for entry in data.get('entities',[]):
  entity=entry.get('nbt',{})
  if str(entity.get('id',''))!='pixelmon:npc_chatting':continue
  for key,value in list(entity.items()):
   text=str(value)
   if key=='Name' and text in NAMES:entity[key]=n.String(NAMES[text]);count+=1
   elif re.fullmatch(r'chat\d+',key) and text in TEXTS:entity[key]=n.String(TEXTS[text]);count+=1
 return count
