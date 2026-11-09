# 赛菲莉娅幻想 Sephiria Fantasy

## 中文

**赛菲莉娅幻想**：把《SEPHIRIA（赛菲莉娅）》的**构筑**搬进 Minecraft 的同人模组——选择并组合你自己的神器、石板与连击。本项目**大量使用 AI 生成代码**，仅供学习与个人游玩，**禁止商业行为**。

> A 测阶段：内容已经不少，但大部分还没有经过充分实测，数值与手感仍在调整。当前版本 **0.5.2-a**，对应 **Minecraft 26.3**；1.20.1 / 1.21.1 移植分支不在本说明范围内。

### 主要内容

#### 武器

六把基础武器：**标准剑盾、钢铁巨剑、匕首、重型弩、刀、长棍**。每把都有自己的普通攻击节奏与右键武器技能：

- **标准剑盾**：按住右键防御，最终伤害 -50%；防御中左键横扫；刚举盾的 0.3 秒内挨打算「完美防御」，下一次横扫免费。
- **钢铁巨剑**：慢速重击；按住右键蓄力 1.5 秒，满蓄松开后突进并打出环形斩。
- **匕首**：右键招架（突进 + 范围伤害 + 无敌）；成功招架获得「专注」并刷新冷却，有专注时右键改为狂怒（突进 + 路径伤害）。
- **重型弩**：10 发弹匣，弩矢固定伤害并穿透，按 R 装填 2 秒；不消耗背包里的箭。
- **刀**：右键切换出鞘 / 入鞘；入鞘重击慢、出鞘快斩；切换瞬间短暂无敌，命中积攒剑意，满层入鞘时打出强力斩击。
- **长棍**：右键两段回击，自动衔接；完美防御会改为回击 3。

六把武器都**不可损坏**；刀使用 GeckoLib 骨骼动画。

![武器](图片占位)

#### 神器与连击

**93 件神器、12 种连击**：坚固、风之歌、精密、影子、谈判、元素、魔法科技、余烬、乌云、太阳剑、冰川、冰霜武具（少数神器没有连击）。

- 把有连击的神器放进**赛菲莉娅背包**（默认 30 格、6 格宽，可用「赛菲莉娅额外背包栏」扩到最多 54 格）后，每件默认提供 1 级对应连击；带【唯一】的同种神器只按等级最高的那件计算；连击在 2 / 4 / 6 / 8 / 10 级逐档生效并累加。
- 神器分**白 / 蓝 / 黄 / 红 / 彩（羁绊）**五档，自身等级从 0 到各自上限；用**神器附魔币**升级。实际等级 = 自身等级 + 所在格子等级，格子等级为负时该神器失效。
- 背包里的神器还会提供主动技能；**6 个技能栏**可自由摆放与改键，当前包括鼓励旗帜、急速、锐利之眼、雷之裁决、冰弓。

![神器与连击](图片占位)

#### 石板

石板也放进赛菲莉娅背包，不直接加属性，而是改变**周围格子的等级**（可加可减）。石板图案里写着每个相对格子的增量，按 R 可旋转（部分固定不可转）；格子等级会与神器自身等级相加，负数会让该格神器失效。

- 石板池共 **57 种**，分白 / 蓝 / 黄 / 红 / 永恒五档；另有「天国」与「合成石板」两种特殊石板。
- **天国**：全背包每格 +9、不可旋转，不进任何常规掉落池，只能以 0.01% 概率从烧炼远古残骸获得。
- **石板合成**：两块普通石板可在赛菲莉娅商人处合成（每只商人、每名玩家一次）。先应用各自朝向，再把同一相对格子的增量相加；合成石板不能再参与合成，只有两块输入都能旋转时结果才能旋转。
- 天赋「生存」三阶段解锁**石板刻印**：长按 F 消耗石板，但保留它对周围格子的效果。

![石板](图片占位)

### 变强与获取道具方式

除了下面的五条主要路线，经验获取还会与**疲劳**联动：疲劳每 tick 恢复 1 点、上限 6000；获得经验时消耗当前点数的 40%，每消耗 30 点额外给 3 点经验。每获得 1000 点实际经验还会得到 1 个升级宝箱。

#### 天赋

天赋点来自会弹出 toast 的成就、击杀 BOSS，以及角色等级（每级 1 点，最多 30 点）。共 **7 页天赋**：愤怒、迅捷、生存、耐心、智慧、意志、急智；每页最多 20 点，5 / 10 / 20 点解锁三阶段。加点与重置需要在**出生点 100 格内**。

