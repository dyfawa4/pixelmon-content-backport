# 2026-10-16 收到的对战日志诊断

来源：用户提供的 `logs.zip` 中 `latest.log`、`debug.log` 和 `battle/battle-1792075321.log`。原始日志只在本地诊断缓存读取，不打入交付包。

## 已确认的直接原因

2026-10-15 23:00:55，苍响与野生亚克诺姆的对战进行至第 60 回合。亚克诺姆的高速星星、幻象光线、封印、太晶爆发全部为 0 PP。原生 `BattleAIBase.getAttackChoices` 找不到可用招式后，会构造 `new Attack(AttackRegistry.STRUGGLE)`，准备使用挣扎。

本次紧接着记录 `Empty attack provided`，说明该 Optional 为空，未初始化 Attack 的基础招式。随后 `Attack.createMoveChoices` 调用 `Attack.getMove`，在基础招式及 originalMove 都为空时抛出空指针。Pixelmon 捕获异常、结束该场对战，并输出截图中的「战斗发生了错误」。这是日志确认的对战中断，没有证据表明这一异常直接关闭整个游戏。

这是服务器 AI 的 PP 耗尽回退路径，与上一轮客户端 Z 招式黄边预览的缺少 originalMove 不是同一个入口。仅凭异常中的 originalMove 字样不能将两者归为相同原因。

## 已确认的运行环境问题

- 启动日志明确为 **Microsoft Java 21.0.7**，不是本整合包要求的 Java 11。
- Pixelmon 启动时已报 `Failed to patch lifecycle fields!`，随后为 `NoSuchFieldException: modifiers`，来自 `ReflectionHelper.flipVariableFinality`。这是本体在当前运行环境的反射初始化失败。
- 同一启动还出现 JEI Nashorn 转换错误：`ReferenceError: "opcodes" is not defined`。
- 日志加载的回迁模组文件为 **1.4.2**，不是当前本地 1.4.4 修订构建。

原生 AttackRegistry 中这类 Optional 为最终字段，并由本体对象持有器填充。反射初始化失败与空的挣扎引用高度一致，但日志没有直接记录该字段写入的全过程；因此应区分已证实的直接触发与运行环境造成的关联，不能承诺只换 Java 已被这位玩家实测解决。

## 处理

在启动器此实例的 Java 设置里明确选择 64 位 Java 11，避免自动选到 Java 21；正常退出后更新主模组到本次 1.4.4 构建，再复测 PP 全耗尽时是否能正常使用挣扎。联机服务端也应核对 Java 与模组版本。

本次不通过吞异常、恢复 PP、强制补错招式或修改伤害来掩盖注册失败，也不改变既定战斗规则。若 Java 11 下仍重现，应保留新的启动日志和对应 battle 日志继续定位；本报告不声称已经在对方设备复现或修复所有崩溃。
