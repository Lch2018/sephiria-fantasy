# SEPHIRIA（Minecraft Fabric Mod）

把《SEPHIRIA（赛菲莉娅）》的武器系统带进 Minecraft **26.2**（Fabric）。

当前版本只做**基础武器**：原作的六大武器分支各一把。**锻造系统（武器升级变种）尚未实现**，
但代码结构已经为它留好了位置。

## 六大分支与基础武器

数值按原作的分支命中倍率换算，以剑与盾为基准（6.0 伤害 / 1.6 攻速 / 250 耐久）。

| 分支 | 基础武器 | 攻击伤害 | 攻击速度 | 耐久 | 命中倍率 |
| --- | --- | --- | --- | --- | --- |
| 剑与盾 | 标准剑盾 | 6.0 | 1.6 | 250 | 100% |
| 大剑 | 钢铁巨剑 | 9.0 | 1.0 | 600 | 150% |
| 匕首 | 匕首 | 4.0 | 2.6 | 160 | 68.75% |
| 弩 | 重型弩 | 远程（取决于弹药） | — | 465 | 120% |
| 刀 | 刀 | 5.0 | 2.0 | 320 | 82.5% |
| 法杖 / 长棍 | 长棍 | 5.5 | 1.5 | 300 | 90% |

六把武器都放在独立的「SEPHIRIA 武器」创造模式标签页里，物品提示框会显示所属分支与命中倍率。

## 环境要求

- Minecraft **26.2**、Fabric Loader **0.19.5**、Fabric API **0.160.0+26.2**
- **JDK 25**（26.2 要求 Java 25 运行与编译；Gradle 自身也必须跑在 JDK 25 上）
- Gradle 9.5.1（用仓库里的 wrapper 即可）

## 构建

```bat
gradlew.bat build
```

产物在 `build/libs/sephiria-0.1.0.jar`，丢进 `.minecraft/mods` 即可（需要 Fabric API）。

调试运行开发客户端：

```bat
gradlew.bat runClient
```

## 26.2 的注意事项

26.2 是新的年份版本号，和 1.21.x 时代差别很大，踩过的坑记在这里：

- **Minecraft 26.1 起不再混淆**，自带参数名。因此 build.gradle 里**没有 `mappings`**，
  Yarn 已废弃（最后支持到 1.21.11），Loom 插件用 `net.fabricmc.fabric-loom`（**不带 `-remap`**），
  依赖用 `implementation` 而不是 `modImplementation`。
- **`ResourceLocation` 已改名为 `Identifier`**（`net.minecraft.resources.Identifier`）。
- **原版 `SwordItem` 已被删除**。剑类物品就是普通 `Item`，攻击力/攻速/耐久由
  `Item.Properties#sword(ToolMaterial, float, float)` 写进数据组件。
  注意传入的是「原版参数」而不是工具栏显示的数值：
  显示伤害 = 1（玩家基础） + 材质加成 + 传入值，显示攻速 = 4（玩家基础） + 传入值。
- **物品模型需要两个文件**：`assets/<ns>/items/<id>.json`（模型定义）与
  `assets/<ns>/models/item/<id>.json`（真正的模型）。
- **`Item#appendHoverText` 已被标记为过时**（原版改为由实现了 `TooltipProvider` 的数据组件
  提供提示行）。所以武器提示框走的是 Fabric 的 `ItemTooltipCallback`，注册在**客户端入口**
  `com.sephiria.client.SephiriaClient`（`fabric.mod.json` 里的 `client` entrypoint）。
- Fabric API 的创造模式标签页模块已从 `fabric-item-group-api-v1` 改名为
  `fabric-creative-tab-api-v1`，入口类是 `net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab`。
- **Gradle 下载走不通 GitHub**：官方 `services.gradle.org` 会重定向到 github.com。
  `gradle/wrapper/gradle-wrapper.properties` 里已经把 `distributionUrl` 换成腾讯云镜像。

## 目录结构

```
src/main/java/com/sephiria/
├── Sephiria.java                      mod 入口
├── registry/
│   ├── ModItems.java                  物品注册表 + 六把基础武器的数值表
│   └── ModCreativeTabs.java           创造模式标签页
└── weapon/
    ├── WeaponBranch.java              六大分支枚举（含命中倍率）
    ├── BaseWeapon.java                基础武器 = 分支 + 物品
    ├── SephiriaWeapon.java            武器接口（近战/远程统一）
    ├── SephiriaWeaponItem.java        近战武器物品
    └── SephiriaCrossbowItem.java      远程武器物品（继承原版 CrossbowItem）
```

## 锻造系统（下一步）的接入点

当前只有「基础武器」这一层，原作里每把武器还要沿分支升级两次（基础 → 一级 → 二级）。
现有结构就是按这个模型铺的：

- `WeaponBranch`：六大分支 + 命中倍率。加分支只改这里。
- `BaseWeapon`：`分支 + 物品` 的记录。升级变种接进来时，在这里补「变种列表」即可，
  注册表、创造模式标签页、提示框都会自动跟着走（它们都由 `ModItems.BASE_WEAPONS` 驱动）。
- `SephiriaWeapon`：近战和远程统一到「按分支处理」的接口，锻造逻辑不必区分父类。
- `ModItems` 的 `sword(...)` / `crossbow(...)` 工厂：变种武器沿用同一套参数
  （显示伤害、显示攻速、耐久），不用再关心原版参数换算。

## 授权

本项目代码以 **MIT** 协议开源，详见 [LICENSE](LICENSE)。

《SEPHIRIA / 赛菲莉娅》游戏本体及其武器名称、设定等知识产权归 TEAM HORAY 所有。
本项目是非官方同人作品，与 TEAM HORAY 无关联，也未获得其授权或认可；
MIT 协议仅覆盖本仓库中的代码与资源文件。

## 贴图

武器贴图是 16×16 的占位像素图，由 `tools/gen-textures.ps1` 按字符画生成
（只用 .NET 自带的 System.Drawing，不需要 Python）。改完字符画重跑脚本即可，
`tools/verify-textures.ps1` 会把 PNG 反解回字符图用于核对。