- **愤怒**：暴击率；阶段给暴击伤害、BOSS 伤害、防御穿透。
- **迅捷**：攻速；阶段给闪避、冲刺次数、冲刺恢复、武器伤害、冲刺无敌。
- **生存**：最大 HP；高级药水随机属性、额外背包格、石板刻印。
- **耐心**：防御力；第一行格子 +1、低血回复、十日一次复活。
- **智慧**：战斗 MP 再生；MP 吸收 / 最大 MP、同种神器合并、BOSS 掉骰子、免费刷新 +1、经验掉落 +20%。
- **意志**：幸运值；许愿池锚定格、最大 HP / MP。
- **急智**：最大 HP / 闪避；背包格、宝箱多选一、每种激活连击增伤。

![天赋](图片占位)

#### 商店

从标签栏的**商店页**打开。货架有 6 个商品格、2 个宝箱格、2 个药水格；商品 75% 神器 / 25% 石板，同一批不重复，宝箱格固定出神器宝箱与石板宝箱。

- 用**叶子**购买；买价按品质：白 500 / 蓝 1000 / 黄 1500 / 红 2000 / 彩 3000。石板是神器价的 70%，神器宝箱 1500、石板宝箱 1050；**谈判力**降低买价。
- 卖价是买价的 **30%**；骰子、附魔币、宝箱不收购（铜锭固定 1 叶）。
- **刷新**：把骰子放进骰子栏，按「刷新」消耗 1 颗。商品每格限购 1 次，两个宝箱格共用 3 次额度；货架跨会话保留。
- 五种宝箱（神器 / 石板 / 升级 / 稀有神器 / 稀有石板）在**首次打开前**可叠 16 个，打开后恢复为不可堆叠。
- 智慧三阶段：免费刷新 +1；用骰子刷新后，下一次刷新免费。

![商店](图片占位)

#### 许愿池

用**水桶 + 树苗**合成许愿池方块，右键打开界面。把**升级宝箱**扔进池子会被吸走，每个宝箱换 1 片四叶草（一叠按实际个数算）；达成会弹 toast 的成就也会给 1 片四叶草。

- 神器池 / 石板池分开；单抽 1 片四叶草，十连 10 片；按品质权重抽取，同一批允许重复。
- **锚定**：默认 1 格，意志天赋最多加到 5 格；同一件可以占多格。掷到某一档时，这一档的锚定物共占 `50% × (1 + 品质增量 × 总锚定次数)`（白 5 / 蓝 10 / 黄 15 / 红与最高档 20，最多 100%），没命中才在整档随机。
- 抽到东西会给**愿力**：神器 2 / 4 / 6 / 8 / 10，石板 3 / 6 / 9 / 12 / 15（按白 / 蓝 / 黄 / 红 / 最高档）。
- **神铸**：普通 30 / 高级 50 / 黄 80 / 红 200 愿力；羁绊 / 永恒不参与。
- 界面里可以打开「溢出的升级宝箱自动转化为四叶草」；许愿池等级系统暂未实现。

![许愿池](图片占位)

#### 选卡

从标签栏的**选卡页**打开。每次给出 3 张同品质神器，只能选 1 张；初始品质在白 / 蓝 / 黄 / 红四档中等概率。每张卡在一次选卡内可刷新 1 次（智慧三阶段 2 次），刷新后仍是神器，5% 概率升一档（红不再升），并尽量避开另外两张。

- 每 1 个游戏日获得 1 次选卡机会，初始 1 次，最多存 10 次。
- 不选就关掉不扣次数；选一张才扣次数并给对应神器。
- 界面显示充能进度；关掉或重进不会重掷同一批；没有机会时仍可打开，但不显示卡。

![选卡](图片占位)

#### 赛菲莉娅商人

一只红袍的**赛菲莉娅流浪商人**：外观与商品换成赛菲莉娅的东西，刷新时机、几率、地点和离场沿用原版流浪商人；独立的刷怪器让它与原版商人同时存在（基础出生延迟 4 分钟），会带 2 只行商羊驼，40 分钟后离场。

- 右键打开自定义界面；9 格全部**免费**：7 格道具（神器附魔币 / 骰子 / 赛菲莉娅额外背包栏三选一，等概率）+ 2 格药水（全部赛菲莉娅药水）。
- 每格只有 1 件，拿走即售罄，**卖完不补货**。
- 每只商人、每名玩家还可以使用一次**石板合成**（成功合成才算）。

