# 1.8.0 r15 验证记录（2026-10-26）

需求先记录在docs/UPDATE_1.8.0_PLAN.md第106～110项。仅本地交付，r14既有游戏功能与关闭万能熔炉更新提醒配置均保留，不发布GitHub。

## 究极空间石头

pixelmon:ultra_space_stone加入minecraft:stone_crafting_materials、minecraft:stone_tool_materials及forge:cobblestone、forge:cobblestone/normal物品标签，均replace=false。

另外在原版式有序/无序工作台配方载入时，将只指定minecraft:cobblestone的普通输入解析到原石标签，覆盖未使用材料标签的配方。仅处理输入，配方产物、烧制、含额外NBT选择条件的输入及其他材料不变。重复载入幂等。

实际原生熔炉配方接受究极空间石头，原石及黑石原有输入仍可用；原生拉杆的直接原石输入、石镐的工具材料输入均通过。未宣称修改所有第三方自定义合成引擎。

## 烧制配方

| 输入 | 设备 | 输出 | 原版时间 |
| --- | --- | --- | --- |
| 究极沙漠镀金沙 | 普通熔炉 | 水晶×1 | 200刻 |
| 究极沙漠镀金沙 | 高炉 | 水晶×1 | 100刻 |
| 究极沙漠沙 | 普通熔炉 | 玻璃×2 | 200刻 |

原生铁矿和镀金沙实际烧制分别在200、100次tile tick出货，时间相同。普通熔炉连续烧制两份究极沙漠沙，实际产出4个玻璃，未丢失第二次产物数量。高炉无匹配配方，实际输入不消耗、不出货。实际Iron Furnaces铁熔炉也通过连续两份沙产出4玻璃及镀金沙产水晶验证。

新增三份配方全部通过原生网络序列化/解码，数量和时间保持；实际JEI普通熔炉/高炉类别均检索到对应配方，玻璃产物数量为2。JEI截屏记录用于界面环境参考，不将普通合成页截图当作烧制数量的证据。

## 防尘护目镜

保留原生pixelmon:safety_goggles物品，Curios存在时注册/沿用head栏位并加入对应物品标签。通过可选API桥读取真实头部饰品库存，缺少Curios时不链接其类。

原生究极空间的雾距离判定与雾密度事件分别只读取护甲头部。补丁扩展这两处的装备读取，保留原头盔栏护目镜判定，并在无头盔栏护目镜时识别头部饰品。实际装备物品、护甲栏和宝可梦携带判定没有被替换；不添加护甲或把饰品效果扩展为宝可梦战斗免疫。

Curios 4.1.0.0实际CurioSlot允许插入护目镜，拒绝普通原石；装备后护甲值不增加，原生Curios NBT保存含该物品。服务端装备同步到真实客户端后，在究极空间通过原生FogDensity与UltraSpaceRenderInfo两条路径验证，效果等同原生护目镜头盔。卸下后客户端同步并恢复原有雾判断，无需重登。

## 玉石汉化

截图中的方块键为block.pixelmon.blue_tumblestone等，旧版外部汉化包可能覆盖早先资源中的名称。因此在既有语言加载末端修正表中补齐三项，确保资源重载后仍为中文。

- 蓝色方块：天蓝玉石原矿。
- 红色方块：玉石原矿。
- 黑色方块：黑玉石原矿。

这些原矿没有与方块同名的三份物品注册项。JEI实际材料ID为tumblestone、sky_tumblestone、black_tumblestone和对应raw_*。六种材料沿用本体正确中文名称，避免将黑玉石成品误标成原矿。所有三种原矿方块的最终翻译和六种真实物品栈名称均在启用Minecraft-Mod-Language-Modpack-Converted-1.16.5.zip的实际客户端通过。

## 构建、范围与证据

- Java11构建通过，802个生产class全部major55；不分发QA或私人自动钓鱼。
- 相对r14有7项现有JAR条目改变，18项新增（其中包含目录条目）；无删除。451项既有建筑、药剂、作者及音乐保护条目与r14逐字节相同。完整列表与核心SHA256见.cache/r15/scope-proof.json。
- 原生通过标记：R15_RECIPE_PASS、R15_FURNACE_PASS、R15_CURIOS_SERVER_PASS、R15_SERVER_PASS、R15_LANGUAGE_JEI_PASS、R15_CURIOS_CLIENT_PASS。
- 日志.cache/r15/native-pass-final.log；测试tests/Update180R15Cases.java；构建.cache/r15/build.log。
- 仅执行对应定向原生检查，未重跑无关整套战斗回归。最终本地包dist/Pixelmon-Backport-1.8.0-test-r15.zip及SHA256，封包回执.cache/update-180/validation-r15.json。

安装仍须客户端和服务端使用同一份回迁核心，仅保留一份。客户端需一并覆盖config文件夹，保持r14关闭Iron Furnaces更新提醒的设置。旧r14包保留。
