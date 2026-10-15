# 赛菲莉亚幻想 模组 — 工作约定



**开工前先读「记忆手册.md」**：那里记着当前做到哪、关键设计决策、26.2/26.3 上踩过的坑、以及还没做的事。
数值与公式看「武器数据表.txt」，每个提交的动机看 git 提交信息。
这个文件是给在本仓库工作的助手（以及人）看的协作约定，优先于默认习惯。

## 构建与实装

- 改动完成后**直接实装**：运行 `tools/deploy.ps1`。它做三件事——Gradle 构建、清掉 mods 里
  其它名字的旧 jar（同 mod id 有两个 jar 会报重复 mod）、把新 jar 复制到 PCL 实例的 mods 目录。
  **部署前确认游戏已关**：脚本会自己拦（检测到在跑的游戏进程就拒绝，除非加 `-Force`），
  因为覆盖正在运行的 jar 会让游戏随后报 `ZipFile invalid LOC header` 崩掉（见记忆手册 §三）。
- jar 名字跟 `gradle.properties` 里的 `version` 走（当前 `0.2.10.15-a`），
  所以是 `build/libs/sephiria-fantasy-<version>.jar` → `mods\sephiria-fantasy-<version>.jar`；改版本号只要改那一处。
- **版本号带日期**：`0.<大版本>.<月>.<日>-a`（10 月 9 日 → `0.1.10.9-a`；`-a` = A 测）。
  大版本号由用户定，2026-10-15 起是 **0.2**。
- **同一天第二次及以后的发布加补丁号**（用户 2026-10-15 定的规则）：当天第一次发还是
  `0.2.<月>.<日>-a`，之后同一天再发（比如刚发完就发现 bug 要重发）就写
  **`0.2.<月>.<日>.<N>-a`**，`N` = 这是当天第几次补丁（第一次补丁 `.1`，第二次 `.2`……）。
  例：10-15 已经发过 `0.2.10.15-a`，同一天再发就是 `0.2.10.15.1-a`。
  **每次「发布」前先把 `gradle.properties` 的 `version` 改成当天的日期**再构建实装——
  jar 名、release 的 tag（`v<版本>`）与游戏里 mod 列表显示的版本都跟着它。
