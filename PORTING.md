# 多版本移植状态

## 分支 / 工作区

- `main`：26.3，当前发布候选 `0.4.0.58-a`，不在本次移植中改动。
- `port/1.21.1`：工作区 `E:\workspace\Zcode\mcmode-1.21.1`。
- `port/1.20.1`：工作区 `E:\workspace\Zcode\mcmode-1.20.1`。

## 1.21.1（核心先行版，已实装）

- 构建：Mojang 官方映射 + Loom `1.17.21` + Fabric API `0.116.17+1.21.1`。
- 核心 `compileJava` 通过；临时无头服务端启动通过（注册、BE、资源、配方可加载）。
- jar：`build/libs/sephiria-fantasy-0.4.0.58-a-mc1.21.1.jar`（1104302 B，
  sha256 `644166f148ee9648a8a02840a23e6ebd65842d5b0f7fbc2277812ba5e7d0483a`）。
- 已实装到 `D:\PCL\.minecraft\versions\1.21.1-Fabric 0.20.1\mods`。
- **暂缺**：客户端界面/HUD/ToroHealth/ModMenu 兼容/GeckoLib 刀动画/原版 mixin。
  当前是可启动的核心功能版，不是完整可玩版。
- 1.21.1 专用的简化点：
  - `SephiriaKatanaItem` 先去掉 GeckoLib 与剑意，保留出鞘/入鞘属性切换。
  - 信标光柱实体渲染器未接（核心 BE 已迁）。
  - 动态显示实体、部分粒子颜色、部分 26.3 专属表现先简化。

## 1.20.1（核心先行版，已实装）

- 构建：Mojang 官方映射 + Loom `1.17.21` + Fabric API `0.92.12+1.20.1` + Java 17。
- 核心 `compileJava` 通过；无头服务端启动通过（注册、BE、资源、7 张配方、1271 个进度可加载）。
- jar：`build/libs/sephiria-fantasy-0.4.0.58-a-mc1.20.1.jar`（1074069 B，
  sha256 `af4a12a884ac1d79ed9ed7c304ac66a67ed969869919485368802a752430ae3d`）。
- 已实装到 `D:\PCL\.minecraft\versions\1.20.1-Fabric 0.20.1\mods`。
- 1.20.1 的数据层与 API 迁移：
  - 物品状态（神器等级/连击、石板朝向/刻印/图案、宝箱奖励、刀入鞘、太阳剑主人、
    冰霜武具焰黄标记）换成 NBT 键，统一走 `com.sephiria.compat.ItemData`；
  - 武器面板数值改由各武器 `getDefaultAttributeModifiers` 提供（`WeaponAttributes`），
    与 26.3 的 Item.Properties 数据组件等价；
  - 菜单用 1.20.1 的 `ExtendedScreenHandlerType<T>(factory)` 与
    `writeScreenOpeningData(ServerPlayer, FriendlyByteBuf)`；
  - 结构类型返回 `Codec`、结构片段用 `(context, tag)` 工厂；
  - 粒子走 `ParticleOptions#writeToNetwork` 旧接口；Display 私有同步字段用访问拓宽打开。
- **暂缺**：客户端界面/HUD/ToroHealth/ModMenu 兼容/GeckoLib 刀动画/原版 mixin/网络同步。
  当前是可启动的核心功能版，不是完整可玩版：
  - 所有客户端同步包已剥离，HUD、技能栏、石板合成预览、商店/背包界面不会收到服务端数据；
  - 屏幕没有注册，右键菜单只会打开空 GUI；
  - 物品模型里的 `items/*.json`（1.21.4+ 格式）在 1.20.1 被忽略，按 `models/item/*.json` 渲染，
    因此按状态/组件换贴图的效果暂时没有。
- 下一步优先级：补客户端入口、屏幕注册与 HUD；再做网络层（旧 channel API）和 GeckoLib 动画。

## 注意

- 两个移植分支都只做本地提交，没有 push / 发布。
- 1.21.1 的核心版缺少客户端 UI，不要当完整版发布。
