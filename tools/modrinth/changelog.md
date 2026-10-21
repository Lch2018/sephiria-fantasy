大版本更新（`0.4.0-a`）：从 `0.3.10.19-a` 之后的全部累积改动。

> **更新日志说明**：上一版 `0.3.99-a` 只写了最近一批（三条新天赋页），漏掉了 `0.3.10.21` 与 `0.3.10.21.1` 的内容，造成断层。这一版从 `0.3.10.19-a` 开始补全；之后每次上传 / 发布，更新日志都会从上一次上传 / 发布的版本开始写，不再只写最近一次改动。

## 这一版有什么

### 天赋系统（七条天赋页）

- 背包顶部新增「天赋」标签；页面顺序为 **愤怒 → 迅捷 → 生存 → 耐心 → 智慧 → 意志 → 急智**。
- 天赋点来源：达成会弹 toast 的成就 +1、击杀 BOSS +1、升级每级 +1（升级来源最多 30 点，按历史最高等级补发，掉级不回收）。
- 每条天赋页 5 / 10 / 20 点解锁三个阶段，每页最多 20 点；只能在出生点 100 格内加点 / 退点 / 重置。
- 天赋页支持滚轮整行滚动，右侧新增滚动进度条；阶段图标点亮时整套调色板染成对应天赋色。
- **愤怒**：每点 +1% 暴击几率；一阶段暴击伤害 +20%；二阶段对 BOSS 伤害 +10%；三阶段防御穿透 +20%。
- **迅捷**：每点 +1% 攻击速度；一阶段闪避 +8、冲刺次数 +1；二阶段冲刺恢复 +20%、武器伤害 +8%；三阶段冲刺无敌 +0.1 秒、武器伤害再 +15%。
- **意志**：每点 +1 幸运值；一阶段许愿池锚定格 +1；二阶段最大 HP +4、最大 MP +25；三阶段锚定格再 +3（最多 5 格）。
- **急智**：每点 +0.3 最大 HP、+0.5 闪避；一阶段背包 +2 格；二阶段宝箱选择 +1（6 选 1）；三阶段每种激活的连击类型使所有伤害 +7%。
- **生存**：每点 +0.6 最大 HP；5 点喝高级及以上品质药水随机一项属性永久 +1；10 点背包 +3 格；20 点背包再 -12 格并解锁**石板刻印**。
- **耐心**：每点 +1 防御力；5 点获得「石板基础」（效果与石板「基础」完全一致）；10 点生命低于 7 时每秒回 1；20 点死亡时以 60% 生命复活，每 10 个游戏日一次，不死图腾优先。
- **智慧**：每点 +1 MP再生（战斗每 10 秒结算）；5 点 MP 吸收 +5、最大 MP +10；10 点所有神器强制【独特】、同种神器放入背包时合并、BOSS 额外掉 1 个骰子；20 点每个宝箱免费刷新 1 次、经验掉落 +20%。

### 石板

- **新增 36 块石板**，总数 21 → **58 块**：10 块普通、16 块高级、5 块稀有、5 块传说，以及特殊「天国」。
- 「天国」：表面永恒，所有格子等级 +9、不可旋转；不进掉落池 / 商店 / 许愿池，只能在烧炼远古残骸时以每份 0.01% 的概率从熔炉弹出。
- 已有的「双星」改成「正上方第二格与正下方第二格各 +2」，并换新图标。
- 12 块生成器石板图标补上 1px 深色描边。
- **石板基础**（耐心一阶段）：效果与石板「基础」相同（同一行左右各 8 格 +1，不可旋转），不能丢出 / 放进普通背包。
- **石板刻印**（生存三阶段）：鼠标指着背包里的石板、按住 **F** 1.5 秒，石板被消耗，但对周围格子的效果保留；三阶段熄灭时石板掉出、效果消失。进度条由界面直接绘制，旋转过的石板也正常显示。
- **稀有宝箱**：BOSS 死亡固定掉落一个稀有神器宝箱和一个稀有石板宝箱，只出稀有及以上品质。

### 宝箱与商店

- 宝箱界面新增**骰子格**与**「刷新」按钮**，可以用骰子换一批奖励；按钮位于骰子格正下方。
- 智慧三阶段每个宝箱免费刷新 1 次，免费次数优先；关掉界面再打开不会重置免费次数。
- 急智二阶段把所有宝箱变成 6 选 1。

### 战斗与属性

