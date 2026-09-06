# Architecture

> 实测生成，2026-09-03。所有结论均有文件路径支撑。

## Core Sections (Required)

### 1) Architectural Style

- **主风格**：分层 + feature 模块化 + **插件化运行时**三者叠加
  - 分层：`ui / api / core / data / util` 五层（`STRUCTURE.md` 模块边界表）
  - feature 模块化：`ui/features/<feature>/{screens,components,viewmodel}`，20 个 feature
  - 插件化运行时：ToolPkg / MCP / Skill 三条扩展通道可在**不改宿主代码**的前提下注入工具、界面、模型提供方、Hook
- **判定依据**：
  - 目录结构直接体现分层（实测 10 个顶级包各自职责清晰，无环状依赖迹象）
  - `AppRouteCatalog.build()` 把宿主路由与 `packageManager.getToolPkgUiRoutes()` 动态路由合并 → 界面层可被插件扩展
  - `plugins/toolpkg/` 11 个 Hook 桥（Prompt/Summary/ChatView/Input/Message/ToolLifecycle）→ 对话链路可被插件劫持
  - `ToolPkgAiProviderRegistry.kt` + `ToolPkgJsAiProviderService.kt` → 连 LLM provider 都可由插件提供
- **主要约束（塑造整个设计的三条）**：
  1. **单机 Android 运行，无自有后端**：所有状态落本地（Room + ObjectBox + DataStore），LLM 请求由设备直连用户配置的服务商
  2. **权限分级适配**：同一个工具在 STANDARD / ACCESSIBILITY / DEBUGGER(Shizuku) / ADMIN / ROOT 五档权限下有不同实现，运行时由 `ToolGetter` 按用户当前授权级别选择
  3. **AGENTS.md 硬约束：禁止 fallback / 兜底 / 降级逻辑**（`AGENTS.md:15-19`，明示"出现一次严肃惩罚"）—— 这直接影响写代码的风格，遇到错误要暴露而非兜底

### 2) System Flow

```text
用户输入(UI/语音/悬浮窗/HTTP/Intent/Tasker/Workflow)
  → ChatViewModel / ChatServiceCore(+8 个 Delegate)
  → EnhancedAIService（对话编排中枢，3209 行）
      ├─ InputProcessor / FileBindingService / ReferenceManager（上下文装配）
      ├─ PromptHookRegistry ← ToolPkgPromptHookBridge（插件改 prompt）
      ├─ SystemToolPromptsInternal（5992 行工具说明书 → 告诉 LLM 有哪些工具）
      └─ MultiServiceManager → AIServiceFactory → 具体 Provider（26 个）
  → LLM 流式响应
  → util/stream 流式管道（StreamXmlPlugin 增量解析 tool call）
  → AIToolHandler.执行 → ToolGetter 按权限选实现 → 179 个内置工具 / JS 脚本包 / MCP / Skill
  → 工具结果回注对话 → 下一轮
  → ChatHistoryManager 落 Room；MemoryRepository 落 ObjectBox + hnswlib 向量索引
```

**关键 6 步（带证据）**：