![赛菲莉娅商人](图片占位)

### 提高游戏难度方式

#### 根之退

用**圆石 + 树苗**合成根之退方块。放在**出生点 100 格内**，或者放满 **1 个游戏日（24000 刻）**后激活；右键打开界面。

- 界面左边调难度，右边是「你的方案」；每个方块各存一套方案，点「应用」才把当前方案复制到玩家身上。设置**只影响自己**，不影响其他玩家，死亡不清零。
- 六项难度：
  - **凶猛的爪子**：敌人对你的伤害 +10% ~ +80%。
  - **顽强之躯**：敌人最终受到的伤害 -20% ~ -90%，**连真实伤害也减**。
  - **防护铠甲**：敌人防御力 +30 ~ +150（按 x/(x+80) 折算减伤，**不减免真实伤害**）。
  - **衰退**：你的所有生命恢复 -5% ~ -50%。
  - **紧张**：战斗中无法恢复生命（1 级，4 阶段；战斗 = 挨打或造成伤害后的 8 秒内）。
  - **心之重担**：死亡时背包与赛菲莉娅背包中的物品有 20% ~ 100% 消失（发生在死亡掉落之前）。
- 总阶段 = 各项阶段之和；每 1 阶段提供 **+10% 经验获取**。
- 祝福：**轻盈**（20 阶段，脱战速度 I、摔落伤害 -20%）、**力量**（30 阶段，战斗中力量 III）、**勇气**（40 阶段，名字变黄）。

![根之退](图片占位)

### 环境要求

| 项目 | 要求 |
| --- | --- |
| Minecraft | **26.3** |
| Loader | **Fabric Loader 0.19.5+** |
| 必需模组 | **Fabric API 0.161.0+26.3**、**GeckoLib 5.5.6**（武器动画） |
| Java | **25** |
| 环境 | **客户端与服务端都需要安装** |

### 安装

1. 安装 Fabric Loader 与上面的两个依赖；
2. 把 `sephiria-fantasy-<版本>.jar` 放进 `mods/`；
3. 单人游戏可直接使用；服务器需要两端都装同一个 jar。

### 已知问题

- 数值与手感仍在调整，大部分内容还没有经过充分实测。
- 武器锻造（基础 → 一级 → 二级）、石板升级、同伴系统尚未实现。
- 部分界面文字与图标仍会调整；许愿池等级系统暂未实现。
- 1.20.1 / 1.21.1 移植分支存在，但不包含在本说明与本次发布范围内。

### 声明

