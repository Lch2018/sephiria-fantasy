版本 `0.5.0-a`：从 `0.4.3-a` 之后的全部改动。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.4.3-a`）开始写。

## 这一版有什么

### 赛菲莉娅信标与宝藏

- 新增方块 / 单格容器「赛菲莉娅信标」：放入物品后向天空发出光柱，颜色跟随物品品质；拿走物品光柱消失，挖掉时内容物掉出。
- 新增地表结构「赛菲莉娅宝藏」：每个宝藏中心有一个赛菲莉娅信标；`/locate` 坐标就是信标所在列。
- 信标奖励类型比例：**神器 8 / 石板 2 / 神器附魔币 2 / 药水 1 / 赛菲莉娅额外背包栏 1**；附魔币一次 3 个；神器与石板先定类型再按稀有 / 传说各半掷品质。
- 结构间距按用户口径定为 **spacing 20 / separation 8**；旧存档已生成的区块不会补。
- 修复开光影后光柱消失（alpha 被抹成 0）、空容器存档报错、`/locate` 与信标错位、部分候选高度/树冠导致整座结构消失等问题。

### 选卡

- 新增「选卡」界面与背包紫色卡片标签页：三张同品质神器三选一，只出神器，不出石板、羁绊、永恒。
- 初始品质白 / 蓝 / 黄 / 红各 25%，三张卡不重复。
- 每张卡可刷新一次，刷新后仍是神器；5% 概率品质升一档，红色不再升；刷新会避开另外两张卡。
- 每 2 个游戏日获得 1 次选卡机会，初始 1 次，最多存 10 次；界面显示充能进度。
- 翻牌动画：卡背按品质色从下往上填充、带流动光，再翻出卡面；关闭 / 重进游戏不会重掷同一组卡。
- 没有选卡机会时界面仍能打开，但不显示三张卡与刷新按钮。

### 疲劳与经验

- 新增疲劳：上限 6000 点，每 tick +1，6000 tick 回满。
- 获得经验时消耗当前疲劳的 **40%**；消耗量每满 **30 点**给 **3 点额外经验**，不满 30 点为 0。
- 新增疲劳 HUD（默认关闭），显示「疲劳：x/6000」，可在 HUD 布局里打开、拖动、缩放。
- 升级宝箱改为每累计 **1000 点实际经验**发 1 个。
- 修复经验转叶子 / 升级宝箱计数缺失：所有原版经验来源最终都会走 `giveExperiencePoints`。

### 宝箱与商店

- 五种宝箱（神器 / 石板 / 升级 / 稀有神器 / 稀有石板）在**首次开启前**可堆叠 16；开启后标记 `chest_opened` 并恢复为 1 个。
- 商店所有买价改为原来的 **200%**；出售价仍为买价的 30%。
- 商店界面显示叶子余额、商品价格与限购剩余。
- 智慧三阶段从「每个宝箱免费刷新 1 次」改为「免费刷新次数 +1」：
  - 宝箱仍各 1 次；
  - 商店首次 1 次，关闭重开不补；用掉后下一次骰子刷新会补回一次免费；
  - 选卡每张卡可刷新 2 次。

### 界面与 HUD

- 原版背包、赛菲莉娅背包、商店、属性、天赋、技能、选卡页统一顶部居中标签栏。
- 属性页补全全部属性行与悬停公式；谈判力行内显示「折扣：x%」。
- 天赋页补全七条天赋、阶段图标、阶段说明与滚动。
- HUD 扩到九个模块：武器、冲刺、MP、乌云、太阳剑、冰霜武具、无敌、技能、神器；每模块独立位置 / 缩放，图标冷却改径向扇形并带完成闪光。
- 背包补滚动条、格子等级、石板旋转图标、连击面板；附魔、石板合成、宝箱补专属绘制。
- 武器手持 3D：五把基础武器使用 `_in_hand` 模型；刀保留平面模型（GeckoLib 分支待继续完善）。
- 属性 / 天赋 / 技能页背景不再二次压暗；标签栏图标统一为方形灰底格子。

### 兼容与其他

- ToroHealth-Continued 完整兼容：服务端伤害元数据、实体 UUID 匹配、元素色文本重绘、HUD 血量变化染色、暴击 2 倍。
- 修复 `applyCrit` 没有调用点导致暴击 / ToroHealth 元数据不生效。
- ModMenu 配置入口（打开 HUD 布局）。
- 根之退合成改为**圆石 + 任意树苗**。
- 道具名字按品质上色；药水品质行改为「稀有 药水」；移除普通品质石板「石板基础」。
- 智慧二阶段合并：只有等级满且连击 +2 的目标才排除；放进去的已双满神器不再白白消耗。

## 已知问题 / 未完成

- 从容器界面点标签切到普通页时光标可能跳到窗口中心；已尝试记录 / 恢复鼠标坐标，仍需继续定位。
- 刀的 GeckoLib 模型与剑意动画仍待完善；目前刀使用平面物品模型。
- 属性 / 天赋 / 技能页与部分界面的专属绘制仍在继续对齐 26.3。
- 本版未做完整游戏内实测。
- 1.20.1 / 1.21.1 移植分支是独立实验版本，**不包含在这次发布中**。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。ModMenu 与 ToroHealth-Continued 可选。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.5.0-a`: everything after `0.4.3-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.4.3-a`).

