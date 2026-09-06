# 功能全景图

> 实测生成 2026-09-03。本文按"用户能看到什么"和"AI 能做什么"两条线索梳理全部功能，用于后续定制需求讨论。
> 所有条目均来自源码实测，非 README 转述。

---

## 一、一句话定位

**一个装在安卓手机上的 AI 助手，能替你操作这台手机、跑完整 Linux 系统、控制浏览器、写代码、记住事情，并且能靠插件无限扩展。**

它不是聊天软件，是"带手脚的 AI"。聊天只是它的操作台。

---

## 二、界面结构（用户能点到的地方）

### 2.1 主侧边栏（9 个入口，分 3 组）

| 组 | 入口 | 干什么 |
|---|---|---|
| **AI** | AI 对话 | 主界面，跟 AI 聊天并让它干活 |
| | 助手配置 | 配虚拟形象、语音、开场白 |
| | 记忆库 | 管理 AI 的长期记忆 |
| **工具** | 扩展中心 | 装/管插件、MCP、Skill、逛市场 |
| | Shizuku 命令 | 高权限命令执行 |
| | 工作流 | 可视化流程编排 |
| **系统** | 设置 | 22 个设置子页面 |
| | 帮助 | 使用说明 |
| | 关于 | 版本信息 |

依据：`ScreenRouteRegistry.kt:110-183`，`NavigationSurface.MAIN_SIDEBAR_{AI,TOOLS,SYSTEM}`

### 2.2 工具箱（18 个独立工具）

| 工具 | 作用 |
|---|---|
| 工具测试中心 | 手动调用 179 个内置工具，调试用 |
| 文件管理器 | 浏览/编辑/传输文件 |
| 文字转语音 | TTS 测试与配置 |
| 语音识别 | STT 测试与配置 |
| 权限管理器 | 应用权限总览 |
| 用户协议 | 协议查看 |
| 默认助理引导 | 设为系统默认语音助手 |
| 终端 | 完整 Ubuntu 24.04 命令行 |
| UI 调试器 | 查看屏幕控件树 |
| FFmpeg 工具箱 | 音视频转码 |
| Shell 执行器 | 直接跑 shell 命令 |
| 日志查看器 | Logcat |
| SQL 查看器 | 看 SQLite 数据库 |
| Token 配置 | Token 用量与计费 |
| 进程限制解除 | 提升后台进程上限 |
| HTML 打包器 | 把网页打包成独立文件 |
| AutoGLM 一键 | 一键屏幕自动化 |
| AutoGLM 工具 | 屏幕自动化高级配置 |

依据：`ScreenRouteRegistry.kt:184-420`，`NavigationSurface.TOOLBOX`

### 2.3 设置（22 个子页面）

| 子页面 | 管什么 |
|---|---|
| 模型配置 | 连哪家大模型、Key、参数、密钥池 |
| 功能配置 | 11 种任务分别用哪个模型（见 3.2） |
| 模型提示词设置 | 改系统提示词与工具说明 |
| 上下文总结设置 | 什么时候自动总结对话 |
| 主题设置（+4 个 section） | 配色、背景、字体、头像、气泡 |
| 全局显示设置 | 显示密度、布局 |
| 布局调整设置 | 界面元素位置 |
| 语言设置 | 8 种界面语言 |
| 语音服务设置 | TTS/STT 引擎选择与参数 |
| 聊天历史设置 | 历史管理、迁移、清理 |
| 聊天备份设置 | 自动备份策略 |
| 工具权限设置 | 每个工具的授权级别 |
| 用户偏好设置 | 个人信息、AI 风格 |
| 角色卡生成 | AI 辅助生成角色卡 |
| 自定义表情管理 | 表情包 |
| Waifu 模式设置 | 虚拟形象互动模式 |
| 标签市场 | 提示词标签 |
| GitHub 账号 | OAuth 登录、发布 Artifact |
| 外部 HTTP 对话设置 | 局域网 API 开关与 Token |
| MNN 模型下载 | 本地模型下载 |

