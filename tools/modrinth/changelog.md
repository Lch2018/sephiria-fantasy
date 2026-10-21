大版本更新（`0.3.99-a`）：三条新天赋页、两个新属性、武器蓝耗、宝箱刷新与一批体验修复。

## 这一版有什么

### 三条新天赋页

天赋页现在共七条，界面顺序：**愤怒 → 迅捷 → 生存 → 耐心 → 智慧 → 意志 → 急智**。

- **生存**（#D49ED8）：每点 +0.6 最大 HP；5 点喝高级及以上品质药水随机一项属性永久 +1；10 点赛菲莉娅背包 +3 格；20 点再 -12 格并解锁「石板刻印」——鼠标指着背包里的石板、按住 **F** 键 1.5 秒，石板被消耗，但对周围格子的效果保留。三阶段熄灭时这些石板会掉出来、效果消失。
- **耐心**（#D4D7DD）：每点 +1 防御力（与敌人防御同口径）；5 点获得「石板基础」（效果与石板「基础」相同，不能丢出/放进普通背包）；10 点生命低于 7 时每秒回 1；20 点死亡时以 60% 生命复活，每 10 个游戏日一次（不死图腾优先）。
- **智慧**（#A6DFDE）：每点 +1 战斗 MP 再生（每 10 秒回一次）；5 点 MP 吸收 +5、最大 MP +10；10 点所有神器强制【独特】、同种神器放入背包时合并（等级 +1、连击 +1）、BOSS 额外掉 1 个骰子；20 点每个宝箱免费刷新 1 次、经验掉落 +20%。

### 新属性

- **防御力**：与敌人的防御力同一条减伤公式（并进护甲值），吃防御穿透。
- **MP 吸收**：与 HP 偷取同口径，每点 = 造成伤害的 0.1% 回蓝。
- **MP 再生**：战斗状态下每 10 秒恢复，与原有的每秒「蓝量再生」分开。

### 武器蓝耗（不显示在武器描述中）

盾剑防御挨打 10 / 完美防御 5、横扫 15；巨剑旋风 15；匕首招架 7 / 完美招架 5 / 狂怒 0；刀入鞘普攻 5（蓝不够时退化成空手伤害、无横扫）、强力斩击 0；长棍回击 7 / 完美回击 3。

### 其他新增与修复

- **宝箱刷新**：宝箱界面新增骰子格与「刷新」按钮，可以用骰子换一批奖励；智慧三阶段每个宝箱免费刷新 1 次，免费次数优先，关掉再开不会重置。
- **石板刻印**：长按 F 的进度条由界面直接绘制，旋转过的石板也能正常显示。
- **神器合并**：智慧二阶段点亮后，重复神器会合并；合并过的神器可以在背包内移动，也可以放进附魔界面的神器槽，但不能丢出或放进普通背包。
- **附魔式微光**：匕首有专注时红白、刀剑意满层时蓝白，与原版紫色附魔光区分。
- **MP 显示**：属性页「蓝量」改成「当前 / 上限」，上限变化时当前值会被夹回上限；天赋改最大 MP 后 HUD 立刻刷新。
- **匕首专注**开局改为 0 层。
- **生存随机属性提示**修复多出的 `%s`，「生存」两个字用天赋色显示。

## 已知问题 / 未完成

- 大部分内容仍未经充分实测，数值和手感还在调整；新天赋页尤其需要实测。
- 「石板基础」本身不能刻印（避免耐心一阶段反复发放导致无限叠加）。
- 石板升级、锻造、同伴系统尚未实现。
- 部分文案与图标仍会变动。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib（必需，客户端与服务端都要装）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Major update (`0.3.99-a`): three new talent pages, two new attributes, weapon mana costs, chest rerolls, and a batch of quality-of-life fixes.

### What's in this version

#### Three new talent pages

There are now seven talent pages, in this order: **Wrath → Swiftness → Survival → Patience → Wisdom → Will → Quick Wit**.

- **Survival** (#D49ED8): +0.6 Max HP per point; at 5 points, drinking an Advanced-or-better potion grants +1 to a random permanent attribute; at 10 points, +3 Sephiria Backpack slots; at 20 points, -12 slots and **Slate Imprint** — hover a slate in the backpack and hold **F** for 1.5 seconds to consume it while keeping its effect on nearby slots. Losing stage 3 drops those slates and removes the effects.
- **Patience** (#D4D7DD): +1 Defense per point (the same stat enemies use); at 5 points, a Slate Base with the same effect as the Foundation slate (cannot be dropped or put into the normal inventory); at 10 points, regenerate 1 HP per second below 7 HP; at 20 points, revive at 60% health on death once every 10 in-game days (totems take priority).
- **Wisdom** (#A6DFDE): +1 combat MP regeneration per point (once every 10 seconds); at 5 points, MP Absorption +5 and Max MP +10; at 10 points, all artifacts are forced to 【Unique】, duplicates merge when placed in the backpack (level +1, combo +1), and bosses drop 1 extra die; at 20 points, each chest can be rerolled once for free and XP Drop +20%.

#### New attributes

- **Defense**: uses the same damage-reduction formula as enemy defense (added to armor value) and is affected by defense penetration.
- **MP Absorption**: same scale as HP Steal — each point restores 0.1% of damage dealt as mana.
- **MP Regeneration**: restores mana every 10 seconds while in combat, separate from the existing per-second "Mana Regen" stat.

#### Weapon mana costs (not shown in weapon tooltips)

Sword & Shield: 10 per blocked hit / 5 on a perfect block, 15 for the defensive sweep; Greatsword Whirlwind 15; Dagger Parry 7 / perfect parry 5 / Fury 0; Blade sheathed normal attack 5 (if you cannot pay, it becomes a bare-handed hit with no sweep) / Powerful Slash 0; Quarterstaff Riposte 7 / perfect riposte 3.

#### Other additions and fixes

- **Chest reroll**: the chest screen now has a dice slot and a "Reroll" button; Wisdom stage 3 gives one free reroll per chest, free rerolls are used first, and closing/reopening does not reset them.
- **Slate Imprint**: the F-key progress bar is drawn by the screen itself, so rotated slates show it correctly too.
- **Artifact merging**: with Wisdom stage 2, duplicate artifacts merge; merged artifacts can be moved within the backpack and placed in the Enchant screen's artifact slot, but cannot be dropped or put into the normal inventory.
- **Enchant-style glint**: red/white for the dagger while it has Focus, blue/white for the blade at full Sword Intent, distinct from vanilla's purple glint.
- **MP display**: the Attributes page now shows `current / max`; current mana is clamped to the max when the cap drops; the HUD updates immediately after talent changes.
- **Dagger Focus** now starts at 0 instead of 1.
- **Survival random-attribute message** no longer shows an extra `%s`, and the "Survival" prefix uses the talent colour.

### Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned, especially the new talent pages.
- The Slate Base itself cannot be imprinted (to avoid infinite stacking from Patience stage 1 re-granting it).
- Slate upgrades, forging and the companion system are not implemented yet.
- Some texts and icons will still change.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib (required on both client and server).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description and LICENSE).
