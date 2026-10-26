"""Idempotent resource updates for the 2026-10-24 reported omissions."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
def update(path,callback):
    file=ROOT/path;data=json.loads(file.read_text('utf-8-sig'));callback(data);file.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n','utf8')
def rewards(data):
    for id,name in [('pixelmon:red_chain','红色锁链'),('sophisticatedbackpacks:netherite_backpack','下界合金背包'),('pixelmon:soul_dew','心之水滴')]:
        if not any(row.get('item')==id for row in data['entries']):data['entries'].append({'item':id,'count':1,'display_zh':name})
update(Path('resources/backport/author_boss_rewards.json'),rewards)
def mixins(data):
    if 'NpcTextureEditorMixin' not in data['client']:data['client'].append('NpcTextureEditorMixin')
    if 'RidingPassengerSafetyMixin' not in data['mixins']:data['mixins'].append('RidingPassengerSafetyMixin')
    if 'RidingStandardInputMixin' not in data['mixins']:data['mixins'].append('RidingStandardInputMixin')
    for name in ['FishingRodEnchantabilityMixin','FishingEnchantmentTypeMixin','FishingLuckRarityMixin','StructureSpacingAccess']:
        if name not in data['mixins']:data['mixins'].append(name)
update(Path('resources/pixelmon_backport.mixins.json'),mixins)
roles={'joy':('乔伊','Joy'),'doctor':('医生','Doctor'),'nurse':('护士','Nurse'),'villager':('村民','Villager'),'fisherman':('钓鱼者','Fisherman'),'quest':('任务委托人','Quest Giver'),'trader':('交换员','Trader'),'relearner':('招式回忆师','Move Relearner'),'tutor':('招式教学者','Move Tutor')}
for language,index in [('zh_cn',0),('en_us',1)]:
    values={'pixelmon.backport.npc.'+key:names[index] for key,names in roles.items()}
    for name in ['max_mushroom','max_mushroom_block']:
        values['block.pixelmon.'+name]=('极巨菇菇','Max Mushrooms')[index]
        values['item.pixelmon.'+name]=('极巨菇菇','Max Mushrooms')[index]
    update(Path(f'resources/assets/pixelmon_backport/lang/{language}.json'),lambda data:data.update(values))
    update(Path(f'resources/backport/description_fixes_{language}.json'),lambda data:data.update(values))
print('Recorded three reward entries, editor fix, NPC roles and mushroom translation overrides')
