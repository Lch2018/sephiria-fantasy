版本 `0.5.2-a`：从 `0.5.1-a` 之后的内容更新。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.5.1-a`）开始写。

## 这一版有什么

### 新神器（10 件）

神器池从 **83 件**扩到 **93 件**：

- **破碎的蓝宝石**（影子 / 稀有 / 【唯一】）：闪避 +2/4/6、防御力 +2/4/6；每 16/14/12 秒获得一层「帷幕」，必然闪避下一次有来源实体的攻击。
- **神射手的曲柄**（坚固 / 传说 / 【唯一】）：弩重装速度 +30/50%、弩攻击伤害 +30/40%；装填完成后 10 秒内弩伤害再 +30/40%。
- **装填用木箱**（坚固 / 稀有 / 【唯一】）：最大弹匣数量 +0/1/1/2。
- **名匠的刀格**（坚固 / 传说 / 【唯一】）：武器伤害 +5/10/15%；刀收刀 / 拔刀无敌时间 +40/70/120%。
- **彩虹羽毛**（风之歌 / 传说 / 【唯一】）：匕首获取专注时额外获取 0/0/1 层；特殊攻击伤害 +10/16/22%；冲刺恢复 +5/5/10%。
- **绝对戒指**（坚固 / 传说 / 【唯一】）：特殊攻击范围 +30/45/60%、特殊攻击伤害 +10/20/45%。
- **翅膀**（风之歌 / 传说 / 【唯一】）：攻击速度 +3/6/9/12/15/18%；每 25/20/15/9/6/5% 额外攻击速度，使所有伤害放大 +1%。
- **贝鲁特之镰**（精密 / 传说 / 【唯一】）：暴击几率 +2.5/5/7.5/10%；攻击暴击几率超过 100% 时，溢出的部分转化为处决几率；处决使该次暴击伤害翻倍。
- **巴尔特的工作用单片眼镜**（精密 / 传说 / 【唯一】）：暴击几率 +10/19/28/35/45%；敌人距离 3 格以内时，暴击伤害 -30 个百分点。
- **黑鳞片**（精密 / 传说 / 【唯一】）：每持有 1 个石板物品，暴击几率 +1/1.5/2/2.5%；石板数量包括赛菲莉娅背包与石板刻印，合成石板算 2 块；提示框显示当前持有数量。

### 新药水（12 瓶）

- 蓝：**马迪朗的平静**（防御 +3）、**格里塞特的浅淡**（闪避 +3）、**贵兹的合奏**（最高元素 +2）。
- 黄：**内比奥罗的执着**（物理 +4）、**乌尼·布兰的雾霭**（冰 +4）、**Tempranillo 的热情**（火 +4）、**马尔贝克的浓郁**（电 +4）、**蘑菇汤**（最高元素 +6，随机一种）、**马德拉的坚韧**（最终生命值 +5%）、**萨格兰蒂诺的忍耐**（防御 +5）、**普萨的留白**（闪避 +5）。
- 红：**黎明之露**（所有伤害放大 +4%，永久）。

新增五种药水效果类型：防御、闪避、最高元素、最终生命值百分比、所有伤害放大。赛菲莉娅宝藏的元素池现在同时包含高级（+2）与稀有（+4）两档，同一元素内等概率。

### 神铸与许愿池

- 神铸新增普通 / 高级两档：普通 30、高级 50 愿力；稀有 80、传说 200；羁绊 / 永恒不可神铸。
- 神铸改为多选批量购买：底部正中间显示「需 x 愿力」，愿力足够时数字绿色、不足时红色；新增「重置」按钮清空已选，「确认」移到「重置」右边。
- 神铸结果页返回神铸界面，不再回到基础许愿池页。
- 锚定页 / 神铸页 / 抽卡结果页悬停物品都显示完整神器描述。
- 抽到已锚定物品时，愿力按该物品的锚定次数额外 +60/70/80/90/100%（向下取整）；结果页显示服务端实际到账的愿力。
- 通过神铸获得物品不再获得愿力。

### 选卡

- 选卡充能周期从 2 个游戏日缩短为 **1 个游戏日**，最多仍可存 10 次。
- 选中一张卡后不再退回游戏：还有机会就立刻展示下一组三张；机会用完也停在选卡界面显示 0 次，由玩家自己关闭。
- 悬停已翻开的卡会显示完整神器提示框。

### 弩与战斗

- 重型弩改为多弹匣：每个弹匣 10 发，打空后自动换下一个弹匣；按 R 可补满当前弹匣与备用弹匣；HUD / 提示框显示「弹匣：x/10（m）」。
- 闪烁的眼睛重做：最高等级改为 4；混沌伤害改为「当前闪避点数 × 150/300/500/900/1500%」，不再按税前伤害计算，也不再额外乘物理增幅。
- HP 偷取 / MP 吸收的伤害基数改为「最终伤害 ÷ 顽强之躯倍率」：只把「顽强之躯」那一层减伤加回去，护甲、防护铠甲等其它减伤仍然保留。

### 界面与修复

- 神器提示框等级行改为「等级 x/y」。
- 装填用木箱的名字由「装填用木箱坚固」更正为「装填用木箱」，所属连击更正为「坚固」。
- 新神器图标按统一规则处理：去掉外框与内部背景、保留原图深色边、14×14 居中；药水图标去掉黑边、15×15 居中，均不再额外描边。
- 神器数量与中英文说明同步更新到 93。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。ModMenu 与 ToroHealth-Continued 可选。

## 已知问题 / 未完成

- 大部分内容仍处于 A 测阶段，尚未经过充分实测；数值与手感可能继续调整。
- 1.20.1 / 1.21.1 移植分支不在本次发布范围内。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.5.2-a`: content updates after `0.5.1-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.5.1-a`).

