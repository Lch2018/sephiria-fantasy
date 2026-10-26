版本 `0.4.3-a`：从 `0.4.2-a` 之后的全部改动。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.4.2-a`）开始写。

## 这一版有什么

### 七件新羁绊神器

- **红宝石胸针**（乌云 + 余烬，【唯一】）：乌云的闪电命中后附加灼伤；闪电 / 火焰属性伤害 +3/5/7/10。
- **雷霜蝶**（乌云 + 冰川，【唯一】）：乌云变成**暴风雪云**——每次攻击拆成电 / 冰两段双属性伤害（较高一方按普通乌云 20% 折算后 ×110%，较低一方 ×15%），冰属性伤害 +2/4/6/8、乌云消耗速度 +6/12/18/24%。
- **等离子头盔**（余烬 + 魔法科技，【唯一】）：把灼伤与触电合并成**等离子体**（层数 / 持续时间相加、叠层不刷新、结束清层），两路伤害按火 + 电结算；新伤害类型 `sephiria:plasma`。
- **对立之秤**（冰川 + 余烬，【唯一】）：按赛菲莉娅背包左 / 右半边分配火 / 冰属性伤害加成。
- **永恒的冬日**（冰川 + 坚固，【唯一】）：普通攻击命中时按概率追加「物理 + 冰」的额外伤害；新伤害类型 `sephiria:eternal_winter`。
- **酷寒断头台**（冰霜武具 + 魔法科技，【唯一】）：定期召唤冰雷巨剑，冰 + 电双属性、触发触电与冻伤，触电伤害缩短冷却。
- **闪烁的眼睛**（影子 + 精密，【唯一】）：暴击积攒「海市蜃楼」，满层后无条件闪避下一次伤害，并对周围造成该次税前伤害 ×150% 的混沌伤害。

### 冰霜武具与太阳剑

- **酷寒断头台并入冰霜武具**：HUD 记作「斩」（同时保留原来的冷却图标），吃冰霜武具连击的伤害 / 额外触发 / 充能速度；冰之翼让它多落剑，冰封的心脏缩短冷却。
- **永恒蚀会改变伤害类型**：
  - 在背包左半边时，冰霜武具（剑 / 锤 / 矛 / 弓 / 斩）整套改成火属性，伤害类型换成 `sephiria:fire_attack`。
  - 在背包右半边时，太阳剑改成冰属性，伤害类型换成 `sephiria:ice_sun_sword`。
- **太阳剑触发收窄**：没有永恒蚀时，只有赛菲莉娅武器攻击（普通攻击 / 横扫 / 弩矢 / 武器技能）能触发；神器 / 魔法书 / 真实伤害 / 减益 / 永恒冬日不再触发。冰霜武具触发太阳剑必须带永恒蚀。

### 减益与伤害

- **触电重做**：持续 2 秒，默认 t=2s 单跳（每层 40% 电元素强度）；魔法科技 10 档变为 t=1s、t=2s 两跳；叠层不刷新持续时间，结束时清层。
- **混沌伤害**：闪烁的眼睛闪避后的范围伤害，视为物理 / 火 / 冰 / 电四属性，吃物理增幅与所有元素提升。
- **伤害显示元素集合**：武器 / 神器 = 物理；持续减益 = 单元素；带固定值的元素攻击 = 元素 + 物理；酷寒断头台 = 冰 + 电；等离子体 = 火 + 电；永恒冬日 = 物理 + 冰。

### 暴风雪云

- 云下持续下自定义 `snowfall` 粒子（从原版天气雪贴图裁切，行为继承雪花粒子）。
- 地面效果每 5 秒触发：玩家周围 4 格随机挑 1 个露天水源结冰、3 个露天实心方块上方铺雪 / 叠雪（最多 8 层），只处理能看到天空的方块。
- **红石检测**：玩家周围 20 格内有红石元件、箱子等容器或红石物品时，地面效果整段跳过；自动索敌排除船 / 矿车上的单位。

### HUD 与设置

- 新增**技能 HUD**（6 个技能栏图标、顺时针冷却扫光、完成闪光）与**神器 HUD**（冷却 / 充能图标）。
- 两个 HUD 都从右向左排列、右边缘固定，新图标加在左边不挤动旧图标；所有模块支持 0.5~2.0 缩放，并按缩放后尺寸夹在屏幕内。
- **H 键**打开「赛菲莉娅 Mod 设置」：内含 HUD 布局设置与伤害显示设置。
- 伤害显示：亮度、大小、税前伤害；「发光」已移除。
- 装了 **ModMenu** 时，也可以从模组列表的「配置」按钮打开同一个设置界面。

### ToroHealth-Continued 兼容（可选）

- 浮动伤害数字按元素集合显示单色：物理 `#C0C0C0`、火 `#EDB12E`、冰 `#6499E8`、电 `#7EFAFF`、混沌 `#741B7C`、真伤 `#FFFFFF`；
  组合色：火电 `#EE8AF8`、冰物理 `#16417C`、冰电 `#3282F6`、火物理 `#F08784`、电物理 `#5A879E`、火冰 `#EF88BE`、冰火电 `#7F82BB`、火电物理 `#FFFD55`、冰电物理 `#3282F6`、火冰物理 `#EA3FF7`。
