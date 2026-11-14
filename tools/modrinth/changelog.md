版本 `0.5.3-a`：从 `0.5.2-a` 之后的全部内容更新。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.5.2-a`）开始写。

## 这一版有什么

### 新连击与神器（93 → 150 件，12 → 15 种连击）

- **学院**（新增）：2 档魔法书加速 +12；4 档魔法书 MP 消耗 -15%；6 / 8 档加速 +16 / +20；10 档魔法书伤害 +15%、MP再生 +8（加速逐档累加，10 级共 +48）。
- **湖泊**（新增）：3 / 6 / 9 档分别最大 MP +20 / +30 / +40、MP再生 +5、消耗 MP 的能力伤害 +10% / +25%（逐档累加，9 级共最大 MP +90、能力伤害 +35%）。
- **守护**（新增）：2 / 4 / 6 / 8 / 10 档防御力 +6 / +7 / +8 / +10 / +12（10 级共 +43）；10 档额外 +5%「无视防御」，与防御穿透乘算，只作用于原版护甲与防御力。

#### 新魔法书（11 本）

- **火焰魔弹 / 寒冰魔弹 / 闪电魔弹**（普通）：1 秒冷却、2 MP；寒冰一次两枚，闪电命中点半径 2 格 AOE，投射物沿准星飞 36 格。
- **火焰箭**（高级）：4 枚火焰箭以 8° 夹角齐射，穿透敌人并结算半径 3 的 AOE，首次命中附加灼伤。
- **霜刀**（高级）：在玩家周围召唤 4 把霜刃，延迟 1 秒后各自锁定并飞向附近敌人，命中后爆炸。
- **雷电回旋镖**（高级）：去程飞 18 格后折返；返程被施放者接住时，对应技能栏冷却减半。
- **火焰马戏团**（稀有）：8 个火球独立爆炸，可重复命中，技能暴击率与暴击伤害额外提高。
- **箭雨**（稀有）：瞄准 48 格内区域，在半径 8 内降下 45 支冰箭，每支独立结算并有概率附加冻伤。
- **雷霆之甲**（稀有）：持续 10 秒，周期性对周围敌人释放连锁闪电。
- **陨石雨**（传说）：在半径 18 的区域降下 30 / 35 / 40 / 45 颗陨石，每颗独立爆炸并带岩浆块本体。
- **冰风暴**（传说）：在命中点召唤持续 8 秒、会追踪敌人的冰风暴，每 0.25 秒攻击范围内敌人；带下雪粒子与地面结霜。
- **雷之裁决**（重做）：伤害改为 3 + 闪电 20 / 24 / 28 / 32 / 36%，释放 10 次电击光束，粒子按伤害走廊铺满。

#### 学院神器（16 件）

魔法师的硬币（书加速 / 书奖励概率）、雷伊星碎片（左侧魔法书的 MP 消耗降低）、鼠魔法师的吊坠（三魔弹追踪、MP再生）、珍珠粉（书伤）、渴望宝珠（占用 MP + 书伤）、海棠果（书加速 + 冲刺恢复）、双角管（魔弹三路分叉，伤害与 MP 消耗联动）、音叉（施放魔法书攒层，武器命中消耗层数打混沌伤害）、宝石盔甲（书暴击 / MP再生 / 物理降低）、发光的沙漏（右侧魔法书加速）、战斗法师的手套（书伤 + 移速）、冥想书（书伤 + 书加速）、学院钢笔（闪避转书伤，闪避成功加速魔法书冷却）、书库之阳（书伤强化并额外触发太阳剑）、空剑柄（书伤 / 最终 MP / MP再生）、莱莉的怀表（书加速 / 暴伤 / 书 MP 降低）。

#### 湖泊神器（16 件）

枯萎的花、恩多迪特的镇纸、稻草人偶（暴击 / MP再生 / 最大 MP）、星红宝石（最终 HP）、星海蓝宝石（最终 MP）、水袋（消耗 MP 提高最大 MP，有上限与冷却）、诱惑的鱼饵（海之眷顾 / 饵钓）、奥布拉斯之血（MP 吸收）、破碎的树根（最大 MP）、心形胡萝卜（最大 HP / MP）、白色树枝（MP再生）、石纺锤（消耗 MP 的能力伤害）、饱满果实桶（发动型回蓝）、乔洛的浇水壶（最大 MP 达标时魔法书额外施放一次）、水之精灵（发动型 WaterGrab，按最大 MP 结算伤害）、普利特维采的水滴（冰霜武具命中召唤旋转冰剑）。

#### 守护神器（14 件）

布甲、护盾（魔法书，10 点护盾 6 秒）、盾牌耳环、破碎的镜子（脱战 / 脱战激活时给盾）、荆棘壳（防御力反伤）、瑟伦潦草的信（累计消耗 MP 给盾）、锅盖（物理暴击叠「忍耐」防御）、学院锁子甲、蜥蜴板甲、小镜子（冲刺给盾）、白边面包（受伤后无敌）、野兽之心（防御 / 所有伤害放大 / 幸运）、盾牌包（每 10 点防御力加火冰电伤害）、岩象（发动型岩石矛）。

### 护盾与防御

- 新增通用**护盾系统**：按层记录护盾量、到期时间与可选来源键；受击时先扣护盾，剩余伤害才进入原版护甲 / 防御力减伤。护盾本身不吃任何减伤，临时盾先消耗、永久盾留到最后。
- 新增**护盾 HUD**：显示「护盾：x」，护盾 > 0 时出现，默认在冲刺 HUD 右侧。
- 「守护」连击的防御力并入玩家防御力；10 档「无视防御」与防御穿透乘算，同时作用于原版护甲、玩家防御力和根之退「防护铠甲」的敌人防御力。
- 白边面包的无敌改为通用无敌窗口，受伤后完全免疫后续伤害，并显示无敌条。

### 魔法书系统

- 魔法书提示框与技能页显示**实际冷却与实际 MP 消耗**（含学院、雷伊星碎片、发光的沙漏、双角管等修正）。
- 技能页技能名按神器品质上色；技能 HUD 顺序改为 1 在最左、6 在最右。
- 新增魔法书暴击率、魔法书伤害、魔法书 MP 减少等统一聚合；音叉、书库之阳、学院钢笔等联动已接入。
- 魔法书伤害改用独立的 `magic_*` 伤害类型，ToroHealth 能正确显示火 / 冰 / 电单一元素。

### 元素、伤害与特效

- ToroHealth 元素显示修正：普通元素攻击显示单一元素；神器伤害按实际来源显示；蓝色灼伤为火 + 冰，等离子体为火 + 电，两者同时生效时为火 + 冰 + 电；酷寒断头台为冰 + 电（永恒蚀左半边时火 + 电）；雷霜蝶两段独立伤害但每段都是冰 + 电。
- 太阳剑伤害改为独立结算，避免和武器本体伤害合并成一个数字。
- 冰风暴最终表现：18 片旋转雪 + 18 片每刻随机散布雪、8 个零重力冰晶高光、随等级增加的下雪粒子与地面结霜。
- 普利特维采的水滴：旋转冰剑改用冰弓箭模型，缩放 50%、无雪片拖尾；结束时模型飞向 8 格内最近敌人，飞抵后才结算伤害；永恒蚀左半边时整把剑改为火属性。
- 修复冰弓火焰箭在永恒蚀火模式下错误显示成神器「火焰箭」魔法书图标的问题，新增独立的火焰箭投射物贴图与模型。

### HUD 与界面

- HUD 设置新增「统一调整」（一次调整所有模块缩放）与「重置」（恢复默认布局）。
- 无敌条默认水平居中，缩放时保持中心不动；「护盾」HUD 默认在冲刺右侧。
- 书的铁砧合并扩展到效率与横扫之刃；攻速接入非赛菲莉娅武器与挖掘。
- 原「疲劳」机制改名「经验加加（EXP++）」，内部 id 保持兼容。
- Mod Menu 简介更新为「把《SEPHIRIA》的 build 带进 Minecraft——武器、连击神器、石板」。

### 其他修正

- 蜥蜴板甲的负词条数字改为红色。
- 瑟伦潦草的信改为任何状态下累计消耗 40 MP 触发，不再要求战斗中或单次消耗 40 MP。
- 破碎的镜子在脱战状态下激活时也会直接获得护盾。
- 湖泊 / 学院 / 守护的图标、语言键、掉落池与创造模式页已补齐。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。ModMenu 与 ToroHealth-Continued 可选。

## 已知问题 / 未完成

- 大部分内容仍处于 A 测阶段，尚未经过充分实测；数值与手感可能继续调整。
- 1.20.1 / 1.21.1 移植分支不在本次发布范围内。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.5.3-a`: all content updates after `0.5.2-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.5.2-a`).

