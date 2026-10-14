# 代码许可与上游资源归属

本项目编写的附加模组代码、Mixin、测试代码、地图控制、构建及转换工具与项目文档按 [MIT License](LICENSE.txt) 公开，可查看、修改和再分发，需保留许可声明。

`resources/` 含来源于 Pixelmon 9.4.1 和用户提供的 `[阿宇定制ZA]太晶修复Pixelmon-9.13-1.9.jar` 的种类数据、模型、贴图、动画等资源，以及本项目的转换或兼容调整。这些上游素材保留原作者权利；MIT 不覆盖上游素材，也不改变 Pixelmon、Minecraft 或参考定制模组的原许可。修改或再次分发上游素材时应遵守其作者许可。

1.2.0 第二轮新增城市的主道馆、住宅、公共设施、街巷与地标由本项目的 `tools/city_architecture.py` 生成原创几何。原旧版本体（文件名 9.1.13，内部版本 9.1.12）仅作为尺度、拼接接口、服务设施与玩法数据的参考；原生 NPC、设备、容器内容、徽章图像及场外圆形菜单素材保留上游权利。对应基线、模板数量与实体/物资保留检查见 `docs/GYM_BASELINE_1.2.0.*` 和 `docs/GYM_ASSET_VERIFICATION_1.2.0.json`。

Minecraft、Forge、Pixelmon 旧版本体及参考定制版完整 JAR 是用户自行取得的外部依赖，不随仓库提供。CFR 也需自行取得；仓库不提供上游反编译代码。仓库中所有 Java 源码都是本附加模组、测试和地图工具的实现。

1.3.0 小智帽形态使用旧版本体皮卡丘的身体、骨架、正常/闪光贴图及待机/行走动画；帽子几何、配色与标志由 `tools/create_ash_pikachu.py` 添加，形态图标由该模型渲染。原生身体、贴图和动画保留上游权利，正常/闪光图标也含这些上游元素。

官方来源：[Pixelmon](https://reforged.gg/)、[Pixelmon Wiki](https://pixelmonmod.com/wiki/)、[Minecraft Forge](https://files.minecraftforge.net/)、[Minecraft](https://www.minecraft.net/)。本项目不是 Pixelmon 官方发布。

1.4.2 将用户提供的 `pixelmon-jei-fix` 与 `pixelmon-raid-catch-fix` 两份 MIT 补丁源码合入附加模组：分别恢复 JEI 的服务端通道判断和修正高帧率巢穴捕捉动画计时。保留来源项目的版权与许可文本，见 `resources/META-INF/licenses/JEI_CONNECTION_FIX.txt`、`resources/META-INF/licenses/RAID_CATCH_ANIMATION_FIX.txt`。不打包独立补丁模组的入口或注册额外模组ID。

1.4.1 的46份ZA Mega普通／闪光头像复用 [PokemonWorkshop/GameDataPacks](https://github.com/PokemonWorkshop/GameDataPacks/tree/d430fb553df98ccde5f9f90ece78e36b07d8083c) 的像素素材，作者为 Batin、ezerart、Luis779，源素材整理为 Aelysya。本项目只提取64×32双帧图的第一张32×32图，没有重新生成、缩放或重绘。素材按其来源的 Pokemon Studio Public License（署名、非商业、相同方式共享）保留许可，MIT代码许可不覆盖这些素材。完整归属见[随JAR打包的声明](resources/META-INF/MEGA_AVATAR_CREDITS.txt)，逐文件来源见[资源清单](resources/backport/mega_avatar_sources.json)。
