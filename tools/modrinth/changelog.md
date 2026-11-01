版本 `0.5.1-a`：从 `0.5.0-a` 之后的修复。

> 按约定，更新日志从**上一次上传并发布的版本**（`0.5.0-a`）开始写。

## 这一版有什么

### 光标修复

- 修复从赛菲莉娅背包切到商店、以及从商店切回赛菲莉娅背包时光标跳到窗口中心的问题。
- 容器目标（背包 / 商店）现在也会在点击标签时记录光标位置；服务端回包、目标容器界面真正打开后，由 `Gui.setScreen` 返回 hook 恢复光标坐标并 warp 回原位置。
- 普通页（属性 / 天赋 / 技能 / 选卡 / 原版背包）原有的光标恢复逻辑保持不变。

## 运行环境

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib（必需，客户端与服务端都要装）。ModMenu 与 ToroHealth-Continued 可选。

## 声明

同人模组、非官方作品；大量使用 AI 生成代码；**禁止任何商业行为**（详见项目正文与 LICENSE）。

---

## English

Version `0.5.1-a`: fixes after `0.5.0-a`.

> By convention this changelog starts from the previously uploaded and released version (`0.5.0-a`).

### Cursor fix

- Fixed the cursor jumping to the window center when switching from the Sephiria backpack to the shop, and back.
- Container targets (backpack / shop) now record the cursor position when their tab is clicked; after the server response opens the target container screen, the `Gui.setScreen` return hook restores the cursor coordinates and warps the real cursor back.
- Existing cursor restore behavior for normal pages (attributes / talents / skills / card selection / vanilla inventory) is unchanged.

## Requirements

Minecraft 26.3 + Fabric Loader 0.19.5+ + Fabric API + GeckoLib (both client and server). ModMenu and ToroHealth-Continued are optional.

## Disclaimer

Unofficial fan mod; much of the code is AI-generated; **commercial use is prohibited** (see the project description and LICENSE).
