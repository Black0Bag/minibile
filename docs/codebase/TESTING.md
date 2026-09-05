# Testing Patterns

> 实测生成，2026-09-03。

## Core Sections (Required)

### 1) Test Stack and Commands

- **主测试框架**：JUnit 4（`org.junit.Assert.*` 静态断言风格，非 `kotlin.test`）
- **断言/Mock 工具**：
  - Mockito core + mockito-kotlin 5.1.0（JVM），mockito-android（仪器测试）
  - `kotlinx-coroutines-test`：`runTest`、`UnconfinedTestDispatcher`、`advanceTimeBy`、`advanceUntilIdle`、`runCurrent`、`Dispatchers.setMain/resetMain`
  - `org.json:json:20240303`（**必需**：Android 的 `org.json` 在 JVM 上是抛 `Stub!` 的桩实现）
  - Espresso + Compose `ui-test-junit4`（仪器测试）
  - **mockk 已引入但被注释禁用**（`app/build.gradle.kts` 注释块）
- **命令**：

```bash
# JVM 单元测试（CI 实际执行的命令，来自 android-tests.yml:160）
./gradlew :app:testDebugUnitTest --stacktrace --no-daemon

# Android 仪器测试（需真机/模拟器）
./gradlew :app:connectedDebugAndroidTest

# CI Python 脚本自测
python3 -m pytest ci/test/

# 覆盖率：无（未配置 jacoco/kover）
```

### 2) Test Layout

- **放置模式**：独立测试目录，包路径镜像源码（不是 co-located）
  - `app/src/test/java/com/ai/assistance/operit/<同源码包路径>/<类名>Test.kt` —— 129 个文件
  - `app/src/androidTest/java/.../<类名>AndroidTest.kt` —— 仪器测试，用 `AndroidTest` 后缀区分
  - `app/src/androidTest/js/` —— **JS 脚本冒烟测试**（`browser_tool_smoke.js`、`browser_tool_suite_probe.js` + `lib/`）
  - `ci/test/test_<脚本名>.py` —— 6 个 Python 测试
- **命名约定**：
  - JVM：`<被测类>Test.kt`；同一个类拆多个测试文件时加语义后缀（如 `EndpointCompleterTrimTest`、`EndpointCompleterHashControlTest`、`EndpointCompleterAnthropicPathTest`）
  - 仪器：`<被测类>AndroidTest.kt`
- **Setup 文件**：无全局 setup/fixture 目录；每个测试类内用 `@Before`/`@After` + `Dispatchers.setMain`

### 3) Test Scope Matrix

| Scope | Covered? | Typical target | Notes |
|-------|----------|----------------|-------|
| Unit（JVM） | ✅ 129 文件 | 纯逻辑类：解析器、策略、序列化器、计算器、条件求值 | 分布见下表 |
| Integration（仪器） | ✅ 部分（约 42 个 kt） | 需要 Android Context 的类：`PathMapper`、`CrashRecoveryState`、序列化器、Markdown 渲染、`WorkflowExecutor`、`FileBindingService`、`ToolExecutionManager` | 需真机，CI 未运行（`android-tests.yml` 只跑 `testDebugUnitTest`） |
| E2E（用户流） | ❌ 无 | — | 唯一接近的是 `app/src/androidTest/js/browser_tool_smoke.js`（浏览器工具冒烟） |
| CI 脚本测试 | ✅ 6 文件 | `check_localizations`、`check_markdown_links`、`check_repo_hygiene`、`pr_check`、`toolpkg_sync`、`android_dependencies` | pytest，跑在 Python 3.12 |

**JVM 单测按包分布（实测 `find | uniq -c`）**：

| 包 | 测试数 | 说明 |
|---|---|---|
| `util` | 30 | 工具类（最多），含 `util/stream` 2 个 |
| `api/chat/llmprovider` | 27 | Provider 相关纯逻辑：`EndpointCompleter`×7、`MediaLinkParser`×4、Codex 策略、Gemini thinking、取消语义 |
| `data/model` | 17 | 数据模型序列化与转换 |
| `data/stats` | 7 | Token 统计归一化 |
| `core/tools/condition` | 7 | 条件表达式求值 |
| `core/tools/calculator` | 7 | 表达式计算器 |
| `core/chat/hooks` | 6 | Prompt/Summary Hook 注册与合并 |
| `ui/*` | 11（分散） | 导航、tokenstats、chat 组件、settings、markdown、composedsl 各 1-2 |
| `data/preferences` | 3 | 偏好迁移 |
| 其他 | 各 1-2 | `services/core`、`data/repository`、`data/mcp`、`data/backup`、`core/tools/javascript`、`core/tools/packTool`、`api/chat/library` |

### 4) Mocking and Isolation Strategy

