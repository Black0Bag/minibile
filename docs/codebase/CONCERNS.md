# Codebase Concerns

> 实测生成，2026-09-03。仅记录可验证的问题，无证据的不写。

## Core Sections (Required)

### 1) Top Risks (Prioritized)

| Severity | Concern | Evidence | Impact | Suggested action |
|----------|---------|----------|--------|------------------|
| **high** | 构建依赖缺失，当前克隆**无法直接构建** | `app/libs/`、`app/src/main/jniLibs/`、`app/src/main/assets/subpack/` 三处只有 `.keep` | 任何代码改动都无法编译验证 | 先从 BUILDING.md 的 Google Drive 链接下载 `subpack.zip`、`jniLibs.zip`、`libs.zip` 并放置 |
| **high** | 5 个"上帝文件"承载核心逻辑，无单测覆盖 | `SystemToolPromptsInternal.kt` 5992 行、`StandardFileSystemTools.kt` 4872 行、`PackageManager.kt` 3790 行、`EnhancedAIService.kt` 3209 行、`ChatViewModel.kt` 3199 行 | 改动回归风险高，且与上游同步时必冲突 | 改动前先隔离要动的部分；大改前补测试 |
| **high** | 工具契约需 7 处同步，无自动化校验 | `docs/doc-src/architecture/DEFAULT_TOOLS_ARCH.md:25-201` 的 checklist | 改一个工具参数漏改任一处 → 运行时工具调用失败或脚本侧类型错 | 改工具时严格按 checklist 走；考虑加一个一致性校验脚本到 `ci/script/` |
| **med** | 无覆盖率工具 + 文件级覆盖约 9% | 实测 `grep jacoco\|kover\|coverage` 全仓无命中；129 测试 / 1397 源文件 | 无法量化改动风险 | 若做大改造，建议引入 Kover 建立基线 `[ASK USER]` |
| **med** | 1351 个 `@Composable` 只有 2 个 `@Preview` | 实测 grep | UI 改动只能整机构建安装验证，迭代极慢 | UI 定制时给新组件补 Preview |
| **med** | 两个 keystore 被 Git 跟踪，口令硬编码 `"android"` | `git ls-files app/src/main/assets/jks.jks pkcs12.keystore` → 均被跟踪；`KeyStoreHelper.kt:180` | 子包/APK 重打包签名可被任何人复现（非发布密钥，影响有限） | 保持现状可接受；若要清理需同步改 `KeyStoreHelper` 的 asset 加载逻辑 |
| **med** | 局域网 HTTP / Web Chat / A2A 三个服务暴露面 | `ExternalChatHttpServer.kt`（默认端口 8094）、`WebChatHttpBridge.kt`、`A2aHttpHandler.kt` | 启用后同网段设备可访问；虽有 Bearer 强制校验，但仍是攻击面 | 已实现 Token 强制（未配置即拒绝服务），保持默认关闭 |
| **med** | 无依赖注入 + 188 个 `object` 单例 | 实测 `grep -c '^object [A-Z]'` = 188 | 业务编排层不可单测；测试间状态污染无防护 | 新增代码用构造注入；不强求重构既有单例 |
| **low** | Android Lint 已被主动移除 | `docs/TODO/ci_remove_android_lint_20260729/`、`ci_skip_android_lint_20260806/` | 失去静态检查（资源泄漏、API 兼容性告警） | 上游有意为之（CI 时长权衡），不建议单方面加回 |
| **low** | 重复库并存 | Coil + Glide；kotlinx.serialization + Gson + Moshi + HJSON | 包体积、认知负担 | 新代码统一用 Coil + kotlinx.serialization |

### 2) Technical Debt

