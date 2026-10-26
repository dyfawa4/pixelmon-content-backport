# 蚀、祸、雷之誓约维护边界（r12）

三个效果拥有独立状态和同步，不相互覆盖。r12只拆分常驻效果与显示，保留r11数值、触发、持续与完整治疗规则。

| 效果 | 状态/生命周期 | 数值 | 同步及显示 |
| --- | --- | --- | --- |
| 忍蛙的蚀 | `Erosion.java`，仅临时回合状态、刷新、到期及副本 | `ErosionMath.java`、独立erosion配置 | `ErosionNetwork.java`、`ErosionClient.java`；原蓝色蚀字图标 |
| 常驻祸 | `Calamity.java`保存与触发，`CalamityBattle.java`战斗绑定与副本，无回合到期 | `CalamityMath.java`、独立calamityReduction配置 | `CalamityPartySync.java`完整队伍缓存、`CalamityNetwork.java`战斗快照、`CalamityClient.java`；同形同字号紫色祸字图标 |
| 雷之誓约 | `LightningOath.java`，独立回合表 | 原电系相性计算保持 | 原独立网络及黄色箭头图标保持 |

`BattleStatModifiers`只负责原生数值接入与多个效果同时存在时的合并。两项基础削减须先合并到种族值，再执行原生IV、EV、等级、性格取整；不能把已经取整的面板值连续相减。单独效果的数值计算仍在各自模块。

`BattleStatusIcons`以及中性的Battle*StatusMixin只负责排列各模块自己的图标，不拥有任何效果状态、回合或计算。战斗、队伍及概要均分别显示；概要状态框内居中排列，合并原生异常状态时保留所有标识并约束在框内。`BattleHealthSync`只发布原生结算后的实际HP，不改变效果。

为兼容旧存档，祸继续读取保存键`BackportDrownedErosion`，不清除旧标签。这是保存格式兼容，祸的实现不调用蚀的类或方法。旧配置缺少calamityReduction时继承原erosionReduction值；两项分别保存、分别读取。

完整治疗入口分别调用两个模块的清除，再执行原生完整治疗和最终缓存同步。仅治疗仪、治疗指令、幸运方块宝可梦手机允许解除常驻祸；回复招式、药物、普通回血、换下场、战斗结束均不解除祸。结束战斗只解绑临时战斗对象，存档标签保留。

网络通道版本从17升至19（18为拆分检查中间版本，19补全PC恢复初始化标志），新增祸战斗包编号10，祸队伍包仍为编号9，蚀仍为编号5。双方必须替换为同一r12核心。不要把祸重新塞入蚀的状态表、计时器、报文或绘制类。

验证入口：`Update180R12StatusCases.java`对照r11保存的1152组原生快照；`Update180R12ExtraCases.java`检查原生触发、旧NBT、三种包独立性、16色床/唱片机、唱反调和原生战斗/概要显示；原生治疗仪、指令、手机六形态36数值缓存检查继续通过。
