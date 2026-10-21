# Sephiria Fantasy

**Sephiria Fantasy** is an **unofficial fan mod** that brings the weapon system of *SEPHIRIA* into Minecraft. This project makes **heavy use of AI-generated code** — shared for learning and personal use only, and **commercial use is not allowed**.

> Alpha stage: there is a decent amount of content, but most of it has not been playtested thoroughly yet — numbers and feel are still being tuned.

## Contents

### Six base weapons

Blade / Steel Greatsword / Dagger / Default Sword and Shield / Quarterstaff / Colossal Crossbow — each with its own normal-attack feel and weapon skills (sweeping, whirlwind, dash, area lunge, …). The blade-type weapons are animated with GeckoLib.

### Twelve combo sets and 76 artifacts

Sturdy / Wind Song / Precision / Shadow / Negotiation / Element / Magitech / Dark Cloud / Sun Sword / Embers / Glacier / Frost Armament.

Equipping artifacts of the same set in the Sephiria Backpack raises that combo's tier, and higher tiers give stronger effects. Hover a combo icon to see its full tier table. Artifacts come in five rarities (Common / Advanced / Rare / Legendary / Bond), and a 【Unique】 artifact only counts once no matter how many copies you carry.

### Slates and rare chests

The Sephiria Backpack can also hold **slates** (58 in total: 15 Common / 22 Advanced / 11 Rare / 8 Legendary / 2 Eternal). Most slates change the level of surrounding slots and can be rotated with **R**; **Heaven** is the exception — +9 to every slot, no rotation, and only obtainable at 0.01% per ancient debris smelted. Bosses drop a **Rare Artifact Chest** and a **Rare Slate Chest**; these only contain Rare-or-better rewards.

### Debuffs and artifact skills

- **Shock** (Magitech), **Burn** (Embers) and **Frostbite / Freeze** (Glacier): periodic damage that scales with stacks, plus a freeze that locks a mob completely in place (no AI, no knockback, animation held on the spot) and turns it ice blue; affected mobs show a stack label under their feet. Debuff damage and true damage can crit as well.
- **Artifact skills**: 6 skill slots with rebindable keys — Encouragement Banner, Haste, Keen Eye, Thunder Verdict.

### Attributes and progression

Physical Power, Fire / Ice / Lightning element strength, crit chance and crit damage, dodge, defense, defense-ignoring (true) damage, HP steal, MP absorption, MP regeneration, movement speed, mana, Leaves (currency), and more. **MP regeneration** restores its value every 10 seconds while in combat; out of combat you instead restore 12.5% of maximum MP every 0.5 seconds. The Attributes page is two columns; hover an entry to see its formula.

### Talents

Seven talent pages (Wrath / Swiftness / Survival / Patience / Wisdom / Will / Quick Wit), each with three stages at 5 / 10 / 20 points. Talent points come from achievements, boss kills and leveling up. Highlights:

- **Survival**: max HP per point; Advanced-or-better potions grant +1 to a random permanent attribute; extra backpack slots; stage 3 unlocks **Slate Imprint** (hover a slate in the Sephiria Backpack and hold **F** to consume it while keeping its effect on nearby slots).
- **Patience**: Defense (the same stat enemies use), a Slate Base with the same effect as the Foundation slate, low-health regeneration, and a revival at 60% health once every 10 in-game days (totems take priority).
- **Wisdom**: MP regeneration (combat: every 10 seconds; out of combat: 12.5% max MP every 0.5 seconds), MP Absorption, all artifacts forced to 【Unique】 with duplicate merging, dice from boss kills, and one free chest reroll per chest.

### Root Retreat

A difficulty shrine: within 100 blocks of spawn (or 24,000 ticks after placement) you can open it and pick **three difficulties** (Decay / Tension / Heart Burden) and **three blessings** (Strength at 30 / Lightness at 20 / Courage at 40), plus a set of smaller options.

### Shop and chests

The shop's shelves are rerolled with **Dice**: put dice into the shop's dice slot and press the "Refresh" button to spend one and get a fresh set of goods (placing dice alone does not reroll anything, so you cannot waste one by accident). Your Negotiation stat lowers buy prices. Dice come from **naturally generated loot chests (~10%)** and from **harvesting fully grown carrots (~1%)**; the shop does not buy them back. Chests come in three kinds: Artifact / Slate / Upgrade, plus the Rare Artifact / Rare Slate chests dropped by bosses. Chest rewards can be rerolled with dice, and Wisdom stage 3 gives one free reroll per chest (free rerolls are used first).

### Effects

Red Snake Eye meteors play a four-stage show (appear / fall / impact / vanish); plus Sun Sword slash marks (a straight, glowing cut in one of three random directions), the Thunder Verdict beam, Dark Cloud lightning, custom particles and procedurally generated sound effects.

## Requirements

