版本 `0.6.0-a`：从上一个发布版本 `0.5.3-a` 起的累积更新。26.3、26.1.2、1.20.1 三个分支统一使用 `0.6.0-a`，不再带 `-mc26.1.2` / `-mc1.20.1` 后缀；1.21.1 不包含。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.5.3-a`）开始写。

## 这一版有什么

### 三个分支
- **26.3（main）**、**26.1.2（port/26.1.2）**、**1.20.1（port/1.20.1-0.5.3.8）** 同时构建为 `0.6.0-a`，三者内容与数值按各自平台适配。
- 1.20.1 移植补齐武器 / 神器 / 石板 / 商店 / 许愿池 / 角色界面等主体内容，并处理了 GeckoLib 4、旧式网络包、模型旋转限制等平台差异。

### 武器
- **提示框精简**：六把武器的描述改为紧凑标签式（`[伤害]`、`[减伤]`、`[免疫]`、`[耗蓝]`、`[位移]`、`[蓄力]`、`[弹反]`、`[消耗]`、`[远程]`），动作提示改为灰色文本，不再重复大段数值说明。
- **技能射程**：所有发动型神器和魔法书技能的射程 / 锁定距离提高到原来的 **150%**（AOE 半径不变）。
- **匕首·狂怒**：位移距离 8 → **10.4** 格。
- **钢铁巨剑·旋风**：位移 5 → **7.5** 格，伤害半径 4 → **6** 格（垂直 1.5 → 2.25），并伤害整条突进路径。
- **刀·强力斩击**：改为类似狂怒的**空放**，朝准星方向突进 **8.32** 格，路径与落点都结算伤害，半径 5.5 → **7.15** 格。
- **技能特效**：匕首招架/狂怒、巨剑旋风、刀强力斩击、长棍三段回击加入原版 TNT 的**部分爆炸粒子**（白爆点 + 烟，不用整段大爆炸动画），分层铺满实际伤害盒，浓度约为原来的 30%，无爆炸伤害与音效。
- **武器耐久**：六把武器统一为原版铁剑的 **250** 点耐久，不再不可破坏；耐久条恢复原版，不再显示剑意 / 弹匣。普通攻击每次命中扣 1；射击扣 1；防御（每次挡下伤害）、横扫、旋风、招架、狂怒、强力斩击、回击 1/2/3 各扣 2。
- **附魔限制**：赛菲莉娅武器不能再与任何原版附魔书在铁砧里合成（结果为空、费用 0）。
- 刀的普通攻击提示去掉 `[充能]`，只保留 `[伤害]` + 「获得剑意」。

### 背包、HUD 与界面
- HUD 默认值调整：**经验加加（EXP++）**与**无敌条**默认关闭，**税前伤害**默认开启；旧配置会按 `layout_version=2` 迁移一次。
- HUD 模块支持鼠标直接拖动；神器升级后技能栏自动同步等级。
- 护盾 HUD 与无敌条、经验加加的显示与层级修正。

### 根之退与掉落
- 根之退总阶段 ≥40 时，BOSS 掉落的稀有神器宝箱 / 稀有石板宝箱各 +1；≥50 时各 +2。
- 宝箱打开再关闭后复制一份到背包的问题修复；创造模式取回与叠加规则同步。

## 运行环境
- **26.3**：Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。
- **26.1.2**：Minecraft 26.1.2 + Fabric Loader 0.19.5+ + 对应 Fabric API / GeckoLib。
- **1.20.1**：Minecraft 1.20.1 + Fabric Loader 0.20.1 + Fabric API 0.92.x + GeckoLib 4.8.x。
- ModMenu 与 ToroHealth-Continued 可选；伤害显示相关修正面向 ToroHealth-Continued。

## 已知问题 / 未完成
- A 测阶段：内容量大，仍有部分系统没有经过完整实测，数值与手感会继续调整。
- 26.1.2 / 1.20.1 是面向对应 Minecraft 版本的移植构建，个别第三方模组组合可能出现平台特有问题。
- 永恒蚀与冰霜武具、书库之阳、伤害显示等联动仍在继续排查与完善。

## 声明
本模组是《SEPHIRIA（赛菲莉娅）》的**同人模组**，**大量使用 AI 生成代码**，仅供学习与个人游玩，**禁止任何商业行为**。

---

## English

Version `0.6.0-a`: cumulative update since the previous release `0.5.3-a`. The 26.3, 26.1.2 and 1.20.1 branches all use `0.6.0-a` with no `-mc26.1.2` / `-mc1.20.1` suffix; 1.21.1 is not included.

### The three branches
- **26.3 (main)**, **26.1.2 (port/26.1.2)** and **1.20.1 (port/1.20.1-0.5.3.8)** are all built as `0.6.0-a`, with content and numbers adapted to each platform.
- The 1.20.1 port now covers weapons, artifacts, slates, shop, Wishing Pool and character screens, with adaptations for GeckoLib 4, legacy networking and model-rotation limits.

### Weapons
- **Compact tooltips**: weapon descriptions now use colored tags (`[Damage]`, `[Reduction]`, `[Immunity]`, `[Mana]`, `[Displacement]`, `[Charge]`, `[Parry]`, `[Consume]`, `[Ranged]`) plus gray action hints instead of long numeric paragraphs.
- **Skill range**: all activated artifact and magic-book skill range / lock-on distance increased to **150%** of the original (AOE radius unchanged).
- **Dagger · Fury**: dash distance 8 → **10.4** blocks.
- **Steel Greatsword · Whirlwind**: dash 5 → **7.5** blocks, damage radius 4 → **6** blocks (vertical 1.5 → 2.25), and enemies along the dash path are damaged too.
- **Blade · Powerful Slash**: can now be cast with no target, dashing **8.32** blocks along the crosshair; both the path and landing area deal damage, radius 5.5 → **7.15** blocks.
- **Skill effects**: Dagger Parry/Fury, Greatsword Whirlwind, Blade Powerful Slash and all three Quarterstaff Ripostes now use **part of the vanilla TNT explosion particles** (white puffs + smoke, not the full explosion animation), distributed across the actual damage box at about 30% of the previous density, with no explosion damage or sound.
- **Durability**: all six weapons now have **250** durability like a vanilla iron sword and are no longer unbreakable. The vanilla durability bar replaces the Sword Intent / magazine bar. Normal attacks cost 1 per hit; firing costs 1; Defend (per blocked hit), Sweep, Whirlwind, Parry, Fury, Powerful Slash and Riposte 1/2/3 cost 2 each.
- **Enchanting limit**: Sephiria weapons can no longer be combined with any vanilla enchanted book in an anvil (empty result, zero cost).
- Blade normal-attack tooltip drops `[Charge]` and keeps `[Damage]` plus “Gain Sword Intent”.

### Backpack, HUD and UI
- HUD defaults: **EXP++** and the **Invulnerability bar** are off by default, **Pre-tax Damage** is on; old configs migrate once via `layout_version=2`.
- HUD modules can be dragged with the mouse; artifact skill slots now resync their levels after an upgrade.
- Shield HUD, invulnerability bar and EXP++ display/layer fixes.

### Root Retreat and loot
- At total Root Retreat stage ≥40, BOSS rare artifact chests and rare slate chests each gain +1; at ≥50, each gains +2.
- Fixed chests duplicating into the player inventory after opening and closing; creative retrieval and stacking rules synchronized.

## Environment
- **26.3**: Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (required on both client and server).
- **26.1.2**: Minecraft 26.1.2 + Fabric Loader 0.19.5+ + matching Fabric API / GeckoLib.
- **1.20.1**: Minecraft 1.20.1 + Fabric Loader 0.20.1 + Fabric API 0.92.x + GeckoLib 4.8.x.
- ModMenu and ToroHealth-Continued are optional; damage-display fixes target ToroHealth-Continued.

## Known issues / unfinished
- Alpha stage: a large amount of content still needs thorough playtesting; numbers and feel will keep changing.
- 26.1.2 / 1.20.1 are ports for their respective Minecraft versions; some third-party mod combinations may expose platform-specific issues.
- Eternal Eclipse / Frost Armament, Sun Sword Library and damage-display interactions are still being investigated.

## Disclaimer
This is an **unofficial fan mod** for *SEPHIRIA*. It makes **heavy use of AI-generated code**, is shared for learning and personal play only, and **commercial use is not allowed**.
