# r9 红绿手电筒

按用户要求重新设计为同款直筒手电筒。缩小灯为红色，放大灯为绿色，均含扩口银色灯头、浅色灯镜、彩色筒身及深色尾盖。

## 最终文件

- `resources/assets/pixelmon_backport/textures/item/shrinking_lamp.png`
- `resources/assets/pixelmon_backport/textures/item/enlarging_lamp.png`
- `resources/assets/pixelmon_backport/models/item/shrinking_lamp.json`
- `resources/assets/pixelmon_backport/models/item/enlarging_lamp.json`
- 对应的 `shrinking_lamp_icon.json`、`enlarging_lamp_icon.json`、`shrinking_lamp_handheld.json`、`enlarging_lamp_handheld.json` 位于同一模型目录。

图标用内置 image_gen 生成，透明背景。最终32×32 RGBA，28×28主体、2像素留白；最近邻裁切导出见 `tools/export_r9_flashlights.ps1`。绿色图以红色图为编辑目标，保持同款造型。源图及导出贴图均已查看。

立体模型直接使用Minecraft原生JSON元素及自带方块材质，由 `tools/build_r9_flashlight_models.py` 构建，每款22个元素。采用项目中已有的Forge分视角模型方式：背包使用像素图标，第一/第三人称左右手、掉落物与物品展示框使用立体模型。两款几何完全相同，仅筒身材质为红色/绿色。

## 实际生成提示词

红色：

> Use case: stylized-concept. Asset: 32x32 Minecraft / Pixelmon inventory sprite, a classic RED FLASHLIGHT. A recognizable ordinary straight cylindrical flashlight: broad flared round silver reflector head with a pale warm-white glass lens, long red cylindrical barrel handle with two short darker red grip bands, and a charcoal tail cap. All parts lie along ONE straight axis; absolutely no perpendicular handle. Show a tidy three-quarter view with the silver lens head at upper left and the long red handle pointing down right. Main barrel is vivid ruby red. Crisp polished pixel art on an approximately 32 by 32 logical pixel grid, deliberate square pixel clusters, 3 restrained shades per material, a thin dark colored outline and tiny highlights. Whole flashlight fills a centered near-square silhouette with small clear margin. Consistent scale, legible at small inventory size, transparent background. One flashlight only. No pistol or gun silhouette, no trigger, no beam, no glow, no particles, no text, no surface shadow, no extra objects. Simple attractive game item art, not a realistic render.

绿色编辑：

> Use case: precise-object-edit. Make the matching GREEN FLASHLIGHT for this inventory sprite. Change ONLY the red barrel and red grip bands to rich fresh emerald green with dark green shadows and pale mint highlights. Preserve exactly the ordinary straight flashlight silhouette, silver reflector, warm-white lens, charcoal tail, camera angle, every shape, pixel grid, size, placement, and transparent background. One green flashlight only. No extra objects, no text, no beam or glow. This green item and the original red item must be a perfectly matched pair.

生成源文件：`exec-6d641a4f-eab4-4b4d-b39a-366ce25ea484.png`、`exec-64f098ab-f64c-4b03-87ae-9b2ebb811af2.png`。运行时仅引用项目内最终文件，不依赖生成缓存。


## 搬家协议与变性魔药

搬家协议源图`exec-6646f4d3-abd3-4314-bb5a-7f2514dd45da.png`：打开的红白球中包含迷你橡树和蓝色小河，替换此前小床。变性魔药源图`exec-8d60959b-1a7c-4073-b515-d7588022b60d.png`：紫色瓶塞、银色瓶口及粉蓝双色药液的像素瓶。两图均由内置image_gen生成，源图已查看；透明主体保持比例、最近邻导出至32×32 RGBA，最长边28像素。最终分别为`textures/item/moving_agreement.png`及`textures/item/gender_potion.png`，模型使用项目内资源，不依赖生成缓存。
