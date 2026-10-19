内容版：许愿池与根之退换成**全新的方块模型**与贴图；新的模组图标；模组名 / 简介在英文界面下会显示英文；新属性「**幸运值**」与第一件给它的神器「**六叶草**」。

## 新增

- **许愿池新模型**（Blockbench 手工建模，30 个元素）：石台与四根角柱、池底水面、键帽状的喷口与四面漫下来的水帘、池面的泡沫环，以及两片十字交叉的四叶草（原版 `cross` 的做法，只对两片平面做 alpha 镂空）；石材 / 水 / 水流 / 泡沫 / 四叶草贴图全部重画。
- **根之退新模型**（26 个元素）：矮树桩底座（根系在底部铺开、正面的根指托住书底）、**像讲台一样斜立的书**（白书页包着封面、封面上有浅色文字块）、书后一根横向根梁与两根斜向上的根枝。石材直接引用原版 `stone` / `cobblestone` / `smooth_stone`，书页与封面自绘。
- **模组图标换新**：换成用户新画的像素图标（96×96）。
- **模组名 / 简介 / 描述按语言显示**：英文界面下模组列表显示 **Sephiria Fantasy**（此前无论什么语言都是中文名）。
- **新属性「幸运值」**：提高**稀有及以上**（稀有 / 传说 / 羁绊 / 永恒）物品的出现概率——**商店货架、宝箱奖励、抽卡**三处都吃。换算 `y = 50×(1−e^(−0.0601x))`（x=39 → y≈45.2%）：稀有及以上的概率乘 (1+0.01y)，多出来的那份从普通 + 高级里按原比例扣。属性页多一行「幸运值」，**悬停显示「稀有及以上物品的出现概率增加 y%」（y% 绿色）**；幸运值每满 6 点还会获得原版「幸运」buff I / II / III……（掉到 6 点以下自动消失）。
- **新神器「六叶草」**（影子 · 传说 · 【唯一】 · 4 档）：闪避 +3/6/9/12、**幸运值 +3/6/9/12**、暴击几率 +2/4/6/8%、无视防御伤害 +1/1/2/2——幸运值的第一个来源。掉落池 75 → **76 件**。

## 已知问题 / 未完成

- 许愿池与根之退的新模型是刚完成的，比例与贴图可能还会调整。
- 「幸运值」的掉落加成已按公式实现，具体手感仍待实测（六叶草可以升到 3 级 = 12 点幸运值来试）。
- 石板升级、锻造、同伴系统尚未实现。
- 部分文案与图标仍会变动。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib（必需）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文）。

---

## English

A content release: brand-new block models and textures for the **Wishing Pool** and **Root Retreat**, a new mod icon, localized mod metadata (the mod list now shows "Sephiria Fantasy" in English), and a new **Luck** attribute with its first artifact, the **Six-Leaf Clover**.

### New

- **New Wishing Pool model** (hand-built in Blockbench, 30 elements): a stone plinth with four corner pillars, water at the bottom of the pool, a keycap-shaped spout with four water curtains spilling down and a foam ring on the surface, plus a cross-shaped four-leaf clover (the vanilla `cross` trick, alpha-cut from two planes); all stone / water / waterfall / foam / clover textures were redrawn.
- **New Root Retreat model** (26 elements): a low tree-stump base with roots spreading out and front roots holding up the book; the **book leans back like a lectern** (white pages around a cover with a pale text block); behind it a horizontal root beam and two branches rising diagonally. The stone textures reuse vanilla `stone` / `cobblestone` / `smooth_stone`; the pages and cover are hand-drawn.
- **New mod icon**: the user's new pixel-art icon (96×96).
- **Localized mod name / summary / description**: the in-game mod list now shows **Sephiria Fantasy** in English (it previously showed the Chinese name in every language).
- **New attribute "Luck"**: raises the chance of **Rare or better** (Rare / Legendary / Bond / Eternal) items from **shop stock, chest rewards and the Wishing Pool gacha**. Conversion `y = 50×(1−e^(−0.0601x))` (x=39 → y≈45.2%): the Rare-or-better share is multiplied by (1+0.01y) and the extra share is taken proportionally from Common + Advanced. The Attributes screen has a new "Luck" row; **hovering shows "Chance of Rare or better items increases by y%" with y% in green**. Every 6 points also grants the vanilla **Luck** effect at level I / II / III… (removed when it drops below 6).
- **New artifact "Six-Leaf Clover"** (Shadow · Legendary · 【Unique】 · 4 levels): Dodge +3/6/9/12, **Luck +3/6/9/12**, Crit chance +2/4/6/8%, damage that ignores defence +1/1/2/2 — the first source of Luck. The artifact pool grows from 75 to **76** items.

### Known issues / not done yet

- The new Wishing Pool and Root Retreat models were finished just now; proportions and textures may still be adjusted.
- The Luck drop bonus follows the formula, but its feel still needs playtesting (levelling the Six-Leaf Clover to 3 gives 12 Luck).
- Slate upgrades, forging and the companion system are not implemented yet.
- Some texts and icons will still change.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib (required).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description).
