"""Keep native guard trials and complete hazard matches separate from failed diagnostics."""
from pathlib import Path
import hashlib,json
ROOT=Path(__file__).resolve().parents[1]
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
baseline=ROOT/'.cache/guard190-baseline-r1';fixed=ROOT/'.cache/guard190-fixed-r9'
before=read(baseline/'guard190-results.json');after=read(fixed/'guard190-results.json')
assert after['cases']==288 and after['guard_prediction_mismatches']==after['decision_damage_mismatches']==0
assert 'GUARD190_DECISIONS PASS' in (fixed/'bossbalance190-result.txt').read_text()
sha=digest(fixed/'mods/backport-dev.jar')
sets={}
for name,folder in [('baseline','hazard190-baseline-r1'),('fixed','hazard190-fixed-r5')]:
    instance=ROOT/'.cache'/folder
    if name=='fixed':assert digest(instance/'mods/backport-dev.jar')==sha
    games=read(instance/'conventional190-results.json');assert len(games)==6
    assert 'MATCH190_SUITE_FINISHED' in (instance/'conventional190-result.txt').read_text()
    log=(instance/'logs/latest.log').read_text(errors='replace')
    assert not any(s in log for s in ['Caught error in battle','MATCH190_FAIL','Too many errors detected in battle'])
    for game in games:
        assert game['outcome'] in ['challenger-win','defender-win']
        for p in game['challengers']:assert sum(p['evs'])==510 and max(p['evs'])<=252 and max(p['ivs'])<=31
        dirty=sum(sum(r['side']==side and r['active'] for r in turn['members'])>1 for turn in game['turns'][:-1] for side in ['challenger','defender'])
        game['multiple_field_flag_snapshots']=dirty
        if name=='fixed':assert dirty==0
        entries=[e for e in game['field_changes'] if e['side']=='defender']
        game['entry_summary']={'observed_changes':len(entries),'outgoing_alive':sum(e['old_alive'] for e in entries),'nonzero_native_hazard_formula':sum(e['native_entry_damage']>0 for e in entries)}
        game.pop('challenger_remaining',None);game.pop('defender_remaining',None)
        game['turns'][-1]={'turn':game['turns'][-1]['turn'],'phase':'post_native_end_cleanup_not_survivor_count'}
        game['source_instance']=folder
    sets[name]=games
data={'date':'2026-10-26','core_sha256':sha,'baseline_sha256':digest(baseline/'mods/backport-dev.jar'),
      'guard_before':before,'guard_after':after,'matches':sets,
      'limits':['Legal six-member challengers, native non-simulation battle engine; only visual/tick waits accelerated.',
                'Native end cleanup restores squads: no post-end survivor counts.',
                'Field changes are sampled; outgoing-alive is not a universal forced-switch classification.',
                'native_entry_damage is the native hazard formula at observation, not an isolated damage event log.',
                'Post-setup matches explicitly preload one Spikes layer and Stealth Rock, not a guaranteed natural setup.',
                'Baseline diagnostic games also expose stale native replacement-preview field flags; their results are diagnostic, not clean acceptance.',
                'Automatic samples and differing random rolls are not paired statistical win-rate evidence.',
                'Private suppressed battle GUI emits camera errors; server battle execution is checked separately.']}
(ROOT/'docs/data/GUARD_1.9.0_MATCHES.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
lines=['# 1.9.0 双钉与保护专项','',f'修正核心SHA256：`{sha}`。基线为阶段1核心`{data["baseline_sha256"]}`。','',
       '守住、碉堡、广域防守分别覆盖单体精神强念／群体冲浪、连续保护计数0至3、每格12次，共288项原生真实执行。基线保护预判62项不一致、伤害判断32项不一致，均来自广域防守错误套用连续守住失败率；守住和碉堡各自的成功／失败原生对照未见该问题。修正后两项不一致均为0。','',
       '新增原生选择验证：广防使用有效单体攻击，成功全防时留场消耗高PP普通招式且真实只扣一次PP；关键换人优先级及明显改善后续输出的换人保留。入场成本在对方守住的安全回合仍计入；原生预览后的双方场上／行动标记都恢复，避免假想动作泄漏。','',
       '挑战队使用合法盔甲鸟双钉、超坏星守住／碉堡、吉利蛋守住，以及巨翅飞鱼守住／广防等。吉利蛋在本地原生可学招式中没有广域防守，故未给它非法招式。自然开场与明确预铺一层撒菱＋隐形岩的对照各3场。','',
       '| 场景 | 对手 | 基线结果／回合／换入 | 修正结果／回合／换入 |','| --- | --- | --- | --- |']
for a,b in zip(sets['baseline'],sets['fixed']):
    def cell(g):return ('馆主胜' if g['outcome']=='defender-win' else '挑战方胜')+f'／{g["total_turns"]}／{g["entry_summary"]["observed_changes"]}'
    lines.append(f'| {a["archetype"]} | {a["opponent"]} | {cell(a)} | {cell(b)} |')
lines+=['','修正后的6场均正常分出胜负。不能将随机样本的胜负／回合差解释成确定收益或真人胜率；普通馆主对受队仍可败，不以封禁正常战术强保AI胜利。换入包含倒下替补，详细输出另列离场者是否存活及钉伤公式，不能把总换入次数全部当无收益主动换人。','',
        '中间诊断`guard190-fixed-r2`至`r6`暴露候选池只做伤害预览未做战术修正，以及后备的战斗修正尚未初始化导致未来输出被低估；已使用完整原生TacticalAI加权池并对后备副本刷新原生属性。保留明确降特攻的实际状态作为有收益换人反例。`hazard190-fixed-r1`有一场回合上限；r2/r3推进到胜负，但归档发现原生倒下选替补预览另有场上标记残留，r4只覆盖倒下替补，另发现伏特替换等枢轴也直接调用原生选人，最终统一封住AggressiveAI.getNextSwitch并以r5完整重跑。基线同样存在该原生残留，因此基线完整比赛只作为诊断对照，不当洁净验收或统计优劣证明。','',
        '完整可核查记录：[JSON](data/GUARD_1.9.0_MATCHES.json)。证据实例：`.cache/guard190-fixed-r9`、`.cache/hazard190-fixed-r5`。玩家GUI、概率穷举和所有对局必胜不在此验收范围。']
(ROOT/'docs/GUARD_1.9.0_MATCHES.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'core':sha,'cases':288,'matches':12,'fixed_turns':sum(g['total_turns'] for g in sets['fixed'])}))
