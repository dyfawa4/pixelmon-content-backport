# 玛机雅娜林地府邸兼容检查

日期：2026-10-15。按用户要求改为结构判定刷新，避免黑森林限定漏掉其他群系的府邸，也避免把玩家自建房屋误判为府邸。

## 实现与保留条件

实际本体`data/pixelmon/spawning/legendaries/magearna.set.json`只有`roofed`群系、白天、室内、白桦木板等条件，没有明确要求真实府邸。本版在原生刷新数据加载时，只将该条目的群系限制替换为`structures: ["mansion"]`，其余原生字段完整保留。服务器若已移除该条目，不重新添加；重载不创建重复刷新池。

玛机雅娜的结构判断与智皮卡村庄判断共用原生SpawnCondition入口，只有801号物种的府邸查询改为真实StructureManager。使用`Structure.field_236368_d_`、跨区块引用、有效起始记录和`pieces=true`片段范围。不是按建筑外观、地面材料或单个起始区块缓存猜测。普通自建房屋没有府邸结构记录，不能通过；府邸外也不满足。

白天、室内、白桦木板地面、50级、legendary刷新计时和稀有度0.3保留。不是每座府邸固定生成一只，不提高刷新概率。当前补丁不修改府邸本身的生成位置或其他宝可梦规则。

## 当前整合包的超多生物群系

实际JAR：BiomesOPlenty 1.16.5-13.1.0.488，SHA256为`2806139055dc330a1e8f7a4324a68b6305a9eb67cc132ef09b81bdec5e89d9bb`。

扫描80个BOP群系模板，核对ModBiomes登记、configureGeneration调用、原版配置结构到结构类型的映射。以下5个模板直接添加府邸，且均在当前群系配置中存在、权重非零：

| 群系 | 注册ID | 府邸结构 |
| --- | --- | --- |
| 针叶林 | `biomesoplenty:coniferous_forest` | `minecraft:mansion` |
| 垂死树森林 | `biomesoplenty:dead_forest` | `minecraft:mansion` |
| 不祥树林 | `biomesoplenty:ominous_woods` | `minecraft:mansion` |
| 季节性森林 | `biomesoplenty:seasonal_forest` | `minecraft:mansion` |
| 时令果园 | `biomesoplenty:seasonal_orchard` | `minecraft:mansion` |

五个模板都调用原版`StructureFeatures.field_244138_d`并添加到群系生成配置；该字段实际登记名称为mansion，指向`Structure.field_236368_d_`。因此无需为这5个群系分别硬编码府邸ID。原版府邸不同房间与朝向也使用同一结构记录。

审计脚本：`tools/audit_bop_mansions.py`。报告：`.cache/bop-mansion-registry.json`。脚本只读取真实JAR与配置，不初始化或写入游戏世界。

## 验证范围

离线回归读取本体所有刷新JSON，经生产加载入口处理后核对只有目标玛机雅娜规则变化；保留字段、其他物种、重载幂等、缺失条目和外部命名空间均检查。真实Mixin变换确认自然刷新与原生传说查询共用同一结构入口。隔离世界服务运行生产结构辅助函数，覆盖真实府邸片段、未登记自建房屋、片段外、空位置及未加载区块。

上述为字节码、配置与离线逻辑验证，未声称已逐一自然生成这5种群系府邸并等候传说刷新。独立府邸结构模组、未来BOP版本及管理员自行改写的结构元数据，需要按实际注册重新核对。