- PCL 实例：`D:\PCL\.minecraft\versions\26.3-Fabric 0.19.5\`（已从 26.2 迁到 26.3；
  以 `tools/deploy.ps1` 里写死的路径为准，别照旧路径找 jar）。
- **不要自动启动游戏。** 用户自己从 PCL2 打开实例测试。开发客户端 `gradlew.bat runClient`
  只在用户明确要求时使用（例如需要看 `run/logs/latest.log` 排查渲染问题）。
- 改完资源跑 `tools/check-resources.ps1`（合成表图案、标签引用、进度条目、原版 id 白名单、
  物品模型引用、中英语言键等）。**动过 `assets/…/items/*.json`（物品模型定义）再跑一次
  `gradlew.bat modelDecodeCheck`**：物品模型只在客户端资源加载时解析，字段名写错要开游戏才发现，
  这个任务用原版真编解码器把本模组的定义解一遍（`src/test/java/ModelDecodeCheck.java`，
  只在开发时用、不进 jar）。
- 实装后可以扫一遍 PCL 实例的 `logs\latest.log`（用户跑过就有）确认没有异常，但不要为此启动游戏。

## 版本管理

- 仓库：https://github.com/Lch2018/sephiria-fantasy （main 分支）
- 用户说「保存」= 打快照：`tools/snapshot.ps1 -Label <名字>`，产物在 `snapshots/`。
- 用户说「上传」= commit + push。提交信息用中文，说明改了什么、以及为什么这么改。
- 推送需要本地代理：git 为 github.com 单独配置了 `http://127.0.0.1:7897`。
  代理没开时推送会报 `Failed to connect ... over proxy`，此时直连也解析不了域名，
  必须等用户把代理打开再推。
- 用户说「发布」= 建 GitHub Release（仓库本身已经是 public）：
  `gh release create v<版本> build/libs/sephiria-fantasy-<版本>.jar --title "赛菲莉亚幻想 <版本>（A 测）" --notes-file <说明.md> --target main`。
  **本机的 `gh` 没有登录**，发布前先用 git 自己的凭据喂给它（别把 token 打印出来）：
  先 `CRED=$(printf "protocol=https\nhost=github.com\n\n" | git credential fill)`，
  再 `export GH_TOKEN=$(printf '%s\n' "$CRED" | sed -n 's/^password=//p')`，
  同时 `export HTTPS_PROXY=http://127.0.0.1:7897`（gh 也要走代理）。
- 发布说明（GitHub Release 与 Modrinth 的项目正文 / 更新说明）**一律中英双语**：中文在前，
  之后 `## English` 一节写同样的四块——运行环境（26.3 + Fabric Loader 0.19.5 + Fabric API + GeckoLib）、
  这一版有什么、已知问题 / 未完成、以及「同人模组 / 大量使用 AI 生成代码 / 禁止商业行为」的声明。
  **译文里的专有名词跟 `src/main/resources/assets/sephiria_fantasy/lang/en_us.json` 走**（那是模组自己的官方英文）：
  武器 Blade / Steel Greatsword / Dagger / Default Sword and Shield / Quarterstaff / Colossal Crossbow，
  连击 Sturdy / Wind Song / Precision / Shadow / Negotiation / Element / Magitech / Dark Cloud /
  Sun Sword / Embers，减益 Shock / Burn，属性页是 Attributes。
- Modrinth 的摘要字段（`description`）上限 **256 字符**，中英都要塞进去（当前那条 246）——改了先数一下长度。
- 同一版**同时发 Modrinth**：`powershell -NoProfile -ExecutionPolicy Bypass -File tools\publish-modrinth.ps1`
  （slug `sephiria`；项目没建过就建、建过就直接传新版本）。它要一份 Modrinth 个人访问令牌（PAT）——
  <https://modrinth.com/settings/pats> 新建，勾 **PROJECT_CREATE** 与 **VERSION_CREATE**，
  然后按 `-Token` → 环境变量 `MODRINTH_TOKEN` → `%USERPROFILE%\.sephiria-fantasy-modrinth-token`（首行）的顺序找。
  **令牌不进仓库**（那个文件刻意放在仓库外）。项目正文 `tools/modrinth/description.md`、
  本次版本说明 `tools/modrinth/changelog.md`（每次发布前改写它），脚本会带上 Fabric API 与 GeckoLib
  两个必需依赖。走 API 而不是网页：内置浏览器不支持文件选择框，网页端传不了 jar。
  改这个脚本时注意两点：文件是**带 BOM 的 UTF-8**（PS 5.1 否则按 ANSI 读、中文会把代码吃掉），
  以及别把自写的 JSON 序列化器换回 `ConvertTo-Json`（见记忆手册 §三）。

## 资源与模型

- 游戏实际读取的资源以 `src/main/resources/assets/sephiria_fantasy/` 为唯一来源；
  Blockbench 工程副本与导出留档放在 `models/`。
- GeckoLib 的资源路径规则（踩过坑）：
  - 模型/动画只扫描 `assets/<ns>/geckolib/models/` 与 `assets/<ns>/geckolib/animations/`，
    代码里用 `命名空间:文件名` 当缓存键引用；
  - 贴图走原版渲染管线，代码里必须给**带 `textures/` 前缀和 `.png` 后缀的完整路径**；
  - 动画按**名字**在 `getAnimationResource` 返回的**那一个文件**里查找，单独导出的动画
    必须合并进主文件（见 `tools/merge-katana-animations.ps1`）。
- 导入流程：`tools/import-katana.ps1`（工程导出 → 资源包，并归一化 `format_version`）。
- 改完资源跑 `tools/check-resources.ps1` 校验，其中包含「UV 必须落在 0~16」的守卫
  （Blockbench 像素空间 UV 曾导致贴图错乱），以及物品模型定义引用到的每个模型 / 贴图是否都在。
- **同一个物品按状态换贴图**（永恒蚀的三种颜色、掉落的太阳剑、冰霜武具的三个飞行体）不用写客户端代码：
  状态写进物品组件，模型定义里用 `minecraft:select` + `minecraft:component`（按值挑模型）或
  `minecraft:condition` + `minecraft:has_component`（只判断有没有）——样板看原版
  `assets/minecraft/items/compass.json` / `clock.json`，细节与坑见记忆手册 §三。

## 代码风格

- 注释与文档用中文，解释**为什么**这样写，而不是复述代码做了什么。
- 可调数值集中在类顶部声明，改参数尽量只动一处常量。
- 现有的数值总览见 `武器数据表.txt`，改数值后同步更新它。