- **主要 Mock 方式**：类级 mock（Mockito）+ 依赖注入到构造函数（可测类）。**没有网络层 mock 框架**（无 MockWebServer / WireMock）
- **隔离保证**：
  - 协程：`Dispatchers.setMain(UnconfinedTestDispatcher())` @Before，`resetMain()` @After
  - 静态状态：无自动重置机制 —— 188 个 `object` 单例的状态在测试间**不会自动清理**，这是潜在的测试污染源
- **常见失败模式**：
  1. **`org.json` Stub 异常** —— 忘记加 `org.json:json` 依赖，JVM 测试里一碰 `JSONObject` 就炸（仓库注释已明示）
  2. **Android Context 依赖** —— 因为业务类普遍用 `getInstance(context)`，想给编排层写单测就必须 mock Context，所以这类测试直接放到了 `androidTest`
  3. **单例状态残留** —— 前一个测试改了 `object` 的状态影响后一个（无强制隔离）

### 5) Coverage and Quality Signals

- **覆盖率工具**：❌ 无（实测 `grep jacoco|kover|coverage` 在 `app/build.gradle.kts`、根 `build.gradle.kts`、所有 workflow 中均无命中）
- **当前覆盖率**：[TODO] 无法获取，没有工具产出数据
- **粗略估算的覆盖倾向**：129 个 JVM 测试对应 1397 个源码 Kotlin 文件 → 文件级覆盖率约 9%。且集中在无副作用的纯函数区。
- **已知缺口**：
  1. **对话主链路零单测**：`EnhancedAIService.kt`（3209 行）、`ChatViewModel.kt`（3199 行）、`ChatServiceCore.kt` + 8 个 Delegate —— 无对应 `*Test.kt`
  2. **工具执行层几乎零单测**：179 个内置工具中，只有 `calculator`、`condition` 有测试；`StandardFileSystemTools.kt`（4872 行）无测试
  3. **UI 层几乎零测试**：1351 个 `@Composable` 只有 11 个分散的 UI 测试文件，且 **`@Preview` 仅 2 个** → 组件无法可视化验证
  4. **仪器测试不进 CI**：`android-tests.yml` 只跑 `testDebugUnitTest`，`connectedAndroidTest` 需要设备，因此 42 个仪器测试实际靠人工/本地执行
  5. **无 flaky 测试记录**：[TODO] 无历史数据可查
- **CI 上的质量信号（实际起作用的）**：
  - `check_localizations.py`：多语言键完整性（8 个语言变体不能漏键）
  - `check_markdown_links.py`：文档链接有效性
  - `check_repo_hygiene.py`：仓库卫生（应包含"不该提交的文件"检查）
  - `pr_check.py`：PR 归属与作用域分类
  - `test_toolpkg_sync.py`：ToolPkg 同步脚本正确性
  - Android Lint 已被**主动移除**（两个 TODO 目录记录了这个决定）

### 6) Evidence

- `app/build.gradle.kts`（测试依赖块 + `org.json` 注释说明）
- `.github/workflows/android-tests.yml:160`（`./gradlew :app:testDebugUnitTest --stacktrace --no-daemon`）
- `.github/workflows/android-tests.yml:69-71,121`（Python 3.12 + `prepare_android_dependencies.py`）
- `app/src/test/java/com/ai/assistance/operit/api/chat/llmprovider/`（27 个代表性单测）
- `app/src/androidTest/java/.../util/PathMapper*AndroidTest.kt`（仪器测试范例，一个类拆 4 个文件）
- `app/src/androidTest/js/browser_tool_smoke.js`（唯一的端到端风格冒烟）
- `ci/test/*.py`（6 个 pytest 文件）
- 实测统计：JVM 129 / androidTest 67（kt+js）/ 覆盖率工具 0

## Extended Sections

### 给"大改造"的测试策略建议

现状是**测试网薄**（文件级覆盖 ~9%，主链路零覆盖）。要在这个基础上做大规模定制，建议：

1. **改动前先给要动的模块补测试**，而不是改完再补 —— 尤其是 `EnhancedAIService`、`ChatViewModel` 这类无测试的巨类，改完无法验证是否回归
2. **优先补可测的边界**：纯逻辑函数抽出来单测（沿用现有 `EndpointCompleter` 那种模式：小类、无 Context、拆多个测试文件）
3. **UI 改动配 `@Preview`**：现在只有 2 个 Preview 是 UI 迭代最大的效率瓶颈。新写/重写组件时补 Preview，能把"改一行等一次完整构建安装"变成"IDE 里直接看"
4. **把 `connectedAndroidTest` 纳入本地流程**：42 个仪器测试是现成资产，CI 跑不了但本地能跑，改数据层/路径映射/序列化时值得跑一遍
5. **考虑引入覆盖率工具**：Kover（Kotlin 官方，比 jacoco 更适合 Kotlin）能给出改动区域的覆盖基线。但这属于工程改造，需要先确认是否在本次定制范围内 —— `[ASK USER]`
6. **单例状态污染防护**：如果新增可测类，避免 `object` 全局状态；用构造注入让测试能替换依赖