| Debt item | Why it exists | Where | Risk if ignored | Suggested fix |
|-----------|---------------|-------|-----------------|---------------|
| 上帝文件 | 功能持续叠加，未拆分 | 上表 5 个文件 | 合并冲突、回归难测 | 按职责抽子模块（如把 `SystemToolPromptsInternal` 按工具族拆多文件） |
| `KeyStoreHelper` 多路尝试逻辑 | 历史代码 | `core/subpack/KeyStoreHelper.kt:158-190`（PKCS12→JKS→assets→默认路径） | 与 `AGENTS.md` 明令禁止的 fallback 精神直接冲突 | 若要遵守 AGENTS.md，应改为单一确定路径 + 失败抛异常 |
| 3 个文件绕过 `AppLogger` 直用 `android.util.Log` | 遗留 | 实测 3 个文件 | 日志不受统一脱敏控制 | 替换为 `AppLogger` |
| 包名拼写错误 `accessbility` | 早期笔误固化 | `core/tools/defaultTool/accessbility/` | 认知混淆 | **不建议改**：牵动 `ToolGetter` + 全部导入，收益低 |
| `mockk` 引入但注释禁用 | 未完成迁移 | `app/build.gradle.kts` 注释块 | 只能 Mockito，无法 mock final 类 | 需要时启用 |
| `exportSchema = false` | Room 配置 | `data/db/AppDatabase.kt:28` | 22 个迁移无 schema 快照可比对，迁移错误只能运行时发现 | 开启 exportSchema + 加 Room migration test |
| ObjectBox 与 Room 无统一事务 | 双库设计 | `data/db/` | 跨库一致性靠应用层（如删对话时清记忆） | 明确边界文档化，或加一致性校验 |
| 7 处生产代码 TODO | 正常演进 | 见下表 | 低 | 按需处理 |

**生产代码 TODO 全量清单（实测仅 7 处，`app/src/main/java` 下）**：

| 文件:行 | 内容 | 性质 |
|---|---|---|
| `api/speech/SherpaMnnSpeechProvider.kt:167` | 需要检查 VAD 模型格式或使用兼容的模型版本 | 功能未完成 |
| `data/repository/UIHierarchyManager.kt:50` | 非 Google Play 发布可改为直接下载 URL | 配置说明 |
| `ui/features/chat/components/lazy/LazyLayoutItemAnimator.kt:213` | 来自 AndroidX 上游源码的 TODO | **第三方拷贝代码** |
| `ui/features/chat/components/lazy/LazyLayoutScrollScope.kt:293` | 同上 | **第三方拷贝代码** |
| `ui/features/chat/components/lazy/LazyListState.kt:491` | 同上 | **第三方拷贝代码** |
| `ui/features/chat/screens/AIChatScreen.kt:1405` | 实现取消导出的逻辑 | 功能缺口 |
| `ui/features/toolbox/screens/ToolboxScreen.kt:221` | 需要重构以适配新的终端架构 | 架构债 |

注：`.codebase-scan.txt` 报了大量 TODO，但绝大多数在 `app/src/main/assets/bridge/spawn-helper.js`（打包的 ajv 库）与 `assets/js/terser.bundle.min.js`（terser 库）中 —— **这些是第三方 bundle，不是本项目债务**。

### 3) Security Concerns

| Risk | OWASP | Evidence | Current mitigation | Gap |
|------|-------|----------|--------------------|-----|
| API Key 明文存于 DataStore | A02 加密失效 | `ModelConfigManager.kt:32` 用 `versionedPreferencesDataStore`，无加密；仅 `CodexAuthPreferences.kt` 用 EncryptedSharedPreferences | Android 应用私有目录隔离 | 未 root 设备安全；root/备份提取可读。可考虑统一走 EncryptedSharedPreferences |
| 局域网 HTTP 服务暴露 | A01 访问控制 | `ExternalChatHttpServer.kt` 默认端口 8094；`WebChatHttpBridge.kt:2565-2589`；`A2aHttpHandler.kt:104` | **默认关闭**（`KEY_ENABLED ?: false`）+ 强制 Bearer（未配置 Token 直接拒绝服务） | 无 IP 白名单、无 HTTPS、无速率限制；局域网内 Token 泄露即失守 |
| 插件（ToolPkg）权限面过大 | A08 软件完整性 | `plugins/toolpkg/` 11 个 Hook 桥；`ToolPkgAiProviderRegistry.kt` | `PluginDenylistRepository.kt`（远程黑名单 + SHA256 校验）、`ToolPkgHookExecutionBudget.kt`（执行预算） | 黑名单是事后拦截；插件能改 prompt、换 provider、读对话内容 |
| Root / Shizuku / 无障碍高危权限 | A01 | `AndroidManifest.xml` 声明 MANAGE_EXTERNAL_STORAGE、QUERY_ALL_PACKAGES、SYSTEM_ALERT_WINDOW、WRITE_SETTINGS 等 | 三档工具权限设置（自动允许/每次询问/禁止），**默认询问模式** | 用户一旦选"自动允许"，AI 可执行任意 shell |
| keystore 入库 + 硬编码口令 | A05 配置错误 | `assets/jks.jks`、`assets/pkcs12.keystore` 被跟踪；口令 `"android"` | 仅用于应用内子包签名，非发布密钥 | `.gitignore` 写了 `*.jks`/`*.keystore` 但这两个是历史例外 |
| JS 脚本沙箱逃逸 | A03 注入 | `JsEngine.kt`（QuickJS）+ `JsJavaBridge.kt` 暴露 Java 能力 | QuickJS 隔离 + `JsTimeoutConfig` + `ToolExecutionLimits` | Java Bridge 本身就是逃逸通道（设计如此），依赖包来源可信 |
| WebView 用户脚本 | A03 | `defaultTool/websession/userscript/` | 需用户主动导入（`UserscriptImportPickerActivity`） | 用户脚本可读写页面数据 |

