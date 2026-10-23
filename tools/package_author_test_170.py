"""Build the private, clean Boss/loot inspection map without starting Minecraft."""
import copy
import hashlib
import io
import json
import os
from pathlib import Path
import struct
import subprocess
from zipfile import ZipFile, ZIP_DEFLATED
import nbtlib as n

ROOT = Path(__file__).resolve().parents[1]
DIST = ROOT / 'dist'
FOLDER = 'backport-author-boss-170'


def main():
    build = json.loads((ROOT / '.cache/last-build.json').read_text(encoding='utf-8-sig'))
    work = ROOT / '.cache/boss-170'
    classes = work / 'map-helper'
    classes.mkdir(parents=True, exist_ok=True)
    javac = str(Path(build['java']).with_name('javac.exe'))
    subprocess.run([javac, '--release', '11', '-proc:none', '-encoding', 'UTF-8', '-cp',
                    build['classes'] + ';' + build['classpath'], '-d', str(classes),
                    str(ROOT / 'tests/AuthorBossTestMap.java')], check=True, cwd=ROOT)
    meta = '''modLoader="javafml"
loaderVersion="[36,)"
license="All Rights Reserved"
[[mods]]
modId="backport_author_test"
version="1.7.0"
displayName="神秘任务男与完整战利品测试地图"
description=''' + '"仅在指定独立测试存档内布置场景、发放测试用品。"' + '''
[[dependencies.backport_author_test]]
modId="pixelmon_backport"
mandatory=true
versionRange="[1.7.0,1.7.1)"
ordering="AFTER"
side="BOTH"
'''
    helper = io.BytesIO()
    with ZipFile(helper, 'w', ZIP_DEFLATED) as jar:
        jar.writestr('META-INF/mods.toml', meta.encode())
        jar.writestr('pack.mcmeta', json.dumps({'pack': {'pack_format': 6, 'description': 'Author Boss 1.7.0 private test map helper'}}).encode())
        for p in sorted(classes.glob('AuthorBossTestMap*.class')):
            data = p.read_bytes()
            assert struct.unpack('>H', data[6:8])[0] == 55
            jar.writestr(p.name, data)
    with ZipFile(io.BytesIO(helper.getvalue())) as jar:
        assert 'AuthorBossTestMap.class' in jar.namelist() and jar.testzip() is None
        assert json.loads(jar.read('pack.mcmeta'))['pack']['pack_format'] == 6

    world = copy.deepcopy(n.load(ROOT / 'test-maps/saves/backport-qa-shop/level.dat'))
    world.pop('fml', None)
    world.pop('forge', None)
    data = world['Data']
    data.pop('Player', None)
    data['LevelName'] = n.String('神秘任务男与完整战利品 1.7.0')
    data['GameType'] = n.Int(1)
    data['Difficulty'] = n.Byte(2)
    data['allowCommands'] = n.Byte(1)
    for k, v in [('SpawnX', 0), ('SpawnY', 65), ('SpawnZ', 34)]:
        data[k] = n.Int(v)
    for k in ('Time', 'DayTime'):
        data[k] = n.Long(6000)
    for k in ('raining', 'thundering'):
        data[k] = n.Byte(0)
    generation = data['WorldGenSettings']
    generation['seed'] = n.Long(17020261020)
    generation['generate_features'] = n.Byte(0)
    generation['bonus_chest'] = n.Byte(0)
    generator = generation['dimensions']['minecraft:overworld']['generator']
    generator.clear()
    generator.update({'type': n.String('minecraft:flat'), 'settings': n.Compound({
        'biome': n.String('minecraft:plains'),
        'layers': n.List[n.Compound]([n.Compound({'block': n.String('minecraft:' + block), 'height': n.Int(height)})
                                    for block, height in [('bedrock', 1), ('stone', 59), ('dirt', 3), ('grass_block', 1)]]),
        'lakes': n.Byte(0), 'features': n.Byte(0), 'structures': n.Compound({'structures': n.Compound()})})})
    rules = data.setdefault('GameRules', n.Compound())
    for k, v in [('doDaylightCycle', 'false'), ('doWeatherCycle', 'false'), ('doFireTick', 'false'),
                 ('randomTickSpeed', '0'), ('doMobSpawning', 'false'), ('keepInventory', 'true'), ('mobGriefing', 'false')]:
        rules[k] = n.String(v)
    level = work / 'author-map-level.dat'
    world.save(level, gzipped=True)
    check = n.load(level)
    assert 'Player' not in check['Data'] and check['Data']['GameType'] == 1
    catalog = json.loads((ROOT / 'resources/backport/author_boss_rewards.json').read_text(encoding='utf-8'))
    assert len(catalog['entries']) == 78
    listing = ['# 完整奖池核对', '', '每次固定大师球 ×1、500000～1000000 宝可梦币，另外不放回抽取下列五项。小智版甲贺忍蛙进化石每位玩家首次击败作者必定额外获得1个，此后独立1%，不占五项随机奖励；溺水Boss不得复制该额外石头。',
               '图示数量为每份数量；戴帽皮卡丘的智皮卡 Z 为携带物，不是额外必掉。', '',
               '|序号|截图-行-列|物品|ID / 数据|每份数量|', '|---|---|---|---|---|']
    for i, e in enumerate(catalog['entries'], 1):
        ident = 'pixelmon:poke_ball {PokeBallID:"' + e['ball'] + '"}' if 'ball' in e else e['item']
        if 'enchantment' in e:
            ident += ' StoredEnchantments:[minecraft:mending 1]'
        listing.append(f'|{i:02}|{e["image"]}-{e["row"]}-{e["column"]}|{e["display_zh"]}|`{ident}`|{e["count"]}|')
    instructions = """1.7.0 作者道馆 / 完整战利品 离线测试地图

需要原整合包Minecraft 1.16.5 / Forge 36.2.42 / Java 11 / Pixelmon。
1. 退出游戏，移走mods内旧回迁核心，放入本包mods中的两个JAR。客户端与服务端须同时更新核心。
2. 将saves/backport-author-boss-170整个文件夹复制到游戏saves，不覆盖个人存档。
3. 进入“神秘任务男与完整战利品 1.7.0”，第一次载入时测试助手自动布置场地。
4. 前方为全部78项按截图顺序排列的箱子，固定大师球单独陈列。附魔书为经验修补I，专属Z共17项。
5. 后方十四座房间对应作者的十四种队伍主题，作者名为神秘任务男，右键开始宝可梦挑战。PVP已取消。
6. 出生点测试台可恢复玩家状态、替换六只测试宝可梦、切创造核对、中止挑战治疗、补回已击败作者。
7. 作者胜利固定给1个大师球、50万～100万宝可梦币，加奖池五份不重复奖励；每位玩家首次胜利必定额外给1个小智版甲贺忍蛙进化石，此后独立1%；通过重铸Boss奖励界面领取，作者随后消失。再次测试需按出生点按钮补回。
8. 击败作者获得挑战成就“这么强？！？！”，描述“击败作者”，图标为作者皮肤头颅。
9. 作者对战玩家禁止使用药品；普通道馆最多3次，其中恢复最多2次、复活最多1次，额度不互换。
10. 房屋外壳按主题显示相应材质，白羊毛地板、内嵌顶灯保持。末地/地狱/究极等队伍主题在平地布置用于快速核对；天然生成须在新生成区块另测。
11. 未安装扩展物品保留空箱和缺失标签，不参与奖励抽取；原完整整合包才能显示全部奖励。
12. 指南针搜索“作者道馆”，或执行/locate pixelmon_backport:author_gym。旧author_pvp_gym入口取消。可用FTB核对发现作者道馆。
13. NPC制造、烧炼、拾取、采摘只需拥有足量匹配物品，旧任务生效，交付仍按原数量扣除。
14. 优化了实力相当npc战斗逻辑，优先可靠击倒；作者房间加入治疗仪、PC、工作台、床和桌椅，并在治疗仪左侧放置林地府邸奖励箱。

进入时空队伍自动补齐测试宝可梦，已有队伍不会自动覆盖。两类巢穴究极异兽限定究极空间对应群系；雕像名称与Jade提示已兼容。
本包不是完整客户端。已完成离线回归与独立客户端专项检查；本地图完整游玩仍需人工验收。测试后可移除backport-author-test助手，保留核心正常游玩。
群友0号问题日志显示服务端仍为1.4.4；更新双方核心后应再核对，不能保证恢复已损坏的服务端个体。
"""
    core = Path(build['jar'])
    entries = {
        'mods/' + core.name: core.read_bytes(),
        'mods/backport-author-test-1.16.5-1.7.0.jar': helper.getvalue(),
        f'saves/{FOLDER}/level.dat': level.read_bytes(),
        '安装与地图说明.txt': instructions.encode('utf-8-sig'),
        '完整奖池核对.md': ('\n'.join(listing) + '\n').encode(),
        '更新说明.md': (ROOT / 'docs/UPDATE_1.7.0_SUMMARY.md').read_bytes(),
        '10月23日追加说明.md': (ROOT / 'docs/FOLLOWUP_1.7.0_2026-10-23.md').read_bytes(),
        '可选模组/ohmygod-greninja-1.16.5-1.0.0.jar': (DIST / 'ohmygod-greninja-1.16.5-1.0.0.jar').read_bytes(),
        '可选模组/安装说明.md': (ROOT / 'addons/ohmygod-greninja/README.md').read_bytes(),
        '0号排查记录.md': (ROOT / 'docs/ZERO_POKEMON_2026-10-20.md').read_bytes(),
    }
    entries['SHA256SUMS.txt'] = ''.join(hashlib.sha256(v).hexdigest() + '  ' + k + '\n' for k, v in entries.items()).encode()
    archive = DIST / ('Pixelmon-Backport-1.7.0-boss-test-map' + os.environ.get('BACKPORT_PACKAGE_SUFFIX', '') + '.zip')
    with ZipFile(archive, 'w', ZIP_DEFLATED, compresslevel=6) as bundle:
        for name, content in entries.items():
            bundle.writestr(name, content)
    with ZipFile(archive) as bundle:
        assert bundle.testzip() is None
        for name, content in entries.items():
            assert bundle.read(name) == content
        assert not any('/playerdata/' in x or '/pokemon/' in x for x in bundle.namelist())
    digest = hashlib.sha256(archive.read_bytes()).hexdigest()
    archive.with_suffix('.zip.sha256').write_text(digest + '  ' + archive.name + '\n', encoding='ascii')
    print(json.dumps({'archive': str(archive), 'sha256': digest, 'entries': len(entries),
                      'loot_entries': 78, 'test_rooms': 14, 'runtime_tested': False}, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    main()
