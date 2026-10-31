"""Preserve complete native match evidence, keeping fixture failures separate from results."""
from pathlib import Path
import hashlib,json

ROOT=Path(__file__).resolve().parents[1]
SHA='5bd6d5112581de7ff125aae7533b1265c38a8b2ad0b0086c21d477881a959ff1'
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
r8=ROOT/'.cache/conventional190-r8';r9=ROOT/'.cache/conventional190-r9'
for instance in [r8,r9,ROOT/'.cache/boss-trial190-r8']:
    assert digest(instance/'mods/backport-dev.jar')==SHA
matches=read(r8/'conventional190-results.json')
assert len(matches)==18 and matches[13]['outcome']=='stalled'
retry=read(r9/'conventional190-results.json');assert len(retry)==1
assert (retry[0]['archetype'],retry[0]['opponent'])==('setup-pass','author-ocean')
matches[13]=retry[0]
for instance in [r8,r9]:
    log=(instance/'manual-launch.log').read_text('utf-8',errors='replace')
    assert not any(flag in log for flag in ['Caught error in battle','MATCH190_FAIL','Too many errors detected in battle'])
    assert 'MATCH190_SUITE_FINISHED' in (instance/'conventional190-result.txt').read_text()
for i,match in enumerate(matches):
    assert match['outcome'] in ('challenger-win','defender-win') and match['total_turns']>0
    assert len(match['challengers'])==len(match['defenders'])==6
    for p in match['challengers']:
        assert p['level']==100 and sum(p['evs'])==510 and all(0<=n<=252 for n in p['evs'])
        assert all(0<=n<=31 for n in p['ivs'])
    expected=1512 if match['opponent'].startswith('author-') else 510
    for p in match['defenders']:assert sum(p['evs'])==expected
    # Native trainer cleanup heals its squad before the harness regains control.
    # Those counters do not describe survivors at the instant of defeat.
    match.pop('challenger_remaining',None);match.pop('defender_remaining',None)
    match['source_instance']='conventional190-r9' if i==13 else 'conventional190-r8'
    match['turns'][-1]['phase']='post_native_end_cleanup_not_final_survivor_counts'
    match['turns'][-1].pop('members',None)

out=ROOT/'docs/data';out.mkdir(exist_ok=True)
evidence={
    'date':'2026-10-26','core_sha256':SHA,'engine':'Minecraft1.16.5 / Forge36.2.42 / Pixelmon9.1.13 / Java11',
    'challenger_controller':'Native AdvancedAI with private PlayerParticipant input adapter',
    'challenger_stats':'IV<=31; EV<=252/stat and total510; native role training',
    'acceleration':'Only tick, visual and UI acknowledgement waits accelerated. Combat simulateMode remains false.',
    'limits':['Not human optimal play or population win rates.','Unattended battle GUI is excluded; suppressed private UI tasks can log camera errors. No server battle exceptions in accepted runs.',
              'The original case13 waited for a missing Baton Pass player-input response; corrected input adapter rerun replaces it. Other completed matches are retained.',
              'Turn snapshots are sampled at turn boundaries, not a verbatim native action log. Squad restoration after battle is not counted as surviving Pokemon.'],
    'matches':matches,
}
target=out/'CONVENTIONAL_1.9.0_MATCHES.json';target.write_text(json.dumps(evidence,ensure_ascii=False,indent=2),'utf-8')
names={'offense':'强攻','balance':'平衡','rain':'雨天','trick-room':'空间','setup-pass':'强化接棒','stall':'受队'}
lines=['# 1.9.0 r3 常规队伍完整对战记录','',
       '本地核心SHA256：`'+SHA+'`。挑战方均为100级、六人，IV单项不超过31，EV单项不超过252、总计510，按职责培养；逐只招式与特性通过本地原生数据合法性检查。作者六项EV各252、普通馆主EV510。作者队伍原始生成等级70，战斗使用原生equal临时等比规则。','',
       '六类队伍各对战平原作者、海洋作者、普通钢系馆主，共18场，均实际推进至原生胜负结算。作者赢12/12，普通钢系馆主赢5/6，受队赢下其中一场普通馆主。**这些是自动操作样本，不能作为真人胜率或“作者无解”的证明。**','',
       '| 挑战队型 | 平原作者 | 海洋作者 | 普通钢系馆主 |','| --- | --- | --- | --- |']
for name,zh in names.items():
    games=[g for g in matches if g['archetype']==name]
    cells=[('挑战方胜' if g['outcome']=='challenger-win' else '馆主胜')+'，'+str(g['total_turns'])+'回合' for g in games]
    lines.append('| '+zh+' | '+' | '.join(cells)+' |')
lines+=['','完整训练、性格、特性、道具、配招与回合快照：[JSON](data/CONVENTIONAL_1.9.0_MATCHES.json)。',
        '', '## 检查边界','',
        '- 挑战方自动操作使用原生AdvancedAI，并补齐玩家输入适配，包括倒下后的替补和瞬间移动／接棒等招式后的选人。它不等于熟练玩家，也未穷举每个配队的最佳操作。',
        '- 战斗simulateMode为false，伤害、PP、异常、场地、换人及结算走原生逻辑；只加速刻／画面等待并代答无交互界面的确认。没有把伤害估算或单回合预览当整场结果。',
        '- 测试使用隔离客户端和私有存档。无交互战斗GUI不在验收范围，私有界面任务有相机报错；接纳的对局没有服务端战斗异常。不能据此声称客户端画面验收通过。',
        '- 其中接棒对海洋作者最初停在测试玩家未选择接棒目标，已修正适配并单独重跑完成；其余17场不重复计数。',
        '- 回合快照是边界采样，包含HP、PP、状态与已选／上一招，不能当作逐条原生执行日志。原生战后会恢复NPC队伍，未使用恢复后的HP统计“剩几只”。',
        '', '## 发现与修复','',
        '- 修正候选行动同优先度时读取旧turnList的判断；改用当前优先度、有效速度与空间规则，平速不当可靠先手。',
        '- 对赶不上对手致命攻击的解场动作降低优先级；保留有效先制、必要反制、可靠击杀及有限药品策略。',
        '- 受队反制的定向原生正反例另行验证，包括限制回复、关键道具、免疫边界、纯朴及毒疗，不以这18场胜负代替机制验证。',
        '- 人为先放任对手完成+6接棒的旧诊断场景中，部分作者队伍仍可能没有安全接点；不承诺已形成的压倒性局面都能救回。正常开场反强化／反接棒仍保留。',
        '', '证据：`.cache/conventional190-r8`、`.cache/conventional190-r9`、`.cache/boss-trial190-r8`。测试助手和存档不进入玩家包。']
(ROOT/'docs/CONVENTIONAL_1.9.0_MATCHES.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'matches':len(matches),'challenger_wins':sum(g['outcome']=='challenger-win' for g in matches),'total_turns':sum(g['total_turns'] for g in matches),'core_sha256':SHA,'evidence_sha256':digest(target)},ensure_ascii=False))
