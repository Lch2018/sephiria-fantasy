这一版加入**第十二套连击「冰霜武具」**：一套围绕「充能」与「弹药」的四件神器玩法，外加 HUD、传说品质配色与新的 mod 图标。

## 新增

### 连击：冰霜武具（第十二套）

档位：(2) +6% 冰霜武具充能速度 / (4) +8% 充能速度 / (6) +6% 冰霜武具的伤害 / (8) +8% 伤害 / (10) 额外触发 1 次。

四件神器各自对应一套机制：

- **驱邪剑鞘**（高级·【唯一】）：6 秒充能一次，**挥击武器**（打中敌人与挥空都算）就沿准星方向放出一道冰属性剑气——飞 18 格、沿途一圈都吃伤害、可以穿墙，不需要瞄准敌人；
- **暴风雪之锤**（高级·【唯一】）：**战斗中**每 7 秒充能，**冲刺**时朝 18 格内最近的敌人扔出一把旋转的铁斧，落地时周围一圈都吃伤害；
- **沃尔斯帕**（稀有·【唯一】）：**战斗中**每 6 秒充能，周围有敌人时**自动**从玩家身边的随机位置飞出冰长矛——贯穿敌人、可以穿墙、飞 36 格，6 级时一轮两根；
- **冰弓**（传说·【唯一】·发动型神器）：0.8 秒自动装填 1 发（最多 7 发），发动技能时把储存的箭矢**每 0.05 秒一发**朝准星方向打出去。

配套：**冰之翼**（传说·【唯一】，冰霜武具额外触发 + 暴击伤害）、**冰封的心脏**（稀有，冰霜武具充能速度 +10..100%）、**小魔法海螺**（高级，冰霜武具的伤害与暴击伤害）。

充能类在**脱战时直接满充能**，但只有进入战斗状态才会自动攻击（沃尔斯帕）。伤害类型都是冰属性：吃暴击、减伤与黄金之手，并照常触发冰川的「霜之触」；此外**全部可以穿墙**。

### HUD

左下角新增一行 **「冰霜武具：剑 锤 矛 弓x/7」**：带着哪几件就显示哪几个字，可用的显示冰蓝、不可用的灰；位置与缩放可以在 HUD 布局设置里调。

### 其他

- **传说（红）品质的名字改成 `#FF008E`**（神器 / 石板 / 药水，以及 10 档连击名同步）；
- **新的 mod 图标**：睡在草方块上的兔子（作者亲绘）。

## 修正

- 剑气的贴图粒子朝向重做（弧的鼓包朝飞行方向），特效整体下移 0.3 格——第一人称下现在能看清那道弧；
- 锤子 / 冰长矛 / 冰弓箭矢的本体改成冰蓝重上色（铁斧 / 铁长矛 / 箭的贴图重映射到冰霜渐变）；
- 锤子与冰弓箭矢改为从玩家周围 2 格内的随机位置射出；长矛的朝向修正（原先矛尖朝后）。

---

## English

This release adds the **twelfth combo, "Frost Armament"**: a set of four artifacts built around *charging* and *ammunition*, plus a HUD line, the legendary colour change and a new mod icon.

### New: Frost Armament combo

Tiers: (2) +6% Frost Armament charge speed / (4) +8% charge speed / (6) +6% Frost Armament damage / (8) +8% damage / (10) one extra trigger.

- **Exorcism Scabbard** (Advanced, Unique): charges every 6 s; **swinging a weapon** (hitting or missing) fires an ice sword qi along your crosshair — 18 blocks, damages everything around its path, passes through walls, no aiming required;
- **Blizzard Hammer** (Advanced, Unique): charges every 7 s **in combat**; **dashing** throws a spinning iron axe at the nearest enemy within 18 blocks, with an area hit on landing;
- **Volspa** (Rare, Unique): charges every 6 s **in combat**; with enemies nearby, ice spears **automatically** fly in from random spots around the player — piercing, passing through walls, 36 blocks of travel, two spears per round at level 6;
- **Ice Bow** (Legendary, Unique, activation artifact): reloads 1 arrow every 0.8 s (max 7); activating fires every stored arrow along your crosshair, **one every 0.05 s**.

Supporting pieces: **Ice Wings** (Legendary, Unique — extra Frost Armament triggers and crit damage), **Frozen Heart** (Rare — Frost Armament charge speed +10..100%), **Small Magic Conch** (Advanced — Frost Armament damage and crit damage).

The charge effects refill **instantly out of combat** but only fire automatically (Volspa) while in combat. All of their damage is ice damage: it crits, respects damage reduction and Golden Hands, triggers Glacier's Frost Touch — and **passes through walls**.

### HUD

A new bottom-left line, **"Frost Armament: 剑 锤 矛 弓x/7"**: only the artifacts you carry show up, usable ones are ice blue and cooling ones grey. Position and scale are adjustable in the HUD layout screen.

### Other

- **Legendary (red) quality names are now `#FF008E`** (artifacts / slates / potions, plus the level-10 combo name);
- **New mod icon**: a rabbit asleep on a grass block (drawn by the author).

### Fixes

- The sword qi's particle facing was reworked (the arc's bulge points along the flight direction) and the effect dropped 0.3 blocks, so the arc is actually visible in first person;
- The hammer / ice spear / ice bow arrow are now ice-blue recolours (the vanilla axe / spear / arrow textures remapped onto the frost ramp);
- The hammer and the ice bow's arrows now come from random spots within 2 blocks of the player, and the spear's tip direction was fixed (it used to fly backwards).
