"""Translations for serialized custom NPC text, which bypasses stock locale resources."""
import re
import nbtlib as n
NAMES={'Mr. Barley':'巴利先生','Daycare Man':'寄放屋先生','Daycare Lady':'寄放屋女士','TCG Trader':'集换卡商人','Geoffrey Twirp':'杰弗里·特威普'}
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
