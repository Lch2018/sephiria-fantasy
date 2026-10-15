补丁版：修掉一个 26.3 战利品表格式变动引起的掉落 bug（胡萝卜），并给校验脚本加了对应的守卫。

## 修正

- **胡萝卜的掉落**：**没长熟的胡萝卜收割时只掉 1 根**（原版口径），**长熟的（age = 7）**才多掉 1 根（吃时运附魔）、并按 1% 概率掉一枚骰子。
  · 之前的行为是「未长熟也固定掉 2 根胡萝卜 + 1 枚骰子」——26.3 把战利品表的字段名换了一套
    （`functions` → `modifier`、`conditions` → 单个 `condition`、方块状态条件换成 `match_block`），
    而**旧写法不会报错**：表照常加载、条件整段失效，于是「只有长熟才生效」的池子永远在掉。
  · 根之退与许愿池的方块掉落表也用了同一种旧写法（`survives_explosion` 没生效），一并改对了
    ——现在被爆炸破坏时不会再掉落。

## 其他

- `tools/check-resources.ps1` 新增一条守卫：所有战利品表里只要还写着 `functions` / `function` /
  `conditions` / `minecraft:block_state_property` 这些旧写法就直接报错（这类问题在游戏里是静默的，
  必须在构建期拦下来）。

---

## English

A patch release fixing a drop bug caused by Minecraft 26.3's loot table format change (carrots), plus a guard for it in the check script.

### Fixes

- **Carrot drops**: harvesting an **immature** carrot now drops **1 carrot** (vanilla behaviour); only a **fully grown** one (age = 7) drops the extra carrot (with Fortune) and rolls the 1% dice.
  · Previously an immature carrot always dropped 2 carrots plus a dice: 26.3 renamed the loot table fields (`functions` → `modifier`, `conditions` → a single `condition`, block-state conditions became `match_block`) and the **old spellings do not error** — the table still loads and the conditions simply never apply, so pools meant to be gated on a fully grown crop always rolled.
  · The Root Retreat and Wishing Pool block loot tables used the same legacy spelling (`survives_explosion` never applied); they are fixed too — neither block drops when destroyed by an explosion any more.

### Other

- `tools/check-resources.ps1` gained a guard: any loot table still using `functions` / `function` / `conditions` / `minecraft:block_state_property` fails the check (in game this class of mistake is silent, so it has to be caught at build time).