依据：`ui/features/settings/screens/`（22 文件）+ `sections/`（8 文件）

### 2.4 扩展中心（16 个页面）

统一市场（搜索/分类/详情/作者/管理）、包管理器、插件标签页、MCP 配置、Skill 配置、Artifact 发布、仓库市场发布、快速插件创建器。

依据：`ui/features/packages/screens/`（16 文件 + 3 子目录）

---

## 三、AI 能力（核心）

### 3.1 对话本身

| 能力 | 说明 |
|---|---|
| 多模态输入 | 图片、音频、视频、文档、工作区文件 |
| 消息分支 | 同一条消息可以有多个回答变体（`MessageVariantEntity`） |
| 流式输出 | 边生成边显示，边生成边执行工具（`util/stream` 增量解析） |
| 自动总结 | 上下文超阈值自动压缩 |
| 并行对话 | 多个对话同时进行 |
| 历史分组与迁移 | 对话归档整理 |
| 对话锁定 | 防误删 |
| 导出 | Markdown / HTML / 纯文本三种格式（`data/exporter/`） |
| 导出成独立 APP | 把对话打包成 Android APK 或 Windows 程序 |

### 3.2 模型接入（26 个 Provider）

**云端**：OpenAI Chat、OpenAI Responses、Anthropic Claude、Google Gemini、DeepSeek、通义千问、豆包、Kimi、Mistral、xAI、OpenRouter、Nvidia、Novita、Nous Portal、4Router、Mimo、Codex（OAuth）、OpenCode

**本地/局域网**：Ollama、LM Studio、MNN（端侧 JNI）、llama.cpp（GGUF 端侧 JNI）

**插件提供**：ToolPkg 可注册自定义 AI Provider

**11 种任务可分别绑定不同模型**（`FunctionType` 枚举实测）：
```
CHAT                    常规对话
SUMMARY                 对话总结
TITLE_GENERATION        自动生成标题
MEMORY                  记忆库处理
UI_CONTROLLER            UI 自动化控制
TRANSLATION             翻译
GREP                    代码搜索规划
ROLE_RESPONSE_PLANNER   多角色回答顺序规划
IMAGE_RECOGNITION       图像识别
AUDIO_RECOGNITION       音频识别
VIDEO_RECOGNITION       视频识别
```

配套机制：密钥池轮换（`ApiKeyProvider`）、限流（滑动窗口）、并发控制、Token 计量、指数退避重试（5 次，1s→16s）、连接测试。

### 3.3 179 个内置工具（按功能族）

| 族 | 数量 | 代表工具 |
|---|---|---|
| **文件系统** | 25 | `read_file` `write_file` `apply_file`(差异编辑) `grep_code` `grep_context`(语义搜索) `zip_files` `download_file` |
| **浏览器自动化** | 22 | `browser_navigate` `browser_click` `browser_fill_form` `browser_snapshot` `browser_evaluate` `browser_network_requests` |
| **蓝牙** | 14 | 经典蓝牙 + BLE 完整读写订阅 |
| **系统集成** | 12 | `device_info` `execute_intent` `send_broadcast` `get_notifications` `modify_system_setting` `get_device_location` |
| **对话管理** | 12 | `create_new_chat` `switch_chat` `send_message_to_ai` `get_chat_messages_range` |
| **角色卡** | 10 | 增删改查 + Tavern JSON 导入导出 + 激活切换 |
| **记忆库** | 9 | `create_memory` `query_memory` `link_memories` `move_memory` |
| **工作流** | 9 | 增删改查 + 启停 + 触发 |
| **UI 自动化** | 9 | `tap` `swipe` `click_element` `set_input_text` `capture_screenshot` `run_ui_subagent` |
| **模型配置** | 8 | 增删改查 + 功能绑定 + 连接测试 |
| **音乐播放** | 8 | 播放/暂停/队列/音量/进度 |
| **终端 Shell** | 8 | 会话创建、流式执行、隐藏执行、输入注入 |
| **插件包管理** | 8 | `use_package` `package_proxy` `list_sandbox_packages` `restart_mcp_with_logs` |
| **应用管理** | 6 | 装/卸/启/停/列表/使用时长 |
| **音视频** | 3 | `ffmpeg_convert` `ffmpeg_execute` `ffmpeg_info` |
| **语音** | 3 | TTS/STT 配置读写 + 播报测试 |
| **网络** | 3 | `http_request` `multipart_request` `visit_web` |
| **其他** | 10 | `calculate` `sleep` `manage_cookies` `agent_status` `close_all_virtual_displays` |

