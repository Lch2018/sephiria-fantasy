修复版（大版本号提到 0.3）：根之退的摆放朝向与物品图标。

## 修正

- **根之退摆放时像讲台一样朝玩家**：此前它没有朝向属性，摆下去永远朝南；现在放下时正面朝你（与讲台 / 熔炉同一套逻辑），用结构方块或命令旋转也会跟着转。
- **根之退的物品图标显示正面**：方块物品的图标走的是独立的物品模型（不经过方块状态），之前图标看到的是模型背面；现在图标专门多转 180°，显示「书朝向你」的那一面。

## 已知问题 / 未完成

- 旧存档里已经放置的根之退如果显示异常（方块状态从 1 种变成 4 种），拆掉重放一次即可。
- 石板升级、锻造、同伴系统尚未实现。
- 部分文案与图标仍会变动。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib（必需）。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文）。

---

## English

A bug-fix release (major version bumped to 0.3): the Root Retreat's placement facing and its item icon.

### Fixes

- **The Root Retreat now faces the player when placed, like a lectern**: it previously had no facing property and always pointed south; now its front faces you when placed (the same logic as the lectern / furnace), and rotating it with structure blocks or commands follows along.
- **The Root Retreat's item icon now shows the front**: a block item's icon comes from its own item model (the blockstate is not involved), and it used to show the back of the model; the icon view is now rotated 180° so it shows the side the book faces.

### Known issues / not done yet

- Root Retreat blocks placed in old worlds may render oddly because the blockstate went from 1 state to 4; break and replace them once.
- Slate upgrades, forging and the companion system are not implemented yet.
- Some texts and icons will still change.

### Requirements

Minecraft 26.3 + Fabric Loader 0.19.5 + Fabric API 0.161.0+26.3 + GeckoLib (required).

### Disclaimer

A fan mod, not an official work; heavily AI-generated code; **any commercial use is forbidden** (see the project description).
