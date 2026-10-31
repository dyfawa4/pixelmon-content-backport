"""Archive current-turn status forecasts against native takeTurn and complete hazard games."""
from pathlib import Path
import hashlib,json,collections
ROOT=Path(__file__).resolve().parents[1]
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
probe=ROOT/'.cache/status190-r7';instance=ROOT/'.cache/hazard190-status-r4'
status=read(probe/'status190-results.json');guard=read(probe/'guard190-results.json')
assert status['cases']==1736 and status['mismatches']==0
assert guard['cases']==288 and guard['guard_prediction_mismatches']==guard['decision_damage_mismatches']==0
log=(probe/'bossbalance190-result.txt').read_text();assert 'GUARD190_DECISIONS PASS' in log and '_FAIL' not in log
sha=digest(probe/'mods/backport-dev.jar');assert digest(instance/'mods/backport-dev.jar')==sha
assert 'MATCH190_SUITE_FINISHED' in (instance/'conventional190-result.txt').read_text()
games=read(instance/'conventional190-results.json');assert len(games)==6
for game in games:
    assert game['outcome'] in ['challenger-win','defender-win']
    for p in game['challengers']:assert sum(p['evs'])==510 and max(p['evs'])<=252 and max(p['ivs'])<=31
    for turn in game['turns'][:-1]:
        for side in ['challenger','defender']:assert sum(r['side']==side and r['active'] for r in turn['members'])<=1
    game.pop('challenger_remaining',None);game.pop('defender_remaining',None)
    game['turns'][-1]={'turn':game['turns'][-1]['turn'],'phase':'post_native_cleanup_not_survivor_counts'}
log=(instance/'logs/latest.log').read_text(errors='replace');assert not any(x in log for x in ['Caught error in battle','Too many errors detected in battle','MATCH190_FAIL'])
counts=collections.defaultdict(collections.Counter)
for row in status['rows']:counts[(row['author'],row['actor'],row['status'])]['hit' if row['actual_hit'] else 'blocked']+=1
for author in [False,True]:
    for actor in ['player','npc']:
        for state in ['paralysis','confused','freeze']:
            assert counts[(author,actor,state)]['hit']>0 and counts[(author,actor,state)]['blocked']>0
data={'date':'2026-10-26','core_sha256':sha,'status':status,'guard':guard,'matches':games,
      'extra_assertions':['Stable repeated forecasts; no live clock/HP mutation.','Cure within current planning scope immediately changes availability.',
                          'Cached canAttack failure does not block native next-attempt recalculation.','Sucker Punch retains submitted damaging-action semantics even when the target later fails paralysis.',
                          'A sleeping own attacker is not forced to stay for impossible PP trading.','High-PP self-damage/crash moves are excluded from blocked PP-trade fallback.'],
      'limits':['Forecast concerns current known status branches, not guaranteed future secondary effects or all abilities.',
                'Targeted move setups exercise native mechanics, not a claim of species learnset legality for every probe move.',
                'Full challenger parties use native legal learnsets/abilities and EV510.',
                'Native takeTurn checks status before useAttack. The first r1 fixture called useAttack directly and bypassed status; it is rejected as an execution comparison.',
                'Private suppressed UI camera errors are separated from server battle execution.','Native end cleanup heals NPC parties; no restored survivor counts.']}
(ROOT/'docs/data/STATUS_1.9.0_MATCHES.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
lines=['# 1.9.0 异常状态与真实行动对照','',f'核心SHA256：`{sha}`。普通／作者、自己／敌方均覆盖；1,736项实际行动和320项保护相关组合（已计入1,736项），预览与真实结果0不一致。另重验288项保护矩阵及6场完整双钉对局。','',
       '原生状态分支采用当前状态计数及与正式执行共用的随机结果：全麻／正常行动、睡眠／本回合醒来、混乱自伤／正常行动／解除、冻结／自行解冻／解冻招式、畏缩及充能硬直分别判断。只读预览不调用状态执行方法，不消耗睡眠／混乱回合或提前伤害真实个体。正式状态判定仍由原生takeTurn执行。','',
       '同回合额外行动使用独立的动作尝试序号，即使上次被异常阻止也不会重复使用旧判定。梦话／打鼾区分仍在睡眠与本回合醒来；状态清除后立即使用最新条件。canAttack是原生缓存／假想换人的临时标记，下一次行动会重算，不能当作持久禁行动状态；预览恢复也补齐此标记。','',
       '“提交攻击”与“实际能够行动”分开：目标提交攻击后即使稍后全麻，先手突袭仍可按原生规则命中；受到伤害的反射等判断则不把全麻当成有实际攻击。无法行动的守住／碉堡不会生成防守预览。无法出招的己方保留原生换人选择，不强迫留场消耗不存在的PP；交换PP排除钢铁光束等固定自伤／撞击失败伤害动作。','',
       '| 队型 | 对手 | 结果 | 回合 |','| --- | --- | --- |']
for g in games:lines.append(f'| {g["archetype"]} | {g["opponent"]} | '+('馆主胜' if g['outcome']=='defender-win' else '挑战方胜')+f' | {g["total_turns"]} |')
lines+=['','全部6场到原生胜负，双方每回合场上标记检查通过。挑战方合法EV510，真实伤害／PP／状态／胜负走本体，只加速画面与刻等待。随机自动样本不代表真人胜率或穷尽战术。','',
        '完整证据：[JSON](data/STATUS_1.9.0_MATCHES.json)。来源`.cache/status190-r7`与`.cache/hazard190-status-r4`。最初r1测试绕过takeTurn直接调用useAttack，遗漏了原生状态资格步骤，已废弃；后续全部按真实行动入口检查。未将夹具错误说成游戏状态规则错误。','',
        '本专项不宣称预知未来所有新增异常／场地／道具效果或覆盖每一种特性；正式执行仍根据当时原生条件进行。私有无交互战斗GUI的相机错误单列，不冒充UI验收；原生战后恢复的HP未计作剩余精灵数。']
(ROOT/'docs/STATUS_1.9.0_MATCHES.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'core_sha256':sha,'status_cases':1736,'guard_cases':288,'matches':len(games),'turns':sum(g['total_turns'] for g in games)}))
