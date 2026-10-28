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

## 1.20.1（WIP，不可用）

- 已建立 Loom 构建配置（Mojang 映射、Java 17、Fabric API `0.92.12+1.20.1`），
  并把 1.21.1 核心源码迁入。
- 当前 `compileJava` 仍有约 **629 个错误**，没有可实装 jar，`1.20.1` 实例的 mods 里没有本模组。
- 2026-10-28 本轮已剥离客户端同步网络层（删除 `network/` 并用最小占位类型保留核心数据结构），
  错误从 1098 降到 629；剩余集中在数据组件 / NBT、ModParticleTypes、菜单 API 和属性 API。
- 主要缺口：
  1. 1.20.1 没有数据组件：`ArtifactItem` / `SlateItem` / `FusedSlateItem` / `PatternSlateItem` /
     `BookItem` / 武器组件等要改成 NBT 键；
  2. 1.20.1 Fabric 网络 API 没有 `CustomPacketPayload` / `StreamCodec` /
     `PayloadTypeRegistry`：34 个网络包与注册/接收端要改成旧的 `PacketByteBuf` + channel API；
  3. `Item.Properties` 没有 `sword` / `component` / `attributes` 的新版便捷方法，
     `ModItems` 的属性与不可破坏组件要改旧版写法；
  4. `MenuScreens` / `ExtendedScreenHandlerFactory` / 菜单类型构造与 1.21.1 不同；
  5. `BlockEntity` NBT、`BlockEntityType` Builder、配方格式、粒子 API 等旧版差异。
- 下一步优先级：先做 NBT 数据层 + 网络层，让核心 `compileJava` 通过；再接客户端。

## 注意

- 两个移植分支都只做本地提交，没有 push / 发布。
- 1.21.1 的核心版缺少客户端 UI，不要当完整版发布。