**已确认的安全正面项**：
- ✅ 生产 Kotlin 代码无硬编码第三方 API Key
- ✅ `local.properties` 与密钥在 `.gitignore`（`.gitignore:2,3,8,20`）
- ✅ HTTP 日志脱敏（`HttpLogSanitizer.kt`：URL 只留 host，path/query 只输出段数）
- ✅ 依赖漏洞已处理过一轮（`docs/TODO/dependency_security_updates/` 标记 `[DONE]`：Vite → 6.4.3、esbuild → 0.25.12、移除 uuid，npm audit 零漏洞）
- ✅ 排除 `bcprov-jdk15to18` 避免重复类冲突（`app/build.gradle.kts` `configurations.all { exclude }`）

### 4) Performance and Scaling Concerns

| Concern | Evidence | Current symptom | Scaling risk | Suggested improvement |
|---------|----------|-----------------|-------------|-----------------------|
| Application 冷启动重 | `OperitApplication.kt` 682 行，20+ 子系统初始化 | 同步段含 PDFBox、LanguageFactory、内置插件 | 插件/Skill 越多启动越慢 | 项目已有 `AnrMonitor.kt` 监控；可把更多同步项挪到异步或懒加载 |
| APK 体积 | `terminal/` 62.6MB Ubuntu rootfs + 3 个内置 APK（desktop 6.3MB / accessibility 2.7MB / shizuku 2.5MB）+ apktool jar 14MB | 安装包极大 | 无法通过 Play 体积限制 | 按需下载而非内置（架构级改动） |
| Compose 重组风险 | 自定义 Lazy 实现（`LazyListState.kt` 491 行、`LazyLayoutItemAnimator.kt`、`LazyLayoutScrollScope.kt` —— 从 AndroidX 拷贝改造） | 聊天列表已深度定制以解决性能 | 改聊天 UI 容易破坏这些优化 | 改前必读这 3 个文件；不要用标准 LazyColumn 简单替换 |
| 向量索引内存 | `util/vector/VectorIndexManager.kt` + hnswlib | 记忆库大时索引常驻内存 | 记忆条目增长线性占用 | 未见分片/落盘策略，[TODO] 需进一步确认 |
| 多 ObjectBox store 并存 | `ObjectBox.kt` `ConcurrentHashMap<String, BoxStore>` 且只在显式 `close()` 时释放 | 每个记忆 profile 一个 store | profile 多时句柄与内存累积 | 加 LRU 关闭策略 |
| 资源池预热 | `ImagePoolManager`/`MediaPoolManager`/`SkillRepoZipPoolManager` 三个池 | 启动后异步 `preloadFromDisk()` | 磁盘 IO 与内存压力 | 已异步化，可接受 |
| 7497 字符串 × 8 语言 | `strings.xml` churn 榜第一（90 天 78 次改动） | 资源表巨大 | 编译期与 R 类膨胀 | `android.nonTransitiveRClass=true` 已开启 |

### 5) Fragile/High-Churn Areas

数据来自 `.codebase-scan.txt` HIGH-CHURN FILES（最近 90 天）+ `git log` 实测（90 天 406 次提交，8 位活跃作者）。