- 暴击数字放大 2 倍；左上角 HUD 的血量变化数字使用同一套颜色。
- 修了 26.3 里 ToroHealth 文字提交过晚的问题（在 `prepareFrame` 之前消费它的 `TextRenderQueue`）。
- 兼容层完全可选：没装 ToroHealth 时不会加载相关类，也不会影响正常游戏。

## 已知问题 / 未完成

- A 测阶段，大部分内容仍未充分实测，数值与手感还会调整。
- 部分旧武器 / 神器只有手持 3D 模型，背包图标仍是 2D。
- 石板升级、锻造、同伴系统尚未实现。
- ToroHealth 的数字渲染依赖它的内部类，升级 ToroHealth 大版本后可能需要重新核对。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。ModMenu 与 ToroHealth-Continued 都是可选。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.4.3-a`: everything after `0.4.2-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.4.2-a`).

### Seven new bond artifacts

- **Ruby Brooch** (Dark Cloud + Embers, Unique): Dark Cloud lightning applies Burn; Lightning / Fire damage +3/5/7/10.
- **Thunderfrost Butterfly** (Dark Cloud + Glacier, Unique): the cloud becomes a **Blizzard Cloud** — each attack is split into Electric and Ice damage (the higher element at 110% of 20% of a normal cloud strike, the lower at 15%), Ice damage +2/4/6/8, cloud consumption speed +6/12/18/24%.
- **Plasma Helmet** (Embers + Magitech, Unique): Burn and Shock merge into **Plasma** (stacks / duration add, restacking does not refresh, cleared at the end); both damage paths use Fire + Lightning; new damage type `sephiria:plasma`.
- **Scales of Opposition** (Glacier + Embers, Unique): Fire / Ice damage bonuses are assigned by which half of the Sephiria Backpack the artifact occupies.
- **Eternal Winter** (Glacier + Sturdy, Unique): normal attacks have a chance to add Physical + Ice damage; new damage type `sephiria:eternal_winter`.
- **Cryo Guillotine** (Frost Armament + Magitech, Unique): periodically summons an ice-lightning greatsword; Ice + Lightning damage, triggers Shock and Frostbite, and Shock damage shortens its cooldown.
- **Flickering Eye** (Shadow + Precision, Unique): crits build Mirage; at full charge the next damage taken is dodged unconditionally and deals 150% of that pre-tax damage as Chaos damage around the player.

### Frost Armament and Sun Sword

- **Cryo Guillotine now belongs to Frost Armament**: shown as "Slash" in the HUD (the original cooldown icon is kept), and it benefits from Frost Armament damage / extra triggers / charge speed; Ice Wings adds extra swords, Frozen Heart shortens the cooldown.
- **Eternal Eclipse changes damage types**:
  - In the left half of the backpack, Frost Armament (sword / hammer / spear / bow / slash) becomes Fire and uses `sephiria:fire_attack`.
  - In the right half, the Sun Sword becomes Ice and uses `sephiria:ice_sun_sword`.
- **Sun Sword trigger narrowed**: without Eternal Eclipse only Sephiria weapon attacks (normal attack / sweep / crossbow bolt / weapon skills) trigger it; artifacts, books, true damage, debuffs and Eternal Winter no longer trigger it. Frost Armament triggers the Sun Sword only while Eternal Eclipse is carried.

### Debuffs and damage

- **Shock reworked**: lasts 2 s, one tick at t=2s by default (40% Lightning element per stack); Magitech tier 10 changes it to ticks at t=1s and t=2s; restacking does not refresh duration and stacks clear at the end.
- **Chaos damage**: the Flickering Eye's dodge retaliation counts as Physical / Fire / Ice / Lightning, benefiting from Physical amplification and all-element bonuses.
- **Damage element sets**: weapons / artifacts = Physical; periodic debuffs = one element; flat elemental attacks = element + Physical; Cryo Guillotine = Ice + Lightning; Plasma = Fire + Lightning; Eternal Winter = Physical + Ice.

### Blizzard Cloud

- Custom `snowfall` particles fall under the cloud (cut from the vanilla weather snow texture, inheriting snowflake particle behavior).
- Ground effects every 5 s: within 4 blocks of the player, freeze 1 exposed water source and place / stack snow on 3 exposed solid blocks (up to 8 layers), only on blocks that can see the sky.
- **Redstone detection**: if any redstone component, chest-like container or redstone item is within 20 blocks, ground effects are skipped entirely; auto-targeting ignores entities in boats / minecarts.

### HUD and settings

- New **skill HUD** (6 skill slots, clockwise cooldown sweep, completion flash) and **artifact HUD** (cooldown / charge icons).
- Both HUDs are right-aligned and fill right-to-left: new icons appear to the left without moving existing ones. Every module supports 0.5–2.0 scaling and is clamped inside the screen after scaling.
- **H** opens "Sephiria Mod Settings", containing HUD layout and Damage Display settings.
- Damage Display: brightness, size, pre-tax damage; the "glow" option was removed.
- With **ModMenu** installed, the same settings screen can be opened from the mod list's "Config" button.

### ToroHealth-Continued compatibility (optional)

- Floating damage numbers use a single color chosen from the element set: Physical `#C0C0C0`, Fire `#EDB12E`, Ice `#6499E8`, Lightning `#7EFAFF`, Chaos `#741B7C`, True `#FFFFFF`;
  combinations: Fire+Lightning `#EE8AF8`, Ice+Physical `#16417C`, Ice+Lightning `#3282F6`, Fire+Physical `#F08784`, Lightning+Physical `#5A879E`, Fire+Ice `#EF88BE`, Ice+Fire+Lightning `#7F82BB`, Fire+Lightning+Physical `#FFFD55`, Ice+Lightning+Physical `#3282F6`, Fire+Ice+Physical `#EA3FF7`.
- Critical hits are drawn at 2× size; the top-left HUD health-change number uses the same colors.
- Fixed ToroHealth's text being submitted too late on 26.3 (its `TextRenderQueue` is now consumed before `prepareFrame`).
- Completely optional: no ToroHealth classes are loaded when it is absent, and normal gameplay is unaffected.

### Known issues / not done yet

- Alpha stage: most content has not been playtested thoroughly; numbers and feel are still being tuned.
- Some older weapons / artifacts only have new in-hand 3D models; inventory icons are still 2D.
- Slate upgrades, forging and the companion system are not implemented yet.
- Damage-number rendering depends on ToroHealth internals; a major ToroHealth update may require re-checking the hooks.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (required on both client and server). ModMenu and ToroHealth-Continued are optional.

### Notice

Unofficial fan mod; heavy use of AI-generated code; **commercial use is not allowed** (see the project description and LICENSE).
