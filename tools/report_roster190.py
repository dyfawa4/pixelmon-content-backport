"""Archive private native author roster, migration, scouting and complete match evidence."""
from pathlib import Path
from zipfile import ZipFile
import hashlib,json

ROOT=Path(__file__).resolve().parents[1]
probe=ROOT/'.cache/roster190-native-r8'
matches=ROOT/'.cache/roster190-matches-r6'
legacy=ROOT/'.cache/roster190-legacy-r1'
read=lambda p:json.loads(p.read_text('utf-8'))
digest=lambda p:hashlib.sha256(p.read_bytes()).hexdigest()
sha=digest(probe/'mods/backport-dev.jar')
assert digest(matches/'mods/backport-dev.jar')==sha
assert 'ROSTER190_PASS' in (probe/'bossbalance190-result.txt').read_text()
assert '_FAIL' not in (probe/'bossbalance190-result.txt').read_text()
rosters=read(probe/'author-roster190-results.json')
scouting=read(probe/'scouting190-results.json')
assert rosters['teams']==224 and rosters['migrations']==4 and rosters['stat_cases']==8 and rosters['scouting_cases']==56
assert rosters['physical_teams']==rosters['special_teams']==rosters['breakthrough_teams']==224
assert len(scouting)==56 and all(r['setup'] and r['disruption'] for r in scouting)
fixed={'plains':(493,'','pixelmon:legend_plate'),'ocean':(382,'primal','pixelmon:blue_orb'),'desert':(383,'primal','pixelmon:red_orb'),'swamp':(890,'eternamax','pixelmon_backport:eternatus_meteorite')}
for team in rosters['rows']:
    members=team['members'];assert len(members)==6
    assert len({p['dex'] for p in members[:3]})==3
    assert all(p['dex'] in [1026,493,658,890,898] for p in members[3:5])
    assert all(p['ivs']==[31]*6 and p['evs']==[252]*6 for p in members)
    if team['theme'] in fixed:
        p=members[0];assert (p['dex'],p['form'],p['item'])==fixed[team['theme']]
    if team['theme']=='badlands':assert members[5]['dex']==1026
    dexes={p['dex'] for p in members[:3]}
    assert not (382 in dexes and (383 in dexes or 1007 in dexes))
games=read(matches/'conventional190-results.json')
assert len(games)==9 and 'MATCH190_SUITE_FINISHED' in (matches/'conventional190-result.txt').read_text()
for game in games:
    assert game['outcome'] in ['challenger-win','defender-win']
    for p in game['challengers']:assert sum(p['evs'])==510 and max(p['evs'])<=252 and max(p['ivs'])<=31
    for turn in game['turns'][:-1]:
        for side in ['challenger','defender']:assert sum(r['side']==side and r['active'] for r in turn['members'])<=1
    game.pop('challenger_remaining',None);game.pop('defender_remaining',None)
    game['turns'][-1]={'turn':game['turns'][-1]['turn'],'phase':'post_native_cleanup_not_survivor_counts'}
log=(matches/'logs/latest.log').read_text(errors='replace')
assert not any(x in log for x in ['Caught error in battle','Too many errors detected in battle','MATCH190_FAIL'])
with ZipFile(probe/'mods/backport-dev.jar') as new,ZipFile(legacy/'mods/backport-dev.jar') as old:
    entries=set(new.namelist())|set(old.namelist())
    changed=sorted(n for n in entries if not n.endswith('/') and (n not in new.namelist() or n not in old.namelist() or new.read(n)!=old.read(n)))
    assert not any(n.startswith('qa/') for n in new.namelist())
    classes=sum(n.endswith('.class') for n in new.namelist())