依据：`ToolRegistration.kt` 精确提取 179 个 `name = "..."`

### 3.4 五档权限模型（同一工具不同实现）

| 级别 | 通道 | 能力 |
|---|---|---|
| STANDARD | 普通 Android API | SAF 文件、HTTP、Intent |
| ACCESSIBILITY | 无障碍服务 | 读屏、模拟点击输入 |
| DEBUGGER | Shizuku（ADB 级） | pm/am 命令、更强文件访问 |
| ADMIN | 设备管理员 | 部分系统设置 |
| ROOT | libsu | 完全系统访问、虚拟显示 |

运行时由 `ToolGetter` 按用户当前授权自动选实现。工具权限有三档设置：自动允许 / 每次询问（默认）/ 禁止。

依据：`ToolGetter.kt`、`system/shell/`（7 个 Executor）、`system/action/`（8 个 Listener）

---

## 四、记忆系统

| 能力 | 实现 |
|---|---|
| 多记忆空间 | 按 `profileId` 独立 ObjectBox 数据库 |
| 图谱化记忆 | 节点 + 带权重和类型的关系链接 |
| 文档导入分块 | `DocumentChunk` 实体，支持按块检索 |
| 混合检索 | 语义（向量）+ 词法（Jieba 分词 + 通配）用 **RRF 融合排序** |
| 向量索引 | hnswlib 近邻检索，索引文件按 profile 分离 |
| Embedding | **云端 API**（`CloudEmbeddingService`，OkHttp 调用），非本地模型 |
| 自动记忆 | 从对话/附件提取候选（`MemoryAutoSaveCandidate`） |
| 文件夹组织 | 记忆可分文件夹，支持批量移动 |
| 导出 | `exportMemoriesToJson` |
| 对外暴露 | `MemoryDocumentsProvider`（其他 App 可通过 SAF 访问） |

依据：`MemoryRepository.kt`（2814 行，实测方法名含 `computeRrfBaseScore`、`buildLexicalQueryTokens`、`cosineSimilarity`）、`CloudEmbeddingService.kt`

---

## 五、角色系统

| 能力 | 说明 |
|---|---|
| 角色卡 | 人设、开场白、提示词、备注 |
| 每角色独立绑定 | 对话模型、记忆空间、工具白名单（内置工具/包/Skill/MCP 四类分别控制） |
| Tavern 兼容 | JSON 导入导出，PNG 角色卡导入 |
| 二维码分享 | 彩色二维码（`ColorQrCodeUtil`） |
| **多角色群聊** | `CharacterGroupCardManager`：建组、克隆绑定、复制组、自动生成组头像；@ 交互；每角色独立历史 |
| 回答顺序规划 | `ROLE_RESPONSE_PLANNER` 功能模型决定谁先说 |
| 虚拟形象 | **7 种格式**：DragonBones、WebP、MP4、MMD、glTF/GLB、FBX（`core/avatar/impl/` 7 个子目录） |

---

## 六、工作流引擎

**5 类节点**（`Workflow.kt` sealed class 实测）：

| 节点 | 作用 |
|---|---|
| `TriggerNode` | 触发器 |
| `ExecuteNode` | 执行动作：调用任意内置工具，**或直接跑 JavaScript 代码** |
| `ConditionNode` | 条件判断，9 种运算符：EQ NE GT GTE LT LTE CONTAINS NOT_CONTAINS IN |
| `LogicNode` | AND/OR 逻辑组合 |
| `ExtractNode` | 数据提取，5 种模式：REGEX JSON SUB CONCAT RANDOM_INT |

