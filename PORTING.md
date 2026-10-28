# 多版本移植状态

## 分支 / 工作区

- `main`：26.3，当前发布候选 `0.4.0.58-a`，不在本次移植中改动。
- `port/1.21.1`：工作区 `E:\workspace\Zcode\mcmode-1.21.1`。
- `port/1.20.1`：工作区 `E:\workspace\Zcode\mcmode-1.20.1`。

## 1.21.1（核心 + 基础客户端，已实装）

- 构建：Mojang 官方映射 + Loom `1.17.21` + Fabric API `0.116.17+1.21.1`。
- 核心 `compileJava` 通过；临时无头服务端启动通过（注册、BE、资源、配方可加载）。
- jar：`build/libs/sephiria-fantasy-0.4.0.58-a-mc1.21.1.jar`（1164140 B，
  sha256 `ebf1c45729d4cd5ec1c19ae5934c1b5853a24282bef01b56e5e80cc3b65f84b7`）。
- 已实装到 `D:\PCL\.minecraft\versions\1.21.1-Fabric 0.20.1\mods`。
- 客户端已有入口 `com.sephiria.client.SephiriaClient`：注册 9 个容器菜单的通用界面
  （深色面板 + 槽位底 + 标题），并接回冲刺（鼠标侧键）与装填（R）按键。
  访问拓宽打开了 1.21.1 包私有的 `MenuScreens.register`。
- HUD 已接基础版：左下角两排图标（技能 / 神器）、冷却遮罩与绿色充能条；
  数据来自服务端 `HudSyncPayload`，两包之间按客户端刻数平滑。
- 提示行已接：`ArtifactTooltip` 显示连击、唯一、词条、稀有度、等级与描述，`ClientStats`
  接收 `StatsSyncPayload` 供数值换算。流浪商人 / 羊驼先用原版渲染器保证可见。
- 信标界面已换回专属绘制：单格 + 品质色外框。
- 第 2 轮已补：背包/附魔滚轮与 R 旋转、附魔升级按钮、宝箱刷新、商店刷新/出售、
  商人石板合成按钮、合成界面合成与输入格 R 旋转。
- 第 3 轮已补：根之退加减/清空/应用、许愿池抽卡/溢出/锚定/神铸、技能页（L）、
  属性页（K）、天赋页（T）。所有容器菜单现在都有可操作界面，不再是纯通用面板。
- 第 4 轮已补：自定义粒子的客户端 Provider（blue_spark/snowfall/plasma/sun_slash/
  frost_qi/flame_qi/ice_slash），粒子能正常显示；26.3 的方向/尺寸参数暂未还原。
- **暂缺**：26.3 的模块化 HUD 布局/缩放/拖拽、ToroHealth/ModMenu 兼容、GeckoLib 刀动画、
  原版 mixin、各界面专属绘制与按钮。当前是「能打开容器、能操作槽位、有基础 HUD」的客户端，
  不是完整可玩版。
- 1.21.1 专用的简化点：
  - `SephiriaKatanaItem` 先去掉 GeckoLib 与剑意，保留出鞘/入鞘属性切换。
  - 信标光柱实体渲染器未接（核心 BE 已迁）。
  - 动态显示实体、部分粒子颜色、部分 26.3 专属表现先简化。

## 1.20.1（核心 + 基础客户端，已实装）

- 构建：Mojang 官方映射 + Loom `1.17.21` + Fabric API `0.92.12+1.20.1` + Java 17。
- 核心 `compileJava` 通过；无头服务端启动通过（注册、BE、资源、7 张配方、1271 个进度可加载）。
- jar：`build/libs/sephiria-fantasy-0.4.0.58-a-mc1.20.1.jar`（1168501 B，
  sha256 `98b863b8aaf452310e8b8c51a2db9a05461de2bb2db3ce26b0621b1dbe73ad37`）。
