> 2026-10-19：平滑变体外观已经用户实机验收。来源关联与资源检查通过；全体 Mega 运行时数值及变回组合尚未穷举。最新外观记录见 [平滑 Mega 补齐](平滑Mega变体外观补齐_2026-10-19.md)。

# ZA 超级进化补全记录（1.6.0）

最初提供的定制 JAR 只有 26 组新增 Mega，之前没有漏导。后补的 `Pixelmon-9.13-1.9.jar` 实际包含以下 16 组缺失模型，本次已接入独立形态、普通／闪光资源、16 枚进化石及独立野外 Mega Boss 表。Atlantis JAR 是 API，不含所需模型，未安装或打包。

| 编号 | 已接入形态 | 进化石 ID |
| --- | --- | --- |
| 358 | Chimecho / mega | chimechite |
| 359 | Absol / megaz | absolite_z |
| 398 | Staraptor / mega | staraptite |
| 445 | Garchomp / megaz | garchompite_z |
| 448 | Lucario / megaz | lucarionite_z |
| 485 | Heatran / mega | heatranite |
| 623 | Golurk / mega | golurkite |
| 678 | Meowstic / mega | meowsticite |
| 740 | Crabominable / mega | crabominite |
| 768 | Golisopod / mega | golisopodite |
| 801 | Magearna / mega | magearnite |
| 807 | Zeraora / mega | zeraorite |
| 952 | Scovillain / mega | scovillainite |
| 970 | Glimmora / mega | glimmorite |
| 978 | Tatsugiri / mega | tatsugirinite |
| 998 | Baxcalibur / mega | baxcaliburite |

新模型来源与哈希见 `resources/backport/za-mega-import.json`。头像采用 PokemonWorkshop/GameDataPacks 的原版尺寸像素图，保留原作者许可。用户指出首版新增进化石满格、风格不符后，已改为复用 Pixelmon 本体的 16×16 进化石像素素材及配色，保持原生 12×12 可见范围；没有缩放或平滑图片，部分新石复用已有石的配色图案。记录见 `za-mega-stone-sources.json`。

特性依据[官方日文图鉴](https://zukan.pokemon.co.jp/)逐形态核对。已公开的 42 项映射（含三个旧 Mega）接入；本体缺少的七个特性采用独立兼容实现。官方尚未公布的席多蓝恩、达克莱伊、基格尔德、玛机雅娜、捷拉奥拉及米立龙 Mega 特性保留参考兼容值，不称作官方确认。原始页面与数据见 `ZA_MEGA_OFFICIAL_DATA_1.6.0.json`。

以 [Pokémon Showdown 数据实现](https://github.com/smogon/pokemon-showdown/blob/master/data/pokedex.ts)交叉核对种族值，纠正七项源数据错误，包括风铃铃／姆克鹰对调。差异记录见 `ZA_MEGA_STAT_CORRECTIONS_1.6.0.json`。不导入源包的全局机制、技能表或虚构物种。

原生注册／生成表／实际 Boss 掉落检查通过：`ZA_MEGA_NATIVE_PASS forms=16`。每个已变身 Boss 仅关联自己的石与自己的 Mega 形态，旧 Mega 与 Z 形态分开，原生 Boss 身份和禁止捕捉入口保留。小智忍蛙通过保留来源的 Mega 形态获得 Protean，继续保留三击、PP 5、蚀、忍者的修养及太晶爆发回忆；普通 Mega 忍蛙不获得小智专属强化。

用户指出忍蛙 Mega 水手里剑过亮、像纸片后，仅调整 336 个水刃三角面厚度、法线与原有蓝色材质坐标，并关闭叠加自发光。5630 个身体三角面、骨架、蒙皮和动画逐项保持，未编辑原 PNG 或改变战斗数据。

## 2026-10-19 用户实机反馈后的恢复

用户确认保留旧模型。已撤下 Navas 方块风格模型，恢复原参考包平滑网格、骨架、动画与普通／闪光材质，保留三种米立龙和两种玛机雅娜的独立来源关联及头像；不再声称独立差异网格和原始颜色专用 Mega 配色已完成。非 Mega 原始颜色玛机雅娜也加入测试场。测试场扩大到 60 个展示位，覆盖本清单全部 49 条目。详见 [恢复与测试场记录](旧模型恢复与测试场补齐_2026-10-19.md)。
