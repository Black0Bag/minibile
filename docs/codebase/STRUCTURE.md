# Codebase Structure

> 实测生成，2026-09-03。仓库根：`/data/data/com.ai.assistance.operit/files/workspace/minibile/minibile`

## Core Sections (Required)

### 1) Top-Level Map

| Path | Purpose | Evidence |
|------|---------|----------|
| `app/` | 主 Android 应用（Gradle `:app`）。全部业务逻辑、UI、工具系统、数据层 | `Repo_Arch_Basic.md:30`；`settings.gradle.kts:480` |
| `avator/dragonbones/` | DragonBones 骨骼动画库（C++/OpenGL/JNI + Compose 封装） | `avator/dragonbones/CMakeLists.txt`、`cpp/JniBridge.cpp` |
| `avator/mmd/` | MMD 模型运行时与预览（Bullet3、上游 Saba Viewer 映射） | `avator/mmd/UPSTREAM_SABA_VIEWER_MAPPING.md` |
| `avator/fbx/` | FBX 原生解析（ufbx C 库，CMake FetchContent） | `avator/fbx/CMakeLists.txt` |
| `llm/mnn/` | 阿里 MNN 本地推理集成 | `Repo_Arch_Basic.md:74` |
| `llm/llama/` | llama.cpp GGUF 推理集成 | `Repo_Arch_Basic.md:66` |
| `quickjs/` | QuickJS JNI 模块（C 源码 + Kotlin 封装 + Host Bridge） | `quickjs/src/main`；`Repo_Arch_Basic.md:78` |
| `showerclient/` | Shower 虚拟显示客户端库（Binder / 截图 / 事件注入） | `Repo_Arch_Basic.md:84` |
| `terminal/` | **Git 子模块** OperitTerminalCore，内含 62.6MB Ubuntu 24.04 rootfs | `.gitmodules`；scan.txt 最大文件榜首 |
| `examples/` | 125 个条目的脚本包/ToolPkg 示例（`.ts` 源 + `.js` 产物成对） | `examples/` 目录实测 |
| `web-chat/` | React + Vite Web Chat 前端，构建产物同步到 `app/src/main/assets/web-chat` | `web-chat/vite.config.ts` |
| `tools/` | 13 个开发辅助工具组（adb、compose_dsl、mcp_bridge、hotbuild、shower 等） | `tools/` 目录实测 |
| `ci/` | CI 与本地自动化 Python 脚本 + 对应单测 | `ci/script/`、`ci/test/` |
| `docs/` | 41 篇 doc-src 正式文档 + 97 个 TODO 任务包 + README 资源 | `docs/doc-src/`、`docs/TODO/` |
| `cmake/` | `operit_git_source.cmake` —— 统一上游源码 FetchContent 策略 | `cmake/operit_git_source.cmake` |
| `AGENTS.md` | **对 AI Agent 的硬性约束**（禁止 fallback/兜底、禁用 PowerShell 改代码、禁用 Search files 用 rg） | `AGENTS.md` 全文 |
| `Repo_Arch_Basic.md` | 官方根目录布局说明（与实测一致） | 本文交叉验证 |

### 2) Entry Points

- **Application**：`app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt`（682 行）
  - `onCreate()` → `initAndroidPermissionPreferences` → `initializeMainApplication()`（加锁，幂等）
  - 初始化顺序（实测行号）：用户偏好 178 → Token 仓库 185 → 语言 194 → Activity 生命周期 198 → 消息管理 202 → 内置插件 203 → 内置表情 233 → PDFBox 272 → 分词 282 → Waifu 287 → 图片/媒体/Skill 池 335/339/342 → 工具处理器 358 → 工作流调度 366 → Room 备份 372 → UI 层级服务绑定 388