## What's new

### New combos and artifacts (93 → 150 artifacts, 12 → 15 combos)

- **Academy** (new): tier 2 Magic Book Haste +12; tier 4 Magic Book MP cost -15%; tiers 6 / 8 Haste +16 / +20; tier 10 Magic Book damage +15% and MP Regen +8 (Haste stacks; +48 total at level 10).
- **Lake** (new): tiers 3 / 6 / 9 grant Max MP +20 / +30 / +40, MP Regen +5, and MP-consuming ability damage +10% / +25% (stacking; +90 Max MP and +35% ability damage at level 9).
- **Guardian** (new): tiers 2 / 4 / 6 / 8 / 10 grant Defense +6 / +7 / +8 / +10 / +12 (+43 total at level 10); tier 10 adds +5% Ignore Defense, multiplicative with Defense Penetration and applying only to vanilla armor and Defense.

#### New Magic Books (11)

- **Fire Bolt / Ice Bolt / Lightning Bolt** (Common): 1 s cooldown, 2 MP; Ice fires two projectiles, Lightning has a 2-block AOE at the impact point, all travel 36 blocks along the crosshair.
- **Flame Arrow** (Advanced): 4 arrows in an 8° spread, piercing enemies with a radius-3 AOE; the first hit applies Burn.
- **Frost Knife** (Advanced): summons 4 frost blades around the player; after 1 s each locks onto a nearby enemy, flies to it, and explodes.
- **Lightning Boomerang** (Advanced): flies 18 blocks out and returns; catching it on the return halves that skill slot's cooldown.
- **Fire Circus** (Rare): 8 independently exploding fireballs that can hit the same enemy repeatedly, with extra skill crit chance and crit damage.
- **Arrow Rain** (Rare): targets a point within 48 blocks and drops 45 ice arrows over a radius-8 area, each with its own hit and a chance to apply Frostbite.
- **Thunder Armor** (Rare): lasts 10 s and periodically chains lightning to nearby enemies.
- **Meteor Shower** (Legendary): drops 30 / 35 / 40 / 45 meteors over a radius-18 area, each with an independent explosion and a magma-block body.
- **Ice Storm** (Legendary): summons an 8 s tracking storm at the impact point, attacking enemies every 0.25 s, with snowfall particles and ground frost.
- **Thunder Verdict** (reworked): damage is now 3 + Lightning 20 / 24 / 28 / 32 / 36%, fires 10 lightning beams, with particles covering the damage corridor.

