# 1.6.1 发布前追加：小数配置保存误报冲突

用户反馈：P 配置菜单中把“Boss 超级／超级进化 Boss 生成概率”从 0.3 改为 0.4，保存提示“配置已被其他人修改，请刷新后再保存”。此时 GitHub 发布页仍为草稿，未公开发布；本项修复直接纳入正式 1.6.1。

## 根因与修复

该原生字段 bossSpawning.bossSpawnChance 是 float。服务端把原值转为 JsonPrimitive(Float)，发送页面时序列化为 JSON 数字 0.3；客户端回传的预期旧值经 JsonParser 解析后是另一种 Number。Gson 的数值 equals 把 Float 扩展为 double 后比较，得到 0.30000001192092896 与 0.3 不等，误判并发修改。刷新后重试同一小数仍可能遇到相同问题。

现在按 JSON 可见数值的精确十进制表示比较，不以 double 容差放宽判断；数组逐项使用相同逻辑。整数、小数、科学计数法按数值比较，字符串和布尔类型不混淆，列表长度／顺序保持，真正修改后的值仍拒绝过期保存。大整数相邻值也不会因 double 精度丢失而被当作相同。

未删除并发检查，未放宽房主／管理员权限、战斗中编辑限制、字段范围或保存失败回滚；配置实际保存和缓存生效流程保留。

## 回归

使用实际原生 SpawningConfig 及 BossSpawning 字段／getter，执行真实 ConfigEditor.page → JSON 序列化与解析 → ConfigInput.parse → ConfigEditor.save。原生磁盘边界采用受控 ValueReference 替身。

- 修复前：在 0.3 → 0.4 的首次保存中重现同一 IllegalStateException；日志 .cache/feedback-161/config-float-before.log。
- 修复后：0.3 → 0.4，以及后续 0.7、0.1、小量科学计数法、0.3 连续保存；原生 getter 和保存边界均读取到新值。
- 把当前值改为 Math.nextUp(0.3f) 后，旧页面提交被拒绝，实时值和保存内容均未覆盖。
- 9007199254740992 与 9007199254740993 保持不同；数值／字符串、布尔／字符串不混淆；浮点列表往返通过，真正变化的列表仍判冲突。
- 原有整型、嵌套配置、图形即时缓存和保存失败回滚检查继续运行。

构建与完整回归日志分别为 .cache/feedback-161/config-float-build.log、.cache/feedback-161/config-float-check.log。本轮未启动游戏，实际操作页面由用户后续复测。旧 test-r3 离线包保留，不改写其哈希；正式核心包含本修复。
