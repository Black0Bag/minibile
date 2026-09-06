# 01 GitHub 云构建、强制版本与原始 Release 基线

## 旧实现情况

2026-09-05 本轮实测：

- 本地 `HEAD` 与 GitHub `main` 均为 `f323d6c50fa661837fad06d4618462861779b562`
- 仓库没有 Tag、GitHub Release、Actions 运行记录、Actions Secrets、Actions Variables 或 Ruleset
- `app/build.gradle.kts` 硬编码 `versionName = "1.12.1+3"` 与 `versionCode = 46`，没有仓库级唯一版本源
- 已安装原始 App 为 `com.ai.assistance.operit`，系统报告 `versionName=1.12.1`、`versionCode=46`，并存在既有签名轮换链
- `AboutScreen.kt` 已从安装包 `PackageInfo.versionName` 显示 App 版本，无需建立第二套 UI 版本状态
- `android-build.yml` 在 `main` push 时只运行 `assembleDebug` 并上传保留 14 天的 Artifact，不创建 Tag 或 Release
- `android-tests.yml` 运行 JVM 单测；两个 Workflow 的 `GITHUB_TOKEN` 都只有 `contents: read`
- 当前 Linux 环境没有 JDK/Android SDK/NDK/CMake，用户已决定不在本地安装 Android 构建工具链
- 仓库中已有的 `assets/jks.jks` 与 `pkcs12.keystore` 只供子包调试签名，不能作为自定义 App 的 Release 身份

因此当前能看到 CI 配置，但不存在“版本推进、固定签名、永久 Release、失败追踪”的完整交付链路。

## 意图修正

本地只负责代码阅读、编辑、静态检查、版本推进、Git 提交与推送。GitHub Actions 作为 Android 测试、构建、签名和发布的唯一权威环境；Agent 持续跟踪运行与失败日志，修复到 GitHub Release 中出现校验通过的 APK 后才通知用户安装。

当前未定制项目的管理起点是 `0.0.0`。这是首次建立本项目独立版本历史的 bootstrap 版本；它成功发布后，任何新的仓库修改都必须使用更高版本。

## 预期新实现

### 唯一版本契约

- 根 `VERSION` 只包含一个 `MAJOR.MINOR.PATCH`，初始为 `0.0.0`
- 根 `CHANGELOG.md` 必须有当前版本章节和明确发布说明
- Gradle 读取 `VERSION`，不再硬编码 App 版本
- `versionName = VERSION`
- `versionCode = major * 1_000_000 + minor * 1_000 + patch + 1`
- `minor < 1000`、`patch < 1000`，并校验 Android `versionCode` 上限
- `0.0.0` 唯一映射到 `versionName=0.0.0`、`versionCode=1`
- Git Tag 和 Release 名称为 `v{VERSION}`
- APK 为 `minibile-v{VERSION}-arm64-v8a.apk`
- 校验文件为 `minibile-v{VERSION}-arm64-v8a.apk.sha256`

一个版本只能绑定一个 Git commit。已创建的 Tag、已发布 Release 和资产不移动、不覆盖、不删除。相同提交因 Runner 或网络瞬时故障可原样重跑；只要修改仓库形成修复提交，就必须提升版本。

### 强制版本门禁

版本检查脚本至少验证：

1. `VERSION` 格式与数值范围
2. bootstrap 提交只能从 `0.0.0` 开始
3. 后续 PR 的版本严格大于目标分支版本，也严格大于历史 SemVer Tag
4. 当前版本在 `CHANGELOG.md` 中恰有一个非空章节
5. `v{VERSION}` 不存在，或仅在发布恢复场景中指向当前提交
6. Gradle 解析值与公式一致
7. 最终 APK 的 package、`versionName`、`versionCode` 与版本源一致
8. Tag 目标提交、Release 名称、APK 文件名、SHA256 和签名证书指纹一致

建立 GitHub `main` Ruleset，要求通过 PR 和必需 CI 检查后才能合并；禁用普通绕过。这样错误版本不仅无法发布，也无法直接进入受保护的发布分支。

### CI 与 Release 流程

1. PR：完整检出历史和 Tag，运行版本门禁、JVM 单测及不需要私钥的候选构建检查
2. 合入 `main`：重复版本门禁和测试，恢复外部依赖，使用固定工具链构建 Release APK
3. 从 GitHub Secrets 临时还原专用 Release keystore，构建后立即清理，禁止进入缓存和 Artifact
4. 使用 `apkanalyzer` 或 `aapt` 读取最终 APK 元数据
5. 使用 `apksigner verify --verbose --print-certs` 校验签名与登记证书 SHA-256 指纹
6. 生成并复核 APK SHA256
7. 全部前置成功后创建只指向当前提交的 Annotated Tag
8. 创建 Draft Release、上传 APK 和 SHA256、反向核对资产，再发布 Release
9. 若 Tag/草稿创建后网络失败，只允许同一提交续建；若需改代码或 Workflow，保留旧 Tag 作为失败记录并提升版本
10. Agent 轮询 Actions 到终态，下载失败日志定位；成功后复查 Release API 和资产，再通知用户