- **武器蓝耗**（不显示在武器描述中）：盾剑防御挨打 10 / 完美防御 5、横扫 15；巨剑旋风 15；匕首招架 7 / 完美招架 5 / 狂怒 0；刀入鞘普攻 5（蓝不够时退化成空手伤害、无横扫）、强力斩击 0；长棍回击 7 / 完美回击 3。
- **新属性「防御力」**：与敌人的防御力同一条减伤公式（并进护甲值），吃防御穿透。
- **新属性「MP 吸收」**：与 HP 偷取同口径，每点 = 造成伤害的 0.1% 回蓝。
- **MP 再生重做**：移除原来的每秒「蓝量再生」，属性页改为「MP再生」；战斗中每 10 秒回复「基础 10 + 神器 + 智慧天赋每点 +1」；**脱战时每 0.5 秒回复 12.5% 最大 MP**。沙波特果实的词条改为显示 +10/20/30/40。
- **神器强制独特与合并**：智慧二阶段点亮后，所有神器按【独特】结算；同种神器放进背包时合并（等级 +1、连击 +1），合并过的原件不能丢出或放进普通背包，但可以在背包内移动、放进附魔界面的神器槽。
- **MP 显示修复**：属性页「蓝量」改成「当前 / 上限」，上限下降时当前值夹回上限；天赋改最大 MP 后 HUD 立刻刷新。
- **匕首专注**开局改为 0 层，第一次右键是招架而不是狂怒。

### 根之退

- 新增交互门槛：出生点 100 格内，或放置后满 24000 刻才能打开。
- 新增三项难度：衰退 / 紧张 / 心之重担。
- 新增三个祝福：力量（30 阶段）、轻盈（20 阶段）、勇气（40 阶段）。
- 祝福悬停浮窗改到鼠标左侧，避免超出屏幕。

### 特效与体验

- **附魔式微光**：匕首有专注时红白、刀剑意满层时蓝白，与原版紫色附魔光区分。
- **生存随机属性提示**：修复多出的 `%s`，「生存」两个字用生存天赋色显示。
- **受保护物品移动规则**：石板基础与合并过的神器可以在背包内移动，合并过的神器还能放进附魔神器槽，但不能丢出、不能放进普通背包、不能换到快捷栏。
- **石板刻印改成长按 F**（鼠标指着石板），左键恢复原版「按下即拿起 / 拖动」。

## 已知问题 / 未完成

- 大部分内容仍未经充分实测，数值和手感还在调整；新的三条天赋页与石板刻印尤其需要实测。
- 「石板基础」本身不能刻印（避免耐心一阶段反复发放导致无限叠加）。
- 石板升级、锻造、同伴系统尚未实现。
- 部分文案与图标仍会变动。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib（必需，客户端与服务端都要装）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Major update (`0.4.0-a`): a cumulative changelog covering everything after `0.3.10.19-a`.

> **About this changelog**: `0.3.99-a` only documented its most recent batch (the three new talent pages) and missed the `0.3.10.21` / `0.3.10.21.1` changes, leaving a gap. This release starts again from `0.3.10.19-a`; from now on, every upload / release will write its changelog starting from the previously uploaded / released version instead of only the latest commit.

### What's in this version

#### Talent system (seven pages)

- A new **Talents** tab at the top of the backpack; page order: **Wrath → Swiftness → Survival → Patience → Wisdom → Will → Quick Wit**.
- Talent points come from toast-granting advancements (+1), boss kills (+1), and leveling up (+1 per level, capped at 30 from this source; granted by historical highest level, never reclaimed or duplicated).
- Each page unlocks three stages at 5 / 10 / 20 points, with a 20-point cap per page; points can only be changed within 100 blocks of spawn.
- The page supports wheel scrolling with a scrollbar on the right; lit stage icons are recolored across their whole palette.
- **Wrath**: +1% crit chance per point; stage 1 +20% crit damage; stage 2 +10% boss damage; stage 3 +20% defense penetration.
- **Swiftness**: +1% attack speed per point; stage 1 +8 dodge and +1 dash charge; stage 2 +20% dash recovery and +8% weapon damage; stage 3 +0.1 s dash invulnerability and another +15% weapon damage.
- **Will**: +1 Luck per point; stage 1 +1 wish-pool anchor slot; stage 2 +4 max HP and +25 max MP; stage 3 +3 more anchor slots (up to 5).
- **Quick Wit**: +0.3 max HP and +0.5 dodge per point; stage 1 +2 backpack slots; stage 2 +1 chest choice (6 choices); stage 3 +7% all damage per active combo type.
- **Survival**: +0.6 max HP per point; at 5 points, Advanced-or-better potions grant +1 to a random permanent attribute; at 10 points, +3 backpack slots; at 20 points, -12 slots and **Slate Imprint**.
- **Patience**: +1 Defense per point; at 5 points, a Slate Base with the same effect as the Foundation slate; at 10 points, regenerate 1 HP per second below 7 HP; at 20 points, revive at 60% health once every 10 in-game days (totems take priority).
- **Wisdom**: +1 MP regeneration per point (combat only, every 10 seconds); at 5 points, MP Absorption +5 and max MP +10; at 10 points, all artifacts forced to 【Unique】, duplicates merge when placed in the backpack, and bosses drop 1 extra die; at 20 points, one free chest reroll per chest and +20% XP Drop.

