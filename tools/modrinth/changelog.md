版本 `0.4.1-a`：从 `0.4.0-a` 之后的全部改动。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.4.0-a`）开始写。

## 这一版有什么

### 出生点判定

- 天赋加点与根之退的「出生点 100 格内」判定，除世界出生点外，现在也认**玩家自己设置的出生点**（床 / 重生锚 / `/spawnpoint`）；个人出生点必须与当前位置在同一维度。
- 天赋界面打开时会向服务端要一次最新状态（个人出生点只有服务端知道），按钮是否可点不再依赖进服时那一次同步。

### 弩

- 「弩矢」从「赛菲莉娅武器」创造标签页移除（物品仍然注册，它是弩的弹药）。
- 修复弩矢打不到**末影龙 / 末地水晶**：之前只对生物实体结算伤害，而末影龙命中的是它的部位实体、末地水晶根本不是生物，两类目标整段被跳过。现在与原版箭一样走 `hurtOrSimulate`；同一发弩矢穿过龙头和身体只结算一次。
- 第三人称手持姿势：原版只在弩**上弦后**才把手臂摆成「举弩朝准星方向」，赛菲莉娅弩没有上弦状态，现在常态就是这个姿势（挥击时手臂正常挥动，第一人称的手不受影响）。

### 武器模型

- **匕首**：新模型（16 个元素）与新贴图。
- **巨剑**：新模型（19 个元素）与新贴图。
- **盾剑**：右手剑（25 个元素）与左手盾（20 个元素，盾面加厚）新模型。
- **刀**：新模型（38 个元素），外加多批动画更新。
- 四把武器的手持模型 JSON 都按新工程重新导出：手持的朝向、位置、缩放跟着工程里的 display 设置走。

### 刀（GeckoLib）

- 手持 display 接进游戏：`models/item/blade_in_hand.json` 写进工程里调好的第三人称右手、第一人称右手 / 左手的位置与缩放，并补回 -90 基准旋转（第一人称左手是 +90 镜像）。
- 收刀 / 拔刀的过渡动画分两套：**第一人称**播带动作的 `switch_to_sheathed` / `switch_to_unsheathed`；**第三人称**播静止版 `switch_to_*_third`（姿态直接到位、全程不动），0.65 秒后进入待机。
- 动画由**每个客户端**按自己看到的视角触发（服务端不再发包）：自己第一人称看到的是带动作那套，第三人称（自己的和别人的）看到的是静止版；第一人称右键后立刻切第三人称，那段过渡会被掐掉。
- 顺带修复：进服时手里拿着一把入鞘的刀，现在直接是入鞘待机（以前会停在出鞘待机）。

### 天赋与属性

- **防御力公式改为 `x / (x + 80)`**（防御力 80 时减伤 50%）：不再并进护甲值，而是在护甲结算之后作为一层独立减伤；坠落 / 火焰 / 真实伤害这类绕过护甲的伤害不吃。
- 属性页「防御力」那一行，鼠标悬停时显示换算出来的「减伤：x%」，只算防御力这一项。
- 耐心二阶段「生命低于 7 点时每秒恢复 1 点」下调为**每秒 0.5 点**。
- 根之退「防护铠甲」的敌人防御力改为 **+30/50/70/100/150**（1..5 级），并按同一条 `x / (x + 80)` 公式折算。

## 已知问题 / 未完成

- 大部分内容仍未经充分实测，数值与手感还在调整。
- 四把武器只换了手持 3D 模型，背包 / 地面图标仍是旧的 2D 图。
- 石板升级、锻造、同伴系统尚未实现。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API + GeckoLib（必需，客户端与服务端都要装）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.4.1-a`: everything after `0.4.0-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.4.0-a`).

### What's in this version

#### Spawn checks

- The "within 100 blocks of spawn" check used by talent editing and the Root Retreat now also accepts **the player's own spawn point** (bed / respawn anchor / `/spawnpoint`), in addition to the world spawn. The personal spawn point must be in the same dimension.
- Opening the Talents screen now asks the server for a fresh state (only the server knows the personal spawn point), so the buttons no longer depend on the single sync sent at join.

#### Crossbow

- The **Crossbow Bolt** is no longer listed in the Sephiria Weapons creative tab (the item is still registered; it is the crossbow's ammo).
- Fixed the bolt dealing no damage to the **Ender Dragon and End Crystals**: the bolt only resolved damage against living entities, but the dragon is hit through its part entities and the crystal is not a living entity at all, so both were skipped entirely. It now goes through `hurtOrSimulate` like a vanilla arrow, and a single bolt passing through the dragon's head and body only counts once.
- Third-person hold pose: vanilla only raises the arms into the "aiming with a charged crossbow" pose after the crossbow is charged; the Colossal Crossbow has no charge state, so it now holds that pose at all times (arms swing normally when attacking; first-person hands are unaffected).

#### Weapon models

- **Dagger**: new model (16 elements) and texture.
- **Steel Greatsword**: new model (19 elements) and texture.
- **Default Sword and Shield**: new right-hand sword (25 elements) and left-hand shield (20 elements, thicker face) models.
- **Blade**: new model (38 elements) plus several animation updates.
- All four in-hand model JSONs were re-exported from the new projects, so orientation, position and scale follow the display settings authored in Blockbench.

#### Blade (GeckoLib)

- In-hand display is now wired into the game: `models/item/blade_in_hand.json` carries the third-person right hand and first-person right / left hand position and scale, plus the -90 base rotation (first-person left hand uses the +90 mirror).
- The sheathe / unsheathe transition now has two variants: **first person** plays the animated `switch_to_sheathed` / `switch_to_unsheathed`, **third person** plays a still version `switch_to_*_third` (pose snaps into place, no motion) and then idles after 0.65 s.
- Animations are triggered **by each client** according to the perspective it is rendering (the server no longer sends animation packets): your own first-person view gets the animated variant, third-person views (yours and other players') get the still one. Switching to third person right after toggling cuts the transition.
- Also fixed: joining the game while holding a sheathed katana now shows the sheathed idle instead of the unsheathed one.

#### Talents and attributes

- **The Defense formula is now `x / (x + 80)`** (80 Defense = 50% reduction): it is no longer merged into the armor value but applied as a separate layer after armor absorption. Damage that bypasses armor (fall, fire, true damage) is unaffected.
- Hovering the **Defense** row on the Attributes page shows the resulting "damage reduction: x%", counting only the Defense stat.
- Patience stage 2 regenerates **0.5 health per second** below 7 health (was 1).
- The Root Retreat's **Protective Armor** now gives enemies **+30/50/70/100/150** Defense (levels 1..5), converted with the same `x / (x + 80)` formula.

### Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned.
- The four weapons only have new in-hand 3D models; the inventory / ground icons are still the old 2D art.
- Slate upgrades, forging and the companion system are not implemented yet.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API + GeckoLib (required on both client and server).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description and LICENSE).
