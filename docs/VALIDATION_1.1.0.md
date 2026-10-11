# 1.1.0 验证记录

验证日期：2026-10-11。所有运行检查使用隔离实例；日常游戏存档未用于测试。

## 环境与结果

- Java 11、Minecraft 1.16.5、Forge 36.2.42。Pixelmon 本体文件名为 9.1.13，内部版本为 9.1.12。
- 功能模组及地图配套模组均完成 Java 11 构建，Mixin 注入目标检查通过，包括道具交互、训练家和巢穴探测器入口。
- 隔离专用服务端未安装 JEI，启动与服务端回归通过，验证 JEI 仍为可选依赖。
- 隔离客户端安装 JEI 7.8.1.1018、探测器、功能模组及发布地图包。实际 JEI 运行时查询到 78 个信息页，覆盖 70 个新增物品及 8 张相关学习器。
- 从发布 ZIP 安装太晶巢穴测试世界，配套模组正常加载并生成测试平台，没有此前的 ResourcePackInfo 加载错误。

## 重点回归

| 项目 | 已完成的检查 |
| :--- | :--- |
| 地图加载 | 地图 JAR 根目录包含有效 `pack.mcmeta`；实际客户端进入发布地图并生成设施 |
| 地图目录 | ZIP 顶层直接提供 `mods/` 与 `saves/`；七个世界的 `level.dat` 位于各自世界目录，解压一次即可复制 |
| 道具说明 | 全部新增物品具有用途和来源；检查简略提示与 Shift 详情两个分支，实际 JEI 信息页可打开 |
| 巢穴显示 | 探测器注入检查通过，客户端显示使用 R 等级；太晶属性信息保留 |
| 一次性技能盘 | 固定教授太晶爆发；直接学习成功消费一张；满招式时确认替换才消费；取消、已学会、不兼容或无盘时不消费 |
| 原有学习器 | 原生 TM171 按原有可重复使用规则完成学习，数量不减少 |
| 巢穴奖励 | R1—R6 太晶巢穴胜利奖励包含新的一次性盘 |
| NPC 对战 | 实际训练家战斗入口、原生 AI 衔接、特殊机制资格、有限药品预算、配置保存与重赛复用通过；原有等级计算保留 |
| 原有内容 | 形态、NBT/网络同步、招式提醒和物品交互回归通过 |

服务端输出：

```text
BACKPORT_FEEDBACK_PASS catalogue, one-use/permanent TM, cancellation/payment, R1-R6 rewards, NPC four mechanics/medicine/persistence
BACKPORT_LIVE_PASS forms=118 NBT/network palette cases=236 reminder entries=1468 masks=5 added move definitions=22 all ability slots=208
```

客户端输出：

```text
BACKPORT_FEEDBACK_CLIENT_PASS real JEI information pages=78 compact/Shift details; distributed map helper loaded and platform generated
```

## 测试范围

客户端验证程序在完成查询与截图后主动正常退出，并保存测试世界。这是测试程序的结束行为，发布的功能 JAR 和地图配套 JAR 不包含该自动退出程序。

Shift 详情通过调用提示生成的两个分支检查；这不等同于人工逐项按键测试。自动对战覆盖关键入口和状态分支，不代表所有队伍组合与长时间人工对战均已验证。测试员的完整 V1.5.2 整合包及其他本体版本尚未逐一复测，欢迎继续提供具体日志和复现步骤。
