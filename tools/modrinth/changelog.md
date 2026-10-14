这一版加入**羁绊神器「永恒蚀」**：一件让「冰」与「火」对调的太阳剑 × 冰霜武具双连击神器。

## 新增

### 永恒蚀（太阳剑 + 冰霜武具，羁绊·【唯一】·4 档）

它看自己待在**赛菲莉亚背包的哪半边**（6 格宽，前三列是左半边）决定把哪一套改成对侧元素：

- **放在左半边**：**冰霜武具**整套改成**火焰属性**——剑气 / 锤子 / 冰长矛 / 冰弓的伤害按**火元素强度**结算、伤害类型换成火属性（于是挂上的是「灼伤」而不是「冻伤」），特效从冰蓝换成**焰黄**（剑气弧、飞行本体、尾迹、落地的雪环全部换色）；
- **放在右半边**：**太阳剑**整套改成**冰属性**——伤害按**冰元素强度**结算、伤害类型换成冰属性（挂「冻伤」），敌人身上那道剑痕与**掉在地上的太阳剑**都变成**冰蓝**。

另外两行词条：

- **冰霜武具的伤害 +6/13/21/30%**、**太阳剑伤害 +6/13/21/30%**（与「小魔法海螺」「索利斯·德库萨 / 陨铁镜」的同类词条相加）；
- **冰霜武具的攻击也会触发太阳剑**：剑气 / 锤击 / 冰长矛 / 箭矢每打中一个敌人，都照太阳剑那一套多划一道剑痕、多掉一支剑。

**图标会跟着换色**：左半边是焰黄（月亮转金黄）、右半边是冰蓝（太阳转蓝）、不在背包里是默认那张——这是本模组第一次让同一个物品按状态换模型（靠物品组件 + 原版 `minecraft:select` 模型定义，不需要客户端代码）。

它同时给**太阳剑与冰霜武具各 +1 级**，所以带上它之后太阳剑能吃到 10 档、冰霜武具能吃到 8 档。

## 说明

- 剑气的粒子、掉落的太阳剑、锤子 / 冰长矛 / 冰弓箭矢的贴图都各多了一套对侧配色，全部由脚本从已有素材重上色生成；
- 音效没有改动（只换了颜色）；
- 掉落池现在 74 件，商店里羁绊档（3 件）的出现率仍按「稀有与传说之间」配权重。

---

## English

This release adds the **bond artifact "Eternal Eclipse"**: a Sun Sword × Frost Armament dual-combo artifact that swaps ice and fire.

### New: Eternal Eclipse (Sun Sword + Frost Armament, Bond, Unique, 4 levels)

It reads **which half of the Sephiria backpack it sits in** (the grid is 6 wide; the first three columns are the left half) and flips one of the two sets to the opposite element:

- **In the left half**: the whole **Frost Armament** set turns **fire-based** — the sword qi, hammer, ice spear and ice bow scale off **Fire element** and deal fire damage (so they apply **Burn** instead of Frostbite), and every effect changes from ice blue to **flame yellow** (the qi arc, the flying bodies, the trails, the impact ring);
- **In the right half**: the whole **Sun Sword** set turns **ice-based** — it scales off **Ice element** and deals ice damage (applying **Frostbite**), and both the slash mark and the **Sun Swords that drop on the ground** turn **ice blue**.

Two more affixes:

- **Frost Armament damage +6/13/21/30%** and **Sun Sword damage +6/13/21/30%** (added on top of the same affixes from Small Magic Conch / Solis Decusa / Meteoric Mirror);
- **Frost Armament attacks also trigger Sun Sword**: every enemy hit by the sword qi, the hammer, a spear or an arrow also takes a slash mark and drops one more sword.

**The icon changes colour with it**: flame yellow in the left half (the moon turns gold), ice blue in the right half (the sun turns blue), and the default art when it is not in the backpack. This is the mod's first "same item, different model by state" (an item component plus a vanilla `minecraft:select` model definition — no client code).

It also gives **+1 level to both Sun Sword and Frost Armament**, so carrying it lets the Sun Sword combo reach tier 10 and Frost Armament reach tier 8.

### Notes

- The qi particle, the dropped Sun Sword and the hammer / spear / arrow sprites each gained a second colour variant, all recoloured from existing art by script;
- No sound changes (colours only);
- The loot pool is now 74 artifacts; the shop keeps the bond tier (3 artifacts) weighted between rare and legendary.
