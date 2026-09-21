# SEPHIRIA 模组 — 工作约定

这个文件是给在本仓库工作的助手（以及人）看的协作约定，优先于默认习惯。

## 构建与实装

- 改动完成后**直接实装**：运行 `tools/deploy.ps1`。它做三件事——Gradle 构建、清掉 mods 里
  其它名字的旧 jar（同 mod id 有两个 jar 会报重复 mod）、把新 jar 复制到 PCL 实例的 mods 目录。
- jar 名字跟 `gradle.properties` 里的 `version` 走（当前 `0.1.0-a`，a = A 测），
  所以是 `build/libs/sephiria-0.1.0-a.jar` → `mods\sephiria-0.1.0-a.jar`；改版本号只要改那一处。
- PCL 实例：`D:\PCL\.minecraft\versions\26.2-Fabric 0.19.5\`。
- **不要自动启动游戏。** 用户自己从 PCL2 打开实例测试。开发客户端 `gradlew.bat runClient`
  只在用户明确要求时使用（例如需要看 `run/logs/latest.log` 排查渲染问题）。
- 实装后可以扫一遍 `run/logs/latest.log`（如果用户跑过）确认没有异常，但不要为此启动游戏。

## 版本管理

- 仓库：https://github.com/Lch2018/sephiria （main 分支）
- 用户说「保存」= 打快照：`tools/snapshot.ps1 -Label <名字>`，产物在 `snapshots/`。
- 用户说「上传」= commit + push。提交信息用中文，说明改了什么、以及为什么这么改。
- 推送需要本地代理：git 为 github.com 单独配置了 `http://127.0.0.1:7897`。
  代理没开时推送会报 `Failed to connect ... over proxy`，此时直连也解析不了域名，
  必须等用户把代理打开再推。

## 资源与模型

- 游戏实际读取的资源以 `src/main/resources/assets/sephiria/` 为唯一来源；
  Blockbench 工程副本与导出留档放在 `models/`。
- GeckoLib 的资源路径规则（踩过坑）：
  - 模型/动画只扫描 `assets/<ns>/geckolib/models/` 与 `assets/<ns>/geckolib/animations/`，
    代码里用 `命名空间:文件名` 当缓存键引用；
  - 贴图走原版渲染管线，代码里必须给**带 `textures/` 前缀和 `.png` 后缀的完整路径**；
  - 动画按**名字**在 `getAnimationResource` 返回的**那一个文件**里查找，单独导出的动画
    必须合并进主文件（见 `tools/merge-katana-animations.ps1`）。
- 导入流程：`tools/import-katana.ps1`（工程导出 → 资源包，并归一化 `format_version`）。
- 改完资源跑 `tools/check-resources.ps1` 校验，其中包含「UV 必须落在 0~16」的守卫
  （Blockbench 像素空间 UV 曾导致贴图错乱）。

## 代码风格

- 注释与文档用中文，解释**为什么**这样写，而不是复述代码做了什么。
- 可调数值集中在类顶部声明，改参数尽量只动一处常量。
- 现有的数值总览见 `武器数据表.txt`，改数值后同步更新它。