| | |
|---|---|
| Minecraft | **26.3** |
| Loader | Fabric Loader 0.19.5+ |
| Required | [Fabric API](https://modrinth.com/mod/fabric-api), [GeckoLib](https://modrinth.com/mod/geckolib) (weapon animations) |
| Environment | Required on **both client and server** |

## Installation

1. Install Fabric Loader and both dependencies above;
2. Drop `sephiria-fantasy-<version>.jar` into `mods/`;
3. Launch the game — it works in singleplayer right away; for a server, install the same jar on the server side too.

## Known issues / not done yet

- Most content has not been playtested thoroughly; numbers and feel are still being tuned, especially the three new talent pages and Slate Imprint;
- Slate upgrades, forging and the companion system are not implemented yet;
- Some texts and icons will still change.

## Disclaimer

- **License**: the mod's binaries and source code are released under the fan-made license notice in the repository ([LICENSE](https://github.com/Lch2018/sephiria-fantasy/blob/main/LICENSE); listed on Modrinth as CC BY-NC 4.0). Personal play, private modifications, and redistribution inside free modpacks / servers are allowed **provided this notice is kept and credit is given**; **any commercial use is prohibited — for the original and for modified versions alike**.
- **Fan project**: Sephiria Fantasy is an **unofficial fan work** based on *SEPHIRIA*. It is not affiliated with, endorsed by, or licensed by TEAM HORAY, Mojang Studios or Microsoft.
- **AI-generated code**: this project makes **heavy use of AI-generated code** (models, texture-processing scripts and docs included). It is for learning and technical exchange only — use it at your own judgment; it is provided "as is", without warranty of any kind.
- **Assets**: the weapon artwork under `assets/sephiria_fantasy/textures/` and `tools/weapon-ref/` is taken from the original game *SEPHIRIA* and is the property of **TEAM HORAY**. It is **not covered** by the license above (which covers only the code, data, scripts and docs written for this project), is included as non-commercial fan reference only, and **may not be extracted, redistributed separately, or used commercially**. Rights holders may request its removal.
- **Trademarks**: Minecraft is a trademark of Mojang Synergies AB ｜ *SEPHIRIA* and its setting and artwork belong to TEAM HORAY.
- Source code and issue tracker: <https://github.com/Lch2018/sephiria-fantasy>

---

# 赛菲莉娅幻想（中文）

**赛菲莉娅幻想**：把《SEPHIRIA（赛菲莉娅）》的武器系统搬进 Minecraft 的同人模组。**非官方同人作品**，**大量使用 AI 生成代码**，仅供学习交流，**禁止商业行为**。

> 目前是 A 测（alpha）阶段：内容量不小，但大部分还没经过充分实测，数值与手感会持续调整。

## 内容

### 六把基础武器

刀 / 钢铁巨剑 / 匕首 / 标准剑盾 / 长棍 / 重型弩，各自有独立的普通攻击手感与武器技能（横扫、旋风、突刺、范围突进等）；刀类武器由 GeckoLib 驱动骨骼动画。

### 十二套「连击」与 76 件神器

坚固 / 风之歌 / 精密 / 影子 / 谈判 / 元素 / 魔法科技 / 乌云 / 太阳剑 / 余烬 / 冰川 / 冰霜武具。

背包里凑齐同一套的神器会提升该连击的档位，档位越高效果越强；悬停连击图标可以查看完整档位表。神器分白 / 蓝 / 黄 / 红 / 彩五个品质，带【唯一】的同种神器只算一份。

### 石板与稀有宝箱

赛菲莉娅背包里还能放**石板**（共 58 块：普通 15 / 高级 22 / 稀有 11 / 传说 8 / 永恒 2）。大多数石板会改变周围格子的等级，可按 **R** 旋转；**天国**例外——所有格子 +9、不可旋转，只能在烧炼远古残骸时以每份 0.01% 的概率获得。击败 BOSS 会固定掉一个**稀有神器宝箱**和一个**稀有石板宝箱**，只出稀有及以上品质。

### 减益与神器技能

- **触电**（魔法科技）、**灼伤**（余烬）与**冻伤 / 冰冻**（冰川）：挂在敌人身上的周期伤害，按层数乘算；冻伤叠满会把敌人**完全冻住**（AI 停摆、被打也不动、动画定格、染成冰蓝）。敌人脚下会显示层数标签；减益伤害与真实伤害同样可以暴击。
- **神器技能**：6 个技能栏 + 可自由绑定的按键 —— 鼓励旗帜 / 急速 / 锐利之眼 / 雷之裁决。

### 属性与成长

物理强度、火 / 冰 / 电元素强度、暴击（几率与伤害）、闪避、防御力、无视防御伤害、HP 偷取、MP 吸收、MP再生、移动速度、蓝量、叶子（货币）等十余项属性。**MP再生**在战斗中每 10 秒回复一次；脱战则改为每 0.5 秒回复 12.5% 最大 MP。属性页分两列，悬停出算式。

### 天赋

七条天赋页（愤怒 / 迅捷 / 生存 / 耐心 / 智慧 / 意志 / 急智），每条 5 / 10 / 20 点解锁三个阶段；天赋点来自成就、击杀 BOSS 与升级。要点：

- **生存**：每点最大 HP；喝高级及以上品质药水随机一项属性永久 +1；额外背包格；三阶段解锁**石板刻印**（鼠标指着赛菲莉娅背包里的石板、按住 **F** 消耗它并保留其对周围格子的效果）。
- **耐心**：防御力（与敌人防御同口径）、与石板「基础」效果相同的石板基础、低血回复、每 10 个游戏日一次的 60% 复活（不死图腾优先）。
- **智慧**：MP再生（战斗每 10 秒；脱战每 0.5 秒回 12.5% 最大 MP）、MP 吸收、所有神器强制【独特】与同种合并、BOSS 掉落骰子、每个宝箱一次免费刷新。

### 根之退

难度祭坛：在出生点 100 格内（或放置后满 24000 刻）可以打开，选择**三种难度**（衰退 / 紧张 / 心之重担）与**三个祝福**（力量 30 阶段 / 轻盈 20 阶段 / 勇气 40 阶段），另有一组小选项。

### 商店与宝箱

商店的货架用**骰子**刷新：把骰子放进商店的骰子栏，点「刷新」按钮消耗一枚换一批货（不会放上去就自动刷，免得手滑浪费）；谈判力影响买价折扣。骰子来自**自然生成的箱子（约 10%）**与**收获成熟的胡萝卜（约 1%）**，商店本身不收购它。宝箱有神器 / 石板 / 升级三种，另有 BOSS 掉落的稀有神器 / 稀有石板宝箱。宝箱奖励可以用骰子刷新；智慧三阶段每个宝箱免费刷新 1 次，免费次数优先。

### 特效

红蛇之眼的陨石有四段演出（出现 / 下落 / 撞击 / 消失），另有太阳剑剑痕（一道笔直的发光划痕，三个方向随机）、雷之裁决光束、乌云雷击，以及自定义粒子与程序生成的音效。

## 运行环境

| | |
|---|---|
| Minecraft | **26.3** |
| 加载器 | Fabric Loader 0.19.5+ |
| 必需依赖 | [Fabric API](https://modrinth.com/mod/fabric-api)、[GeckoLib](https://modrinth.com/mod/geckolib)（武器骨骼动画） |
| 环境 | 客户端与服务端都要装 |

## 安装

1. 装好 Fabric Loader 以及上面的两个依赖；
2. 把 `sephiria-fantasy-<版本>.jar` 放进 `mods/`；
3. 启动游戏即可——单人存档直接用，服务器需要服务端也装同样的 jar。

## 已知问题 / 未完成

- 大部分内容尚未充分实测，数值与手感仍在调整，新的三条天赋页与石板刻印尤其需要实测；
- 石板升级、锻造、同伴系统尚未实现；
- 部分文案与图标仍会变动。

## 声明

- **授权**：本模组的二进制与源码按仓库里的《同人模组许可声明》发布（[LICENSE](https://github.com/Lch2018/sephiria-fantasy/blob/main/LICENSE)；
  Modrinth 上标注为 CC BY-NC 4.0，条款以仓库那份为准）。
  允许个人游玩、自用修改，以及在**免费**的整合包 / 服务器中携带分发（**须保留本声明并注明出处**）；
  **原版与修改版一律禁止任何形式的商业行为**。
- **同人性质**：赛菲莉娅幻想是《SEPHIRIA（赛菲莉娅）》的**非官方同人作品**，与原作者 TEAM HORAY、
  与 Mojang Studios / Microsoft 均无任何隶属、授权或合作关系。
- **AI 生成代码**：本项目**大量使用 AI 生成代码**（模型、贴图处理脚本与文档同样如此），
  仅供学习与技术交流，请自行判断是否使用；按「现状」提供，不附带任何形式的担保。
- **素材例外**：`assets/sephiria_fantasy/textures/` 与 `tools/weapon-ref/` 下的武器美术来自原作
  《SEPHIRIA（赛菲莉娅）》，版权归 **TEAM HORAY** 所有，**不在上面的授权范围内**
  （上面的许可只覆盖本项目自己编写的代码、数据、脚本与文档）。这部分美术仅随模组本体在同人范围内
  引用，**不得单独提取、二次分发或用于任何商业用途**；相关权利方如有异议，请联系删除。
- **商标**：Minecraft 是 Mojang Synergies AB 的商标 ｜《SEPHIRIA（赛菲莉娅）》的名称、设定与美术归 TEAM HORAY 所有。
- 源码与问题反馈：<https://github.com/Lch2018/sephiria-fantasy>
