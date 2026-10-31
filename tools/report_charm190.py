"""Record native charm UI, authoritative state and actual private world restart checks."""
from pathlib import Path
from zipfile import ZipFile
import json,hashlib,shutil
ROOT=Path(__file__).resolve().parents[1]
probe=ROOT/'.cache/charm190-fixed-r3';baseline=ROOT/'.cache/charm190-baseline-r1'
read=lambda p:json.loads(p.read_text('utf-8'))
rows=read(probe/'charm190-results.json');relog=read(probe/'charm190-relog-results.json');old=read(baseline/'charm190-results.json')
physical=read(ROOT/'.cache/charm190-item-r1/charm190-item-results.json')
assert physical['first_consumed']==1 and physical['duplicate_consumed']==0 and physical['server_owned'] and physical['client_after_inventory']==2
assert all(r['passed'] for r in rows+relog)
assert sum(r['case'].startswith('click-') for r in rows)==16
assert any(r['case']=='tracking-dirty' for r in rows)
assert any(r['case']=='owned-inactive-repair' for r in relog)
assert not next(r for r in old if r['case']=='stale-client-repair')['passed']
assert not next(r for r in old if r['case']=='client-cannot-revoke')['passed']
assert 'CHARM190_RELOG_PASS' in (probe/'bossbalance190-result.txt').read_text()
jar=probe/'mods/backport-dev.jar';sha=hashlib.sha256(jar.read_bytes()).hexdigest()
with ZipFile(jar) as z:classes=sum(n.endswith('.class') for n in z.namelist());assert not any(n.startswith('qa/') for n in z.namelist())
for name in ['charm190-owned.png','charm190-owned-inactive.png']:shutil.copy2(probe/'screenshots'/name,ROOT/'docs/previews'/name)
assert hashlib.sha256((ROOT/'.cache/charm190-item-r1/mods/backport-dev.jar').read_bytes()).hexdigest()==sha
data={'date':'2026-10-27','core_sha256':sha,'production_classes':classes,'baseline':old,'checks':rows,'actual_restart':relog,'physical_item':physical,
      'limits':['The user cannot currently distinguish a missing icon from missing XP bonus; the original reported case has not been reproduced from their save.',
                'Controlled stale client metadata hides an owned charm and now repairs from native server ownership; this does not prove that stale metadata was the original cause.',
                'No automatic compensation is issued without ownership evidence; already deleted historical ownership cannot be reconstructed from an unowned flag alone.',
                'All tests and cursor/inventory fixtures run in a copied disposable private world, not player saves.',
                'Native false return means an equipment click is handled; its semantics are retained.',
                'Owner snapshots preserve live dirty metadata for other tracking players; real-time retry/rate limiting also works while integrated worlds pause.',
                'No XP multiplier, task reward condition or administrator remove permission is changed.']}
(ROOT/'docs/data/CHARM_1.9.0.json').write_text(json.dumps(data,ensure_ascii=False,indent=2),'utf-8')
lines=['# 经验护符显示／拥有状态：原生验证','',f'核心SHA256 `{sha}`，{classes}个生产class。','',
       '**反馈边界：**用户目前不清楚原反馈属于图标消失还是经验加成失效，不能认定固定展开直接删除了护符数据。已修复并验证客户端过期元数据导致的隐藏，以及客户端开关包可撤销拥有权的入口；未向无获得凭据的玩家自动补发。历史数据若真的已被删除，仍需该玩家具体存档／日志才能追查，不能凭未拥有标记推断曾拥有。','',
       '打开背包后请求服务端真实状态，并作有限重试。保留Available（拥有但停用）、Active（拥有且启用）与Disabled（未拥有）的区分。客户端开关只能切换拥有道具的启停，不能撤销拥有权或凭空获得；原管理员expcharm发放／移除命令仍正常。','',
       '四种GUI尺寸×创造／生存×护符／旧箭头，共16次完整原生界面点击，光标7个带测试NBT的钻石及护符拥有状态保留。原生false代表点击已处理，保留现有点击语义。五种护符三种状态共15项NBT状态还原；10个客户端非法撤销／越权发放请求按实际网络处理验证。','',
       '客户端已有与迟到的过期显示均能从服务端校准；主动停用仍显示为拥有但未启用，未被强行开启。同步快照不清空原生实体跟踪的dirty标记，避免吞掉其他玩家待接收的更新；限频／重试使用真实经过时间，单机背包暂停世界时仍生效。','',
       '完整关闭测试游戏并保存，再对同一私有存档启动新进程：服务端护符拥有状态、客户端显示及测试物品数量均保留。实际重登与NBT内存还原分别验证，没有用命令重新发放冒充重登保存。','',
       '另按本地任务奖励使用原生pixelmon:exp_charm：首次右键消耗1个并永久解锁，重复使用不再消耗。原生道具流程中记录到服务端已拥有、打开背包前客户端仍显示未拥有；打开背包后正确校准为已启用。实体道具右键正常消耗不等于护符拥有状态被删除，不能混为一谈。此场景可验证同步修复，仍不能断言它就是原反馈的具体原因。','',
       '启用状态原生截图：','', '![已拥有且启用](previews/charm190-owned.png)','', '主动停用且仍拥有：','', '![已拥有但停用](previews/charm190-owned-inactive.png)','',
       '证据：[JSON](data/CHARM_1.9.0.json)，来源`.cache/charm190-fixed-r3`与`.cache/charm190-baseline-r1`。XP倍率／任务奖励条件未改；原始反馈的具体原因仍未确认。']
(ROOT/'docs/CHARM_1.9.0.md').write_text('\n'.join(lines)+'\n','utf-8')
print(json.dumps({'core_sha256':sha,'classes':classes,'ui_clicks':16,'nbt_states':15,'network_checks':10,'actual_restart':True,'original_report_cause':'unconfirmed'}))