**6 种触发方式**（`triggerType` 注释实测）：`manual` `schedule`（定时）`tasker` `intent` `speech`（语音）`app_open`（冷启动）

**参数传递**：`StaticValue`（静态值）或 `NodeReference`（引用上游节点输出）

**分支**：`on_success` / `on_error`

**配套**：WorkManager 调度、开机自启（`WorkflowBootReceiver`）、执行日志（4 级：DEBUG/INFO/WARN/ERROR）、失败阶段标记、批量管理、取消。

---

## 七、开发环境（手机上写代码）

| 能力 | 说明 |
|---|---|
| **Ubuntu 24.04 终端** | 完整 ARM64 用户空间（62.6MB rootfs），默认 PRoot，条件允许可 chroot |
| 终端功能 | 多会话、Python、Node.js、vim、SSH、tmux、自定义按键、换源 |
| **项目工作区** | 9 种模板：android、flutter、go、java、node、office、python、typescript、web |
| 代码编辑器 | Canvas 自绘编辑器、语法高亮、自动补全、格式化、语言自动检测 |
| 高亮语言 | Kotlin、JavaScript、Dart、HTML（+ 基类可扩展） |
| 工作区文件系统 | 应用内部目录、SAF、SFTP、SSH 四种后端 |
| 实时预览 | 网页预览、图片预览、只读文档预览 |
| 变更追踪 | 备份管理、变更确认对话框 |
| 聊天绑定工作区 | AI 可读项目规则、引用文件、直接改代码 |
| 开发工具 | Logcat、SQLite 查看器、Git、APKTool、HTML 打包 |
| 对外暴露 | `WorkspaceDocumentsProvider`（其他 App 可访问工作区） |

---

## 八、三条扩展通道（不改宿主代码就能加功能）

### 8.1 Sandbox Package / ToolPkg（能力最强）

QuickJS 引擎执行 JS，可注册：

| 能力 | 事件常量 |
|---|---|
| 自定义工具 | — |
| Compose DSL 界面 | `TOOLPKG_RUNTIME_COMPOSE_DSL` |
| 导航入口 | `TOOLPKG_NAV_SURFACE_TOOLBOX` / `MAIN_SIDEBAR_PLUGINS` |
| **AI Provider** | `AI_PROVIDER_LIST_MODELS` / `SEND_MESSAGE` / `TEST_CONNECTION` |
| **Prompt 劫持** | `PROMPT_INPUT` `PROMPT_HISTORY` `SYSTEM_PROMPT_COMPOSE` `TOOL_PROMPT_COMPOSE` `PROMPT_FINALIZE` |
| 总结劫持 | `SUMMARY_GENERATE` |
| 聊天界面注入 | `CHAT_INPUT` `CHAT_VIEW` `CHAT_MESSAGE` `XML_RENDER` `INPUT_MENU_TOGGLE` |
| 工具生命周期 | `TOOL_LIFECYCLE` |
| 应用/Activity 生命周期 | `APPLICATION_ON_CREATE/FOREGROUND/BACKGROUND/LOW_MEMORY/TRIM_MEMORY/TERMINATE`、`ACTIVITY_ON_CREATE/START/RESUME/PAUSE/STOP/DESTROY` |
| 消息处理 | `MESSAGE_PROCESSING` |
| 桌面小组件 | Glance |

**内置 47 个示例包**：`ai_chat`、`browser`、`code_runner`、`daily_life`、`super_admin`、`system_tools`、`operit_editor`、`extended_*`（chat/file/http/memory）、`workflow`、`tasker`、绘图系列（openai/qwen/zhipu/xai/minimax/nanobanana/siliconflow）、搜索系列（google/duckduckgo/tavily/various/zhipu）、UI 自动化系列（base/subagent/bilibili/小红书/百度地图）、`12306`、`jmcomic`、`douyin_download`、`crossref`、`pdf_vision_parser`、`hex_editor`、`ffmpeg`、`file_converter`、`java_bridge`、`qq_intelligent`、`reader`、`time`

