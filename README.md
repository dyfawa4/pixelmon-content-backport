# 重铸内容回迁

**为 Minecraft 1.16.5 的旧版 Pixelmon 重铸补充新宝可梦、新形态和太晶玩法。**

这是一个独立的附加模组：在保留旧版重铸本体与整合包的基础上，加入新种类、ZA Mega 形态、携带道具、进化物品和中文资料，并提供太晶战斗、独立太晶巢穴及对应获取渠道。适合希望继续使用 1.16.5，同时体验更多新内容的玩家。

`正式版 1.1.0` · `Minecraft 1.16.5` · `Forge 36.2.42` · `Java 11`

**[下载最新版](https://github.com/dyfawa4/pixelmon-content-backport/releases/tag/v1.1.0)** · **[完整玩法手册](HANDBOOK.md)** · **[测试地图指南](TEST_MAPS.md)**

---

## 可以体验什么

| 内容 | 玩法 |
| :--- | :--- |
| 新宝可梦与形态 | 15 种新宝可梦、更多地区与特殊形态，接入刷新、升级、学习和存取流程 |
| ZA Mega 与进化道具 | 新增 Mega 进化石、物品进化、厄诡椪面具和花蜜等，提供相应获取方式 |
| 太晶战斗 | 太晶珠、19 种太晶属性、属性修改、太晶爆发，以及太乐巴戈斯和厄诡椪的特殊分支 |
| 独立太晶巢穴 | 不同等级的首领、护盾、战斗充能、专属碎片奖励与捕捉 |
| 特色玩法 | 永久小智甲贺忍蛙、携带无极陨星的形态变化、多丽米亚美容等 |
| 道具查询与对战 | Shift 查看详细说明、JEI 查询用途与来源，优化实力相当 NPC 的对战逻辑 |

各物品的适用条件、配方、掉落和操作步骤，见 [完整玩法与获取手册](HANDBOOK.md)。

## 下载与安装

| 下载 | 用途 |
| :--- | :--- |
| [功能包 · 1.1.0](https://github.com/dyfawa4/pixelmon-content-backport/releases/download/v1.1.0/Pixelmon-Backport-1.1.0.zip) | 模组 JAR、完整手册与安装说明 |
| [七张独立测试地图](https://github.com/dyfawa4/pixelmon-content-backport/releases/download/v1.1.0/Pixelmon-Backport-test-maps-1.1.0.zip) | 地图存档与配套控制模组，供选择性测试 |
| [独立手册](https://github.com/dyfawa4/pixelmon-content-backport/releases/download/v1.1.0/Pixelmon-Backport-Handbook-1.1.0.md) | 单独阅读或分享完整玩法说明 |

1. 准备 Minecraft **1.16.5**、Forge **36.2.42**、**Java 11** 和旧版 Pixelmon 重铸本体。已验证文件名为 9.1.13、内部版本为 9.1.12 的本体。
2. 关闭游戏与服务器，备份存档；升级时移除旧版回迁功能模组，只保留一份。
3. 解压功能包，将 `mods/pixelmon-content-backport-1.16.5-1.1.0.jar` 放入实际游戏实例的 `mods` 目录，然后启动游戏。多人游戏中，服务器和所有客户端使用相同版本。
4. 如需道具查询，安装 JEI **1.16.5 / 7.x**；如需测试地图，按 [地图指南](TEST_MAPS.md) 安装独立地图包。

功能包不附带 Pixelmon 本体；测试地图单独下载。地图 ZIP 顶层直接是 `mods/` 与 `saves/`，将 `saves` 下选定的世界文件夹复制到实例中，避免再套一层目录。

## 1.1.0 更新

修复测试地图模组的资源包加载问题与地图包安装结构，补齐道具 Shift 说明和 JEI 信息页。太晶巢穴等级显示改为 R1—R6，胜利奖励改为独立的一次性太晶爆发技能盘，原有永久盘维持原规则。优化实力相当 NPC 的对战逻辑。

[查看本次发布说明](docs/RELEASE_1.1.0.md) · [查看验证记录](docs/VALIDATION_1.1.0.md)

## 完整开源

仓库公开全部附加模组 Java 源码、Mixin、运行资源、构建脚本、转换工具、测试与七张地图初始文件。准备对应依赖后，可自行构建功能包与地图包。

[从源码构建](BUILDING.md) · [Java 源码](src/local/pixelmon/backport/) · [测试](tests/) · [资源](resources/) · [工具](tools/)

项目代码、工具、测试和文档采用 **MIT**。上游素材保留原作者权利，见 [许可与素材归属](THIRD_PARTY.md)。

## 兼容与内容边界

这是面向旧版引擎的内容适配，不是 Pixelmon 官方发行。已完成隔离 Forge 回归与 JEI 客户端检查，其他本体版本和所有整合包组合尚未逐一验证。统一太晶冠冕、研究任务、官方异步团战与完整欢呼等内容仍未包含，具体规则以手册为准。

参考素材来自官方 Pixelmon 9.4.1 与用户提供的定制 ZA 素材；本项目感谢原作者的工作。