#### Academy artifacts (16)

Magician's Coin (book haste / book reward chance), Ray Star Fragment (reduces the MP cost of the book to its left), Mouse Magician Pendant (bolt tracking / MP Regen), Pearl Powder (book damage), Orb of Desire (reserved MP + book damage), Crabapple (book haste + dash recovery), Double Horn Pipe (three-way bolt split with damage / MP tradeoff), Tuning Fork (stores stacks on book casts, weapon hits consume them for chaos damage), Jewel Armor (book crit / MP Regen / physical penalty), Glowing Hourglass (haste for the book to its right), Battle Mage Gloves (book damage + movement speed), Meditation Book (book damage + haste), Academy Pen (dodge converts to book damage; dodging accelerates book cooldowns), Library Sun (book damage strengthens and triggers extra Sun Swords), Empty Hilt (book damage / final MP / MP Regen), Leilis' Pocket Watch (book haste / crit damage / book MP reduction).

#### Lake artifacts (16)

Wilted Flower, Endodite Paperweight, Scarecrow Doll (crit / MP Regen / Max MP), Star Ruby (final HP), Star Sea Sapphire (final MP), Water Bag (spending MP raises Max MP, with a cap and cooldown), Lure Bait (Luck of the Sea / Lure), Obras' Blood (MP Absorb), Broken Root (Max MP), Heart Carrot (Max HP / MP), White Branch (MP Regen), Stone Spindle (MP-consuming ability damage), Full Fruit Bucket (activated MP restore), Jolo's Watering Can (casts a Magic Book an extra time when Max MP is high enough), Water Spirit (activated WaterGrab scaling with Max MP), Plitvice Water Drop (Frost Armament hits summon rotating ice swords).

#### Guardian artifacts (14)

