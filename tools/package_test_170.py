"""Package only the reviewed 1.7.0 addon and tester-facing documentation."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
from hashlib import sha256
from io import BytesIO
import json
import struct
import os

root = Path(__file__).resolve().parents[1]
dist = root / 'dist'
core = dist / 'pixelmon-content-backport-1.16.5-1.7.0.jar'
data = core.read_bytes()
digest = sha256(data).hexdigest()
assert (core.with_suffix('.jar.sha256')).read_text().split()[0] == digest
with ZipFile(BytesIO(data)) as jar:
    assert jar.testzip() is None
    names = jar.namelist()
    assert len(names) == len(set(names))
    assert 'version="1.7.0"' in jar.read('META-INF/mods.toml').decode()
    classes = [n for n in names if n.endswith('.class')]
    assert classes and all(struct.unpack('>H', jar.read(n)[6:8])[0] == 55 for n in classes)
    config = json.loads(jar.read('pixelmon_backport.mixins.json'))
    for group in ('mixins', 'client', 'server'):
        mixins = config.get(group, [])
        assert len(mixins) == len(set(mixins))
        for name in mixins:
            assert config['package'].replace('.', '/') + '/' + name + '.class' in names
    for path in names:
        if path.endswith('.json'):
            json.loads(jar.read(path))
    assert not any(n.startswith(('saves/', 'logs/', 'resourcepacks/')) for n in names)
    assert 'local/pixelmon/backport/AuthorBossTeams.class' in names
    assert not any('PCSync170Cases' in n or 'AuthorBossTestMap' in n for n in names)

readme = '''Pixelmon Content Backport 1.7.0 离线测试包

适用：Minecraft 1.16.5 / Forge 36.2.42 / Java 11 / 原有 Pixelmon 9.1.12～9.1.13 环境。
这是回迁附加模组测试包，需要原有重铸本体和依赖，非完整客户端。

安装：
1. 退出游戏，保留原存档备份。
2. 从 mods 中移走旧版 pixelmon-content-backport 核心，放入本包 mods 中的 1.7.0 JAR。
3. 客户端与服务端使用同一版本；单人游戏只需替换自己的核心。
4. 本次内容已集成到核心，不需要独立资源包。

主要内容：
合入 1.6.2 战斗背包崩溃修复；PC 盒子同步与补同步队列；NPC“球果危机”库存检查；
老渔夫名称、新版成就精灵球图标、起源球/公园球/智皮卡 Z 文案；
四个金属膜 2×2 合成一个复合金属，默认奖励箱不再掉落；
主菜单两条定制标语、引导线奖励箱/Boss 列表中文。

新增作者宝可梦道馆与78项奖池，取消PVP道馆；独立地图见 boss-test-map 包。
全部制造、烧炼、拾取、采摘任务改为拥有足量即可，旧任务生效，实际交付仍按原规则扣除。
优化了实力相当npc战斗逻辑。
四种花蜜添加蜂蜜十字配方并移出大师球奖励箱；修复道馆定位、搜索取消和巢穴加载期间的实体插入问题。
兰紫色花蜜中心改为究极沙漠仙人掌；新增甜甜/酸酸/蜜汁苹果、香袋、泡沫奶油、联系绳和索财灵硬币配方。
蜜汁苹果和硬币从宝可梦奖励箱移除；碧草面具改为丛林神殿20%获取，另外三种面具在对应探索箱保持7%，获取说明同步。
智皮卡禁止场外捕捉；战斗中喜欢的球固定2倍，其他可用球按原生有效倍率乘0.5，大师球、起源球、公园球拒绝丢球；仅生成和进战斗提示，球名绿色。
修复黄昏之鬃、拂晓之翼携带奈克洛兹玛Z的场外究极爆发，按J打开轮盘操作。
JEI新增极巨/太晶巢穴、建筑与奖励箱查询，物品R反查来源，箱子/许愿星块/太晶巢穴生成器U查看奖池。
补齐厄诡椪太晶形态编辑器名称；修复16色培育屋上半部分无法选中，旧方块直接生效。
完整离线构建与回归通过；独立客户端完成捕捉、存档、成就、末地城市自然生成/实际加载/指南针及紫颂清理专项。
末地超能城市恢复完整街区，自然生成实测17座住宅、9段道路；旧区块的小城市不会自动扩大。
完整游玩与城市实际摆放观感仍需人工核对。
请优先测试迁移存档的 PC、新捕获个体进入 PC、七色球果与原生 NPC 后续对话。
新增宝可梦草丛配方；修正星念徽章末地说明、七色球果种植音效。
取消宝可梦机器和家具成品换色；各色PC可直接用对应染色玻璃板制作。
Comforts补齐汉化；溺水入口任务全年开放；史诗/传说/终极/溺水Boss改用传说对战音乐。
道馆馆主（含作者道馆）及首领对战唱片使用用户提供的剑盾馆主音乐，原道馆唱片保持原曲。
野生战斗及野生唱片使用最新野生对战bgm；极巨巢穴唱片使用极巨巢穴音频，普通极巨团战也使用极巨巢穴音频，太晶团战单独配乐。
JEI“植物巨碑”来源改称究极发电厂黑曜石塔。
新增太晶巢穴唱片与战斗音乐，五段用户音频统一响度；现有两种星块兼容太晶巢穴。
中心/商店补隐形音乐方块；新增装备和加工配方，Boss掉落JEI及领取音效。
溺水Boss野外生成、队伍最高等级+60、出场永久蚀，治疗仪、治疗指令和宝可梦手机可清除；奖励规则见追加说明。
地图自然雨不影响战斗；馆主与作者个体值全31、合法满努力值、定位性格。
基础忍蛙取消水系增益和额外行动，可选“我的天哪忍蛙大人”恢复这两项；不默认启用。
普通皮卡丘Z不再激活戴帽皮卡丘Z按钮，爆裂场地球倍率加2。
新增小智版Mega石，每位玩家首次作者胜利必定额外掉落，之后1%，溺水Boss不掉落；普通忍蛙石拒绝智蛙与羁绊忍蛙。
千万伏特威力50/PP10，雷之誓约包含当回合持续五回合，换下保留，黄色圆形图标，与蚀等共存。
修复智皮卡海洋/水下生成和概率误显示，击败掉落为50%大师球/50%GS球二选一，保留智皮卡Z。
太乐巴戈斯普通/太晶/星晶场外切换、携面具厄诡椪场外太晶；修复进场HP错误同步。
作者战新增参战限制、常驻蚀与命中/成功率削弱，侧边显示同步；普通道馆不受影响。
蚀在概要及同行栏头像显示，支持治疗指令、治疗仪和宝可梦手机清除。
新增奶桶+任意树果+碗的树果汁配方并返还桶；1星极巨/太晶新增高权重双矿石。
四种替换唱片按游戏来源更新副标题，删除太晶唱片灰色获取说明；中心音乐范围缩小、离开淡出缩短，旧方块自动适配。
新增终极Boss（原生ultimate）最高等级+50，无主动蚀；传说奖池每组双倍抽取，保留对应Mega石规则。
新增七级Boss击败成就，刷新权重和两类抽取池见“10月23日追加说明.md”。
修复溺水领奖包空显示名引起的异常；保留作者随机奖池，蚀支持治疗仪、治疗指令和宝可梦手机清除。
修复太乐巴戈斯只认露天地面的规则，山地夜间Y≤40洞窟可生成。
普通野生对战两首音乐各50%，每场固定；其一名称补齐“音乐”，其二使用新生成的蓝金色重铸风格光盘，两张均无获取说明。
声音选项页新增“宝可梦音乐”独立滑条，统一控制重铸的对战、中心/商店音乐方块和唱片；本机保存，主音量仍生效。
删除透明玻璃板红色PC配方与全部PC说明；培育屋改购买说明，草丛改简洁功能说明。
烟熏炉究极深海海带改15秒，熔炉仍30秒；星块仅留第一行说明。
太晶生成器补原生同款放置声，投入两种星块成功时使用原生许愿声。
移除16色交换机及果木板附加说明/Shift提示。
修复雷之誓约非标准克制倍率被属性克制模组画成灰色；生效期间电系招式显示绿色。
馆主/作者只剩最后一只时优先用有限活力块复活王牌，残血或即将被击倒也会尝试，仍占正常用药回合。
新增基格尔德群聚变形/完全体/Mega成就及作者累计十胜挑战“大败而归”。
新增神兽无性别（智皮卡例外），灵幽马夜间沼泽权重与古简蜗相同为3。
孤忍自修下一回合招式优先度+1；作者道馆五份随机奖励，补齐备战设施和林地府邸奖励箱。
修复草面具厄诡椪探索期间重建AI引起崩溃、TrashSlot单机服务端识别和作者侧栏百分号错误。
详细步骤见“更新说明.md”。
'''
summary = (root / 'docs/UPDATE_1.7.0_SUMMARY.md').read_text(encoding='utf-8')
summary = summary.replace('详细需求及作者 Boss 的暂缓记录见 [1.7.0 工作清单](UPDATE_1.7.0_PLAN_2026-10-20.md)。',
                          '作者 Boss 详细规则及最新任务调整见工作清单；测试地图另包提供。')
entries = {
    'mods/' + core.name: data,
    'mods/' + core.name + '.sha256': (digest + '  ' + core.name + '\n').encode(),
    '安装与测试.txt': readme.encode('utf-8-sig'),
    '更新说明.md': summary.encode('utf-8'),
    '10月23日追加说明.md': (root / 'docs/FOLLOWUP_1.7.0_2026-10-23.md').read_bytes(),
    '可选模组/ohmygod-greninja-1.16.5-1.0.0.jar': (dist / 'ohmygod-greninja-1.16.5-1.0.0.jar').read_bytes(),
    '可选模组/安装说明.md': (root / 'addons/ohmygod-greninja/README.md').read_bytes(),
    '0号排查记录.md': (root / 'docs/ZERO_POKEMON_2026-10-20.md').read_bytes(),
}
archive = dist / ('Pixelmon-Backport-1.7.0-test' + os.environ.get('BACKPORT_PACKAGE_SUFFIX', '') + '.zip')
with ZipFile(archive, 'w', ZIP_DEFLATED, compresslevel=6) as bundle:
    for name, content in entries.items():
        bundle.writestr(name, content)
with ZipFile(archive) as bundle:
    assert bundle.testzip() is None
    assert set(bundle.namelist()) == set(entries)
    assert sha256(bundle.read('mods/' + core.name)).hexdigest() == digest
archive_hash = sha256(archive.read_bytes()).hexdigest()
archive.with_suffix('.zip.sha256').write_text(archive_hash + '  ' + archive.name + '\n', encoding='ascii')
print(json.dumps({'core': str(core), 'core_sha256': digest, 'java11_classes': len(classes),
                  'package': str(archive), 'package_sha256': archive_hash,
                  'entries': len(entries)}, ensure_ascii=False, indent=2))
