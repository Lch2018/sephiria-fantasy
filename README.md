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

本项目**代码**以 **MIT** 协议开源，详见 [LICENSE](LICENSE)。

《SEPHIRIA / 赛菲莉娅》游戏本体及其武器名称、武器美术等知识产权均归 TEAM HORAY 所有。
本项目是非官方同人作品，与 TEAM HORAY 无关联，也未获得其授权或认可。

因此 **MIT 协议不覆盖 `src/main/resources/assets/sephiria/textures/` 与 `tools/weapon-ref/`
下的武器美术**（那是原作的素材，仅作为个人同人用途引用）；若要把本项目公开分发或商用，
请先自行替换这部分美术或取得授权。

## 贴图

武器贴图**取自《赛菲莉娅》原作的分支武器像素图**——`tools/weapon-ref/` 下的六张 102×120 原图，
按分支命名（shield_sword / great_sword / dagger / crossbow / katana / staff）。

生成方式是**零重采样**：`tools/gen-textures.ps1` 把 102×120 原图逐像素拷到 128×128 画布正中，
贴图与原作美术完全一致，没有缩放也没有重新上色。`tools/verify-textures.ps1` 会核对不透明像素数
与调色板是否与原图完全相同，用来证明过程中没有任何有损处理。

为什么要用这六张：游戏内 UI 的同类图标只有 9×16、9×27、17×18 这类尺寸，wiki 上这六张 102×120
是能找到的最高分辨率版本。它们的调色板与游戏原生素材完全一致
（`#000000 / #BDBECF / #7E87A7 / #4B4B64 / #705052 / #493843 / #FFFFFF`），
轮廓剖面也与游戏内图标逐行吻合（平均差 0.02–0.05），可以确认是同一套美术。

脚本只用 .NET 自带的 System.Drawing，不需要 Python / PIL。

## 手持与图标的朝向

贴图是原作的朝向（竖直的剑、水平的弩），而原版 Minecraft 的剑/弩贴图是斜着画的，
所以两者共用原版 `item/handheld` 变换时，手里握持的角度会差 45° 甚至 135°。
解决办法是**不动贴图，给武器做自定义 `display` 变换**：

- 物品栏图标：不加任何变换（父模型是 `item/generated`，它没有 `gui` 项），
  所以图标就是原作贴图本身，竖的就是竖的。
- 手持：`models/item/weapon.json`（近战）与 `models/item/crossbow.json`（弩）里
  覆盖了第一/第三人称的四个变换，让武器在手中的朝向**与原版同类型武器完全一致**。

角度不是估的，是用游戏自己的 `net.minecraft.client.resources.model.cuboid.ItemTransform`
反解出来的（见 `tools/TransformProbe.java`）：固定原版变换，暴力搜索 Z 分量，
使原版贴图的指向向量与我们的贴图指向向量在持握后重合。结果残差 0.00–0.03°：

| 场景 | 原版 Z | 本 mod Z |
| --- | --- | --- |
| 第一人称 右手 | 25 | -20 |
| 第一人称 左手 | -25 | 20 |
| 第三人称 右手 | 55 | 10 |
| 第三人称 左手 | -55 | -10 |

弩用的原版 `item/crossbow` 变换（X = -90），对应修正 135°：
第一人称右手 80 / 左手 -100，第三人称右手 75 / 左手 -105。

## 手持的体素 3D 模型

**物品栏图标用平面原画，手持时换成体素风格的 3D 模型**——这是原版长矛/三叉戟的做法：
物品定义用 `minecraft:select` 按 `minecraft:display_context` 分流，`gui / ground / fixed / on_shelf`
走平面模型，其余场景走 `<武器>_in_hand` 的 3D 模型。

3D 模型由 `tools/Voxelizer.java` 从原作贴图**程序化生成**（`tools/Preview.java` 负责离线预览渲染），
不是手工摆的方块。做法与取舍：

1. **1:1 体素化**：贴图的每个像素对应一个体素，映射到与平面版完全相同的 16×16 模型投影面上，
   所以 3D 模型与图标位置严格对齐。
2. **描边处理**：原作是粗黑描边风格。全部保留会变成一块黑砖，全部去掉又会丢掉清晰的形体边界，
   所以只把**内部**描边填成最近的材质色，**外轮廓的描边保留**——这正是原作读起来的样子。
3. **断开部件分层**：连通分量分析把不相连的部件分到不同深度层（例如剑盾里的剑整体在盾之前），
   避免两块糊成一个浮雕。
4. **浅浮雕厚度**：按材质给厚度（钢/高光 1.4、深蓝 1.8、木质 2.4，模型单位），
   差值刻意收窄，防止每个色块边界都形成台阶噪声。
5. **矩形合并 + 面剔除**：同色同深的体素合并成立方体（每把 20–145 个元素），
   被同色邻居挡住的面积直接不输出。
6. **调色板贴图**：颜色写进 64×64 调色板纹理，面 UV 取色块中心，避免 mipmap 渗色。

保真度是可量化的：`tools/Voxelizer.java` 会把模型的正视投影与原画**逐像素比对**，
六把武器全部 **100% 吻合**（描边像素按设计被填充为材质色）。

```bat
:: 重新生成（先编译工具）
javac -d <临时目录> tools\Voxelizer.java tools\Preview.java
java -cp <临时目录> Voxelizer <贴图.png> <输出模型.json> <调色板.png> 128 <名字>_in_hand sephiria:item/weapon_3d <预览.png>
```

（`tools/gen-item-definitions.ps1` 负责写六个物品定义，`tools/check-resources.ps1` 会校验
3D 模型、调色板贴图与 display_context 分流是否齐全。）

### 手工设计的部分

图标之外，有些武器**没法只靠描图**，这部分用 `tools/ModelBuilder.java`（体素建模 DSL：
32×32×16 半单位网格、矩形合并、面剔除、调色板贴图、离线预览）在 `tools/WeaponModels.java`
里按特征重新设计：

- **剑盾**：剑和盾是各自独立的模型，由 `display_context` 按手别分配——**副手持盾、主手持剑**。
  盾是带金属包边、木面、加强筋和凸起盾心的圆盾；剑是白高光刃 + 护手 + 缠柄 + 配重。
- **刀**：带剑鞘，有**收鞘 / 拔刀中 / 出鞘**三套模型。右击蓄力触发拔刀动作：物品定义用
  `using_item` + `use_duration` 切换模型，Java 侧（`SephiriaWeaponItem`）给刀加了 `use` 行为
  与 SPEAR 持握姿态，所以按住右键会看到刀从鞘中滑出、松开收回。
- **弩**：改用**原版弩的机制**——`charge_type` 区分已装填、`crossbow/pull` 三档蓄力；
  模型按原版结构搭（木托、弩臂、弓弦、扳机），弓弦随蓄力沿枪身后移，满弦时弩箭上膛。

其余三把（大剑、匕首、长棍）仍由 `Voxelizer` 从原画直接体素化，特征与原画逐像素一致。
