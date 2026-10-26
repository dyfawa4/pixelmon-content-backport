# 1.8.0 r8 贴图重设计

使用内置 image_gen，不使用外部API。三张所选源图及最终游戏贴图均已查看。

## 最终路径

- `resources/assets/pixelmon_backport/textures/item/shrinking_lamp.png`
- `resources/assets/pixelmon_backport/textures/item/enlarging_lamp.png`
- `resources/assets/pixelmon_backport/textures/item/moving_agreement.png`
- 放大预览：`docs/previews/r8-items.png`

三张均为32×32 RGBA透明贴图，继续使用原生生成型物品模型。两灯先按16×16逻辑像素导出（14像素主体、1像素边距），再最近邻放大到32像素，保持原生粗像素感及一致大小。协议保持32像素并按比例适配，不改变功能。标准裁切/缩放脚本：`tools/export_r8_assets.ps1`。

## 风格参考

分别解析用户提供的9.4.2及当前1.16 Pixelmon模组中 `models/item/ability_capsule.json`、`ability_patch.json`，得到真实贴图名 `abilitycapsule.png`、`abilitypatch.png`，而非猜测带下划线的贴图名。两版本16×16原图逐字节相同。

- 特性胶囊SHA256：`f75d8358213078101f6609bdf55ada9ffff1824dbb53e43039032201bfcac619`
- 特性膏药SHA256：`a355f85613462d0548252c49a8e7ecd79e29754937ca242f319e87c1f9ffcc55`

## 实际提示词

缩小灯（两张原生贴图仅作为风格参考）：

> Use case: stylized-concept. Asset: one Minecraft Pixelmon item sprite. Images 1 and 2 are STYLE REFERENCES only: the native 16x16 ability capsule and ability patch. Design a SHRINKING LAMP in exactly their very simple, coarse square-pixel visual language: about 14 by 14 logical pixels total, limited palette, big solid color clusters, one tiny white highlight, cool blue-gray colored outline, no black border. The subject is a compact handheld lamp, silver-white rounded rectangular head with a clearly visible coral pink lens at the front, short dark blue-gray rectangular grip below the rear, slight diagonal pose. Keep it like a small friendly Pokemon utility gadget, readable in a 16x16 inventory cell. Flat sprite with minimal three-quarter shading similar to the capsule and patch; only 2-3 shades per material. The pink lens should be a simple solid inset circle/square shape. Pixel edges must be perfectly hard. Transparent empty background. One isolated lamp only. No gun barrel, no trigger, no screen, no text, no sparkle, no beam, no soft light, no gradient, no detailed 3D rendering.

放大灯（使用缩小灯输出为编辑目标）：

> Use case: precise-object-edit. Produce the matching ENLARGING LAMP sprite. Change ONLY the coral pink lens of this exact coarse-pixel lamp to bright fresh green, with a pale mint highlight. Keep every other part identical: silver-white rounded head, cool blue-gray colored outline, blue-gray handle, simple color clusters, exact pixel grid and silhouette, same size and placement, same angle. It must match the Pixelmon ability capsule / ability patch 16x16 sprite style. No added details, no black outline, no soft glow, no beam, no words, no gradient. Preserve transparent background.

搬家协议（以r7图为编辑目标）：

> Use case: precise-object-edit. Edit this Minecraft Pixelmon 32x32 inventory sprite. Preserve the opened classic red-and-white Pokeball, hinged red upper lid, white lower bowl, dark rim, centered round button, pixel-art style and transparent background. Add a clearly recognizable miniature BED INSIDE the lower half of the ball: one pale cream pillow at the back, a warm turquoise blanket on a rectangular mattress, two tiny brown wooden bed feet. Tilt the view slightly more downward if needed so the miniature bed is readable within the ball, with the front rim low enough not to hide it. The bed must be safely nested inside the Pokeball, not floating outside or beside it. One compact coherent inventory icon, chunky crisp square pixels, few colors, no text, no particles, no glow, no background or ground shadow. Make the bed visible as a few simple colored shapes at small inventory size, with no extra furniture.

生成源图：`exec-bc9d9923-7567-4def-85bc-cf74273bfb9c.png`、`exec-9728e4f8-9675-48ae-a912-30de9940f212.png`、`exec-4547b0cd-8f42-451f-8863-6b70b45fc33e.png`。实际使用的是上述最终项目路径，运行时不依赖生成缓存。