### Sephiria Beacon and Treasure

- New block / single-slot container **Sephiria Beacon**: emits a sky beam whose color follows the stored item's quality; removing the item removes the beam and breaking the block drops its contents.
- New surface structure **Sephiria Treasure**, with one beacon at its center; `/locate` returns the beacon column.
- Reward type weights: **artifact 8 / slate 2 / enchant coin 2 / potion 1 / extra backpack row 1**. Enchant coins come in stacks of 3; artifacts and slates roll rare/legendary after the type roll.
- Structure spacing is **20 / separation 8** as requested; already-generated chunks are not retrofitted.
- Fixed invisible beams with shaders (alpha was masked to 0), empty-container serialization errors, `/locate` offset, and candidates disappearing due to height/tree checks.

### Card Selection

- New **Card Selection** screen and purple card tab: pick one of three artifacts of the same rarity. Artifacts only; no slates, Bond or Eternal.
- Initial rarity is Common / Advanced / Rare / Legendary at 25% each; the three cards are distinct.
- Each card can be refreshed once; refresh keeps it an artifact and has a 5% chance to upgrade one rarity step (Legendary cannot upgrade); refresh avoids the other two cards.
- One pick every 2 in-game days, starting with 1, capped at 10 stored; the screen shows charge progress.
- Card-back fill, flowing light and flip animation; closing / relogging does not reroll the same set.
- With no picks left the screen still opens, but shows no cards and no refresh buttons.

### Fatigue and Experience

- New Fatigue: cap 6000, +1 per tick, full in 6000 ticks.
- Gaining XP consumes **40%** of current Fatigue; every **30** consumed points grants **3** bonus XP (0 below 30).
- New Fatigue HUD (off by default) showing `Fatigue: x/6000`, configurable in the HUD layout editor.
- Upgrade chests now require **1000 actual XP** each.
- Fixed missing leaf / upgrade-chest progress: all vanilla XP sources eventually pass through `giveExperiencePoints`.

### Chests and Shop

- All five chest types stack to 16 **before first opening**; opening marks `chest_opened` and makes that stack size 1.
- All shop buy prices are now **200%** of the previous values; sell price remains 30% of buy price.
- Shop shows leaf balance, prices and remaining stock.
- Wisdom stage 3 is now **Free rerolls +1**:
  - one per chest (unchanged);
  - one for the shop, not replenished by reopening; using a dice refresh restores the next free refresh;
  - card selection allows 2 refreshes per card.

### UI and HUD

- Unified top-center tab bar on vanilla inventory, Sephiria backpack, shop, attributes, talents, skills and card pages.
- Full attribute page with hover formulas; negotiation row shows `Discount: x%`.
- Full talent page with seven talents, stage icons, descriptions and scrolling.
- HUD expanded to nine modules: weapon, dash, MP, cloud, sun sword, frost armament, invulnerability, skills, artifacts; each with independent position / scale; radial cooldowns and completion flash.
- Backpack scrollbar, slot levels, rotated slate icons and combo panel; dedicated drawing for enchant, slate fusion and chest screens.
- 3D held models for the five base weapons via `_in_hand`; Blade remains flat until the GeckoLib path is finished.
- Fixed double-darkened attribute / talent / skill backgrounds; tab icons use uniform grey squares.

### Compatibility and Misc

- Full ToroHealth-Continued support: server damage metadata, entity UUID matching, element-colored text redraw, HUD health-change tinting, 2× critical text.
- Fixed `applyCrit` having no call site (critical hits and ToroHealth metadata were not actually running).
- ModMenu config entry (opens the HUD layout editor).
- Root Retreat recipe changed to **cobblestone + any sapling**.
- Item names are colored by quality; potion quality reads “Potion”; removed the common slate “Slate Base”.
- Wisdom stage 2 merge now excludes only targets that are max level **and** at combo +2; a fully maxed incoming artifact is no longer wasted.

## Known Issues / TODO

- Switching tabs from container screens can still jump the cursor to the window center; the cursor save/restore approach needs more work.
- Blade GeckoLib model and sword-intent animation still need work; Blade currently uses a flat item model.
- Attribute / talent / skill and some other screens are still being aligned with 26.3.
- No full in-game verification for this release.
- The 1.20.1 / 1.21.1 port branches are separate experiments and are **not part of this release**.

## Requirements

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (both client and server). ModMenu and ToroHealth-Continued are optional.

## Disclaimer

Unofficial fan mod; much of the code is AI-generated; **commercial use is prohibited** (see the project description and LICENSE).