1. **入口收敛**：所有触发路径最终都进 `services/core/` 的 8 个 Delegate（`MessageProcessingDelegate`、`MessageCoordinationDelegate`、`ChatHistoryDelegate`、`AttachmentDelegate`、`ApiConfigDelegate`、`TokenStatisticsDelegate`、`CurrentChatWindowController`、`ChatDisplayWindowPaging`）→ `ChatServiceCore.kt`
2. **编排**：`api/chat/EnhancedAIService.kt` 负责整轮生命周期。`enhance/` 下 8 个服务分工：输入处理、文件绑定、引用管理、轮次管理、标记管理、多服务管理、工具执行管理、对话服务
3. **Prompt 装配**：`core/config/SystemToolPromptsInternal.kt`（5992 行，全仓最大 Kotlin 文件）生成工具 schema；`core/chat/hooks/PromptHookRegistry` 允许插件在发送前改写 prompt turns
4. **Provider 抽象**：`AIService` 接口 + `AIServiceFactory` 工厂 + 装饰器链（`RateLimitedAIService` → `TokenTrackingAIService`）。26 个 provider 实现（OpenAI / OpenAI Responses / Claude / Gemini / Deepseek / Qwen / Doubao / Kimi / Mistral / xAI / OpenRouter / Ollama / MNN / Llama / Codex / OpenCode / Nous / Novita / 4Router / Mimo / Nvidia 等）
5. **流式解析**：`util/stream/` 自研流管道 —— `StreamXmlPlugin` + `splitBy` 在 token 到达时就增量识别 `<tool>` 标记，`TextStreamRevisionTracker` 处理修订。这是"边生成边执行工具"的基础
6. **持久化双轨**：Room（`AppDatabase` v21，5 实体：Chat/Message/MessageVariant/TokenUsageRecord/TokenStatsModel）+ ObjectBox（3 实体：Memory/DocumentChunk/MemoryAutoSaveCandidate，按 profileId 多 store）

### 3) Layer/Module Responsibilities

| Layer or module | Owns | Must not own | Evidence |
|-----------------|------|--------------|----------|
| `ui/features/*` | Compose 屏幕、ViewModel、UI 状态 | LLM 协议、工具执行、DB 迁移 | `ui/features/chat/viewmodel/ChatViewModel.kt` |
| `services/core/*` | Service 生命周期内的对话委托、分页、Token 统计 | Compose 渲染 | `services/ChatServiceCore.kt` + 8 Delegate |
| `api/chat` | 对话编排、Provider 选择、流式协议适配 | 界面状态、DB schema | `api/chat/EnhancedAIService.kt` |
| `api/chat/llmprovider` | 单个厂商 HTTP/流式协议细节、限流、Token 计量 | 对话编排、工具执行 | 26 个 `*Provider.kt` |
| `api/{speech,voice}` | STT（4 provider）/ TTS（12 provider）适配 | 对话逻辑 | `SpeechServiceFactory.kt`、`VoiceServiceFactory.kt` |
| `core/tools` | 工具注册、权限分级选择、执行调度 | UI、Provider 细节 | `AIToolHandler.kt`、`ToolRegistration.kt` |
| `core/tools/javascript` | QuickJS 沙箱、Java Bridge、JsTools 封装 | 内置工具实现 | `JsEngine.kt`（2627 行，注释明示 QuickJS） |
| `core/tools/packTool` | ToolPkg 加载/解析/Compose DSL/Hook 分发 | 宿主 UI 逻辑 | `PackageManager.kt`（3790 行） |
| `core/workflow` | 工作流执行、调度、WorkManager Worker | 工具实现 | `WorkflowExecutor.kt`、`WorkflowScheduler.kt` |
| `data/preferences` | 38 个 DataStore 偏好管理器 | 业务编排 | `data/preferences/` 实测 38 文件 |
| `data/repository` | 8 个仓库（聊天历史、记忆、头像、工作流、表情、UI 层级） | Compose、Provider | `ChatHistoryManager.kt`（2840 行）、`MemoryRepository.kt`（2814 行） |
| `integrations` | 对外协议边界（HTTP/A2A/Intent/Tasker） | 内部工具实现 | `integrations/http/WebChatHttpBridge.kt`（2884 行） |
| `util/stream` | 流式文本管道与 KMP 匹配 | 业务语义 | `util/stream/` 12 文件 + 2 README |
| `util/vector` | hnswlib 向量索引管理 | 记忆业务规则 | `util/vector/VectorIndexManager.kt` |

### 4) Reused Patterns

