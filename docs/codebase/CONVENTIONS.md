# Coding Conventions

> 实测生成，2026-09-03。**重点：本仓库有一份强约束 `AGENTS.md`，其规则优先于任何通用最佳实践。**

## Core Sections (Required)

### 1) Naming Rules

| Item | Rule | Example | Evidence |
|------|------|---------|----------|
| Kotlin 文件 | PascalCase，一文件一主类，100% 遵守 | `EnhancedAIService.kt`、`ToolGetter.kt` | 实测 1136/1136 个 `.kt` 首字母大写 |
| 包名 | 全小写；少数 camelCase 历史例外 | `llmprovider`、`defaultTool`、`accessbility`(拼写错误保留) | `core/tools/defaultTool/accessbility/` |
| 函数 | camelCase | `getFileSystemTools()`、`nextDelayMs()` | `ToolGetter.kt`、`LlmRetryPolicy.kt` |
| Composable | PascalCase（Compose 规范） | `AppContent()`、`DrawerContent()` | `ui/main/components/` |
| 类型/接口 | PascalCase；sealed 层级用 `object`/`data class` 子类 | `Screen.AiChat`、`Screen.ToolPkgComposeDsl` | `ui/main/screens/OperitScreens.kt` |
| 常量 | UPPER_SNAKE_CASE，放在 `companion object` 或 `internal object` | `MAX_RETRY_ATTEMPTS`、`RETRY_BASE_DELAY_MS` | `LlmRetryPolicy.kt` |
| DataStore Key | UPPER_SNAKE_CASE + `stringPreferencesKey("snake_case")` | `TOOL_PROMPT_VISIBILITY_JSON` ↔ `"tool_prompt_visibility_json"` | `ApiPreferences.kt:110-136` |
| 内置工具名 | snake_case（对 LLM 暴露的契约） | `read_file`、`browser_navigate`、`create_memory` | `ToolRegistration.kt` 179 个 name |
| 权限分级实现类 | `<Level>` + 功能名，Standard 为基类 | `StandardFileSystemTools` / `RootFileSystemTools` / `DebuggerFileSystemTools` | `defaultTool/*/` |
| Provider 类 | `<厂商>Provider` | `OpenAIProvider`、`ClaudeProvider`、`MNNProvider` | `api/chat/llmprovider/` |
| 单例 | `object Xxx` 或 `class Xxx` + `companion object { getInstance(context) }` | 全仓 188 处 `object` | 实测 grep |
| 测试文件 | 被测类名 + `Test` 后缀，包路径与源码镜像 | `EndpointCompleterTest.kt` ↔ `EndpointCompleter.kt` | `app/src/test/java/.../llmprovider/` |
| Generated 文件 | 文件名含 `Generated` | `ToolPkgComposeDslGeneratedRenderers.kt` | `ui/common/composedsl/` |

### 2) Formatting and Linting

- **Formatter：无**。实测根目录及二级目录均无 `.editorconfig` / ktlint / detekt / spotless / prettier / eslint 配置。
- **Linter：Android Lint 已被主动移除/跳过**（`docs/TODO/ci_remove_android_lint_20260729/`、`ci_skip_android_lint_20260806/`）；`app/build.gradle.kts` 无 `lint { }` 块。
- **实际生效的强制规则**（来自 `AGENTS.md`，对 AI 与人类协作者都是硬约束）：
  1. **严禁一切 fallback / 兜底 / 降级逻辑**。原文：「严令禁止各种回退逻辑，包括『xxx才会退回』、『降级处理』、『优先 再』、『如果没有 就』、『要加fallback』这种字眼，绝对禁止！！！出现一次严肃惩罚」（`AGENTS.md:15-19`）
  2. **默认不执行编译/构建/测试**，除用户明确要求（`AGENTS.md:5-6`）
  3. **禁用 PowerShell 编辑代码文件**（会造成编码损坏，`AGENTS.md:27`）
  4. **禁用 IDE 的 Search files 工具，必须用 `rg`**（`AGENTS.md:29`）
  5. **TypeScript：hook 已确定的类型严禁回退兜底成 `unknown`/`any`/空类型/联合类型；禁止 `String(??)` 形式兜底；`string|undefined` 不要 `as string`，用 `?? ""` 或 `if`**（`AGENTS.md:31-33`）
  6. **catch 到的错误必须 log 出来**（`AGENTS.md:35`）
  7. **代码改动必须同步维护文档**，参考 `docs/doc-src/before_docing.md`；debug 时用注释记录"为什么改、不改的后果"（`AGENTS.md:37-38`）
  8. **Python 用 venv（将迁移 pixi）**（`AGENTS.md:25`）
