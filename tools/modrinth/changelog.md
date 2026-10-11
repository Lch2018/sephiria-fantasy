本版新增第十一套连击 **冰川**（霜之触 / 冻伤 / 冰冻）与 4 件冰系神器，重做了太阳剑的剑痕特效，并真正修好了「打不出伤害」的老问题。

## 新连击：冰川（第十一套）

- **霜之触**：造成伤害时给目标附加**冻伤**，冷却 2 秒（6 档加速后 0.8 秒）；触发口径与电击之触 / 火焰之触一致（武器 / 魔法书伤害，以及攻击打出的冰属性伤害），冰冻自己的结算算减益伤害、不再触发它。
- **冻伤**：移动速度 −25%、持续 10 秒、最多 5 层（叠加刷新时间），特效是原版雪片粒子；叠到 5 层就**变成冰冻**。
- **冰冻**：一次相当于冰元素强度 200% 的冰属性伤害，并让目标**完全冻住** 2 秒 —— AI 停摆（不攻击、不寻路、不转头）、**被打也不会动**（击退 / 重力 / 水流全拦）、四肢停在冻住那一刻的姿势，同时整体**染成冰蓝**、脚下显示「冰冻」标签。
- 档位：(2) 激活霜之触 / (4)(6) 冰属性伤害 +6/+8 / (8) 霜之触冷却加速 150% / (10) 冰冻所需的冻伤层数 −1。

## 新神器（冰川 3 件 + 余烬×冰川 1 件）

- **睿智护符**（普通）：冰属性伤害 +3/4/6/8；
- **蓝爪**（传说）：冻伤赋予时 30/40/50/60% 概率额外附加一次；冰属性伤害 +2/4/6/9；最大 MP +5/10/15/20；
- **普罗修姆戒指**（普通）：武器 / 魔法书命中附加 5/10/15 冰属性伤害（冷却 2/1.5/1 秒）；
- **封印的特加斯**（羁绊，双连击：余烬 + 冰川）：**灼伤变更为蓝色灼伤** —— 每跳强度改成「火元素强度 + 冰元素强度」，伤害元素类型取两者中更高的一方（蓝火特效）；灼伤异常状态额外伤害 +3/6/9/12%。

## 太阳剑剑痕重做

敌人身上那道划痕从「尘埃粒子拼的线」换成**一张贴图粒子**：一道**笔直的发光划痕**（观感照原版横扫之刃的剑痕，但直形），更细、带柔光，**三个方向每发随机**（3→9 点 / 3→7 点 / 5→11 点）。

## 修复

- **「打有减益的敌人 / 高攻速时丢伤害」真正修好了**：26.3 里清受击无敌帧要写 `LivingEntity.damageCooldownTime`，而之前写的是 `Entity#setInvulnerableTime` —— **是两个不同字段**，所以那几处「清帧」一直是空操作。现在全仓库统一走一个入口，减益每跳不再给目标留无敌窗口。
- **商店的羁绊概率**：调到稀有与传说之间（每格约 3.1%，稀有 8.9%、传说 1.0%）。
- 修掉一处文案格式化 bug（「+33%」里的裸 `%` 会让整行退回未替换的原文，火红的夕阳提示框因此显示成 `%s%%`）；资源检查脚本加了守卫，以后写错直接构建失败。

## 说明

- 神器共 66 件（11 套连击）；冰系 4 件已进掉落池、创造模式页签与商店 / 宝箱抽取。
- 冰川目前 4 件神器 → 连击最高 4 级，6/8/10 档要等以后的神器（余烬 / 魔法科技同理）。

---

## English

This version adds the eleventh combo **Glacier** (Frost Touch / Frostbite / Freeze) with 4 ice artifacts, reworks the Sun Sword's slash mark, and actually fixes the long-standing "attacks deal no damage" problem.

### New combo: Glacier (11th)

- **Frost Touch**: damaging a mob applies **Frostbite**, cooldown 2 s (0.8 s at tier 6); the trigger rules match Electric / Flame Touch (weapon / grimoire damage plus attack-dealt ice damage), while Freeze's own hit counts as debuff damage and does not trigger it.
- **Frostbite**: −25% movement speed, 10 s, up to 5 stacks (stacking refreshes the timer), vanilla snowflake particles; at 5 stacks it turns into **Freeze**.
- **Freeze**: one ice hit equal to 200% of your Ice element strength and the target is **completely frozen** for 2 s — no AI (no attacks, no pathfinding, no head turning), **no movement even when hit** (knockback, gravity and water are all blocked), limbs held in the pose they had at that instant, plus an ice-blue tint and a "Frozen" label under its feet.
- Tiers: (2) activate Frost Touch / (4)(6) ice damage +6/+8 / (8) Frost Touch cooldown accelerated by 150% / (10) Frostbite stacks needed to Freeze −1.

### New artifacts (3 Glacier + 1 Embers×Glacier)

- **Amulet of Wisdom** (Common): Ice element damage +3/4/6/8;
- **Blue Claw** (Legendary): 30/40/50/60% chance to apply one extra Frostbite stack; Ice element damage +2/4/6/9; max MP +5/10/15/20;
- **Proshium Ring** (Common): weapon / grimoire hits deal an extra 5/10/15 Ice damage (cooldown 2/1.5/1 s);
- **Sealed Tegas** (Bond, dual combo: Embers + Glacier): **Burn becomes Blue Burn** — each tick uses Fire + Ice element strength and takes its damage type from whichever is higher (blue flame visuals); Burn extra damage +3/6/9/12%.

### Sun Sword slash rework

The scratch on the enemy is no longer a line of dust particles but a **textured particle**: one **straight glowing cut** (the look of the vanilla Sweeping Edge mark, but straight), thinner, softly glowing, and **randomly one of three directions** per throw (3→9 o'clock / 3→7 / 5→11).

### Fixes

- **"Attacking a debuffed enemy / high attack speed loses damage" is actually fixed now**: in 26.3 the hurt cooldown lives in `LivingEntity.damageCooldownTime`, while the old code wrote `Entity#setInvulnerableTime` — **two different fields**, so those "clear the frames" calls were silent no-ops. All of them now go through one entry point, so a debuff tick no longer leaves an invulnerability window on the target.
- **Shop Bond odds**: moved to sit between Rare and Legendary (about 3.1% per shelf slot; Rare 8.9%, Legendary 1.0%).
- Fixed a text-formatting bug (a bare `%` in "+33%" made the whole line fall back to its unformatted source — Crimson Sunset's tooltip showed `%s%%`); the resource checker now guards against it.

### Notes

- 66 artifacts across 11 combos; the four ice artifacts are in the loot pool, creative tab and shop / chest rolls.
- Glacier has only 4 artifacts, so the combo caps at level 4 for now — tiers 6/8/10 need future artifacts (same for Embers / Magitech).