- **授权**：本模组的二进制与源码按仓库里的 [LICENSE](https://github.com/Lch2018/sephiria-fantasy/blob/main/LICENSE)（《同人模组许可声明》，Modrinth 上标为 CC BY-NC 4.0）发布。允许个人游玩、自用修改，以及在**免费**的整合包 / 服务器中携带分发（**须保留本声明并注明出处**）；**原版与修改版一律禁止任何形式的商业行为**。
- **同人性质**：本模组是《SEPHIRIA（赛菲莉娅）》的**非官方同人作品**，与原作者 TEAM HORAY、与 Mojang Studios / Microsoft 均无任何隶属、授权或合作关系。
- **AI 生成代码**：本项目**大量使用 AI 生成代码**（模型、贴图处理脚本与文档同样如此），**仅供学习与技术交流**，请自行判断是否使用；按「现状」提供，不附带任何形式的担保。
- **素材例外**：`src/main/resources/assets/sephiria_fantasy/textures/` 与 `tools/weapon-ref/` 下的武器美术来自原作《SEPHIRIA（赛菲莉娅）》，版权归 **TEAM HORAY** 所有，**不在上面的授权范围内**，**不得单独提取、二次分发或用于任何商业用途**。相关权利方如有异议，请联系删除。
- **商标**：Minecraft 是 Mojang Synergies AB 的商标；《SEPHIRIA（赛菲莉娅）》的名称、设定与美术归 TEAM HORAY 所有。
- 源码与问题追踪：[https://github.com/Lch2018/sephiria-fantasy](https://github.com/Lch2018/sephiria-fantasy)

## English

**Sephiria Fantasy** is an **unofficial fan mod** that brings the **Construct/Constructed buildcraft** of *SEPHIRIA* into Minecraft — choosing and combining your own artifacts, slates and combos. This project makes **heavy use of AI-generated code**; it is shared for learning and personal play only, and **commercial use is not allowed**.

> Alpha stage: there is a decent amount of content, but most of it has not been playtested thoroughly yet — numbers and feel are still being tuned. Current version: **0.5.2-a** for **Minecraft 26.3**. The 1.20.1 / 1.21.1 port branches are not covered by this description.

### Main Content

#### Weapons

Six base weapons: **Default Sword and Shield, Steel Greatsword, Dagger, Colossal Crossbow, Blade and Quarterstaff**. Each has its own normal-attack feel and a right-click weapon skill:

- **Default Sword and Shield**: hold right-click to defend and take 50% less final damage; left-click while defending to sweep; a hit within 0.3 s of raising the shield is a Perfect Guard and makes the next sweep free.
- **Steel Greatsword**: slow heavy hits; hold right-click to charge for 1.5 s, then release to dash forward and unleash a circular slash.
- **Dagger**: right-click Parry (dash + area damage + invulnerability); a successful parry grants Focus and refreshes the cooldown, and with Focus, right-click becomes Fury (dash + path damage).
- **Colossal Crossbow**: 10-round magazine, fixed-damage piercing bolts, press R to reload in 2 s; it does not consume arrows from your inventory.
- **Blade**: right-click to toggle sheathed / unsheathed; sheathed hits are heavy and slow, unsheathed hits are fast; switching grants brief invulnerability, hits build Sword Intent, and a full-intent sheathed strike becomes a powerful slash.
- **Quarterstaff**: right-click performs a two-stage Riposte that chains automatically; a Perfect Guard upgrades it to Riposte 3.

All six weapons are **unbreakable**; the Blade uses GeckoLib skeletal animation.

![Weapons](image-placeholder)

#### Artifacts and Combos

**93 artifacts across 12 combos**: Sturdy, Wind Song, Precision, Shadow, Negotiation, Element, Magitech, Embers, Dark Cloud, Sun Sword, Glacier and Frost Armament (a few artifacts have no combo).

- An artifact with a combo gives 1 level of it by default when placed in the **Sephiria Backpack** (30 slots by default, 6 wide; up to 54 with Extra Backpack Row items). A 【Unique】 artifact only counts once, using its highest-level copy. Combo tiers trigger at 2 / 4 / 6 / 8 / 10 and stack.
- Artifacts come in five rarities — **Common / Advanced / Rare / Legendary / Bond (rainbow)** — and have their own level from 0 to a per-item cap, raised with an **Artifact Enchant Coin**. Effective level = artifact level + slot level; a negative slot level disables the artifact.
- Artifacts in the backpack also provide active skills. **Six skill slots** can be arranged and rebound; current skills include Encouragement Banner, Haste, Keen Eye, Thunder Verdict and Ice Bow.

![Artifacts and Combos](image-placeholder)

#### Slates

Slates also go into the Sephiria Backpack. They do not add stats directly; they change the **level of surrounding slots** (up or down). A slate's pattern lists a delta for each relative slot, and it can be rotated with R (some are fixed). Slot levels are added to artifact levels; a negative slot disables the artifact in it.

- The slate pool has **57 slates** across Common / Advanced / Rare / Legendary / Eternal. Two special slates exist: **Heaven** and the **Fusion Slate**.
- **Heaven**: +9 to every slot in the backpack, cannot rotate, never appears in normal loot pools, and only has a 0.01% chance to drop when smelting ancient debris.
- **Slate Fusion**: two normal slates can be fused at the Sephiria Merchant (once per merchant per player). Their rotations are applied first, then deltas on the same relative slot are added. A Fusion Slate cannot be fused again, and the result is rotatable only if both inputs were rotatable.
- Survival talent stage 3 unlocks **Slate Imprint**: hold F to consume a slate while keeping its effect on nearby slots.

![Slates](image-placeholder)

### Getting Stronger and Getting Items

Besides the five main routes below, XP gain also interacts with **Fatigue**: Fatigue regenerates 1 point per tick up to 6000; gaining XP consumes 40% of the current points and grants 3 bonus XP per 30 points consumed. Every 1000 actual XP also grants one Upgrade Chest.

#### Talents

Talent points come from advancements that show a toast, boss kills, and character levels (1 point per level, up to 30). There are **seven talent pages**: Wrath, Swiftness, Survival, Patience, Wisdom, Will and Quick Wit. Each page caps at 20 points, with stages at 5 / 10 / 20. Spending and resetting points requires being **within 100 blocks of spawn**.

- **Wrath**: crit chance; stages add crit damage, boss damage and defense penetration.
- **Swiftness**: attack speed; stages add dodge, dash charges, dash recovery, weapon damage and dash invulnerability.
- **Survival**: max HP; Advanced-or-better potions grant a random stat, extra backpack slots, and Slate Imprint.
- **Patience**: defense; +1 slot level for the first backpack row, low-health regeneration, and a revival once every 10 in-game days.
- **Wisdom**: combat MP regeneration; MP absorption / max MP, duplicate artifact merging, dice from boss kills, +1 free reroll, and +20% XP drop.
- **Will**: luck; Wishing Pool anchors, max HP / MP.
- **Quick Wit**: max HP / dodge; backpack slots, one extra chest choice, and damage per active combo type.

![Talents](image-placeholder)

#### Shop

Open it from the **Shop tab** in the tab bar. The shelf has 6 goods slots, 2 chest slots and 2 potion slots. Goods are 75% artifacts / 25% slates with no duplicates in one batch; the chest slots always hold an Artifact Chest and a Slate Chest.

- Buy with **Leaves**. Prices by rarity: Common 500 / Advanced 1000 / Rare 1500 / Legendary 2000 / Bond 3000. Slates cost 70% of the artifact price; Artifact Chest 1500 and Slate Chest 1050. **Negotiation** lowers buy prices.
- Sell price is **30%** of the buy price; dice, Enchant Coins and chests are not bought (copper ingots sell for 1 Leaf).
- **Refresh**: put dice into the dice slot and press Refresh to spend one. Each goods slot has 1 purchase, and the two chest slots share 3 purchases. The shelf persists across sessions.
- All five chest types (Artifact / Slate / Upgrade / Rare Artifact / Rare Slate) stack up to 16 **before first opening**; an opened chest becomes unstackable again.
- Wisdom stage 3: +1 free reroll; after a dice refresh, the next refresh is free.

![Shop](image-placeholder)

#### Wishing Pool

Craft a Wishing Pool block with **a water bucket + a sapling**, then right-click to open it. Dropping **Upgrade Chests** into the pool pulls them in and turns each chest into 1 Clover (a stack counts per chest); completing an advancement that shows a toast also grants 1 Clover.

- The artifact pool and slate pool are separate. A single draw costs 1 Clover, a ten-pull costs 10; rarity is rolled by weight, and duplicates within one batch are allowed.
- **Anchor**: 1 slot by default, up to 5 with the Will talent; the same item can occupy multiple slots. When a rarity is rolled, anchored items of that rarity share `50% × (1 + rarity step × total anchor count)` (Common 5 / Advanced 10 / Rare 15 / Legendary and top tier 20, capped at 100%). Only if that fails is the item picked randomly from the whole tier.
- Draws grant **Wish Power**: artifacts 2 / 4 / 6 / 8 / 10, slates 3 / 6 / 9 / 12 / 15 (by Common / Advanced / Rare / Legendary / top tier).
- **Forge**: Common 30 / Advanced 50 / Rare 80 / Legendary 200 Wish Power; Bond / Eternal are not forgeable.
- The screen has an option to turn overflowing Upgrade Chests into Clovers automatically. A Wishing Pool level system is not implemented yet.

![Wishing Pool](image-placeholder)

#### Card Selection

Open it from the **Card Selection tab**. Three artifacts of the same rarity are offered and you pick one; the initial rarity is Common / Advanced / Rare / Legendary at equal odds. Each card can be refreshed once per selection (twice with Wisdom stage 3). A refresh keeps it an artifact, has a 5% chance to upgrade one rarity step (Legendary cannot upgrade), and avoids the other two cards.

- One selection chance is gained every in-game day, starting with 1 and capped at 10 stored.
- Closing without picking does not consume a chance; picking one consumes it and grants that artifact.
- The screen shows charge progress; closing or relogging does not reroll the same set. With no chances left it still opens, but shows no cards.

![Card Selection](image-placeholder)

#### Sephiria Merchant

A red-robed **Sephiria Wandering Trader**. Its look and stock are Sephiria-themed, while spawning timing, chance, location and despawning follow the vanilla Wandering Trader. An independent spawner lets it coexist with the vanilla trader (4-minute base spawn delay); it brings 2 trader llamas and leaves after 40 minutes.

- Right-click to open a custom screen. All 9 slots are **free**: 7 goods slots (Artifact Enchant Coin / Dice / Extra Backpack Row, equal chance) plus 2 potion slots (all Sephiria potions).
- Each slot holds 1 item; taking it sells it out, and **there is no restock**.
- Each merchant offers **Slate Fusion** once per player (counted only on a successful fusion).

![Sephiria Merchant](image-placeholder)

### Increasing Difficulty

#### Root Retreat

Craft a Root Retreat block with **cobblestone + a sapling**. It activates when placed **within 100 blocks of spawn**, or after **one full in-game day (24,000 ticks)**; right-click to open it.

- The left side sets difficulty and the right side is "Your Plan"; each block stores its own plan, and pressing Apply copies the current plan onto the player. Settings **only affect that player**, not other players, and are kept on death.
- Six difficulty options:
  - **Fierce Claws**: enemies deal +10% to +80% more damage to you.
  - **Tenacious Body**: enemies take 20% to 90% less final damage, **including true damage**.
  - **Protective Armor**: enemy defense +30 to +150 (reduction uses x/(x+80); **does not reduce true damage**).
  - **Decay**: all healing you receive is reduced by 5% to 50%.
  - **Tension**: you cannot regenerate health in combat (1 level, 4 stages; in combat = within 8 seconds of being hit or dealing damage).
  - **Heart's Burden**: on death, 20% to 100% of items in your inventory and Sephiria Backpack disappear (before death drops).
- Total stage = the sum of all option stages; each stage grants **+10% XP gain**.
- Blessings: **Lightness** (stage 20: Speed I out of combat, -20% fall damage), **Strength** (stage 30: Strength III in combat), **Courage** (stage 40: your name turns yellow).

![Root Retreat](image-placeholder)

### Requirements

| Item | Requirement |
| --- | --- |
| Minecraft | **26.3** |
| Loader | **Fabric Loader 0.19.5+** |
| Required mods | **Fabric API 0.161.0+26.3**, **GeckoLib 5.5.6** (weapon animation) |
| Java | **25** |
| Environment | Required on **both client and server** |

### Installation

1. Install Fabric Loader and both dependencies above;
2. Put `sephiria-fantasy-<version>.jar` into `mods/`;
3. It works in singleplayer right away; for a server, install the same jar on both sides.

### Known Issues

- Numbers and feel are still being tuned; most content has not been playtested thoroughly.
- Weapon forging (base → tier 1 → tier 2), slate upgrades and the companion system are not implemented yet.
- Some UI text and icons will still change; the Wishing Pool level system is not implemented yet.
- The 1.20.1 / 1.21.1 port branches exist but are not part of this description or this release.

### Disclaimer

- **License**: The mod's binaries and source are released under the repository's [LICENSE](https://github.com/Lch2018/sephiria-fantasy/blob/main/LICENSE) (listed as CC BY-NC 4.0 on Modrinth). Personal play, private modifications, and redistribution inside free modpacks / servers are allowed **provided this notice is kept and credit is given**; **any commercial use is prohibited, for the original and for modified versions alike**.
- **Fan project**: Sephiria Fantasy is an **unofficial fan work** based on *SEPHIRIA*. It is not affiliated with, endorsed by, or licensed by TEAM HORAY, Mojang Studios or Microsoft.
- **AI-generated code**: This project makes **heavy use of AI-generated code** (models, texture-processing scripts and docs included). It is for learning and technical exchange only; use it at your own judgment, and it is provided "as is" without warranty of any kind.
- **Assets**: The weapon artwork under `src/main/resources/assets/sephiria_fantasy/textures/` and `tools/weapon-ref/` is taken from the original game *SEPHIRIA* and is the property of **TEAM HORAY**. It is **not covered** by the license above, and **may not be extracted, redistributed separately, or used commercially**. Rights holders may request its removal.
- **Trademarks**: Minecraft is a trademark of Mojang Synergies AB; *SEPHIRIA* and its setting and artwork belong to TEAM HORAY.
- Source and issue tracker: [https://github.com/Lch2018/sephiria-fantasy](https://github.com/Lch2018/sephiria-fantasy)

---

开发文档 / Development docs: [DEVELOPMENT.md](DEVELOPMENT.md)
