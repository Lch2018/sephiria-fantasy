补丁版：修掉「背包满时获得的物品被吞掉」——现在装不下的会从玩家身上掉出来。

## 修正：背包满时物品不再凭空消失

受影响的交付点：攒经验发的**升级宝箱**、**商店买的东西**、**开箱奖励**、**药水给的骰子**。

根因是判断条件本身：这些地方原本写「`背包.add(物品)` 返回 false 才掉落」，但 `add` 的返回值只回答「有没有装进去一点」，不是「全装进去了」。于是有两种丢东西的情况：

- 背包**真的满**且玩家在**创造模式**时，`add` 会把整份物品直接清空并返回 true（原版认为创造玩家可以自己刷），掉落那一步永远不会执行，物品谁也没拿到；
- 只装得下**一部分**时（例如药水一次给 5 颗骰子、背包只剩 3 颗的位置），`add` 同样返回 true，调用方以为交付完了，剩下 2 颗被吞掉。

现在统一走原版自带的「逐个格子塞、塞不下就把剩下的整份掉出来」那条路（`Inventory#placeItemBackInInventory`），生存与创造模式行为一致。附带：**整摞物品只装得下一部分时，剩下的也会掉出来**（以前会被静默吞掉）。

## 未实测

根之退（难度调整方块）、许愿池（抽卡）、配方书的「赛菲莉亚」页、击退冷却、太阳剑掉落物满上限消失，以及上一版的经济调整——都还等着实机过一遍。

---

## English

A patch release fixing "items obtained with a full inventory get swallowed" — whatever does not fit now drops from the player instead.

### Fix: items no longer vanish when the inventory is full

Affected delivery points: the **Upgrade Chest** from experience, **shop purchases**, **chest rewards**, and **dice granted by potions**.

The root cause was the check itself: these sites decided whether to drop with "if `inventory.add(stack)` returns false, drop it", but `add`'s return value only answers "did *anything* go in", not "did *everything* go in". Two ways to lose items followed:

- with a **genuinely full** inventory **in creative mode**, `add` clears the whole stack and returns true (vanilla assumes creative players can spawn anything), so the drop never ran and nobody got the item;
- when only **part** of the stack fits (e.g. a potion grants 5 dice with room for 3), `add` also returns true, the caller believes it is done, and the remaining dice are swallowed.

All of them now go through vanilla's own "fill slot by slot, drop the whole remainder when nothing fits" path (`Inventory#placeItemBackInInventory`), behaving the same in survival and creative. Bonus: when only part of a **stack** fits, the remainder now drops as well (it used to be silently swallowed).

### Not yet play-tested

The Root Retreat (difficulty block), the Wishing Pool (gacha), the "Sephiria" recipe book tab, the knockback cooldown, dropped Sun Swords vanishing at the capacity cap, and the previous release's economy tuning.
