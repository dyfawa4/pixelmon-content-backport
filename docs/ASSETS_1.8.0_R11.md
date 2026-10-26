# r11 搬家协议最终贴图

最终采用内置 image_gen 生成的明显大开口精灵球，红白球壳、黑色边框、白色按钮，内部为迷你橡树与小河。源图：`C:/Users/29910/.codex/generated_images/01a16d31-51dd-7541-aa90-3e7ebe59fda4/exec-5a3b5fcc-8c1c-4830-8aee-42e4d7d463fa.png`。

最终资源`resources/assets/pixelmon_backport/textures/item/moving_agreement.png`为32×32 RGBA，保留透明通道，裁去透明边界后按比例最近邻缩放至最长边28像素并居中。库存与手持共同使用原生generated模型。最终预览`.cache/daycare-r11/final-icon-preview.png`，原生库存截图`qa-r11-inventory.png`。

最终实际提示词（内置工具模式）：

> A single transparent pixel-art inventory icon for a Minecraft creature-collecting game: a red and white spherical creature home, visibly OPEN with its red upper hemispherical lid hinged back about 65 degrees. The wide open interior contains a miniature oak tree and a little blue river. White lower hemispherical shell, thick black circular rim, large white circular front button. Compact balanced three-quarter view. Red lid in upper third, green landscape middle third, white shell lower third. Clearly an opened spherical ball, recognizable at 32x32 size. Coarse clean 24x24 logical pixel sprite with big solid color shapes, 10 colors, no dithering, no noise, no tiny decorative highlights, no text, no cast shadow. Centered with transparent margin. Lid must be raised widely, not a narrow crack.

以下保留最初候选记录，仅用于追踪；用户认为其像素密度与球形辨识不足，已被上面的最终版本替换。

内置 image_gen 编辑，替换用户明确要求重做的搬家协议贴图。保留打开精灵球、迷你橡树和小河，改为大色块与少量高光，不再绘制细碎景观。

源图：`C:/Users/29910/.codex/generated_images/01a16d31-51dd-7541-aa90-3e7ebe59fda4/exec-f8df7e1c-2d46-475d-895f-2075411ed9ad.png`。

最终文件：`resources/assets/pixelmon_backport/textures/item/moving_agreement.png`。保留生成透明通道，主体按比例最近邻采样为16逻辑像素、最长边14像素，再以整数2倍输出32×32；因此每个逻辑像素为2×2，库存和手持都使用同一原生generated模型。已查看源图及最终放大预览`.cache/daycare-r11/preview.png`。没有改动名称、描述或切换球种逻辑。

## 实际提示词

> Use case: style-transfer. Asset type: Minecraft/Pixelmon tiny inventory sprite, rework the edit target for readability at 16x16 pixels. Keep its concept: ONE open red-and-white Poké Ball, with a miniature oak tree and a short blue river inside. The existing sprite is far too densely detailed. Redesign as extremely simple classic 16x16 pixel art: large flat square color clusters, maximum about 10 solid colors total, dark charcoal outline, a single broad red lid at top, white lower bowl, the tree has ONE simple blocky green canopy and ONE brown trunk, the river is ONE broad clear cyan/blue stripe in the lower bowl. Crisp chunky silhouette, no tiny landscape, no leaves, no stones, no reflections, no gradients, no dithering, no antialiasing, no noise. Every logical pixel must be a large obvious square; the entire picture should read cleanly when reduced to just 16x16. Front three-quarter view, centered item occupying roughly 14x14 logical pixels with a small transparent margin, matches vanilla item scale. Genuine transparent background. No text, numbers, glow or extra objects. Input image is the edit target; preserve only the open-ball/tree/river concept, simplify all details aggressively.
