# 1.5.0 主菜单背景验收

用户于 2026-10-17 最终要求：只换背景，原来的按钮和上方 Minecraft 标题都不变；随后确认实机截图没有问题。

- 使用用户提供的村庄截图，原图字节未改，SHA256 为 `0e9cbdb45033776d382757cbb8a6eb4610ebe01e5ca99d6aa4ad5e3b2edc0426`。
- 只重定向原主菜单的 `RenderSkybox.func_217623_a(FF)V` 背景绘制，图片等比例居中覆盖。保留 Minecraft / Java Edition 标题、闪烁文字、原按钮、图标、位置、字体、Forge 信息、Pixelmon 原菜单功能。
- 撤除开发中的自定义侧栏、按钮包装、字体纹理和整合包标题，不额外添加 1.5.0 版本标签。
- 隔离客户端 `.cache/manual-menu-150-native-r3/menu-game-result.json` 通过：1280×800、1920×1080、960×540；检查原生按钮类型，并实际打开单人、选项、语言、多人界面。
- 验收截图位于该隔离目录 `screenshots/journey-menu-{0,1,2}.png`。

本项不改变已发布正式 1.4.4。