- **主 Activity**：`ui/main/MainActivity.kt` → `ui/main/OperitApp.kt`（Compose 根）
- **次级入口（Manifest 声明，实测 31 个组件）**：
  - Activity：`OperitAssistActivity`（系统助理）、`CrashReportActivity`、`DataRecoveryActivity`、`ScreenCaptureActivity`、`WebSessionPermissionRequestActivity`、`UserscriptImportPickerActivity`、`ActivityConfigAIAgentAction`（Tasker）、`ToolPkgDesktopWidgetConfigActivity`
  - Service：`FloatingChatService`（悬浮窗）、`AIForegroundService`（对话前台服务）、`ScreenCaptureService`、`OperitNotificationListenerService`、`UIDebuggerService`、`OperitVoiceInteractionService` + `SessionService`
  - Receiver：`ScriptExecutionReceiver`、`ToolPkgDebugInstallReceiver`、`PackageDebugRefreshReceiver`、`ToolPkgComposeDslDebugDumpReceiver`、`ExternalChatReceiver`、`ShowerBinderReceiver`、`VoiceAssistantWidgetReceiver`、`ToolPkgDesktopWidgetReceiver`、`WorkflowTaskerReceiver`、`WorkflowBootReceiver`
  - ContentProvider：`WorkspaceDocumentsProvider`、`MemoryDocumentsProvider`、`OperitDataDocumentsProvider`（对外暴露 SAF 文档树）
- **入口选择方式**：Manifest intent-filter + Compose 路由（`ScreenRouteRegistry` + `AppRouteCatalog`，见下）

### 3) Module Boundaries

`app/src/main/java/com/ai/assistance/operit/` 下 10 个顶级包，Kotlin 文件数实测：

| Boundary | 文件数 | What belongs here | What must not be here |
|----------|-------|-------------------|------------------------|
| `ui/` | 521 | Compose 界面、ViewModel、主题、导航、悬浮窗 UI | LLM 协议细节、工具执行实现、DB schema |
| `core/` | 216 | 工具系统、JS 引擎、ToolPkg、工作流执行、Prompt 配置、Application 初始化、APK 子包 | 界面渲染、具体 provider HTTP 细节 |
| `data/` | 169 | Room/ObjectBox 实体与 DAO、38 个 Preferences、Repository、MCP/Skill 仓库、备份/导出 | Compose、工具执行逻辑 |
| `api/` | 84 | LLM provider（26 个）、语音 STT/TTS provider、EnhancedAIService 对话编排 | UI 状态、DB 迁移 |
| `util/` | 71 | 通用工具：流式管道、向量索引、日志、文档转换、OCR、Markdown | 业务规则、界面 |
| `services/` | 23 | Android Service 与其委托（对话核心、悬浮窗、通知监听、助理） | Compose 屏幕、DB schema |
| `integrations/` | 19 | 对外协议：HTTP/WebChat、A2A、Intent、Tasker、外部对话 | 内部工具实现 |
| `plugins/` | 16 | ToolPkg Hook 桥（Prompt/Summary/ChatView/Input/Message/Tool 生命周期） | 内置工具实现 |
| `widget/` | 7 | Glance 桌面小组件 | 主界面逻辑 |
| `provider/` | 3 | DocumentsProvider（工作区/记忆/应用数据） | 业务逻辑 |

**工具系统内部分层（`core/tools/`）**：

| 子包 | 职责 | 文件数/说明 |
|---|---|---|
| `AIToolHandler.kt` | 工具注册表 + 执行调度 + 权限拦截 | 核心入口 |
| `ToolRegistration.kt` | **179 个内置工具的注册点**（2736 行） | 加工具必改 |
| `defaultTool/standard/` | 标准权限实现（22 文件） | 默认实现 |
| `defaultTool/{accessbility,debugger,admin,root}/` | 按权限级别的替代实现（各 4 文件） | `ToolGetter.kt` 依 `AndroidPermissionLevel` 选择 |
| `defaultTool/websession/` | 浏览器会话与 Web 自动化（25 文件） | browser_* 工具族 |
| `javascript/` | QuickJS 引擎 + Java Bridge + JsTools 封装（21 文件） | 脚本包运行时 |
| `packTool/` | ToolPkg 加载/解析/Compose DSL/Manifest（14 文件） | 插件运行时 |
| `mcp/`、`skill/` | MCP 客户端与 Skill 装载 | 扩展通道 |
| `agent/` | Shower / UI 子代理 | 屏幕自动化 |
| `system/` | 权限授权（Shizuku/Root）、Shell 执行、屏幕捕获、终端管理 | 底层通道 |
| `climode/`、`condition/`、`calculator/` | CLI 工具暴露模式、条件判断、表达式计算 | 辅助 |