| Area | Why fragile | Churn signal | Safe change strategy |
|------|-------------|-------------|----------------------|
| `res/values*/strings.xml` | 8 语言必须同步，漏键 CI 失败 | 78 / 76 / 46×4 / 45 / 34 次 | 改文案先跑 `ci/script/check_localizations.py` |
| `api/chat/llmprovider/OpenAIProvider.kt` | 2758 行，各厂商兼容性补丁集中地 | 36 次 | 改前看 `EndpointCompleter*Test` 覆盖了哪些 case；新增厂商优先新建 Provider 而非改这里 |
| `api/chat/llmprovider/GeminiProvider.kt` | 2140 行，thinking config 频繁变动 | 24 次 | 有 `GeminiThinkingConfigTest` 可依赖 |
| `api/chat/llmprovider/OpenAIResponsesProvider.kt` | 新 API 适配中 | 21 次 | 上游仍在迭代，改动易冲突 |
| `data/preferences/ApiPreferences.kt` | 全局配置聚合点 | 20 次 | 加字段要考虑 `versionedPreferencesDataStore` 迁移 |
| `app/build.gradle.kts` | 签名/依赖/资源准备逻辑都在这 | 31 次 | 改依赖时注意 `configurations.all` 的 exclude |
| `ui/features/packages/screens/ArtifactPublishScreen.kt` | 市场发布流程持续变化 | 19 次 | — |
| `ui/features/chat/screens/AIChatScreen.kt` + `ChatViewModel.kt` | 主界面 + 3199 行 VM | 16 / 15 次 | **这是 UI 定制的主战场**，改前建议先读 `services/core/` 8 个 Delegate 理解状态流 |
| `ui/features/chat/components/style/input/agent/AgentChatInputSection.kt` | 3345 行输入区 | 15 次 | 输入区功能极密（附件/语音/工具/模式切换） |
| `api/chat/llmprovider/AIServiceFactory.kt` | Provider 装配点 | 16 次 | 加 provider 必改此处 |

**与上游同步的冲突预警**：本 fork（`Black0Bag/minibile`）目前与上游 `AAswordman/Operit` 的 `main` **完全同步、零本地改动**（`git status` 干净，`git log` 无本地提交）。一旦开始定制，上述高 churn 文件将是每次同步上游时的主要冲突点。上游 90 天 406 次提交、8 位活跃作者，迭代非常快。
### 6) `[ASK USER]` Questions

**已决策（2026-09-03 用户确认）**：

| # | 问题 | 用户决策 | 技术含义 |
|---|------|---------|---------|
| 1 | 是否保持与上游 `AAswordman/Operit` 同步？ | **不同步，基于当前代码独立演进** | 可自由重构/删功能/改架构，无需顾虑合并冲突；高 churn 文件不再是禁区；同时失去上游 bug 修复与新功能 |
| 4 | 与 `minibox` 项目的关系？ | **无任何关系，禁止建立任何关联** | minibile 是完全独立项目；分析、设计、命名均不得引入 minibox 概念 |

**待讨论**：

| # | 问题 | 状态 |
|---|------|------|
| 2 | 定制方向（裁剪 / 增强 / 替换） | **下一轮专门讨论** |
| 3 | 三个依赖 zip 如何处理 | 已说明用途（见 `STACK.md` 环境章节与下方补充） |

**仍未决策**：

5. **[ASK USER] 是否沿用上游 `AGENTS.md` 的硬约束？** 特别是"禁止一切 fallback/兜底/降级"这条。既然不再同步上游，这份约束可以自定。
6. **[ASK USER] 是否引入覆盖率工具（Kover）建立测试基线？** 当前文件级覆盖约 9%，主链路零覆盖。
7. **[ASK USER] APK 体积是否是约束？** 内置 62.6MB Ubuntu rootfs + 3 个 APK + 14MB apktool jar，裁掉可省 80MB+。

### 6.1) 外部依赖归档的真实作用（实测）

三个 zip 由 `ci/script/download_android_dependencies.sh` 从固定 Google Drive 文件 ID 下载，`ci/script/prepare_android_dependencies.py` 校验并解压。

