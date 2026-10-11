# 从完整源码构建

1.2.0 本地验收源码包含全部附加模组 Java 源码、运行资源、测试源码、转换工具，以及九张功能地图、十八张城市地图和徽章展示的初始文件。`resources/` 已是转换后的可构建资源，普通构建不必重新下载或转换上游模组。

## 环境与外部依赖

- Windows、PowerShell 5.1 或 7、**JDK 11**（JRE 不含 `javac`，不能代替 JDK）。
- 已安装 Minecraft 1.16.5 / Forge **36.2.42** 的客户端依赖目录 `libraries`。构建直接使用旧版引擎的 SRG 类名；需要安装时生成的 `forge-1.16.5-36.2.42-client.jar` 和相应 Minecraft/Forge 依赖，单独的 Forge universal JAR 不够。
- 自行取得 Pixelmon 1.16.5 9.1.12 / 9.1.13 本体。已验证文件名为 9.1.13、内部版本为 9.1.12 的版本。本体、Minecraft、Forge 与定制 ZA 完整 JAR 不随本仓库发布。
- JEI 1.16.5 **7.x** 的 JAR 用作编译 API；运行时可选。不在本体旁时传 `-JeiJar` 指定。
- Python **3.11 或以上**用于工具、编译 API 预处理和隔离测试。普通构建不依赖额外 Python 库；重新生成 NBT 素材时使用 `requirements-tools.txt`。

将下列路径替换为自己的安装位置，不能照搬示例。所有构建产物写入本仓库的 `.cache/` 和 `dist/`。

```powershell
git clone https://github.com/dyfawa4/pixelmon-content-backport.git
cd pixelmon-content-backport

.\build.ps1 `
  -JdkDirectory 'C:\Java\jdk-11' `
  -LibrariesDirectory 'D:\Minecraft\.minecraft\libraries' `
  -PixelmonJar 'D:\Minecraft\mods\Pixelmon-1.16.5-9.1.13-universal.jar'
```

脚本查找 Forge client JAR；存在多份时，加 `-ForgeClientJar '完整路径\forge-1.16.5-36.2.42-client.jar'` 指定。若 PowerShell 执行策略阻止脚本，可在此次调用中使用 `powershell -ExecutionPolicy Bypass -File .\build.ps1 ...`，无需更改系统策略。

产物是 `dist/pixelmon-content-backport-1.16.5-1.2.0.jar`。脚本用 `javac --release 11` 编译全部 `src/**/*.java`，再原样打包 `resources/`。资源文件保持原始字节。JAR 时间戳与压缩封装可能造成文件 SHA-256 不同，应对比内部 class / 资源内容；这不代表 Java 版本或功能不同。

## 源码与目录

| 路径 | 内容 |
|---|---|
| `src/local/pixelmon/backport/` | 注册、道具、特性、招式、小智形态、太晶、巢穴、网络和客户端界面 |
| `src/local/pixelmon/backport/mixin/` | 旧版 Pixelmon / 兼容模组的注入实现 |
| `resources/` | 模组元数据、Mixin 配置、语言、数据、模型、贴图和动画 |
| `tests/` | 全部自动验收、隔离客户端、测试地图控制和准备工具的 Java 源码 |
| `tools/` | 上游比较、资源转换、结构校验及隔离测试启动工具 |
| `test-maps/saves/` | 九张功能地图的干净初始文件 |
| `test-maps/gym-saves/` | 十八张完整城市及徽章展示世界的干净初始文件 |
| `test-maps/resources/` | 地图控制模组的元数据 |
| `docs/history/` | 初期开发记录；不是当前版本的验收结论 |

源码均为本附加模组和开发工具的实现，未包含反编译的 Minecraft / Pixelmon 本体代码。完整代码采用 MIT；引用或转换的上游资源归属见 [THIRD_PARTY.md](THIRD_PARTY.md)。

## 离线类转换验证

先构建，再运行：

```powershell
.\check.ps1 `
  -PixelmonJar 'D:\Minecraft\mods\Pixelmon-1.16.5-9.1.13-universal.jar' `
  -LibrariesDirectory 'D:\Minecraft\.minecraft\libraries'