### 4) Naming and Organization Rules

- **文件命名**：100% PascalCase（实测 1136/1136 个 `.kt` 文件首字母大写），一个文件一个主类
- **目录组织**：混合模式 —— `ui/features/<feature>/{screens,components,viewmodel}` 是 feature-first；`core/data/api/util` 是 layer-first
- **包名**：全小写单词（`llmprovider`、`defaultTool` 例外为 camelCase，`accessbility` 是既存拼写错误，不要"顺手修"）
- **工具命名**：内置工具名统一 snake_case（`read_file`、`browser_navigate`、`create_memory`）
- **导入约定**：全限定包名导入，无路径别名（Kotlin 无此机制）
- **Compose 约定**：`@Composable` 函数 PascalCase；`ui/common/` 放跨 feature 共享组件
- **资源**：`strings.xml` 7497 条字符串，8 个语言变体目录（`values-{en,es,id,ko,ms,pt-rBR,ro}` + `values-night`）

### 5) Evidence

- `docs/codebase/.codebase-scan.txt`（DIRECTORY TREE / CODE METRICS 段）
- `Repo_Arch_Basic.md`（官方布局说明，已交叉验证一致）
- `app/src/main/AndroidManifest.xml`（31 个组件声明）
- `app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt`
- `app/src/main/java/com/ai/assistance/operit/ui/main/navigation/AppRouteCatalog.kt`
- `app/src/main/java/com/ai/assistance/operit/core/tools/defaultTool/ToolGetter.kt`
- 终端实测：`find ... -name '*.kt' | wc -l` 各包计数

## Extended Sections

### 路由与导航（改 UI 必读）

Compose 路由是**注册表驱动**而非 NavHost 字符串路由：

1. `ui/main/screens/OperitScreens.kt`（1539 行）定义 `Screen` sealed 层级 —— 29 个 object/data class 路由（AiChat、MemoryBase、Packages、Market*、Toolbox、Settings、Workflow、ToolPkgComposeDsl 等）
2. `ScreenRouteRegistry` 提供 `hostRouteSpecs` / `mainSidebarEntries` / `toolboxEntries`
3. `AppRouteCatalog.build(context)` 把宿主路由与 **ToolPkg 动态路由**合并，按 `surface.ordinal → order → title` 排序
4. `ToolPkg` 可注入两类导航面：`TOOLBOX`、`MAIN_SIDEBAR_PLUGINS`；其界面由 `RouteRuntime.TOOLPKG_COMPOSE_DSL` 走 Compose DSL 解释执行
5. 布局分叉：`layout/PhoneLayout.kt` 与 `layout/TabletLayout.kt`

**含义（给定制改造）**：新增一个功能页有两条路，改宿主代码（加 `Screen` + 注册）或做成 ToolPkg（不改宿主，用 Compose DSL）。

### 生成物 vs 源码边界（绝不能改错的地方）

| 类型 | 路径 | 规则 |
|---|---|---|
| 脚本包源码 | `examples/*.ts` | ✅ 改这里 |
| 脚本包产物 | `examples/*.js` | ❌ 不手改，由构建生成 |
| App 运行时包 | `app/src/main/assets/packages/*.js` | ❌ 不手改，由 `sync_example_packages.py` 同步 |
| Web Chat 源码 | `web-chat/src/` | ✅ 改这里 |
| Web Chat 产物 | `app/src/main/assets/web-chat/` | ❌ 由 `npm run build:webchat` 生成 |
| Compose DSL 渲染器 | `ui/common/composedsl/ToolPkgComposeDslGeneratedRenderers.kt`（3360 行） | ❌ 文件名含 Generated，由 `tools/compose_dsl` 生成 |
| 内置 APK 资源 | `app/src/main/assets/{accessibility,desktop,shizuku}.apk` | ❌ 外部构建产物 |

依据：`docs/doc-src/architecture/DEFAULT_TOOLS_ARCH.md:111-137`