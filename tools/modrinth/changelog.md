这一版加入两个新的功能方块——**根之退**（调难度）与**许愿池**（抽卡），配方书里也多了单独的一页「赛菲莉亚」。

## 新增

### 根之退（难度调整）

一块石碑状的功能方块（**信标 + 任意树苗**合成）。右键打开界面，可以给自己**加难度换收益**：

- **凶猛的爪子**：敌人对你的伤害 +10/20/30/40/50/60/70/80%（1..8 级）；
- **顽强之躯**：敌人最终受到的伤害减少 20/40/50/65/75/80/85/87.5/90%（1..9 级，**最底层的减免，真实伤害也照减**）；
- **防护铠甲**：敌人的防御力 +20/40/60/80/100（1..5 级，按 `k = R/(100+R)` 折算成减伤，**减不了真实伤害**）。

每条选项都有等级，加出来的等级之和就是「**阶段**」——**每层阶段给你 +10% 经验获取**。界面左侧是方块自己存的方案（可以放两块、各存一套），点「清空」清零、点「应用」才套到自己身上；右侧上方显示这块方块的阶段与祝福，右下角是你自己正在生效的方案。鼠标放到选项上有说明（数值是红色——对玩家不利的都这个色）。

### 许愿池（抽卡）

一个四叶草装饰的水池（**水桶 + 任意树苗**合成）。右键打开抽卡界面：

- **两个卡池**（神器池 / 石板池，界面左侧切换），**单抽**与**十连**，**概率与商店完全一致**；
- **锚定**：任选一件神器 / 石板，抽到它所属品质时约有一半是它；选择页可按**品质**与**连击**筛选；
- **愿力**：抽卡会积攒愿力（白 1 / 蓝 2 / 黄 4 / 红 8 / 彩 10），够 80 / 200 可以在「**神铸**」页直接换一件黄 / 红品质的神器或石板（点物品先选中、按「确认」才花愿力，防误点）；
- **四叶草**（抽卡货币，是一个**数值**不是物品）：把**升级宝箱**丢进许愿池，水池会像漏斗一样吸走它、给你 1 片四叶草；
- 抽到的东西会**从许愿池上方四散弹出**，同时播一段抽卡动画（一张张亮出来、品质色一直保留，点一下返回）。

### 配方书新页

原版配方书里多了一页**「赛菲莉亚」**（页签图标是盾剑），本模组全部合成表都在里面，**进游戏即全部解锁**。

## 调整与修正

- **击退加了冷却**：本模组的伤害以前清掉了受击无敌帧，于是高频攻击会把敌人连续击退、推得很远；现在**本模组造成的击退最多每 0.5 秒一次（与原版受击无敌间隔一致），每个敌人各算各的**；
- **太阳剑**：数量到达上限时，掉在地上的剑会消失（满容量收不回来，留着只会一直躺着）；
- **永恒蚀**（上一版加入的羁绊神器）配套的图标换色、根之退与许愿池的界面都按反馈调整过：锚定/神铸页选中项有白框、连击筛选用图标按钮、羁绊的品质色条是彩色、界面尺寸放大、标题在最左上角等。

---

## English

This release adds two new utility blocks — **Root Retreat** (difficulty tuning) and the **Wishing Pool** (gacha) — plus a dedicated **Sephiria** page in the vanilla recipe book.

### New: Root Retreat (difficulty tuning)

A stele-shaped block (**beacon + any sapling**). Right-click to open its screen and trade comfort for rewards:

- **Fierce Claws**: enemies deal +10/20/30/40/50/60/70/80% damage to you (levels 1..8);
- **Tenacious Body**: enemies take 20/40/50/65/75/80/85/87.5/90% less final damage (levels 1..9 — the **lowest layer of reduction, so it applies to true damage too**);
- **Protective Armor**: enemy defence +20/40/60/80/100 (levels 1..5, converted with `k = R/(100+R)`; **it does not reduce true damage**).

Every option has levels, and their total is your **Stage** — **each stage grants +10% XP gain**. The left column is the plan stored **in that block** (place two blocks and they keep separate plans): **Clear** zeroes it, **Apply** copies it onto you. The top-right shows that block's stage and blessing, the bottom-right shows the plan you currently have active. Hovering an option explains it (the numbers are red — the colour used for anything that hurts the player).

### New: Wishing Pool (gacha)

A clover-decorated water basin (**water bucket + any sapling**). Right-click to open the draw screen:

- **Two pools** (artifacts / slates, switched on the left), **single draw** and **ten-pull**, with **the same probabilities as the shop**;
- **Anchoring**: pin any artifact or slate and roughly half of the draws of its rarity tier will be it; the picker filters by **rarity** and **combo**;
- **Wish Power** accumulates from draws (common 1 / advanced 2 / rare 4 / legendary 8 / bond 10) and 80 / 200 of it can be spent on the **Forge** page to take a rare / legendary item directly (click to select, then **Confirm** — no accidental spending);
- **Four-leaf clovers** (the draw currency, a **stat**, not an item): throw an **upgrade chest** into the pool and it is hoovered up like a hopper, giving you 1 clover;
- Drawn items **burst out of the pool** while a draw animation plays (items are revealed one by one and keep their rarity colour; click to return).

### New: recipe book page

The vanilla recipe book gained a **Sephiria** page (shield-and-sword icon) holding every recipe this mod adds, **all unlocked from the start**.

### Tweaks and fixes

- **Knockback now has a cooldown**: this mod's damage clears the vanilla hurt-invulnerability, so high-frequency hits used to knock enemies back over and over and send them flying. Knockback caused by this mod is now **limited to once every 0.5 s — the same rate as vanilla — per enemy**;
- **Sun Sword**: once the capacity is full, swords lying on the ground disappear (they can no longer be picked up, so they would just linger);
- **Eternal Eclipse** (the bond artifact from the previous release) and both new screens were polished from feedback: a white frame marks the selected anchor / forge item, combo filters are icon buttons, the bond quality strip is a rainbow, the panels are bigger and the title sits in the top-left corner.