```

此步骤编译测试源码，检查 Mixin 对原生类的应用。若 Pixelmon 本体所在文件夹有 `PixelmonDetector-*.jar`，也会验证其兼容注入；没有该可选模组时跳过对应分支。离线检查不代替真实 Forge 运行测试。

## 资源结构验证与转换

```powershell
python -m pip install -r requirements-tools.txt
python tools/validate_assets.py
```

Pillow、NumPy 仅用于转换 / 校验工具。已经提交的 `resources/` 可直接参与 Java 构建。重新生成资源属于开发操作，会覆盖该目录，建议在独立分支操作；正式版还包含人工调整，不应将运行一次转换器视为完整复现所有调整。

主要工具入口：

```powershell
python tools/download_upstream.py
python tools/audit_custom.py --old '旧版.jar' --upstream '9.4.1.jar' --reference '定制参考.jar'
python tools/audit_completeness.py --old '旧版.jar' --upstream '9.4.1.jar' --reference '定制参考.jar'
python tools/convert_content.py --old '旧版.jar' --upstream '9.4.1.jar' --reference '定制参考.jar'
python tools/extend_reference.py --old '旧版.jar' --reference '定制参考.jar'
python tools/extend_gameplay.py --old '旧版.jar' --upstream '9.4.1.jar'
python tools/inspect_visual_assets.py --old '旧版.jar' --reference '定制参考.jar'
```

`download_upstream.py` 从 Modrinth 获取官方 9.4.1，下载到 `.cache/` 并核验 SHA-512；它不是日常构建步骤。`extend_tera_raids.py` 默认读取该缓存。`inspect_classes.py` 需要自行在 `.cache/cfr.jar` 放置 CFR，仅供本地研究；反编译产物留在忽略的 `.cache/`，不属于仓库源码。

## 地图配套模组与全部地图打包

先运行 `build.ps1`，再运行：

```powershell
.\build-test-maps.ps1
```

此脚本编译 `tests/MapSupport.java` 和八个地图辅助源码，打包地图控制模组，生成九张功能地图包 `dist/Pixelmon-Backport-test-maps-1.2.0.zip` 和十九张城市/徽章地图包 `dist/Pixelmon-Backport-gym-test-maps-1.2.0.zip`。两包使用同一份地图控制模组，只安装一份。控制模组按世界名称识别测试地图，正式功能模组不包含自动 QA 或自动退出代码。实际使用步骤见 [TEST_MAPS.md](TEST_MAPS.md)。

如需从自己的新建超平坦空白世界生成地图，`PrepareTestMaps` 接收两个位置参数：原始世界文件夹、输出 saves 文件夹。它复制世界并写入七个测试世界名称；进入地图后由控制模组准备设施。请使用空白世界，勿使用个人存档作模板。

## 隔离 Forge 服务器回归

自行在**独立测试目录**安装 Forge 36.2.42 服务器，生成服务器依赖；阅读并接受 Minecraft EULA 后创建 `eula.txt`。准备 `server.properties`，例如 `level-name=backport-test`、`level-type=flat`、`online-mode=false`、`server-ip=127.0.0.1` 和一个空闲端口。只在本地测试环境使用离线模式。

目录需包含 `forge-1.16.5-36.2.42.jar`、对应 Minecraft 服务端和 `libraries/`。确认 `mods/` 中没有其他版本的本附加模组或重复 Pixelmon 本体，再运行：

```powershell
.\live-check.ps1 `
  -ServerDirectory 'D:\Backport-Test-Server' `
  -PixelmonJar 'D:\Minecraft\mods\Pixelmon-1.16.5-9.1.13-universal.jar'
```

脚本编译并安装开发验收模组、功能模组与指定本体，启动服务器，等待 `BACKPORT_LIVE_PASS` / `BACKPORT_LIVE_FAIL`，最后发送正常停止指令。日志在 `.cache/live-test.log`；默认等待 180 秒。测试会创建和修改隔离世界，不要传入日常服务器目录。

客户端测试入口是 `tools/run_test_client.py --version-json ... --minecraft-root ... --pixelmon ...`，需要先准备 `.cache/test-client/mods/backport-client-test.jar`（用全部测试 class 和 `tests/client-resources/` 打包），以及隔离服生成的 `backport-test` 世界。它只使用测试用户名和占位 token `0`；源代码不含任何登录账号。`--full-pack` 复制启用的模组和配置到独立测试实例，用于整合包兼容验收。

功能包和校验文件打包：完成两次构建后运行 `python tools/package_release.py --version 1.2.0`。

`FeedbackClientVerification` 是独立的测试客户端入口，使用 `tests/feedback-client-resources/` 的元数据打包，配合 JEI 与正式地图配套模组打开隔离地图并逐项查询信息页。测试结束截图后主动正常关闭客户端；不要安装到日常游戏中。


1.2.0 另有离线资源检查：`tools/verify_gym_city_assets.py --pixelmon <本体.jar>`、`tools/verify_city_structure.py`、`tools/verify_gym_rewards.py`、`tools/verify_npc_dialogue.py --pixelmon <本体.jar>` 和 `tools/verify_release_package.py`。`check.ps1` 会对本体同目录的 Explorer’s Compass 做可选兼容检查。当前工作分工不运行自动游戏验收，旧 `live-check.ps1` 仅保留为开发工具。
