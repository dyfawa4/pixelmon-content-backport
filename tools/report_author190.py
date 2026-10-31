"""Archive native obstruction, terrain decoration, furnishing migration and container screenshots."""
from pathlib import Path
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1]
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
fixed=ROOT/'.cache/author190-fixed-r4';natural=ROOT/'.cache/author190-natural-r5';baseline=ROOT/'.cache/author190-baseline-r1'
sha=digest(fixed/'mods/backport-dev.jar');assert digest(natural/'mods/backport-dev.jar')==sha
log=(fixed/'bossbalance190-result.txt').read_text();assert 'AUTHOR190_PASS' in log and 'AUTHOR190_CONTAINER_PASS diamond' in log and 'AUTHOR190_CONTAINER_PASS netherite_scrap' in log and '_FAIL' not in log
assert 'AUTHOR190_NATURAL_PASS' in (natural/'bossbalance190-result.txt').read_text()
current=read(fixed/'author190-results.json');old=read(baseline/'author190-results.json');real=read(natural/'author190-natural-results.json')
assert old['baseline_generated'] is False and current['generated'] is True
assert current['container_preserved'] and current['compass_matches'] and current['migration_once']
assert sum(current['prize_samples'].values())==64 and len(current['prize_samples'])==2
assert set(current['quantity_samples'])=={'1','2','3'} and len(current['slot_samples'])>4
assert real['biome']=='minecraft:forest' and real['generator']=='NoiseChunkGenerator'
out=ROOT/'docs/previews';out.mkdir(exist_ok=True)
images={}
for name in ['author-room-190.png','author-table-diamond-190.png','author-table-netherite-190.png']:
    source=fixed/'screenshots'/name;assert source.is_file();shutil.copy2(source,out/name);images[name]=digest(source)
evidence={'date':'2026-10-27','core_sha256':sha,'baseline_sha256':digest(baseline/'mods/backport-dev.jar'),
          'controlled':current,'baseline':old,'natural':real,'images_sha256':images,
          'furniture':{'chair':'pixelmon:orange_chair facing NORTH toward table/workplace','table':'pixelmon:end_table','workplace':'pixelmon:lightworkplace','migration':'AuthorArenaFurnished5 repairs version4 SOUTH chair without resetting loot marker','reward':'one chosen material, 1..3 items, one random native inventory slot'},
          'limits':['Controlled terrain uses real world blocks and the production feature, not an untouched natural scene.',
                    'Natural proof uses a real noise generator with forest biome fixed; not exhaustive biome/dimension density statistics.',
                    'The initial flat-world attempts are not accepted as normal-noise proof; r4 explicitly verifies biome and generator.',
                    'The first visual interpretation faced the chair toward the room center; the user corrected it, and version5 now faces the workplace.',
                    'NPC moved aside only for private visual inspection; production NPC positioning is unchanged.',
                    'Two container screenshots choose one exact item per visual fixture after independently checking 64 random single-type 1..3-count creations; never two simultaneous rewards.']}
(ROOT/'docs/data/AUTHOR_GENERATION_1.9.0.json').write_text(json.dumps(evidence,ensure_ascii=False,indent=2),'utf-8')
lines=['# 1.9.0 作者道馆生成与家具','',f'核心SHA256：`{sha}`。','',
       '作者建筑在自然装饰之后落地；候选点保留海洋／海滩减半、其他主题双候选规则。取消整区块结构引用的一刀切排除，改为实际占位及门口检查；容器与负硬度方块保留。采用去除树干／树叶等装饰的地面中位高度，取消局部高差超过3格就放弃的条件；13×13室内及入口自然障碍可清理，写入仍在本区块。既有作者建筑的位置与索引不重选。','',
       '原生对照：同一受控世界候选中心树干高度118，地面100并有11格局部石坡。旧核心拒绝，新核心在100层完整放置、清理室内石坡和入口树干，容器占位则在写入前拒绝。实际NPC之家、持久索引和指南针返回位置一致。','',
       f'自然流程另在固定森林生物群系的原生NoiseChunkGenerator中加载新候选区块，未直接调用生成函数或铺人工地面，实际生成作者建筑与训练师，位置({real["x"]}, {real["y"]}, {real["z"]})，索引、房屋完整性和茶几奖励通过。此证明原生区块装饰调用可达，不当所有地形建筑数比例的统计保证。','',
       '## 家具与奖励','',
       '按用户最新纠正，椅子面向茶几上的工作桌，椅背朝房间中央；新椅子NORTH，工作桌SOUTH。Furnished5迁移也修正已处理过的Furnished4旧椅子，保留角色记录和容器数据。之前朝房间中央的画面已废弃，不能当成此次朝向验收。','',
       '茶几为原生pixelmon:end_table，上方放pixelmon:lightworkplace（浅色工作桌）。茶几选择钻石或下界合金碎片一种，等概率二选一；数量随机1–3个，放在随机库存格。原生64个新茶几样本：'+str(current['prize_samples'])+'；数量样本'+str(current['quantity_samples'])+'，出现'+str(len(current['slot_samples']))+'个不同格位。领取后、重复布置、原生NBT保存重载及家具版本迁移均不补发；旧茶几已有玩家物品时保留物品并标记迁移，不在以后清空时补奖。','',
       '原生茶几方块交互能够在上方有工作桌时打开容器。两张物品界面使用分别选择的单种材料视觉夹具（3钻石与2碎片位于不同格）；随机生成与防重已另外检查，不代表同一张茶几同时有两种奖励。NPC仅为拍摄移开，生产站位不改。','',
       '![椅子面向工作桌](previews/author-room-190.png)','',
       '![钻石茶几原生界面](previews/author-table-diamond-190.png)','',
       '![下界合金碎片茶几原生界面](previews/author-table-netherite-190.png)','',
       '完整证据：[JSON](data/AUTHOR_GENERATION_1.9.0.json)。私有实例author190-baseline-r1、author190-fixed-r4、author190-natural-r5。自然森林采用真实噪声地形但固定生物群系，未穷举海洋、下界、究极空间等全部世界种子；既有密度纯检查96区域／13类陆地和20万海洋旧点保留100285通过，规则class保持不变。']
(ROOT/'docs/AUTHOR_GENERATION_1.9.0.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'core_sha256':sha,'natural':real,'prizes':current['prize_samples'],'images':len(images)}))
