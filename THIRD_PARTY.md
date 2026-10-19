# 代码许可与上游资源归属

本项目编写的附加模组代码、Mixin、测试代码、地图控制、构建及转换工具与项目文档按 [MIT License](LICENSE.txt) 公开，可查看、修改和再分发，需保留许可声明。

`resources/` 含来源于 Pixelmon 9.4.1 和用户提供的 `[阿宇定制ZA]太晶修复Pixelmon-9.13-1.9.jar` 的种类数据、模型、贴图、动画等资源，以及本项目的转换或兼容调整。这些上游素材保留原作者权利；MIT 不覆盖上游素材，也不改变 Pixelmon、Minecraft 或参考定制模组的原许可。修改或再次分发上游素材时应遵守其作者许可。

1.2.0 第二轮新增城市的主道馆、住宅、公共设施、街巷与地标由本项目的 `tools/city_architecture.py` 生成原创几何。原旧版本体（文件名 9.1.13，内部版本 9.1.12）仅作为尺度、拼接接口、服务设施与玩法数据的参考；原生 NPC、设备、容器内容、徽章图像及场外圆形菜单素材保留上游权利。对应基线、模板数量与实体/物资保留检查见 `docs/GYM_BASELINE_1.2.0.*` 和 `docs/GYM_ASSET_VERIFICATION_1.2.0.json`。

Minecraft、Forge、Pixelmon 旧版本体及参考定制版完整 JAR 是用户自行取得的外部依赖，不随仓库提供。CFR 也需自行取得；仓库不提供上游反编译代码。仓库中所有 Java 源码都是本附加模组、测试和地图工具的实现。

1.3.0 小智帽形态使用旧版本体皮卡丘的身体、骨架、正常/闪光贴图及待机/行走动画；帽子几何、配色与标志由 `tools/create_ash_pikachu.py` 添加，形态图标由该模型渲染。原生身体、贴图和动画保留上游权利，正常/闪光图标也含这些上游元素。

