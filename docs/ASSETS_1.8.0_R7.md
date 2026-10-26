# 1.8.0 r7 物品像素素材

使用内置 image_gen 工具，透明背景；按现有银白色灯的像素图编辑，不使用外部 API。最终素材均已复制进模组，不依赖生成缓存。

- `resources/assets/pixelmon_backport/textures/item/shrinking_lamp.png`
- `resources/assets/pixelmon_backport/textures/item/enlarging_lamp.png`
- `resources/assets/pixelmon_backport/textures/item/moving_agreement.png`

三图均为32×32 RGBA。两灯统一28×28主体和2像素留白，银白灯头、深色握柄与红/绿灯面保持同一轮廓。搬家协议按比例缩放至最长边28像素。标准裁切和最近邻导出脚本：`tools/export_r7_assets.ps1`。模型沿用原生 `minecraft:item/generated`。

提示词：

1. Shrinking lamp: “Refine this exact silver-white cuboid shrinking lamp, a simple box-shaped silver lamp head with a flat coral-pink square lens on the left-facing front and a short black rectangular grip hanging below the rear. Preserve this modest blocky handheld lamp design, not a gun. Make the head about 70 percent of silhouette height, visible grip 30 percent. Clear dark gray outline, three crisp silver shading levels, white top highlight, saturated pink lens inset with a one-pixel pale highlight. Three-quarter isometric angle, large readable silhouette. Strict low resolution square pixel art, no blur, no glow, no text, no floor or shadow, transparent background. One lamp centered with small consistent margin.”
2. Enlarging lamp, edited from the first output: “Change ONLY the pink/red lens on the front of this exact lamp to bright lime green, with its one pale yellow-green highlight. Preserve exactly the silhouette, head geometry, black grip, silver top highlight, outline, pixel grid, camera angle, size and placement. Transparent background preserved. Do not redesign, add text, add glow, or move anything.”
3. Moving agreement: “One opened classic red-and-white Pokeball with red hemispherical lid tilted open on a back hinge, white bottom bowl, thick dark band and small central round button, pale empty interior clearly visible. The open lid and bottom are connected and form one compact readable silhouette, three-quarter view. Crisp chunky square pixels with dark outlines and 3 shades per color, matching Minecraft Pixelmon inventory icons. Red, white, cool gray and charcoal palette. No particles, no Pokemon, no letters, no background, no ground shadow. Transparent background, centered, near-square silhouette with small margin.”

生成源文件：`exec-0ad70130-709a-4254-b11b-e63f0175672f.png`、`exec-3ccb1f34-f418-4025-9ed8-f632e4c92268.png`、`exec-97aeeddc-dcd0-4690-9497-c85e5505e4a5.png`。所选源图均已查看，导出32像素成品也已查看。