- 已实装到 `D:\PCL\.minecraft\versions\1.20.1-Fabric 0.20.1\mods`。
- 客户端同样有 `SephiriaClient` 与通用容器界面（9 个菜单），访问拓宽打开
  `MenuScreens.register`；提示行已接 `ArtifactTooltip`（1.20.1 的三参回调）。
- **基础 HUD 已接**：用 1.20.1 的 ResourceLocation channel 重做 `HudSyncPayload`，
  `ArtifactHud` 每 5 刻推送、进服先推一份；客户端显示左下角技能/神器图标、冷却遮罩与充能条。
  信标界面也已换回品质色外框。冲刺（鼠标侧键）与装填（R）已用旧 channel 接回，
  服务端分别调用 `DashSkill.perform` 与 `SephiriaCrossbowItem.tryReload`；
  神器技能 6 个按键（默认未绑定）也走 `CastArtifactSkillPayload` → `ArtifactSkills.cast`。
  原版背包上沿已加「赛菲莉娅背包 / 商店」两个按钮（`OpenBackpackPayload` / `OpenShopPayload`），
  作为自绘标签栏的临时替代。商店的刷新/出售按钮已接（`RefreshShopPayload` / `SellItemPayload`），
  货架格点击购买沿用原版 `clicked`；商店现在可以买、刷新、出售。
  宝箱界面的「刷新」也已接（`ChestRerollPayload`）；背包与附魔界面的滚轮翻行走
  `ScrollBackpackPayload` → `BackpackGridMenu.scrollBackpack`。
  其余专属界面与网络包仍未迁。
- 1.20.1 的数据层与 API 迁移：
  - 物品状态（神器等级/连击、石板朝向/刻印/图案、宝箱奖励、刀入鞘、太阳剑主人、
    冰霜武具焰黄标记）换成 NBT 键，统一走 `com.sephiria.compat.ItemData`；
  - 武器面板数值改由各武器 `getDefaultAttributeModifiers` 提供（`WeaponAttributes`），
    与 26.3 的 Item.Properties 数据组件等价；
  - 菜单用 1.20.1 的 `ExtendedScreenHandlerType<T>(factory)` 与
    `writeScreenOpeningData(ServerPlayer, FriendlyByteBuf)`；
  - 结构类型返回 `Codec`、结构片段用 `(context, tag)` 工厂；
  - 粒子走 `ParticleOptions#writeToNetwork` 旧接口；Display 私有同步字段用访问拓宽打开。
- **暂缺**：26.3 的自绘 HUD、ToroHealth/ModMenu 兼容、GeckoLib 刀动画、原版 mixin/网络同步。
  当前是「能打开容器、能操作槽位」的基础客户端，不是完整可玩版：
  - 所有客户端同步包已剥离，HUD、技能栏、石板合成预览、商店/背包界面不会收到服务端数据；
  - 界面是统一的深色面板，没有 26.3 的专属绘制、按钮与滚动条；
  - 物品模型里的 `items/*.json`（1.21.4+ 格式）在 1.20.1 被忽略，按 `models/item/*.json` 渲染，
    因此按状态/组件换贴图的效果暂时没有。
- 第 4 轮已补自定义粒子的客户端 Provider。
- 两版都补了刀的 GeckoLib 模型、待机动画与右键出鞘/入鞘切换动画；剑意动画还没接。
  商人界面在商品格下方显示物品名；两版 H 键可开关 HUD 并切换 100/125/150% 缩放
  （配置持久化到 `config/sephiria-fantasy-hud.properties`）。
  ModMenu 配置入口已接（打开 HUD 设置页），ToroHealth/ModMenu 声明为可选依赖；
  ToroHealth 1.5.1 已接轻量染色：命中后 150ms 内按武器分支改伤害数字颜色
  （`HealthChangeParticleMixin`，required:false）；元素/暴击精确着色仍未移植。
  仍缺：26.3 的模块化 HUD 拖拽/分模块开关、部分专属绘制与客户端实测。

## 注意

- 两个移植分支都只做本地提交，没有 push / 发布。
- 两个移植版目前都只有基础容器界面，不能当完整版发布。