Cloth Armor, Shield (Magic Book; 10 Shield for 6 s), Shield Earring, Broken Mirror (Shield after leaving combat / when activated out of combat), Thorn Shell (Defense-based thorns), Seren's Hurried Letter (Shield from cumulative MP spending), Pot Lid (physical crits stack Endurance Defense), Academy Chainmail, Lizard Plate Armor, Small Mirror (Shield on dash), White-Edge Bread (invulnerability after being hit), Beast Heart (Defense / all damage amp / Luck), Shield Bag (Fire, Ice and Lightning damage per 10 Defense), Rock Elephant (activated rock spears).

### Shield and defense

- Added a general **Shield system**: shields are tracked as layers with amount, expiry and an optional source key. Incoming damage consumes Shield first; only the remainder goes through vanilla armor / Defense. Shield itself is not reduced by any mitigation, temporary layers are consumed before permanent ones.
- Added a **Shield HUD**: shows "Shield: x" only while Shield > 0, defaulting to the right of the Dash HUD.
- Guardian Defense is added to the player's Defense. Tier-10 Ignore Defense is multiplicative with Defense Penetration and applies to vanilla armor, player Defense and Root Retreat's Protective Armor enemy Defense.
- White-Edge Bread now uses the general invulnerability window, fully blocking subsequent damage and showing the Invulnerability HUD bar.

### Magic Book systems

- Magic Book tooltips and the skill page now show **actual cooldown and actual MP cost**, including Academy, Ray Star Fragment, Glowing Hourglass and Double Horn Pipe modifiers.
- Skill names in the skill page use the artifact rarity color; the skill HUD now orders slot 1 leftmost and slot 6 rightmost.
- Added unified Magic Book crit chance, damage and MP reduction aggregation; Tuning Fork, Library Sun and Academy Pen interactions are wired in.
- Magic Book damage now uses dedicated `magic_*` damage types, so ToroHealth shows the correct single Fire / Ice / Lightning element.

### Elements, damage and effects

- ToroHealth element fixes: normal elemental attacks show a single element; artifact damage follows its actual source; Blue Burn is Fire + Ice, Plasma is Fire + Lightning, and both together are Fire + Ice + Lightning; Cryo Guillotine is Ice + Lightning (Fire + Lightning with Eternal Eclipse on the left); Thunderfrost Butterfly deals two independent hits, each Ice + Lightning.
- Sun Sword damage is now resolved separately from the weapon hit, so it no longer merges into one number.
- Ice Storm final presentation: 18 rotating snowflakes + 18 randomly scattered each tick, 8 zero-gravity ice glints, level-scaled snowfall and ground frost.
- Plitvice Water Drop: rotating swords use the ice-bow arrow model at 50% scale with no snowflake trail; on expiry the model flies to the nearest enemy within 8 blocks and deals damage on arrival; Eternal Eclipse on the left turns the whole sword into Fire.
- Fixed the Ice Bow's fire-mode arrow incorrectly displaying the Flame Arrow artifact's Magic Book icon; added a dedicated fire arrow projectile texture and model.

### HUD and interface

- HUD settings gained **Uniform Scale** (scale every module at once) and **Reset** (restore the default layout).
- The Invulnerability bar is horizontally centered by default and keeps its center when scaled; the Shield HUD defaults to the right of Dash.
- Anvil book merging now supports Efficiency and Sweeping Edge; attack speed now affects non-Sephiria weapons and mining.
- The old Fatigue mechanic is renamed **EXP++**, while internal ids stay compatible.
- Mod Menu summary updated to "bring the buildcraft of SEPHIRIA into Minecraft — weapons, combo artifacts and slates".

### Other fixes

- Lizard Plate Armor's negative affix numbers are now red.
- Seren's Hurried Letter now triggers from cumulative MP spending in any state, no longer requiring combat or a single 40 MP spend.
- Broken Mirror also grants its Shield when activated while out of combat.
- Lake / Academy / Guardian icons, language keys, loot pools and creative tab entries are complete.

## Requirements

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (both client and server). ModMenu and ToroHealth-Continued are optional.

## Known issues / unfinished

- Most content is still in alpha and has not been thoroughly playtested; numbers and feel may keep changing.
- The 1.20.1 / 1.21.1 port branches are not part of this release.

## Disclaimer

Unofficial fan mod; much of the code is AI-generated; **commercial use is prohibited** (see the project description and LICENSE).