### 8.2 MCP（Model Context Protocol）

本地进程（npx/uvx/node）或远程 endpoint，标准 MCP 工具接入。

### 8.3 Skill

含 `SKILL.md` 的目录，注入流程文档与提示词。

---

## 九、语音与交互入口

### 9.1 语音

| 方向 | 引擎 |
|---|---|
| **STT（4 个）** | Sherpa-NCNN（本地）、Sherpa-MNN（本地）、OpenAI Whisper、Deepgram |
| **TTS（12 个）** | 系统 TTS、VITS/Piper（本地 ONNX）、OpenAI TTS、OpenAI Realtime WS、MiniMax、SiliconFlow、Mimo、豆包、自定义 HTTP（可配响应管线）等 |
| 附加 | Silero VAD 语音活动检测、连续对话、后台唤醒、**个人唤醒词训练**（`PersonalWakeEnrollment` + `PersonalWakeFeatureExtractor`）、自动朗读、音乐播放队列 |

### 9.2 调用入口（7 种）

| 入口 | 实现 |
|---|---|
| 主界面 | `MainActivity` |
| 悬浮窗 / 气泡 | `FloatingChatService` |
| 桌面小组件 | `VoiceAssistantWidgetReceiver`、`ToolPkgDesktopWidgetReceiver` |
| **系统默认助理** | `OperitVoiceInteractionService`（长按电源键唤起） |
| 局域网 Web Chat | 浏览器访问，NanoHTTPD，端口 8094 |
| 外部 App Intent | `ExternalChatReceiver` |
| Tasker | `AIAgentTasker` |

---

## 十、对外协议（把 AI 能力给别人用）

| 协议 | 用途 | 鉴权 | 默认 |
|---|---|---|---|
| **Web Chat HTTP** | 浏览器端完整对话 + 管理 API | Bearer Token（未配置直接拒服务） | 关闭 |
| **A2A（Agent-to-Agent）** | 标准 Agent 任务接口，支持任务生命周期与分页 | Bearer Token | 关闭 |
| **Intent / Broadcast** | 其他 App 触发对话 | 无 | — |
| **Tasker 插件** | 双向：Tasker 触发工作流，工作流触发 Tasker 事件 | 无 | — |
| **3 个 DocumentsProvider** | 工作区/记忆/应用数据通过 SAF 暴露给文件管理器 | Android 权限 | — |

---

## 十一、市场与发布

| 能力 | 说明 |
|---|---|
| 统一市场 | 脚本、ToolPkg、Skill、MCP 搜索/安装/管理，按分类与作者浏览 |
| 提示词标签市场 | 标签化提示词复用 |
| Artifact 发布 | 把项目产物发布到市场（含继续发布、编辑） |
| 仓库市场发布 | 发布代码仓库（含版本管理） |
| 快速插件创建器 | 引导式创建插件 |
| GitHub 集成 | OAuth 登录（浏览器选择 + 回调）、发布、Release 检查更新 |
| 插件黑名单 | 远程拉取 + SHA256 校验，拦截恶意插件 |

---

## 十二、数据与备份

| 类型 | 存储 | 说明 |
|---|---|---|
| 对话/消息/变体/Token | **Room SQLite** v21（22 个迁移） | 5 张表 |
| 记忆/文档块/候选 | **ObjectBox** | 按 profile 分库 |
| 配置（38 类） | **DataStore** | 模型配置含 API Key（明文） |
| Codex OAuth | **EncryptedSharedPreferences** | 全仓唯一加密存储（AES256） |
| 自动备份 | `RoomDatabaseBackupWorker` | WorkManager 定时 |
| 原始快照备份 | `RawSnapshotBackupManager` | 全量快照 |
| 恢复 | `RoomDatabaseRestoreManager` + `DataRecoveryActivity` | 崩溃后可恢复 |
| 崩溃处理 | `GlobalExceptionHandler` + `CrashReportActivity` + `AnrMonitor` | 三重防护 |

---

## 十三、UI 定制能力（用户可调的外观）