## What's new

### New artifacts (10)

The artifact pool grows from **83 to 93**:

- **Broken Sapphire** (Shadow / Rare / [Unique]): Dodge +2/4/6 and Defense +2/4/6; every 16/14/12 seconds, gain a Veil that fully dodges the next attack from an entity.
- **Sharpshooter's Crank** (Sturdy / Legendary / [Unique]): crossbow reload speed +30/50%, crossbow damage +30/40%; for 10 seconds after reloading, crossbow damage +30/40% more.
- **Reloading Crate** (Sturdy / Rare / [Unique]): maximum magazines +0/1/1/2.
- **Master's Sword Guard** (Sturdy / Legendary / [Unique]): weapon damage +5/10/15%; katana sheathe / draw invulnerability +40/70/120%.
- **Rainbow Feather** (Wind Song / Legendary / [Unique]): dagger focus gain +0/0/1; special attack damage +10/16/22%; dash recovery +5/5/10%.
- **Absolute Ring** (Sturdy / Legendary / [Unique]): special attack range +30/45/60%; special attack damage +10/20/45%.
- **Wings** (Wind Song / Legendary / [Unique]): attack speed +3/6/9/12/15/18%; every 25/20/15/9/6/5% extra attack speed grants +1% to all damage.
- **Beirut's Scythe** (Precision / Legendary / [Unique]): crit chance +2.5/5/7.5/10%; when attack crit chance exceeds 100%, the excess becomes Execution chance; Execution doubles that critical hit's damage.
- **Bart's Work Monocle** (Precision / Legendary / [Unique]): crit chance +10/19/28/35/45%; crit damage -30 percentage points while the enemy is within 3 blocks.
- **Black Scale** (Precision / Legendary / [Unique]): crit chance +1/1.5/2/2.5% per slate item held; the count includes slates in the Sephiria backpack and imprinted slates, with Fusion Slates counting as 2; the tooltip shows the current count.

### New potions (12)

- Blue: **Madiran's Calm** (Defense +3), **Grisette's Paleness** (Dodge +3), **Gewürz's Ensemble** (highest element +2).
- Yellow: **Nebbiolo's Persistence** (Physical +4), **Ugni Blanc's Mist** (Ice +4), **Tempranillo's Passion** (Fire +4), **Malbec's Richness** (Lightning +4), **Mushroom Soup** (highest element +6, one random element), **Madeira's Fortitude** (final max HP +5%), **Sagrantino's Patience** (Defense +5), **Poulsard's Whitespace** (Dodge +5).
- Red: **Dawn Dew** (all damage +4%, permanent).

Five new potion effect kinds were added: Defense, Dodge, Highest Element, Max HP percent, and All Damage amplification. Sephiria treasure elemental pools now include both Advanced (+2) and Rare (+4) potions, with equal odds within the same element.

### Forge and Wishing Pool

- Forge now supports Common and Advanced tiers: Common 30, Advanced 50 Wish Power; Rare 80, Legendary 200; Bond / Eternal cannot be forged.
- Forge is now a batch multi-select. The bottom center shows "x Wish Power required", with the number green when affordable and red when not. A new Reset button clears the selection, and Confirm moved to its right.
- The forge result screen returns to the forge page instead of the base Wishing Pool page.
- Hovering items on the anchor page, forge page, and draw result page now shows the full artifact tooltip.
- Drawing an anchored item grants extra Wish Power based on its anchor count: +60/70/80/90/100% (rounded down). The result screen shows the actual amount granted by the server.
- Items obtained through forging no longer grant Wish Power.

### Card Selection

- The Card Selection charge period is shortened from 2 in-game days to **1 in-game day**; the cap remains 10.
- After choosing a card, the screen no longer returns to the game: if chances remain, the next set of three cards is shown immediately; if no chances remain, the screen stays open showing 0 chances and the player closes it manually.
- Hovering a revealed card shows the full artifact tooltip.

### Crossbow and combat

- The Colossal Crossbow now has multiple magazines: each magazine holds 10 bolts, and the next magazine is loaded automatically when one is empty. Press R to refill the current and reserve magazines. HUD / tooltip show "Magazine: x/10 (m)".
- Flickering Eye rework: max level is now 4; chaos damage is now "current dodge points × 150/300/500/900/1500%", no longer based on pre-tax damage and no longer multiplied by physical amplification.
- HP steal / MP absorb now use "final damage ÷ Tenacious Body multiplier" as their base: only the Tenacious Body reduction is added back; armor, Protective Armor, and other reductions remain applied.

### Interface and fixes

- The artifact tooltip level line now reads "Level x/y".
- The Reloading Crate's name was corrected from "Sturdy Reloading Crate" to "Reloading Crate", and its combo to Sturdy.
- New artifact icons follow the unified rules: outer frame and inner background removed, original dark edge kept, fitted to 14×14 and centered. Potion icons have their black border removed, are fitted to 15×15 and centered, with no extra outline.
- Artifact counts and the Chinese / English descriptions were updated to 93.

## Requirements

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (both client and server). ModMenu and ToroHealth-Continued are optional.

## Known issues / unfinished

- Most content is still in alpha and has not been thoroughly playtested; numbers and feel may keep changing.
- The 1.20.1 / 1.21.1 port branches are not part of this release.

## Disclaimer

Unofficial fan mod; much of the code is AI-generated; **commercial use is prohibited** (see the project description and LICENSE).