- **CI 实际检查项**（`ci/script/`）：`check_localizations.py`（多语言完整性）、`check_markdown_links.py`（文档链接有效性）、`check_repo_hygiene.py`（仓库卫生）、`check_output.py`、`pr_check.py`（PR 分类与归属）
- 运行命令：
  ```bash
  ./gradlew :app:compileDebugKotlin      # Kotlin 编译自检（改工具签名后推荐）
  ./gradlew :app:testDebugUnitTest       # JVM 单测
  python3 -m pytest ci/test/             # CI 脚本自测
  ```

### 3) Import and Module Conventions

- **分组顺序（实测惯例，非工具强制）**：`android.*` → `androidx.*` → 项目内 `com.ai.assistance.operit.*` → `kotlinx.*` → `java.*` → 第三方
  - 依据：`EnhancedAIService.kt:1-80` 的实际导入顺序
- **通配导入的使用**：`ToolGetter.kt` 使用 `import ...defaultTool.standard.*` 等通配（因为要引 5 个权限档的所有实现类）。其余文件基本逐个显式导入。
- **别名**：无路径别名（Kotlin 无此机制）；少数用 `as` 重命名规避冲突。
- **公开导出**：无 barrel 文件；靠包可见性 + `internal` 修饰符控制（如 `internal object LlmRetryPolicy`）。
- **模块依赖方向**：`:app` 依赖全部 8 个子模块，子模块之间无相互依赖（`app/build.gradle.kts` dependencies 首段）。

### 4) Error and Logging Conventions

- **错误策略按层**：
  - Provider 层：抛具体异常（`HttpStatusCodeException`、`TtsException`），由 `LlmRetryPolicy` 决定重试
  - 工具执行层：返回 `ToolResult(success = false, error = ...)` 而非抛异常，让 LLM 看到失败原因
  - UI 层：ViewModel 收集 Flow 异常转为 UI 状态
  - 全局：`util/GlobalExceptionHandler.kt` 捕获未处理异常 → `ui/error/CrashReportActivity`；`util/AnrMonitor.kt` 监控 ANR
  - 惯用法：`runCatching` 全仓 508 处
- **日志**：统一 `util/AppLogger.kt`（357 个文件使用），仅 3 个文件直接 `import android.util.Log`（属于遗留，可视为约定违反）
  - 标准调用：`AppLogger.e(TAG, "描述: ${e.message}", e)`，`TAG` 一般是类名常量
  - 计时日志：`core/chat/logMessageTiming` / `messageTimingNow`（对话性能埋点）
- **敏感数据脱敏**：
  - `util/HttpLogSanitizer.kt`：URL 只保留 scheme/host/port，路径段与 query 只输出数量而非内容
  - `util/MediaBase64Limiter.kt`、`ImageBitmapLimiter.kt`：限制大对象进日志
  - `CONTRIBUTING.md:22` 明确要求 Issue 不得提交 API Key / Token / Cookie

### 5) Testing Conventions

- **位置与命名**：`app/src/test/java/<与源码相同包路径>/<被测类>Test.kt`（129 个 JVM 单测）；`app/src/androidTest/`（67 个 kt/js，仪器测试）；`ci/test/test_*.py`（Python，pytest 风格）
- **框架**：JUnit 4（`org.junit.Assert.*` 静态断言，非 Kotlin `kotlin.test`）+ `kotlinx-coroutines-test`（`runTest`、`UnconfinedTestDispatcher`、`advanceUntilIdle`、`Dispatchers.setMain`）
- **Mock 策略**：Mockito + mockito-kotlin。**mockk 被注释掉未启用**（`app/build.gradle.kts` 有注释行）。
- **重要坑（仓库注释明示）**：JVM 单测必须显式加 `org.json:json:20240303`，因为 Android 的 `org.json` 在 JVM 上是抛 `Stub!` 异常的桩实现
- **覆盖倾向（实测）**：单测集中在**纯逻辑、无 Android Context 依赖**的类 —— `EndpointCompleter`（7 个测试文件）、`MediaLinkParser`（4 个）、`Codex*Parser/Policy`、`ChatConfigReadiness`、`ColdStreamCancellation`、`GeminiThinkingConfig`、`ChatMemoryWindowPlanner`。业务编排层（`EnhancedAIService`、`ChatViewModel`）几乎无单测覆盖 —— 这是 188 个单例 + 无 DI 的直接后果。
- **覆盖率目标**：[TODO] 未在任何配置或文档中声明

