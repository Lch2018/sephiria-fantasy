内容版：新神器「书」（铁砧里合附魔）、12 块新石板（石板说明改成自动生成）、四种新药水，以及一批经济与概率调整（石板宝箱 −30%、商店 / 升级宝箱的神器·石板比例、挖矿掉石板、概率可配置）。

## 新增

- **新神器「书」**（稀有 · 【唯一】 · 无连击）：铁砧里「书 + 附魔书」可以把**火焰附加 / 抢夺**合上去，等级相加、但**不超过原版上限**（火焰附加 II、抢夺 III；到顶之后那一手不出结果、也不扣经验）。待在赛菲莉娅背包里时，火焰附加让**神器 / 连击 / 减益**打出的伤害点燃目标（武器普攻不算），抢夺并进掉落结算。⚠ 图标目前是程序生成的临时贴图。
- **12 块新石板**：断绝 / 楔子 / 防御招式 / 干燥 / 剥削 / 团结 / 和平 / 白日梦 / 反抗 / 接合 / **报恩（石板最高品质·永恒）** / 到来。石板的「说明」现在**自动生成**：有加等级就写「提高神器等级」（绿）、有减等级写「降低神器等级」（红）、不可旋转写「不可旋转」（红）。
- **四种新药水**：**索莱拉的偶然**（黄：随机一种连击 +1 级，永久；喝下时动作栏会报是哪一种连击）、**梅尔岑之烟 / 阿凡提努斯的水晶 / 高泽之晨**（蓝：火 / 冰 / 电属性伤害 +2，永久）。药水提示框现在也会显示风味文本。
- **挖矿掉石板**：不带精准采集时，铜 / 铁 / 金 / 钻石 / 绿宝石矿按品质加权掉石板。
- **品质概率可配置**：整合包 / 服主可用 `config/sephiria-fantasy-loot-weights.json` 覆盖四种来源（宝箱 / 商店 / 抽卡 / 药水）的品质概率。

## 调整

- **宝箱价格**：石板宝箱 750 → **525**（−30%）；神器宝箱与升级宝箱仍是 750。
- **商店货架**：改成**先掷神器 / 石板（石板 25%）再掷品质**。
- **升级宝箱**：改成**石板 40% / 神器 60%**。
- **石板价格**：175 / 350 / 525 / 700 / 1050（神器那套的 7 折）。
- **抽卡愿力**：神器与石板各一套（神器 2/4/6/8/10、石板 3/6/9/12/15）。
- **流浪商人**出生延迟 24000 → 4800 刻。
- 品质概率按来源分开（宝箱 47/47/3.32/0.917/1.608、商店 20/20/10/3/5、抽卡 30/30/10/3/5、药水 200/10/5/1）；权重的含义改成「这一档实际开出的份数」，与这一档里有几件东西无关。

## 已知问题 / 未完成

- 「书」的图标是程序生成的临时贴图，后续会替换。
- 「反抗」「报恩」这类大型图案石板：赛菲莉娅背包只有 6 列宽，超出背包的格子**不会生效**（图案会被裁掉）。
- 石板升级、锻造、同伴系统尚未实现。
- 这一版的内容（书 / 石板 / 药水 / 经济调整）是实装后直接发布的，尚未充分实测，数值仍会调整。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib（必需）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文）。

---

## English

A content release: the new "Book" artifact (enchant merging at the anvil), 12 new slates with auto-generated descriptions, four new potions, plus an economy and probability pass (slate chest −30%, artifact/slate splits for the shop and upgrade chests, slates from mining, configurable rarity weights).

### New

- **New artifact "Book"** (Rare, 【Unique】, no combo): at an anvil, "Book + enchanted book" merges **Fire Aspect / Looting** into it — the levels add up but **never exceed the vanilla caps** (Fire Aspect II, Looting III; once at the cap the anvil shows no result and charges nothing). While it sits in the Sephiria Backpack, Fire Aspect sets targets on fire from **artifact / combo / debuff** damage (plain weapon attacks are excluded), and Looting is added to drop rolls. ⚠ Its icon is a generated placeholder for now.
- **12 new slates**: Severance / Wedge / Defense / Drought / Exploitation / Unity / Peace / Daydream / Rebellion / Junction / **Gratitude (Eternal, the top slate rarity)** / Arrival. A slate's description is now **generated**: "Raises artifact level" (green) when it boosts slots, "Lowers artifact level" (red) when it reduces them, and "Cannot rotate" (red) when it is fixed.
- **Four new potions**: **Solera's Chance** (Rare: a random combo gains +1 level, permanent; the action bar names which one), **Meltzen's Smoke / Avantinus' Crystal / Morning of Gaoze** (Advanced: Fire / Ice / Lightning Damage +2, permanent). Potion tooltips now also show flavor text.
- **Slates from mining**: copper / iron / gold / diamond / emerald ore drop slates by rarity (without Silk Touch).
- **Configurable rarity weights**: modpacks and server owners can override the four sources (chest / shop / gacha / potion) in `config/sephiria-fantasy-loot-weights.json`.

### Changes

- **Chest prices**: the Slate Chest is 750 → **525** (−30%); the Artifact Chest and Upgrade Chest stay at 750.
- **Shop stock**: now rolls **artifact vs slate first (25% slates)**, then the rarity.
- **Upgrade Chest**: now **40% slates / 60% artifacts**.
- **Slate prices**: 175 / 350 / 525 / 700 / 1050 (70% of the artifact table).
- **Wish power**: separate tables for artifacts (2/4/6/8/10) and slates (3/6/9/12/15).
- **Wandering trader** spawn delay 24000 → 4800 ticks.
- Rarity weights are now per source (chest 47/47/3.32/0.917/1.608, shop 20/20/10/3/5, gacha 30/30/10/3/5, potions 200/10/5/1); a weight now means the share of items rolled at that rarity, independent of how many items the tier contains.

### Known issues / not done yet

- The Book's icon is a generated placeholder and will be replaced.
- Large-pattern slates (Rebellion, Gratitude): the Sephiria Backpack is only 6 columns wide, so cells outside it do **not** apply (the pattern is clipped).
- Slate upgrades, forging and the companion system are not implemented yet.
- This version's content (Book / slates / potions / economy) was released right after it was implemented; it has not been play-tested thoroughly and the numbers keep being tuned.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib (required).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description).
