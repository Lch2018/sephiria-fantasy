版本 `0.4.2-a`：从 `0.4.1-a` 之后的全部改动。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.4.1-a`）开始写。

## 这一版有什么

### 赛菲莉娅流浪商人

- 交易改成独立的自定义界面：一排 **9 格**（7 格道具 + 2 格药水），点一格直接拿走；全部免费，每格一件、卖完不补货。
- 打开界面时商人会被定住，关掉后恢复走动；走开 8 格以上自动关闭。商品库存仍用原版 `MerchantOffer` 的 `uses`，随存档走。
- 去掉「点一格免费领取…」那行提示，面板相应收紧；左下角新增「石板合成」按钮。
- 每个商人、每个玩家只能**成功合成一次**石板：用掉后按钮置灰，服务端也会拒绝再次打开。

### 石板合成（新）

- 新物品 **「合成石板」**（红色 / 传说品质）：效果由参与合成的两块石板决定，图案存在物品组件里，所以每块合成石板可以不同。
- 界面：最上面标题，中间 **7×7** 预览（一屏画完、不滚动），下面一行「两个输入格 / 合成 / 输出格」，再下面是玩家背包。
- 输入格只收普通石板：天国、石板基础、已经合成过的合成石板都不能放。
- 两块输入按各自当前朝向展开，同一相对格子的增量**相加**，抵消成 0 的格子丢掉；鼠标指着输入石板按 **R** 可旋转。
- 可旋转性随输入：两块输入都可旋转，结果才能按 R 旋转；**任一输入不可旋转，结果也不可旋转**。
- 合成石板不能再参与合成；不进宝箱 / 商店 / 许愿池 / 挖矿掉落，卖回商店也不收。
- 每个商人每个玩家只能成功合成一次；只打开界面看看、没合成就关掉不会消耗。
- 关界面时输入 / 输出格里剩下的东西会还给玩家。

### 神器与天赋

- 新属性 **「增加连击数量」**：每件有连击的神器默认提供 1 级连击，提示框显示「连击名 +1」（灰色）；合并一次后变 **+2**（绿色）并封顶。
- **智慧二阶段「神器合并」重做**：等级低的那件被消耗，留下的等级 **+= 被消耗的等级 + 1**（至少 +1，夹在神器上限）；连击 +1（封顶 2）；合并后的神器就是普通神器，可以移出背包，熄灭阶段不还原；点亮阶段不再把重复神器丢出来。
- **生存三阶段**不再扣 12 格背包；石板刻印照旧。
- **耐心一阶段**不再发「石板基础」，改成背包第一行（前 6 格）格子等级 +1。
- 删掉 `BackpackRules`：两类受保护物品没了，背包 / 附魔菜单的拦截与关界面光标物品回收一起删掉。
- **石板刻印改成永久**：生存三阶段熄灭后，已刻印石板的效果仍然保留，石板也不会掉出来；阶段只决定「能不能刻」。

### 进度页

- 「赛菲莉娅幻想」标签页整个重做，标签页图标改为**模组图标**；旧的武器展示条目删除，新根仍用 `minecraft:tick` 解锁全部 8 张合成表。
- 三条线：战斗（剑盾）、收集（小麦）、探索（桅杆模型）。
  - 战斗：杀戮1（任意击杀 10）→ 匕首1（匕首击杀僵尸 5）/ 刀1（刀击杀僵尸 5）；狙击1（10 格外击杀 5）→ 弩1（弩在 10 格外击杀骷髅 3）。
  - 收集：神器收集1（5 种高级品质及以下神器）/ 石板收集1（5 种高级品质及以下石板）。
  - 探索：观光客1（进入过地狱和末地）/ 旅行1（经过 2 个不同群系）。
- 计数在服务端：击杀 / 距离 / 武器 / 收集（按物品种类去重）/ 维度 / 群系；匕首、刀、弩要等父节点完成才补发。
- 任务节点完成会弹成就、送四叶草与天赋点；根和分类节点 `show_toast:false`，不送。
- 说明：原版进度页**不显示实时 x/10**，只显示描述里的目标数；达到目标才完成。

## 已知问题 / 未完成

- 大部分内容仍未经充分实测，数值与手感还在调整。
- 进度页不显示实时计数；要实时 `x/10` 需要另做 HUD 或自定义进度界面。
- 石板升级、锻造、同伴系统尚未实现。
- 四把武器只换了手持 3D 模型，背包 / 地面图标仍是旧的 2D 图。
- 石板合成、进度页、商人一次限制都是本版新增，尚未进游戏验收。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API + GeckoLib（必需，客户端与服务端都要装）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.4.2-a`: everything after `0.4.1-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.4.1-a`).