#### Slates

- **36 new slates**, bringing the total from 21 to **58**: 10 Common, 16 Advanced, 5 Rare, 5 Legendary, plus the special **Heaven**.
- **Heaven**: Eternal rarity, +9 to every slot, cannot be rotated; it is not in any loot pool, shop or wish pool — only a 0.01% chance per ancient debris smelted.
- The existing **Double Star** slate now gives +2 to the second slot above and below.
- Twelve generated slate icons received a 1px dark outline.
- **Slate Base** (Patience stage 1): same effect as the Foundation slate (the whole row, 8 slots to each side, +1; no rotation); cannot be dropped or placed in the normal inventory.
- **Slate Imprint** (Survival stage 3): hover a slate in the backpack and hold **F** for 1.5 seconds to consume it while keeping its effect on nearby slots; losing stage 3 drops the slates and removes the effects. The progress bar is drawn by the screen, so rotated slates show it correctly.
- **Rare chests**: bosses always drop one Rare Artifact Chest and one Rare Slate Chest, containing only Rare-or-better rewards.

#### Chests and shop

- The chest screen now has a **dice slot** and a **Reroll** button; dice can be used to reroll the rewards. The button sits directly below the dice slot.
- Wisdom stage 3 gives one free reroll per chest; free rerolls are used first and are not reset by closing/reopening the screen.
- Quick Wit stage 2 turns every chest into a 6-choice chest.

#### Combat and attributes

- **Weapon mana costs** (not shown in weapon tooltips): Sword & Shield 10 per blocked hit / 5 on a perfect block, 15 for the defensive sweep; Greatsword Whirlwind 15; Dagger Parry 7 / perfect parry 5 / Fury 0; Blade sheathed attack 5 (bare-handed if unaffordable, no sweep) / Powerful Slash 0; Quarterstaff Riposte 7 / perfect riposte 3.
- **New attribute Defense**: the same damage-reduction formula as enemy defense (added to armor value) and affected by defense penetration.
- **New attribute MP Absorption**: same scale as HP Steal — each point restores 0.1% of damage dealt as mana.
- **MP regeneration rework**: the old per-second "Mana Regen" is removed; the Attributes page now shows **MP Regeneration**. In combat it restores base 10 + artifacts + Wisdom points every 10 seconds; **out of combat it restores 12.5% of max MP every 0.5 seconds**. The Sapote Fruit affix now displays +10/20/30/40.
- **Forced Unique and artifact merging**: with Wisdom stage 2, all artifacts count as 【Unique】; duplicates merge when placed in the backpack (level +1, combo +1). Merged originals cannot be dropped or placed in the normal inventory, but can be moved within the backpack and placed in the Enchant screen's artifact slot.
- **MP display fix**: the Attributes page shows `current / max`; current mana is clamped when the cap drops; the HUD updates immediately after talent changes.
- **Dagger Focus** now starts at 0; the first right-click is a parry instead of Fury.

#### Root Retreat

- Interaction gate: within 100 blocks of spawn, or 24,000 ticks after placement.
- Three new difficulties: Decay / Tension / Heart Burden.
- Three new blessings: Strength (30 stages), Lightness (20), Courage (40).
- The blessing tooltip now opens to the left of the cursor so it does not run off-screen.

#### Effects and quality of life

- **Enchant-style glint**: red/white for the dagger while it has Focus; blue/white for the blade at full Sword Intent; distinct from vanilla's purple glint.
- **Survival random-attribute message**: fixed the extra `%s`; the "Survival" prefix uses the talent colour.
- **Protected-item movement rules**: the Slate Base and merged artifacts can move within the backpack; merged artifacts can also go into the Enchant artifact slot, but none of them can be dropped, placed in the normal inventory, or swapped to the hotbar.
- **Slate Imprint uses F**: hover a slate and hold F; left-click is back to the vanilla pick-up/drag behaviour.

### Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned, especially the three new talent pages and Slate Imprint.
- The Slate Base itself cannot be imprinted (to avoid infinite stacking from Patience stage 1 re-granting it).
- Slate upgrades, forging and the companion system are not implemented yet.
- Some texts and icons will still change.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib (required on both client and server).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description and LICENSE).
