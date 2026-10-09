# SEPHIRIA

An **unofficial fan mod** that brings the weapon system of *SEPHIRIA* into Minecraft. This project makes **heavy use of AI-generated code** — shared for learning and personal use only, and **commercial use is not allowed**.

> Alpha stage: there is a decent amount of content, but most of it has not been playtested thoroughly yet — numbers and feel are still being tuned.

## Contents

### Six base weapons

Blade / Steel Greatsword / Dagger / Default Sword and Shield / Quarterstaff / Colossal Crossbow — each with its own normal-attack feel and weapon skills (sweeping, whirlwind, dash, area lunge, …). The blade-type weapons are animated with GeckoLib.

### Ten combo sets and 55 artifacts

Sturdy / Wind Song / Precision / Shadow / Negotiation / Element / Magitech / Dark Cloud / Sun Sword / Embers.

Equipping artifacts of the same set in the Sephiria Backpack raises that combo's tier, and higher tiers give stronger effects. Hover a combo icon to see its full tier table. Artifacts come in five rarities (Common / Advanced / Rare / Legendary / Bond), and a 【Unique】 artifact only counts once no matter how many copies you carry.

### Debuffs and artifact skills

- **Shock** (Magitech) and **Burn** (Embers): periodic damage that scales with stacks; affected mobs show a stack label under their feet. Debuff damage and true damage can crit as well.
- **Artifact skills**: 6 skill slots with rebindable keys — Encouragement Banner, Haste, Keen Eye, Thunder Verdict.

### Attributes and progression

Physical Power, Fire / Ice / Lightning element strength, crit chance and crit damage, dodge, defense-ignoring (true) damage, movement speed, mana, Leaves (currency), and more. The Attributes page is two columns; hover an entry to see its formula.

### Shop and chests

The shop's shelves are rerolled with **Dice**: put dice into the shop's dice slot and press the "Refresh" button to spend one and get a fresh set of goods (placing dice alone does not reroll anything, so you cannot waste one by accident). Your Negotiation stat lowers buy prices. Dice come from **naturally generated loot chests (~10%)** and from **harvesting fully grown carrots (~1%)**; the shop does not buy them back. Chests come in three kinds: Artifact / Slate / Upgrade.

### Effects

Red Snake Eye meteors play a four-stage show (appear / fall / impact / vanish); plus Sun Sword slash marks, the Thunder Verdict beam, Dark Cloud lightning, custom particles and procedurally generated sound effects.

## Requirements

| | |
|---|---|
| Minecraft | **26.3** |
| Loader | Fabric Loader 0.19.5+ |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api), [GeckoLib](https://modrinth.com/mod/geckolib) (weapon animations) |
| Environment | Required on **both client and server** |

## Installation

1. Install Fabric Loader and both dependencies above;
2. Drop `sephiria-<version>.jar` into `mods/`;
3. Launch the game — it works in singleplayer right away; for a server, install the same jar on the server side too.

## Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned;
- Backpack expansion, slate upgrades, forging and the companion system are not implemented yet;
- Some texts and icons will still change.

## Disclaimer

- Not affiliated with Mojang Studios or Microsoft. *SEPHIRIA* and its settings belong to their original author, TEAM HORAY.
- This project makes **heavy use of AI-generated code**. It is for learning and personal use only, and **commercial use is prohibited** — see the LICENSE file in the repository for the full terms.
- Source code and issue tracker: <https://github.com/Lch2018/sephiria>

---

# SEPHIRIA（中文）

把《SEPHIRIA（赛菲莉娅）》的武器系统搬进 Minecraft 的同人模组。**非官方同人作品**，**大量使用 AI 生成代码**，仅供学习交流，**禁止商业行为**。

> 目前是 A 测（alpha）阶段：内容量不小，但大部分还没经过充分实测，数值与手感会持续调整。

## 内容

### 六把基础武器

刀 / 钢铁巨剑 / 匕首 / 标准剑盾 / 长棍 / 重型弩，各自有独立的普通攻击手感与武器技能（横扫、旋风、突刺、范围突进等）；刀类武器由 GeckoLib 驱动骨骼动画。

### 十套「连击」与 55 件神器

坚固 / 风之歌 / 精密 / 影子 / 谈判 / 元素 / 魔法科技 / 乌云 / 太阳剑 / 余烬。

背包里凑齐同一套的神器会提升该连击的档位，档位越高效果越强；悬停连击图标可以查看完整档位表。神器分白 / 蓝 / 黄 / 红 / 彩五个品质，带【唯一】的同种神器只算一份。

### 减益与神器技能

- **触电**（魔法科技）与**灼伤**（余烬）：挂在敌人身上的周期伤害，按层数乘算，敌人脚下会显示层数标签；减益伤害与真实伤害同样可以暴击。
- **神器技能**：6 个技能栏 + 可自由绑定的按键 —— 鼓励旗帜 / 急速 / 锐利之眼 / 雷之裁决。

### 属性与成长

物理强度、火 / 冰 / 电元素强度、暴击（几率与伤害）、闪避、无视防御伤害、移动速度、蓝量、叶子（货币）等十余项属性；属性页分两列，悬停出算式。

### 商店与宝箱

商店的货架用**骰子**刷新：把骰子放进商店的骰子栏，点「刷新」按钮消耗一枚换一批货（不会放上去就自动刷，免得手滑浪费）；谈判力影响买价折扣。骰子来自**自然生成的箱子（约 10%）**与**收获成熟的胡萝卜（约 1%）**，商店本身不收购它。宝箱有神器 / 石板 / 升级三种。

### 特效

红蛇之眼的陨石有四段演出（出现 / 下落 / 撞击 / 消失），另有太阳剑划痕、雷之裁决光束、乌云雷击，以及自定义粒子与程序生成的音效。

## 运行环境

| | |
|---|---|
| Minecraft | **26.3** |
| 加载器 | Fabric Loader 0.19.5+ |
| 必需依赖 | [Fabric API](https://modrinth.com/mod/fabric-api)、[GeckoLib](https://modrinth.com/mod/geckolib)（武器骨骼动画） |
| 环境 | 客户端与服务端都要装 |

## 安装

1. 装好 Fabric Loader 以及上面的两个依赖；
2. 把 `sephiria-<版本>.jar` 放进 `mods/`；
3. 启动游戏即可——单人存档直接用，服务器需要服务端也装同样的 jar。

## 已知问题 / 未完成

- 大部分内容尚未充分实测，数值与手感仍在调整；
- 背包扩容、石板升级、锻造、同伴系统尚未实现；
- 部分文案与图标仍会变动。

## 声明

- 与 Mojang Studios / Microsoft 无任何关系；《SEPHIRIA（赛菲莉娅）》相关设定归原作者 TEAM HORAY 所有。
- 本项目**大量使用 AI 生成代码**，仅供学习交流，**禁止任何商业行为**；完整条款见仓库里的 LICENSE。
- 源码与问题反馈：<https://github.com/Lch2018/sephiria>