| 可调项 | 位置 |
|---|---|
| 主题配色 | `ThemeSettingsColorSection` |
| 背景（图片/视频） | `ThemeSettingsBackgroundSection` + ExoPlayer + 图片裁剪 |
| 字体与头像 | `ThemeSettingsFontAvatarSections` |
| 聊天气泡样式 | `components/style/` |
| 液态玻璃效果 | `libs.liquid` 依赖 |
| Markdown 渲染 | 自定义渲染器 + LaTeX（jlatexmath/RenderX）+ 表格 + GIF + SVG |
| 布局调整 | `LayoutAdjustmentSettingsScreen` |
| 手机/平板双布局 | `PhoneLayout` / `TabletLayout` |
| 深色模式 | `values-night` |
| 8 种语言 | 中/英/韩/西/马来/印尼/巴葡/罗马尼亚 |

---

## 十四、功能规模总览（便于评估裁剪）

| 维度 | 数量 |
|---|---|
| 主侧边栏入口 | 9 |
| 工具箱工具 | 18 |
| 设置子页面 | 22 |
| 扩展中心页面 | 16 |
| 内置 AI 工具 | 179 |
| LLM Provider | 26 |
| 可绑定功能模型的任务类型 | 11 |
| STT 引擎 | 4 |
| TTS 引擎 | 12 |
| 虚拟形象格式 | 7 |
| 工作流节点类型 | 5 |
| 工作流触发方式 | 6 |
| 内置示例插件 | 47 |
| 工作区项目模板 | 9 |
| 权限级别 | 5 |
| 对外协议 | 5 |
| 调用入口 | 7 |
| 界面语言 | 8 |
| ToolPkg 可挂 Hook 事件 | 30+ |

---

## 十五、给定制讨论的分层建议

按"改动成本"从低到高排列，供需求讨论时定位：

| 层 | 改什么 | 成本 | 举例 |
|---|---|---|---|
| **L0 配置层** | 不改代码，只改设置 | 极低 | 关掉不用的工具、只留一个模型、换主题 |
| **L1 插件层** | 写 ToolPkg | 低 | 加自定义工具、加新页面、改 prompt、接自己的模型 |
| **L2 裁剪层** | 删功能模块 | 中 | 删虚拟形象/市场/蓝牙/音乐，改 `ScreenRouteRegistry` 与依赖 |
| **L3 界面重构层** | 重写 UI | 中高 | 重做聊天界面、重做导航结构（注意 `components/lazy/` 已深度优化） |
| **L4 核心改造层** | 改对话链路/数据模型/权限体系 | 高 | 改 `EnhancedAIService`、加 Room 迁移、改工具注册机制 |

**注意事项**：
- 删任何工具要走 7 处同步（见 `DEFAULT_TOOLS_ARCH.md`）
- 删 UI 页面要同步删 `ScreenRouteRegistry` 条目 + 8 个语言的 strings
- 改聊天列表前必读 `components/lazy/` 三个文件（从 AndroidX 拷贝改造的性能优化）
- 现有 `@Preview` 只有 2 个，UI 改动验证成本高

---

## 十六、Evidence

- `ScreenRouteRegistry.kt`（654 行，导航注册单一来源）
- `ToolRegistration.kt`（2736 行，179 个工具）
- `data/model/FunctionType.kt`（11 种任务）
- `data/model/Workflow.kt`（5 类节点 + 枚举）
- `data/repository/MemoryRepository.kt`（2814 行，RRF 混合检索）
- `services/CloudEmbeddingService.kt`（云端 embedding）
- `core/tools/packTool/ToolPkgCommonPluginConstants.kt`（30+ Hook 事件常量）
- `core/avatar/impl/`（7 种形象格式）
- `api/{speech,voice}/`（STT 4 + TTS 12）
- `core/tools/system/{shell,action}/`（5 档权限各自实现）
- `app/src/main/assets/templates/`（9 种项目模板）
- `examples/*.ts`（47 个示例插件）
- `ui/features/{settings,packages,toolbox}/screens/`（页面清单）