data={'date':'2026-10-27','core_sha256':sha,'production_classes':classes,'previous_core_sha256':digest(legacy/'mods/backport-dev.jar'),'changed_entries':changed,'rosters':rosters,'scouting':scouting,'matches':games,
      'limits':['Internal evidence only; exclude from player package and release notes.',
                'Reported challenger species are representative legal builds, not a reconstruction of the screenshot moves/items/forms.',
                'Native battles accelerate visual waits only; NPC cleanup heals parties, so restored survivor counts are excluded.',
                'Nine automated matches are not a human win-rate or an impossibility proof.',
                'Four version3 migration fixtures were captured using the immutable previous production core, with actual native NBT.',
                'Eight targeted stat fixtures compare actual wrapper stats with detached native previews, without claiming that their arbitrary probe moves are legal species builds.',
                'The initial scouting fixture lacked a gym theme marker and was rejected; subsequent fixtures use the actual eligible trainer configuration.',
                'Private suppressed battle GUI camera errors are separate from server battle execution.']}
(ROOT/'docs/data/AUTHOR_ROSTER_1.9.0.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
lines=['# 作者三神兽位专项：内部验证记录','',f'核心SHA256：`{sha}`，生产class {classes}个。私有验证程序未进入核心。本文件及数据不纳入玩家包／公开更新说明。','',
       '固定主题成员占三个神兽位之一：沙漠原始固拉多、海洋原始盖欧卡、沼泽无极巨化无极汰那、平原阿尔宙斯，专属道具保留；另有两个原强力抽选位，岩地第六位保留固定智皮卡。神兽实际形态总种族值至少650。','',
       '候选按完整神兽组合比较抗性、免疫、物攻／特攻、回复能力及天气冲突，避免单个贪心首选把后续组合锁死。保留独立的回复／空间干扰来源，不能完全依赖已占用黑雾／抢夺两格的辅助成员。每只至少两个有效攻击，保留突破手段。','',
       f'14主题×16种子，共224支／1,344只原生队伍检查通过，出现{rosters["core_combinations"]}种神兽组合；{rosters["core_recovery_teams"]}支神兽成员带实际回复／吸取招式。224支均有实际物攻、特攻与突破／强化／空间手段。另56组混合强化、接棒、双钉、回复、空间与受队威胁的原生侦察／配招检查通过，保持两个攻击和清强化／干扰来源。','',
       '四种旧第3版作者队伍使用上一份核心真实构造并保存NBT，在新核心中迁移；原两神兽、两强力成员的UUID／招式／道具保留，保留一个旧补位，岩地固定智皮卡保留。仅新增第三神兽所需替换一个补位；重复升级不重抽，不重置挑战记录。','',
       '8组实际战斗属性对照：四种宝可梦×无祸／已有20点祸。作者不新增祸；无祸不凭空扣减，旧祸只计算一次；原生临时战斗个体与脱离真实个体的预览六项属性一致，治疗后两者均恢复。未更改溺水祸来源。','',
       '| 挑战配置 | 对手 | 结果 | 回合 |','| --- | --- | --- | --- |']
for game in games:lines.append(f'| {game["archetype"]} | {game["opponent"]} | '+('馆主胜' if game['outcome']=='defender-win' else '挑战方胜')+f' | {game["total_turns"]} |')
lines+=['',f'共9场{sum(g["total_turns"] for g in games)}回合，全部到原生胜负。挑战方合法EV510／IV不超31；截图队伍配置为代表样例，未声称还原玩家原打法。两种作者及普通钢馆均覆盖，双方每回合场上标记检查通过，没有原生服务器战斗异常。','',
        '随机自动样本不代表真人胜率，不据此宣称作者无法通关。原生战后恢复的HP不作存活数；无交互相机错误单列。旧逐个选取版本的高苍响比例，以及缺少主题标记／缺少干扰来源的中间夹具均非最终验收证据。','',
        '证据：[JSON](data/AUTHOR_ROSTER_1.9.0.json)。原始输入与日志在`.cache/roster190-native-r8`、`.cache/roster190-matches-r6`、`.cache/roster190-legacy-r1`。']
(ROOT/'docs/AUTHOR_ROSTER_1.9.0.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'core_sha256':sha,'classes':classes,'teams':rosters['teams'],'combinations':rosters['core_combinations'],'core_counts':rosters['core_counts'],'scouting':len(scouting),'matches':len(games),'turns':sum(g['total_turns'] for g in games)}))