### What's in this version

#### Sephiria Wandering Trader

- Trading now uses a dedicated custom screen: a row of **9 slots** (7 goods + 2 potions), click a slot to take the item. Everything is free, one item per slot, never restocked.
- The trader is held in place while the screen is open and resumes walking when it closes; walking more than 8 blocks away closes the screen. Stock still uses the vanilla `MerchantOffer` `uses`, saved with the world.
- The "click a slot to take it for free" hint was removed and the panel tightened accordingly. A **Slate Fusion** button was added at the bottom-left.
- **Each player can successfully fuse slates only once per trader**: after that the button is greyed out and the server rejects opening the fusion screen again.

#### Slate Fusion (new)

- New item **Fusion Slate** (red / Legendary): its effect is decided by the two slates used in the fusion. The pattern is stored in an item component, so every Fusion Slate can be different.
- Screen: title at the top, a **7×7** preview in the middle (fully visible, no scrolling), then a row with two input slots / Fuse button / output slot, and the player inventory below.
- Input slots accept regular slates only: Heaven, Slate Base and already-fused slates are rejected.
- Both inputs are expanded using their current rotation, and deltas on the same relative cell are **added**; cells that cancel to 0 are dropped. Hover an input slate and press **R** to rotate it.
- Rotatability follows the inputs: the result can be rotated only if both inputs are rotatable; **if either input cannot rotate, the result cannot rotate either**.
- Fusion Slates cannot be used in another fusion, are not in chests / shops / the wish pool / mining drops, and cannot be sold back to the shop.
- Each player can successfully fuse only once per trader; merely opening the screen and closing it without fusing does not consume the chance.
- Closing the screen returns anything left in the input / output slots to the player.

#### Artifacts and talents

- New property **Combo Count**: every artifact with a combo provides 1 combo level by default, shown as "combo +1" in grey; after one merge it becomes **+2** (green) and is capped there.
- **Wisdom stage 2 "artifact merge" was reworked**: the lower-level artifact is consumed and the remaining one gains **the consumed artifact's level + 1** (at least +1, capped at the artifact's maximum); combo count +1 (capped at 2). The merged artifact is a normal artifact: it can be moved out of the backpack and toggling the talent stage off does not restore the consumed copy. Lighting the stage no longer throws duplicate artifacts out.
- **Survival stage 3** no longer removes 12 backpack slots; slate imprinting is unchanged.
- **Patience stage 1** no longer gives a Slate Base; instead the first backpack row (6 slots) gets +1 slot level.
- `BackpackRules` was removed: the two protected item types are gone, and the menu blocking / cursor-item return logic was removed with them.
- **Slate imprinting is now permanent**: turning Survival stage 3 off no longer reverts imprinted slate effects or drops the slates; the stage only decides whether imprinting is allowed.

#### Progress tab

- The "Sephiria Fantasy" advancement tab was rebuilt; the tab icon is now the **mod icon**. The old weapon display entries were removed, and the new root still unlocks all 8 recipes with `minecraft:tick`.
- Three lines: Combat (sword and shield), Collection (wheat), Exploration (mast model).
  - Combat: Slaughter I (kill any mob 10) → Dagger I (kill 5 zombies with a Dagger) / Blade I (kill 5 zombies with a Blade); Snipe I (kill 5 from at least 10 blocks away) → Crossbow I (kill 3 skeletons with a Colossal Crossbow from at least 10 blocks away).
  - Collection: Artifact Collection I (5 different artifacts of Advanced rarity or lower) / Slate Collection I (5 different slates of Advanced rarity or lower).
  - Exploration: Tourist I (visit the Nether and the End) / Traveler I (visit 2 different biomes).
- Counting is server-side: kills / distance / weapon / collection (deduplicated by item type) / dimensions / biomes. Dagger I, Blade I and Crossbow I are only granted after their parent advancement is done.
- Task advancements show a toast and award a clover and a talent point when completed; the root and category nodes use `show_toast:false` and award nothing.
- Note: the vanilla advancement screen does **not** show live `x/10` counters; it only shows the target in the description and completes when reached.

### Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned.
- The progress tab has no live counters; showing live `x/10` would need a HUD or a custom progress screen.
- Slate upgrades, forging and the companion system are not implemented yet.
- The four weapons only have new in-hand 3D models; the inventory / ground icons are still the old 2D art.
- Slate fusion, the progress tab and the per-trader fusion limit are all new in this version and still need in-game acceptance testing.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API + GeckoLib (required on both client and server).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description and LICENSE).