测试/构建 Job 保持 `contents: read`；只有发布 Job 使用最小 `contents: write`。第三方 Actions 固定完整提交 SHA。

### 固定签名身份

- 生成一套仅用于本项目 App Release 的长期密钥，不复用仓库已有子包测试密钥
- keystore、口令和 Base64 仅进入经用户批准的仓库外备份及 GitHub Actions Secrets
- 仓库只记录 keystore 文件 SHA256 和公开签名证书 SHA-256 指纹
- 首次发布前必须实测备份可恢复；无法证明可恢复时停止发布
- 首个成功 Release 后冻结 `applicationId` 与签名证书，后续 APK 必须可覆盖升级

## 作用域

候选仓库改动：

- `VERSION`
- `CHANGELOG.md`
- `app/build.gradle.kts`
- `.github/workflows/android-build.yml`
- `.github/workflows/android-tests.yml`
- 必要的 `ci/script/` 版本与发布校验脚本及测试
- 本步骤证据文档

GitHub 侧改动：

- Actions Secrets
- `main` Ruleset 与必需状态检查
- Annotated Tag
- GitHub Release 与 APK/SHA256 资产

本地不安装 JDK、Android SDK、NDK、CMake，也不提交 SDK、构建缓存、APK 或私钥。

## 已确认的包身份与密钥位置

用户于 2026-09-05 确认：

- 首个自定义 App 的 `applicationId` 固定为 `io.github.black0bag.minibile`，与当前 `com.ai.assistance.operit` 并行安装，不卸载当前 App
- Kotlin 源码包声明和 Android `namespace` 暂时保留 `com.ai.assistance.operit`；只改变安装身份，不做无收益且高风险的全仓源码包迁移
- T01 必须扫描并修正真正依赖安装包 ID 的测试、ADB/ToolPkg 调试脚本、广播 action/component、外部目录和文档；使用 `context.packageName`、`BuildConfig.APPLICATION_ID` 或 Manifest `${applicationId}` 的动态路径保持动态，不做全局字符串替换
- Release 签名资产目录固定为工作区根级 `../.release-signing/`，预定 keystore 为 `../.release-signing/minibile-release.jks`
- 该目录已创建、权限为 `700` 且当前为空；keystore 生成后权限必须为 `600`
- 工作区根级 `../WORKSPACE.md`（从 Git 仓库根目录计算）是项目目录之外文件和目录的首读索引；它位于仓库外，因此不使用会在 GitHub 上断裂的 Markdown 链接

风险仍然成立：工作区处于当前 Operit App 私有目录，清除数据、卸载当前 App 或删除整个工作区会删除签名资产。用户选择让密钥跟随工作区，因此每次复制/归档工作区必须包含隐藏目录 `.release-signing/`；GitHub Secrets 只用于 CI 注入，不是可下载备份。

## 实施前剩余门禁

包身份和密钥目录已经确认。开始生成密钥、写入 GitHub Secrets、配置 Ruleset、修改 Gradle/Workflow、提交、推送、创建 Tag/Release 前，仍需用户明确批准执行 T01。

## 验证

- PR 必需检查与 `main` Ruleset 生效，错误版本 PR 无法合并
- `0.0.0` 映射为 App `versionName=0.0.0`、`versionCode=1`
- About 页面仍从安装包元数据展示 `0.0.0`
- GitHub Actions JVM 单测与签名 Release 构建成功
- APK package、版本、ABI 和文件名正确
- APK 证书指纹等于登记值，keystore 未进入日志、缓存或 Artifact
- APK SHA256 与发布校验文件一致
- Tag 精确指向构建提交，Release 名称和当前版本一致
- Release API 中存在 APK 和 SHA256 两个非空资产
- 用户下载后完成安装、启动、基础对话、工作区和终端烟测
- 模块数、工具数、路由数、权限数、APK 大小和启动时间基线留档

## 风险

- 当前仓库公开且 `main` 无保护，必须先建立门禁再进入长期发布
- GitHub/Google Drive/CMake 上游网络波动可能导致瞬时失败
- 原始源码可能存在真实测试或构建失败
- GitHub Secrets 不等于可下载备份；若无仓库外备份，签名身份可能永久丢失
- 私钥若被输出到日志或 Artifact 将不可逆泄露
- 保留原包名会要求卸载当前 App，进而删除其私有目录中的本工作区
- Release 创建跨 Tag、草稿和资产上传多个步骤，必须校验幂等恢复，避免半成品发布

## 回滚

- Ruleset 配置前先保留可恢复参数，误配置时只回退该规则，不改提交历史
- 未创建 Tag 的失败提交保留 Actions 日志；仓库修复以更高版本的新提交继续
- 已创建 Tag 但未发布的失败只允许同一提交恢复；若需改仓库则不移动旧 Tag，改用更高版本
- 已发布版本不原地回滚；使用 `git revert` 形成新提交并发布更高版本
- GitHub Secrets 可轮换但不得删除唯一备份；签名私钥丢失或泄露时停止发布并提交新的 L3 密钥迁移计划