| Pattern | Where found | Why it exists |
|---------|-------------|---------------|
| **Object 单例** | 全仓 188 处 `object Xxx` | Android 无 DI 框架（无 Hilt/Koin），靠 object + `getInstance(context)` 手工单例 |
| **策略 + 工厂（权限分级）** | `defaultTool/ToolGetter.kt` | 同一工具在 5 档权限下换实现；调用方无需知道当前权限 |
| **装饰器链（Provider）** | `RateLimitedAIService` / `TokenTrackingAIService` 包裹 `AIService` | 限流与计量横切关注点，不污染 26 个 provider |
| **注册表（Registry）** | `ToolRegistration`、`ScreenRouteRegistry`、`PromptHookRegistry`、`SummaryHookRegistry`、`ToolPkgAiProviderRegistry`、`MessageProcessingPluginRegistry`、`RateLimiterRegistry` | 插件化的核心机制：宿主提供注册表，扩展往里注册 |
| **Delegate 拆分巨类** | `services/core/` 8 个 Delegate 服务于 `ChatServiceCore` | 对话状态过于复杂，用委托拆职责 |
| **Repository** | `data/repository/` 8 个 | 隔离 Room/ObjectBox 与上层 |
| **多 Store 按 Profile 隔离** | `data/db/ObjectBox.kt` `ConcurrentHashMap<String, BoxStore>` | 支持"多记忆空间"，每个 profile 独立 DB 目录 |
| **Bridge（跨语言）** | `JsJavaBridge.kt`、`avator/dragonbones/cpp/JniBridge.cpp`、`plugins/toolpkg/*Bridge.kt` | Kotlin ↔ JS ↔ C++ 三向互操作 |
| **Sealed class 状态建模** | 全仓 60 处 `sealed class/interface`（如 `Screen`） | 穷尽式路由与状态 |
| **`runCatching` 错误处理** | 全仓 508 处 | Kotlin 惯用法，配合 `AppLogger` 记录 |

### 5) Known Architectural Risks

1. **上帝文件风险**：`SystemToolPromptsInternal.kt` 5992 行、`StandardFileSystemTools.kt` 4872 行、`PackageManager.kt` 3790 行、`EnhancedAIService.kt` 3209 行、`ChatViewModel.kt` 3199 行。改动这些文件的合并冲突和回归风险极高，且单元测试难覆盖。
2. **工具契约七处同步**：新增/改一个工具参数要同步改 7 个地方（prompt schema / 注册 / Kotlin 实现 ×5 权限档 / JsTools / TS 类型 / examples / assets 产物 / 文档）。`DEFAULT_TOOLS_ARCH.md` 专门写了 checklist 说明这个坑，但没有自动化校验。
3. **无依赖注入 + 188 个单例**：`getInstance(context)` 模式让单元测试必须 mock Android Context，这解释了为什么 129 个单测集中在纯逻辑类（EndpointCompleter、MediaLinkParser 等）而非业务编排层。
4. **双数据库并存**：Room（关系型，v21 + 22 迁移）与 ObjectBox（记忆图谱）职责边界靠约定维持，没有统一事务。跨库一致性（例如删除对话时清理关联记忆）依赖应用层保证。
5. **插件权限面过大**：ToolPkg 能注入 Prompt Hook、替换 AI Provider、注册工具、绘制界面。恶意/低质插件可以改写发给模型的 prompt 或截获对话内容。市场来源可信度是安全边界的关键。
6. **Application 初始化重**：`initializeMainApplicationLocked()` 串联 20+ 个子系统初始化（含 PDFBox、分词器、图片池、Skill 池、WorkManager、工作流调度）。冷启动时间与 ANR 风险集中在这里（仓库有 `util/AnrMonitor.kt` 佐证他们意识到了这点）。
7. **本地化面 7497 × 8 语言**：`strings.xml` 是 churn 榜第一（90 天内 78 次改动）。任何功能改动都牵动 8 个语言文件，`ci/script/check_localizations.py` 是唯一防线。

### 6) Evidence

