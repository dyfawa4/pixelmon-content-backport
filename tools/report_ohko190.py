"""Archive scoped OHKO forecast/native execution and legitimate complete space games."""
from pathlib import Path
from zipfile import ZipFile
from collections import Counter,defaultdict
import hashlib,json
ROOT=Path(__file__).resolve().parents[1]
probe=ROOT/'.cache/ohko190-fixed-r1';gamesdir=ROOT/'.cache/ohko190-matches-fixed-r1';baseline=ROOT/'.cache/ohko190-baseline-r2';regression=ROOT/'.cache/ohko190-regression-r1'
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
sha=digest(probe/'mods/backport-dev.jar')
assert digest(gamesdir/'mods/backport-dev.jar')==digest(regression/'mods/backport-dev.jar')==sha
matrix=read(probe/'ohko190-results.json');old=read(baseline/'ohko190-results.json')
assert matrix['cases']==old['cases']==4608 and matrix['mismatches']==0 and old['mismatches']>0
counts=defaultdict(Counter)
for r in matrix['rows']:counts[(r['scenario'],r['move'])].update({'cases':1,'hits':int(r['actual_hit'])})
for (scenario,move),v in counts.items():
    if scenario in ['higher-target','sturdy','protect'] or scenario=='ice' and move=='Sheer Cold' or scenario=='flying' and move=='Fissure' or scenario=='ghost' and move in ['Horn Drill','Guillotine']:assert v['hits']==0
    elif scenario=='no-guard':assert v['hits']==v['cases']
    else:assert 0<v['hits']<v['cases']
status=read(regression/'status190-results.json');guard=read(regression/'guard190-results.json')
assert status['cases']==1736 and status['mismatches']==0
assert guard['cases']==288 and guard['guard_prediction_mismatches']==guard['decision_damage_mismatches']==0
assert 'GUARD190_DECISIONS PASS' in (regression/'bossbalance190-result.txt').read_text()
games=read(gamesdir/'conventional190-results.json');assert len(games)==18
assert 'MATCH190_SUITE_FINISHED' in (gamesdir/'conventional190-result.txt').read_text()
for game in games:
    assert game['outcome'] in ['challenger-win','defender-win']
    for p in game['challengers']:assert sum(p['evs'])==510 and max(p['evs'])<=252 and max(p['ivs'])<=31
    for turn in game['turns'][:-1]:
        for side in ['challenger','defender']:assert sum(m['side']==side and m['active'] for m in turn['members'])<=1
    game.pop('challenger_remaining',None);game.pop('defender_remaining',None);game['turns'][-1]={'turn':game['turns'][-1]['turn'],'phase':'post_cleanup_not_survivor_counts'}
log=(gamesdir/'logs/latest.log').read_text(errors='replace');assert not any(s in log for s in ['MATCH190_FAIL','Caught error in battle','Too many errors detected in battle'])
ohko=[a for g in games for a in g['native_actions'] if a['move'] in ['Fissure','Sheer Cold','Horn Drill','Guillotine']]
assert any(a['room'] for a in ohko) and any(a['damage']>0 for a in ohko) and any(a['damage']==0 for a in ohko)
with ZipFile(probe/'mods/backport-dev.jar') as z:classes=sum(n.endswith('.class') for n in z.namelist());assert not any(n.startswith('qa/') for n in z.namelist())
summary={'core_sha256':sha,'classes':classes,'matrix_cases':4608,'baseline_mismatches':old['mismatches'],'mismatches':0,'status_cases':1736,'guard_cases':288,'matches':len(games),'turns':sum(g['total_turns'] for g in games),'actual_ohko_attempts':len(ohko),'actual_damaging_ohko':sum(a['damage']>0 for a in ohko),'room_ohko_attempts':sum(a['room'] for a in ohko)}
data={'summary':summary,'matrix':matrix,'counts':[{**dict(v),'scenario':s,'move':m} for (s,m),v in counts.items()],'matches':games,'limits':['Internal verification only; do not ship AI details/player data.',
 'Semantic probes deliberately isolate mechanics and may assign non-native abilities/moves; complete challenger parties are native-learnset/ability legal.',
 'Corrected baseline suppresses trainer roster expansion only during fixture initialization, then restores ordinary/author eligibility; actual target UUIDs are asserted.',
 'Nine natural and nine controlled initial-room games are distinguished; initial room is not claimed guaranteed to set up.',
 'Native inner OHKO chance and native outer move accuracy are both retained; no forced success or global chance rebalance.',
 'Action traces observe native Use events and final MoveResults; cleanup HP is not a survivor count, and damage events alone are not an OHKO win-rate.',
 'Eighteen automated games do not establish human win-rate or absence of every possible counterexample.']}
(ROOT/'docs/data/OHKO_1.9.0_MATCHES.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
lines=['# 一击必杀与空间队：内部验证','',f'核心SHA256 `{sha}`，{classes}个生产class。此文件／JSON为内部验证，不进入玩家包及公开AI更新说明。','',
 f'原生OHKO效果内部还有一次抽签，模拟模式跳过它，原先预览因此误把模拟命中当实际命中。修复后外层原生命中与内部OHKO随机分别与实际出招共用，保留等级、绝对零度冰系免疫、无防守及原有结实／属性免疫。未强制命中，未改招式概率。4,608项对照从{old["mismatches"]}项不一致降为0，普通／作者、双方、四个招式分别覆盖。','',
 '定向矩阵包含等等级、对方等级更高／更低、冰／飞行／幽灵目标、结实、破格、无防守、命中闪避变化、守住、戏法空间。实际免疫分支零伤害；非必中的合法命中分支既有成功也有失败。定向夹具可能给物种设置非原生招式／特性，仅验证机制；完整对战队伍另外严格检查原生合法性。最早r1夹具被普通馆主组队替换了目标，已废弃，接受的旧核心基线为r2。','',
 '美梦神＋拉普拉斯／帝牙海狮／白海狮／龙头地鼠／巨钳蟹，合法IV及总EV510，原生道具与技能。9场尝试自然开空间，9场从明确预置空间的受控局面开始；平原作者、海洋作者、普通钢馆各覆盖6场。预置局面不冒充自然保证。','',
 '| 配置 | 对手 | 结果 | 回合 |','| --- | --- | --- | --- |']
for g in games:lines.append(f'| {g["archetype"]} | {g["opponent"]} | '+('馆主胜' if g['outcome']=='defender-win' else '挑战方胜')+f' | {g["total_turns"]} |')
lines+=['',f'合计18场{summary["turns"]}回合，原生出招事件记录到{len(ohko)}次一击必杀尝试，{summary["actual_damaging_ohko"]}次产生伤害，{summary["room_ohko_attempts"]}次发生在空间内。实际出现先制攻击、反开空间、挑衅等应对；样本没有因一击必杀偶发成功而擅自削弱招式。真实PP／伤害／免疫／胜负由本体执行，双方场上标记检查通过。','',
 '共享命中方法同时回归1,736项异常行动＋288项防守，均0不一致，单体反广防／高PP与保留有收益换人分支通过。','',
 '这18场不构成人类胜率，也不能证明不存在任何可通关或漏洞队伍。原生结束会恢复队伍HP，不报告恢复后的存活数；私有无交互相机错误不当成服务器战斗错误。详细证据见[JSON](data/OHKO_1.9.0_MATCHES.json)。']
(ROOT/'docs/OHKO_1.9.0_MATCHES.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps(summary))