### 6) Evidence

- `AGENTS.md`（全文，硬约束来源）
- `docs/doc-src/dev-core/CONTRIBUTING.md`（协作流程、分支命名、检查清单）
- `app/src/main/java/com/ai/assistance/operit/util/{AppLogger,HttpLogSanitizer,GlobalExceptionHandler,AnrMonitor}.kt`
- `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/LlmRetryPolicy.kt`
- `app/build.gradle.kts`（测试依赖与注释）
- `ci/script/*.py`、`ci/test/*.py`
- 实测统计：`grep -rl AppLogger`(357)、`grep -c runCatching`(508)、`grep -c '@Composable'`(1351)、`grep -c '@Preview'`(2)

## Extended Sections

### 分支与提交规范（改代码前必读）

来自 `docs/doc-src/dev-core/CONTRIBUTING.md:97-108`：

- 上游 PR 默认目标分支是长期集成分支 **`dev`**，不是 `main`；`main` 只用于稳定发布与维护者同步
- 新建贡献分支**必须**使用前缀 + 简短描述，例如 `ci/skip-android-lint`
- **禁止用代理/模型/个人身份做前缀**（不能用 `codex/xxx`、`claude/xxx`）
- PR 要保持聚焦，不把格式化、重命名与功能改动混在一起（`CONTRIBUTING.md:52`）
- **不要提交**：`local.properties`、本地密钥、手动下载的模型和二进制依赖（`CONTRIBUTING.md:47`）

**对本 fork（`Black0Bag/minibile`）的含义**：这是私有定制 fork，如果不打算回贡上游，分支规范可自定；但如果将来要同步上游或提 PR，遵守上述规则能省掉大量返工。

### 已知约定违反（可清理项）

| 违反 | 位置 | 影响 |
|---|---|---|
| 直接用 `android.util.Log` 而非 `AppLogger` | 3 个文件 | 日志不受统一控制/脱敏 |
| 包名拼写错误 `accessbility`（应为 accessibility） | `core/tools/defaultTool/accessbility/` | 改名会牵动 `ToolGetter` 与所有导入，收益低风险高，**建议保留** |
| Compose `@Preview` 仅 2 个，对应 1351 个 `@Composable` | 全 UI 层 | 无法快速预览组件，UI 改动只能整机跑；这是 UI 定制时最痛的点 |
| 图片加载库 Coil 与 Glide 并存 | `app/build.gradle.kts` | 包体积与缓存重复 |
| 序列化方案 kotlinx.serialization / Gson / Moshi / HJSON 四套并存 | `app/build.gradle.kts` | 认知负担，新代码应统一用 kotlinx.serialization |
| `mockk` 依赖被注释未启用 | `app/build.gradle.kts` | 只能用 Mockito，mock final 类受限 |

### 给"UI 大改"的额外约定提示（compose-ui Skill 交叉检查）

依据 compose-ui Skill 最佳实践与本仓实测对照：

| 最佳实践 | 本仓现状 | 定制时建议 |
|---|---|---|
| 状态提升，Composable 无状态化 | 24 个 ViewModel + `collectAsState`，大体遵循 | 新组件继续 `value + onValueChange + modifier` 签名 |
| `modifier: Modifier = Modifier` 首个可选参数 | 大部分遵循 | 保持 |
| `MaterialTheme.colorScheme` 而非硬编码色值 | 有完整主题系统（`ui/theme/` + `ThemeSettingsCoreSections.kt` 2439 行） | 新 UI 必须走主题，否则深色模式/液态玻璃主题会破 |
| 每个 public Composable 配 `@Preview` | **仅 2 个 Preview** | 新写组件建议补 Preview，能大幅加速 UI 迭代 |
| `derivedStateOf` 优化高频状态 | 有自定义 Lazy 实现（`components/lazy/` 含 `LazyListState.kt` 491 行、`LazyLayoutItemAnimator.kt`） | 聊天列表已深度定制，改动前先读这些文件 |