| 归档 | 解压到 | 构建阻断？ | 实际内容与用途 |
|---|---|---|---|
| `libs.zip` | `app/libs/` | ✅ **是** | 提供 `ffmpeg-kit-local.aar`（自编译的 FFmpegKit，含 10 个 arm64 `.so`：libavcodec/libavformat/libavfilter/libavutil/libavdevice/libswscale/libswresample/libffmpegkit/libffmpegkit_abidetect/libc++_shared）。`app/build.gradle.kts:595` 直接 `implementation(files(...))` 引用 |
| `jniLibs.zip` | `app/src/main/jniLibs/` | ✅ **是** | 提供 `arm64-v8a/liboperit_ripgrep.so`（Rust 编译的原生 ripgrep，供 `util/ripgrep/` 全文搜索）。注意 `libstreamnative.so` 与 `libtoolpkgwasm.so` 由本地 CMake 构建（`app/src/main/cpp/CMakeLists.txt`），**不在这个 zip 里** |
| `subpack.zip` | `app/src/main/assets/subpack/` | ❌ **否** | 提供 `subpack/android.apk` 与 `subpack/windows.zip`，仅在运行期由 `ui/features/chat/components/ExportDialogs.kt:782,893` 读取，用于"把对话/项目导出成独立 APP"功能。`grep -c subpack app/build.gradle.kts` = 0，构建流程完全不校验 |

**构建阻断的具体机制**（`app/build.gradle.kts:170-206, 561-564`）：
```kotlin
val verifyExternallyBuiltNativeLibraries by tasks.registering { ... }
tasks.named("preBuild") { dependsOn(verifyExternallyBuiltNativeLibraries) }
```
该任务在 `preBuild` 阶段用 `require()` 强校验：
1. `src/main/jniLibs/arm64-v8a/liboperit_ripgrep.so` 存在且非空
2. `libs/ffmpeg-kit-local.aar` 存在且非空
3. 打开该 AAR，逐个确认 10 个 arm64 `.so` 条目存在且 size > 0

任一条不满足即 `assembleDebug` 在 `preBuild` 就失败。

**可自建（不依赖 Google Drive）**：
- `liboperit_ripgrep.so` ← `tools/native_ripgrep/`（Rust 工程，有 `Cargo.toml` + `Cargo.lock`；现成脚本是 `.ps1`/`.bat` 仅 Windows，Linux 需自行用 cargo + NDK target 编译）
- `ffmpeg-kit-local.aar` ← `tools/ffmpeg/build_ffmpeg_kit_wsl.sh`（WSL 脚本，从源码编译 FFmpeg，耗时以小时计）
- `subpack/*` ← 无自建脚本，是预先打包好的模板产物

**风险**：下载脚本把 Google Drive 文件 ID 硬编码（`download_android_dependencies.sh:35-39`），链接失效或内容被替换都无法察觉（无 SHA256 校验，只有 `test -s` 非空检查）。既然本仓不再同步上游，建议把这三份归档在本地/私有存储做一份带哈希的备份。



### 7) Evidence

- `docs/codebase/.codebase-scan.txt`（TODO 段、HIGH-CHURN 段、CODE METRICS 段）
- `docs/TODO/dependency_security_updates/01_upgrade_vulnerable_dependencies.md`（`[DONE]` 记录）
- `docs/TODO/ci_remove_android_lint_20260729/`、`ci_skip_android_lint_20260806/`
- `docs/doc-src/architecture/DEFAULT_TOOLS_ARCH.md`（7 处同步 checklist）
- `app/src/main/java/com/ai/assistance/operit/core/subpack/KeyStoreHelper.kt:158-190`
- `app/src/main/java/com/ai/assistance/operit/data/preferences/{ModelConfigManager,CodexAuthPreferences,ExternalHttpApiPreferences}.kt`
- `app/src/main/java/com/ai/assistance/operit/integrations/{http,a2a}/`
- `app/src/main/java/com/ai/assistance/operit/data/security/PluginDenylistRepository.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/features/chat/components/lazy/`（拷贝改造的 AndroidX Lazy 实现）
- 实测：`git log --since='90 days ago'`（406 提交 / 8 作者）、`git rev-list --count HEAD`（1597 总提交，起于 2025-03-14）
- 实测：`git status` 干净、`git log` 无本地提交 → fork 零改动状态确认