- `app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt`（启动编排）
- `app/src/main/java/com/ai/assistance/operit/api/chat/EnhancedAIService.kt`（对话中枢）
- `app/src/main/java/com/ai/assistance/operit/core/tools/{AIToolHandler,ToolRegistration}.kt`（工具系统）
- `app/src/main/java/com/ai/assistance/operit/core/tools/defaultTool/ToolGetter.kt`（权限策略）
- `app/src/main/java/com/ai/assistance/operit/data/db/{AppDatabase,ObjectBox}.kt`（双持久化）
- `app/src/main/java/com/ai/assistance/operit/ui/main/navigation/AppRouteCatalog.kt`（可扩展路由）
- `app/src/main/java/com/ai/assistance/operit/plugins/toolpkg/`（11 个 Hook 桥）
- `docs/doc-src/architecture/DEFAULT_TOOLS_ARCH.md`、`RENDERER_ARCH.md`
- `docs/codebase/.codebase-scan.txt`（HIGH-CHURN FILES 段）

## Extended Sections

### 启动顺序细节（改初始化必读）

`OperitApplication.onCreate()` → `initAndroidPermissionPreferences(applicationContext)` → `initializeMainApplication()`（`synchronized` 保护 + 幂等标记）。

同步阶段（阻塞主线程）：用户偏好 → 语言 → ActivityLifecycleManager → AIMessageManager → PluginRegistry 内置插件 → PDFBox → LanguageFactory。

异步阶段（协程）：Token 仓库、内置表情、分词器、图片/媒体/Skill 资源池预热、AIToolHandler、WorkflowScheduler、Room 备份计划、UIHierarchyManager 服务绑定。

**风险点**：同步阶段任何一步抛异常都会导致启动崩溃，因此有 `util/GlobalExceptionHandler.kt` + `ui/error/CrashReportActivity` + `ui/recovery/DataRecoveryActivity` 三重兜底 UI（注意：这里的"兜底"是崩溃恢复 UI，不违反 AGENTS.md 禁止的业务逻辑 fallback）。

### 五档权限模型（改工具必读）

`core/tools/system/AndroidPermissionLevel.kt` 定义等级，`ToolGetter` 按 `androidPermissionPreferences.getPreferredPermissionLevel()` 分派：

| 级别 | 通道 | 实现目录 | 能力 |
|---|---|---|---|
| STANDARD | 普通 Android API | `defaultTool/standard/`（22 文件） | SAF 文件、HTTP、Intent、基础系统查询 |
| ACCESSIBILITY | 无障碍服务 | `defaultTool/accessbility/`（4 文件） | 读屏、模拟点击输入 |
| DEBUGGER | Shizuku（ADB 级） | `defaultTool/debugger/`（4 文件） | pm/am 命令、更强文件访问 |
| ADMIN | 设备管理员 | `defaultTool/admin/`（4 文件） | 部分系统设置 |
| ROOT | libsu | `defaultTool/root/`（4 文件） | 完全系统访问、虚拟显示 |

注意 `ToolGetter.getShellToolExecutor()` **不分级**，恒返回 `StandardShellToolExecutor`（内部再判断可用通道）。

### 三条扩展通道对比（定制时选路的关键）

| 通道 | 载体 | 能力上限 | 改宿主代码？ | 入口 |
|---|---|---|---|---|
| **Sandbox Package（脚本包）** | `.js` / `.toolpkg`，QuickJS 执行 | 注册工具、Compose DSL 界面、AI Provider、6 类 Hook、桌面组件、导航项 | ❌ 不用 | `core/tools/packTool/PackageManager.kt` |
| **MCP** | 本地进程（npx/uvx/node）或远程 endpoint | 提供 MCP 协议工具 | ❌ 不用 | `data/mcp/MCPLocalServer.kt` |
| **Skill** | 含 `SKILL.md` 的目录 | 注入流程文档/提示词 | ❌ 不用 | `data/skill/SkillRepository.kt` |
| **改宿主** | Kotlin 源码 | 无限制 | ✅ 需要 | 全仓 |

**对个性化定制的意义**：如果需求能用 ToolPkg 表达（新工具、新界面页、改 prompt、换 provider），走插件通道可以完全避开 444k 行 Kotlin 的改动风险与合并冲突；只有需要改动核心对话链路、数据模型、权限体系时才必须动宿主。