官方来源：[Pixelmon](https://reforged.gg/)、[Pixelmon Wiki](https://pixelmonmod.com/wiki/)、[Minecraft Forge](https://files.minecraftforge.net/)、[Minecraft](https://www.minecraft.net/)。本项目不是 Pixelmon 官方发布。

1.6.0 的新增 Mega 模型／动画／贴图来自用户补充的 `Pixelmon-9.13-1.9.jar`，仅导入清单列明的形态资源，不打包其代码或 Atlantis API。进化石图标复用安装的 Pixelmon 本体像素素材及配色，保留原始 PNG 字节、16×16 画布和原生留白；来源和哈希见 `resources/backport/za-mega-stone-sources.json`。32 张新增 Mega 头像沿用下述 PokemonWorkshop 素材和相同许可，记录见 `resources/backport/za-mega-avatar-sources.json`。这些上游素材均保留原作者权利，不因本项目代码使用 MIT 而改变。

太晶化外观回迁使用官方 Pixelmon 1.21.1 9.4.1（Modrinth 版本 `tY57GRtu`）的 19 种太晶冠冕。原始路径为 `assets/pixelmon/textures/hat/tera_jewel/`；保留网格坐标、法线、UV 与原始 PNG 字节，仅将静态骨架合并为旧版可稳定读取的单根骨架。逐文件来源、原始/转换后哈希和面数见 `resources/backport/tera_visual_sources.json`，转换工具为 `tools/convert_tera_visuals.py`。这些冠冕与贴图保留 Pixelmon 及原作者权利，不在本项目 MIT 代码许可范围内。身体结晶遮罩 `textures/tera/facets.png` 由本项目工具生成；渲染与同步代码为本项目实现，未打包高版本反编译代码。

2026-10-17 的材质修正进一步参考同一官方版本 `assets/pixelmon/shaders/core/terrastalization.*`、`stellar.*` 的逐三角面明暗、闪烁及边缘反光思路，并参考宝可梦朱／紫官网的太晶化外观说明。参考文件哈希与链接见 `resources/backport/tera_material_reference.json`。旧版使用本项目的固定管线三角面适配实现，没有将高版本 Java 类或无法直接运行的 ShaderInstance 着色器打入正式 JAR；相关上游参考内容仍保留原作者权利。原先生成的 `facets.png` 仍保留在资源中，当前身体渲染不再读取该 UV 图案，避免细长部位产生拉伸条纹。

1.4.2 将用户提供的 `pixelmon-jei-fix` 与 `pixelmon-raid-catch-fix` 两份 MIT 补丁源码合入附加模组：分别恢复 JEI 的服务端通道判断和修正高帧率巢穴捕捉动画计时。保留来源项目的版权与许可文本，见 `resources/META-INF/licenses/JEI_CONNECTION_FIX.txt`、`resources/META-INF/licenses/RAID_CATCH_ANIMATION_FIX.txt`。不打包独立补丁模组的入口或注册额外模组ID。

1.4.1 的46份ZA Mega普通／闪光头像复用 [PokemonWorkshop/GameDataPacks](https://github.com/PokemonWorkshop/GameDataPacks/tree/d430fb553df98ccde5f9f90ece78e36b07d8083c) 的像素素材，作者为 Batin、ezerart、Luis779，源素材整理为 Aelysya。本项目只提取64×32双帧图的第一张32×32图，没有重新生成、缩放或重绘。素材按其来源的 Pokemon Studio Public License（署名、非商业、相同方式共享）保留许可，MIT代码许可不覆盖这些素材。完整归属见[随JAR打包的声明](resources/META-INF/MEGA_AVATAR_CREDITS.txt)，逐文件来源见[资源清单](resources/backport/mega_avatar_sources.json)。


1.5.0 主菜单背景由用户提供并指定使用，为 Minecraft 村庄与戴帽皮卡丘截图；原画面中的 Minecraft／Pixelmon 等上游素材权利保持不变。文件为 `resources/assets/pixelmon_backport/textures/gui/journey_background.png`。

2026-10-19 根据用户实机反馈，撤下 Navas 方块风格模型／动画／贴图，恢复用户提供的 Pixelmon 旧模型和普通／闪光材质；包内不再分发这些 Navas 视觉资源。变体仍沿用既有 PokemonWorkshop 像素头像及相应署名许可。当前引用及哈希见 `resources/backport/za-variant-assets.json`，恢复边界见 [旧模型恢复记录](docs/旧模型恢复与测试场补齐_2026-10-19.md)。

本次平滑变体基于原用户提供模型派生：米立龙只调整原鱼体与匹配骨架的排列，玛机雅娜只调整材质坐标以取用原图集已有颜色；原 PNG 字节不改。属于原模型的适配衍生，保留原上游权利，未新增方块模型资源。派生文件及哈希见 `resources/backport/za-variant-assets.json`。


1.6.1 避难背包图标直接复用用户提供 Pixelmon-9.13-1.9.jar 内原始 eject_pack.png，未重绘。来源与 SHA-256 见 resources/backport/eject-pack-source.json，素材保留上游权利。展示框引用 Minecraft 内置物品图标，无额外分发该原版图片。刷新检查器当前为下述自制图标。

1.6.1 刷新检查器图标为内置 image_gen 生成的项目专用像素素材，非借用第三方模组图标；生成提示词与32×32整理记录见 resources/backport/spawn-checker-icons-source.json。立体模型为本项目原生 JSON 几何，金属／混凝土材质引用游戏自带资源。

1.6.1 精灵球物品外观迁移用户提供的 `Pixelmon-9.13-1.9.jar` 中 76 张不同的物品图标、球盖、球盘和底座 PNG，保留原始字节；这些上游素材保留原作者权利，不由本项目 MIT 重新授权。逐文件 SHA-256 和来源 JAR 哈希见 `resources/backport/pokeball-item-art-source.json`。原生三维精灵球模型与该参考一致，没有重复复制。

1.6.1 为 Farmer's Delight 1.16.5-0.6.0 增加本项目编写的中文漏译及配置名称／说明。原模组为 vectorwing 的 Farmer's Delight，原模组许可为 MIT；未打包其 Java 类或完整 JAR，原有中文继续由其资源提供。

1.6.1 修订 2 为保证实际材质读取，额外将同一参考模组的全部 85 张精灵球相关 PNG 原字节置于回迁命名空间，并提供本项目的材质 getter 及球盘／底座模型路由。上游素材权利保持不变，全部哈希见上述来源记录；未复制上游 Java 模型实现。
