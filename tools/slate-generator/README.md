# 赛菲莉娅 石板生成器（`tools/slate-generator/`）

给「赛菲莉娅幻想」做石板的图形工具：在 9×9 画布上摆出图案、给格子定等级，填好名字 / 品质 /
是否可旋转，按「制作」记一条 key；做完一批点「复制」，把 key 整段发给助手（AI），
由助手生成模组里的 Java 图案、贴图与语言文件。

## 怎么用（用户）

1. 双击 `run.bat`（等价于 `powershell -NoProfile -ExecutionPolicy Bypass -File tools\slate-generator\slate-generator.ps1`）。
2. 最上面一行：**石板名**（中文名）、**图标…**（选 png / jpg / bmp，右边有预览）。
   ⚠ **没选图标不能「制作」**——防的是一块没有图标的石板。
3. 第二 / 三 / 四行：**品质**（白 / 蓝 / 黄 / 红 / 粉 = 普通 / 高级 / 稀有 / 传说 / 永恒）、
   **说明（自动）**（只读，见下）、**是否可以旋转**（是 = 绿、否 = 红；游戏里鼠标放上去按 R）。
4. 中间 **9×9 画布**（石板在正中间）：

   | 操作 | 效果 |
   | --- | --- |
   | 左键点格子 | 选中 / 取消（白框）——只有选中的格子能改等级 |
   | 拖石板那一格 | 移动整张图案（石板格是把手，不能选中） |
   | 右侧 `+` / `-` | 所有选中格的等级 +1 / −1；**归零 = 这一格取消** |
   | 「旋转 90°」 | 逆时针转（与游戏里按 R 同一个方向），**等级跟着格子一起转** |
   | 「清空选择」 | 取消所有选中 |

   格子里显示的就是等级：绿色 `+1`、红色 `-1`……这些数字是相对石板的偏移
   （**左右** dx 右为正、**上下** dy 下为正），游戏里石板影响的就是这些格子。
5. 按「制作」：这一块被记下来（**图标会被复制进 `inbox/`**），下方 key 区域多一条；
   「清空（做下一块）」把表单清干净做下一块。
6. 一批做完，点 **「复制」**，把 key 整段发给助手。

> **说明（自动）**：描述不再是作者写的——游戏里按图案自动生成那几行，界面上只是照出来给你看：
> 图案里有**加**等级的格子 →「提高神器等级」（绿）；有**减**等级的格子 →「降低神器等级」（红）；
> **不可旋转** →「不可旋转」（红）。三句可以同时出现，用「，」隔开。

> 提醒：游戏里的赛菲莉娅背包只有 **6 列宽**（默认 5 行，最多 9 行）。
> 图案伸得太远，放在背包边缘时会被裁掉——「制作」时状态栏会提醒。
> 石板**不影响自己那一格**（游戏机制如此），所以石板格画不了。

## key 的格式（助手按这个解析）

```
=== SLATE v2 ===
name: 未来
rarity: advanced
rotatable: true
icon: inbox/20261018-1712_未来.png
turns: 1
anchor: 4,4
cells: 0,-1,1 -1,-1,1 1,-1,1 -1,0,1
=== END ===
```

| 字段 | 含义 |
| --- | --- |
| `name` | 中文名 |
| `rarity` | `common` / `advanced` / `rare` / `legendary` / `eternal`（= 白/蓝/黄/红/粉） |
| `rotatable` | `true` / `false`（游戏里能不能按 R 转） |
| `icon` | `inbox/` 下的相对路径（界面上没图标不让制作，所以这里不会是 `-`） |
| `turns` / `anchor` | 记录用：转过几次、画布上的位置——**生成时不看它们** |
| `cells` | 效果：`dx,dy,delta` 三连，空格分隔；dx 右为正、dy 下为正、delta 可负 |

> 旧的 `v1` key 带一行 `desc:`（作者手写描述）——仍然认，会照旧写进风味行；
> `v2` 没有这一行（描述由游戏自动生成）。

## 助手的流程（key → 模组）

```powershell
# 1) 把用户发来的 key 存成 tools\slate-generator\inbox\keys.txt，然后：
powershell -NoProfile -ExecutionPolicy Bypass -File tools\slate-generator\parse-keys.ps1

# 2) 打开 tools\slate-generator\out\slates.json，补上 id / nameEn
#    （id 用 lower_snake_case，跟现有石板一致：slate_of_future、slate_of_wave……）
#    旧 v1 key 带描述的，还要补 descEn

# 3) 一键落到模组里（下面每一处都是幂等的：已存在就跳过）
powershell -NoProfile -ExecutionPolicy Bypass -File tools\slate-generator\apply-slates.ps1

# 4) 老规矩：编译 + 校验，然后实装
gradlew.bat compileJava
gradlew.bat modelDecodeCheck
powershell -NoProfile -ExecutionPolicy Bypass -File tools\check-resources.ps1
powershell -NoProfile -ExecutionPolicy Bypass -File tools\deploy.ps1
```

`apply-slates.ps1` 会写这些地方：

| 位置 | 写什么 |
| --- | --- |
| `slate/Slates.java` | 图案常量（`Cell` 列表） |
| `registry/ModItems.java` | 物品 + `registerSlate(...)` |
| `registry/ModCreativeTabs.java` | 石板创造页 |
| `artifact/ArtifactLoot.java` | 石板掉落池（宝箱 / 挖矿 / 许愿池都读它） |
| `lang/zh_cn.json` + `en_us.json` | 名字与描述 |
| `items/<id>.json` + `models/item/<id>.json` | 物品模型定义 |
| `tools/check-resources.ps1` | 校验清单里的 id |
| `tools/import-artifact-icons.ps1` | 图标任务（`slates` 根 = `tools/slate-generator/inbox`） |

图标会自动跑一遍切图（与其它神器同一套：抠底板 → 裁主体 → 16×16），
原图留在 `inbox/` 里（先复制成 `<id>.png` 再切）。

## 给改这个工具的助手（约定）

- `slate-generator.ps1`（界面）是**带 BOM 的 UTF-8**——PS 5.1 否则按 GBK 读，界面中文全乱。
  用编辑器改完记得确认 BOM 还在（`[IO.File]::ReadAllBytes($p)[0..2]` 是 `239 187 191`）。
- `parse-keys.ps1` / `apply-slates.ps1` 是 **ASCII-only**：中文文案只从
  `out/slates.json` 与 `labels.zh.json`（显式按 UTF-8 读）里来，脚本里不写中文。
- **数组字面量里不要直接拼接**：`@('a' + $x + 'b', …)` 在 PS 5.1 里会被拆成多个元素
  （记忆手册 §三 有这条坑）——要写成 `@(('a' + $x + 'b'), …)`。
- `apply-slates.ps1` 会**保留每个被改写文件的 BOM**：`check-resources.ps1` 有 BOM，
  丢了它再跑，PS 会把中文注释按 GBK 读、连带吃掉下一行代码（2026-10-18 踩过）。
