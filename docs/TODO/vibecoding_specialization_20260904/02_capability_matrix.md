# 02 VibeCoding 能力矩阵

## 一、基线契约

- 仓库 `Black0Bag/minibile`，`main` = `10cc9e60`，Tag `v0.0.2`，`VERSION` = `0.0.2`
- Android 官方校准（developer.android.com/permissions，2026-07-15 版）：数据最小化/请求最少权限作为裁剪判据

## 二、179 内置工具族概览

| 族 | 数量 | 判定 | 依据概要 |
|---|---|---|---|
| 文件系统 | 24 | 拆分后保留 | `read_file`/`apply_file`/`grep_code`/`grep_context`/`edit_file` 为编码核心；`open_file`/`share_file` 手机用途删 |
| 终端 Shell | 8 | 保留 | `create_terminal_session`/`execute_in_terminal_session*` 全链路保留 |
| 网络研究 | 3 | 保留 | `visit_web`/`http_request`/`multipart_request` 查文档与调用 API |
| 扩展包 | 8 | 保留 | `use_package`/`package_proxy`/`list_sandbox_packages` 是 Skill/MCP/ToolPkg 入口 |
| 模型配置 | 8 | 保留 | 配置与测试模型连接是运行前提 |
| 杂项 | 3 | 保留 | `calculate`/`sleep`/`agent_status` |
| 记忆库 | 12 | 拆分后保留 | 项目记忆/经验沉淀保留，含 `update_user_profile`（2026-09-09 改判：user.md 承载编码个性化上下文）；`update_user_preferences` 兼容垫片删 |
| 对话管理 | 12 | 拆分后保留 | 会话增删查切保留；`start/stop_chat_service`、语音浮窗相关删 |
| 系统集成 | 14 | 拆分后保留 | `device_info`/`install_app`/`uninstall_app` 保留（2026-09-09 定：装机验证闭环）；其余 11 项属手机控制，删 |
| 浏览器自动化 | 24 | 保留 | 2026-09-09 定：全保留（Turnstile 半自动验证 + Web E2E 前瞻） |
| UI 自动化 | 10 | 拆分后保留 | `capture_screenshot` 保留（2026-09-09 定：读屏验证 Compose UI）；其余 9 项属手机控制，删 |
| 蓝牙 | 19 | 删除 | 与编码无关 |
| 角色卡 | 10 | 删除 | 通用聊天/角色扮演 |
| 工作流 | 10 | 删除 | 通用自动化编排，非编码闭环 |
| 音乐播放 | 8 | 删除 | 与编码无关 |
| FFmpeg | 3 | 删除 | 音视频转码 |
| 语音 TTS/STT | 3 | 删除 | 语音子系统 |

总计：保留 54、拆分后保留 70、删除 61、漏分修正 2（`execute_shell`→终端、`link_memories`→记忆）

## 三、31 扩展包矩阵

| 判定 | 项 |
|---|---|
| 保留 | `super_admin` `extended_file_tools` `extended_memory_tools` `extended_http_tools` `code_runner` `operit_editor` `google_search` `github` `browser` `time` |
| 保留（默认停用） | `various_search` `duckduckgo`（2026-09-09 定：重叠搜索工具，需要时手动启用） |
| 删除 | `daily_life` `system_tools` `workflow` `extended_chat` `Automatic_ui_base` `Automatic_ui_subagent` `file_converter` `crossref` `apk_reverse` `linux_ssh` `remote_operit` `windows_control` `worldbook_tools` `plan_mode_tools` `agent-governance` `ai-ready` `android-accessibility` `navigation-3` `migrate-xml-views-to-jetpack-compose` `brag-sheet` `compose-ui` `go-backend` `release-skills` `use-modern-go` `acquire-codebase-knowledge` `Vibe-coding-workflow-zh` |

注：`Vibe-coding-workflow-zh` 为本次重构的开发流程 Skill，打包进 APK 供最终 App 使用，非运行时依赖；T11 UI 收敛时再评估是否随包移除。

## 四、导航与设置矩阵

| 判定 | 页面 |
|---|---|
| 保留 | 聊天（`AIChat`）、设置（`AssistantConfig`、`MemoryBase`、`Packages`、`Settings`、`Help`、`About`）、工具箱（`ToolTester`、`FileManager`、`Terminal`、`ShellExecutor`、`Logcat`、`SqlViewer`、`TokenConfig`、`UIDebugger`） |
| 拆分后保留 | `ModelConfig`、`ModelPromptsSettings`、`FunctionalConfig`、`ToolPermission`、`LanguageSettings`、`GlobalDisplaySettings`、`ThemeSettings`、`ChatHistorySettings`、`ContextSummarySettings`、`ExternalHttpChatSettings`、`ChatBackupSettings`；`Settings` 下保留最低必要子集 |
| 删除 | `TextToSpeech`、`SpeechToText`、`FfmpegToolbox`、`AutoGlmOnClick`、`AutoGlmTool`、`HtmlPackageManager`、`ProcessLimitRemover`、`DefaultAssistantGuide`、`AppPermissions`；市场 12 屏（`ShizukuCommands`、`Workflow`、`Screen`、`Market`、`MarketAuthor`、`MarketCategory`、`MarketEntryDetail`、`MarketManage`、`MarketNotifications`、`TagMarket`、`RepoEdit`、`RepoPublish`、`RepoPublishVersion`、`ArtifactPublish`、`ArtifactEdit`、`ArtifactContinuePublish`、`WaifuModeSettings`、`PersonaCardGeneration`、`CustomEmojiManagement`、`MnnModelDownload`、`LayoutAdjustmentSettings`、`SpeechServicesSettings`） |

## 五、Manifest 组件矩阵

| 判定 | 组件 |
|---|---|
| 保留 | `MainActivity`、`DataRecoveryActivity`、`CrashReportActivity`、`FloatingChatService`、`AIForegroundService`、`ScriptExecutionReceiver`、`ToolPkgDebubInstallReceiver`、`PackageDebubRefreshReceiver`、`ToolPkgComposeSlbDebubDumpReceiver`、`ExternalChatReceiver`、`FileProvider`、`WorkspaceDocumentsProvider`、`MemoryDocumentsProvider`、`OperitDataDocumentsProvider`、`InitializationProvider` |
| 拆分后保留 | `WebSessionPermissionRequestActivity`、`UserscriptImportPickerActivity`（T05 工具策略评估后定）、`ScreenCaptureService`/`ScreenCaptureActivity`（2026-09-09 定：保留，读屏验证）、`OperitAccessibilityActivity`（保留：无障碍服务承载终端与终端会话，非手机控制） |
| 删除 | `OperitVoiceInteractionService`、`OperitVoiceInteractionSessionService`、`UIDebuggerService`、`ActivityConfigAIAgentAction`、`WorkflowTaskerActivity`、`WorkflowTaskerReceiver`、`WorkflowBootReceiver`、`VoiceAssistantWidgetReceiver`、`ToolPkgDesktopWidgetReceiver`、`ToolPkgDesktopWidgetConfigActivity`、`ShizukuProvider`、`live.pw.renderX.LatexView`、`ShowerBinderReceiver` |

## 六、权限矩阵（37 项）

| 判定 | 权限 |
|---|---|
| 保留 | `INTERNET` `ACCESS_NETWORK_STATE` `FOREGROUND_SERVICE` `FOREGROUND_SERVICE_DATA_SYNC` `FOREGROUND_SERVICE_SPECIAL_USE` `WAKE_LOCK` `POST_NOTIFICATIONS` `RECEIVE_BOOT_COMPLETED` `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` `MANAGE_EXTERNAL_STORAGE` `READ/WRITE_EXTERNAL_STORAGE`（2026-09-09 定：装卸应用走 Shizuku/Root pm 命令通道，不依赖包查询 API，`QUERY_ALL_PACKAGES` 删，其唯一消费点 `list_installed_apps` 工具已定删） |
| 拆分后保留 | `SYSTEM_ALERT_WINDOW`（2026-09-09 定：保留，浮动编码窗口）`SUSTAINED_PERFORMANCE_MODE` `KILL_BACKGROUND_PROCESSES` `PACKAGE_USAGE_STATS` |
| 删除 | `BLUETOOTH` `BLUETOOTH_ADMIN` `BLUETOOTH_CONNECT` `BLUETOOTH_SCAN` `CAMERA` `RECORD_AUDIO` `BIND_VOICE_INTERACTION` `FOREGROUND_SERVICE_MICROPHONE` `FOREGROUND_SERVICE_MEDIA_PROJECTION` `CALL_PHONE` `READ_SMS` `RECEIVE_SMS` `SEND_SMS` `SCHEDULE_EXACT_ALARM` `WRITE_SETTINGS` `READ_MEDIA_AUDIO` |

## 七、Gradle 模块矩阵（9 个）

| 模块 | 依赖数 | 判定 | 依据 |
|---|---|---|---|
| `:app` | — | 保留 | 主应用 |
| `:terminal` | — | 保留 | PRoot 终端，编码执行核心 |
| `:quickjs` | 3 | 保留 | JS 脚本运行时，沙盒包依赖 |
| `:mnn` | 20 | 删除 | 本地 MNN 推理，非必需 |
| `:llama` | 13 | 删除 | 本地 llama 推理，非必需 |
| `:dragonbones` | 8 | 删除 | 虚拟形象 |
| `:mmd` | 8 | 删除 | 虚拟形象 |
| `:fbx` | 10 | 删除 | 虚拟形象 |
| `:showerclient` | 7 | 删除 | Shower 语音/唤醒 |

## 八、资源与依赖矩阵

| 判定 | 项 |
|---|---|
| 保留 | `assets/packages`（裁剪后）`assets/js` `assets/bridge` `assets/web` `assets/templates`（保留 `android flutter go java node python typescript web`，删 `office`）`res/values` `res/values-en` `res/values-night` |
| 删除 | `assets/pets` `assets/dragonbones` `assets/emoji` `assets/accessibility.apk` `assets/desktop.apk` `assets/subpack` `assets/test`（2026-09-09 修正：`assets/shizuku.apk` 移入保留——ShizukuInstaller.kt 第 22 行引用其为启动器，Shizuku 工具链是终端/shell 核心依赖；`assets/model_logos` 移入保留——ProviderLogoLoader.kt 引用，21 个 Provider 均留则 logo 资产对应保留，但本地推理相关 logo（MNN/LLAMA_CPP/Ollama/LMSTUDIO）随模块删除后无引用点，可在 T10 波次清理） |
| 语言 | 保留 `values` `values-en` `values-night`；删除 `values-es` `values-id` `values-ko` `values-ms` `values-pt-rBR` `values-ro` |

## 九、模型 Provider 与功能模型矩阵

| 判定 | 项 |
|---|---|
| 保留 | Provider 全部 21 个（编码依赖外部模型，保留云端；`LlamaProvider` `MNNProvider` `OllamaProvider` `LMSTUDIO` 类本地推理随模块取舍） |
| 功能模型类型保留 | `CHAT` `SUMMARY` `TITLE_GENERATION` `MEMORY` `GREP` |
| 功能模型类型删除 | `UI_CONTROLLER` `TRANSLATION` `ROLE_RESPONSE_PLANNER` `IMAGE_RECOGNITION` `AUDIO_RECOGNITION` `VIDEO_RECOGNITION` |

## 十、取舍点状态

### 已定（2026-09-09 用户确认）

1. 浏览器自动化：24 个工具全保留（Fl API Hub Turnstile 半自动验证 + Web E2E 场景）
7. 搜索扩展包：4 个全保留，`various_search` 与 `duckduckgo` 默认停用（需要时手动启用，避免重叠工具占用上下文）

### 已定（模块/权限/资产级，5 项，2026-09-09 用户确认）

2. 截图能力 `capture_screenshot`/`ScreenCaptureService`：**已定：保留**（2026-09-09 用户确认）
   - 证据：`MediaProjectionHolder.kt`、`ScreenCaptureActivity.kt`、`ScreenCaptureService.kt` 三文件级联
   - 依据：minibox 项目是 Android APP 开发，Agent 验证 Compose UI 渲染结果、排查真机截图与设计稿差异，都需要读屏能力
3. 装/卸应用与 `QUERY_ALL_PACKAGES`：**已定：拆开判**（2026-09-09 用户确认）
   - `QUERY_ALL_PACKAGES`（Manifest 第 59 行）：**删**。列已装应用是手机管理场景，与编码无关，且这是 Google Play 高敏权限；实测 grep 确认其消费点 `StandardSystemOperationTools.kt` 仅服务于 `list_installed_apps` 工具（已定删）
   - `install_app`/`uninstall_app` 工具：**留**。minibox 开发闭环需要"构建 APK → 安装到手机 → 验证运行"的最后一公里（实测：`DebuggerSystemOperationTools.kt` 第 171/235/237 行走 `pm install/uninstall`，依赖 Shizuku/Root shell 通道而非包查询 API）
4. 悬浮窗 `SYSTEM_ALERT_WINDOW`：**已定：保留**（2026-09-09 用户确认）
   - 证据：`FloatingChatService.kt`、`FloatingWindowManager.kt`、`UIDebuggerService.kt` 等 9 文件引用
   - 依据：浮动编码窗口让你在别的 App 里也能看到 Agent 进度，等于把"长任务后台执行+随时可查看"变成可能
5. 本地推理模块 `llm/mnn`（3.8M，含 libsherpa-mnn-jni.so 3.6M）+ `llm/llama`（112K）：**已定：删**（2026-09-09 用户确认）
   - 依据：你用云端模型（GLM/Anthropic 等），手机本地推理没有场景；体积收益 3.9M
5b. 同族项 `:showerclient`（Shower 语音唤醒）：**删**。语音子系统整体已定删
6. 子包调试签名 `assets/jks.jks` `assets/pkcs12.keystore`：**已定：保留**（2026-09-09 用户确认）
   - 证据：仅 `core/subpack/KeyStoreHelper.kt` 引用（第 164/172/180 行），用于沙盒子包的运行时调试签名
   - 依据：沙盒包执行链路（T05 工具策略）依赖这个签名机制给子包签名；删了 `execute_sandbox_script_direct` 会断链

> 全部 7 个取舍点（2 工具级 + 5 模块级）已定稿，T02 能力矩阵定稿（2026-09-09）。判定变更全记录见十四节。

## 十一、179 工具全量清单（A：编码核心族，54 个）

> 作用列标注来源：`[官方]` = 取自 `ToolRegistration.kt` 引用的字符串资源；`[推断]` = 无注册期描述，按名称与 executor 实现判定。

### 文件系统（24：留 22 / 删 2）

| 工具 | 作用 | 建议 |
|---|---|---|
| `list_files` | [官方] 列目录 | 保留 |
| `read_file` | [官方] 读文件 | 保留 |
| `read_file_part` | [官方] 按行区间读 | 保留 |
| `read_file_full` | [官方] 整读文件 | 保留 |
| `read_file_binary` | [官方] 读二进制 | 保留 |
| `write_file` | [官方] 写文件 | 保留 |
| `write_file_binary` | [官方] Base64 写二进制 | 保留 |
| `delete_file` | [官方] 删除文件/目录 | 保留 |
| `file_exists` | [官方] 判断存在 | 保留 |
| `move_file` | [官方] 移动/重命名 | 保留 |
| `copy_file` | [官方] 复制 | 保留 |
| `make_directory` | [官方] 建目录 | 保留 |
| `find_files` | [官方] 按模式查找 | 保留 |
| `file_info` | [官方] 文件元信息 | 保留 |
| `apply_file` | [官方] AI 差异合并 | 保留 |
| `create_file` | [推断] 新建文件 | 保留 |
| `edit_file` | [推断] 精准替换编辑 | 保留 |
| `zip_files` | [官方] 打包 | 保留 |
| `unzip_files` | [官方] 解压 | 保留 |
| `grep_code` | [官方] 正则搜代码 | 保留 |
| `grep_context` | [官方] 语义找相关文件 | 保留 |
| `download_file` | [官方] 下载依赖/资源 | 保留 |
| `open_file` | [官方] 用系统 App 打开 | 删除（手机用途） |
| `share_file` | [官方] 系统分享 | 删除（手机用途） |

### 终端 Shell（8：全留）

| 工具 | 作用 | 建议 |
|---|---|---|
| `execute_shell` | [官方] 执行 ADB shell | 保留 |
| `create_terminal_session` | [官方] 建/取终端会话 | 保留 |
| `execute_in_terminal_session` | [官方] 会话内执行 | 保留 |
| `execute_in_terminal_session_streaming` | [官方] 流式执行 | 保留 |
| `execute_hidden_terminal_command` | [官方] 隐藏终端执行 | 保留 |
| `close_terminal_session` | [官方] 关闭会话 | 保留 |
| `input_in_terminal_session` | [官方] 向会话注入输入 | 保留 |
| `get_terminal_session_screen` | [官方] 取会话屏幕 | 保留 |

### 网络研究（3：全留）

| 工具 | 作用 | 建议 |
|---|---|---|
| `visit_web` | [官方] 访问链接取内容 | 保留 |
| `http_request` | [官方] HTTP 请求 | 保留 |
| `multipart_request` | [官方] 多部分上传 | 保留 |

### 扩展包（8：全留）

| 工具 | 作用 | 建议 |
|---|---|---|
| `use_package` | [官方] 加载包 | 保留 |
| `package_proxy` | [推断] 调用包内工具 | 保留 |
| `list_sandbox_packages` | [推断] 列沙盒包 | 保留 |
| `set_sandbox_package_enabled` | [推断] 启停包 | 保留 |
| `execute_sandbox_script_direct` | [推断] 直跑沙盒脚本 | 保留 |
| `restart_mcp_with_logs` | [推断] 重启 MCP 取日志 | 保留 |
| `read_environment_variable` | [推断] 读环境变量 | 保留 |
| `write_environment_variable` | [推断] 写环境变量 | 保留 |

### 模型配置（8：全留）

| 工具 | 作用 | 建议 |
|---|---|---|
| `list_model_configs` | [推断] 列模型配置 | 保留 |
| `create_model_config` | [推断] 新建 | 保留 |
| `update_model_config` | [推断] 修改 | 保留 |
| `delete_model_config` | [推断] 删除 | 保留 |
| `list_function_model_configs` | [推断] 列功能模型绑定 | 保留 |
| `get_function_model_config` | [推断] 查单个绑定 | 保留 |
| `set_function_model_config` | [推断] 设置绑定 | 保留 |
| `test_model_config_connection` | [推断] 连通性测试 | 保留 |

### 杂项（3：全留）

| 工具 | 作用 | 建议 |
|---|---|---|
| `calculate` | [官方] 表达式计算 | 保留 |
| `sleep` | [官方] 暂停毫秒 | 保留 |
| `agent_status` | [官方] 查会话状态 | 保留 |

## 十二、179 工具全量清单（B：判定组，62 个）

### 记忆库（12：留 11 / 删 1）

| 工具 | 作用 | 建议 |
|---|---|---|
| `query_memory` | [官方] 检索记忆/经验 | 保留 |
| `get_memory_by_title` | [官方] 按标题取记忆 | 保留 |
| `create_memory` | [官方] 写入记忆 | 保留 |
| `update_memory` | [官方] 更新记忆 | 保留 |
| `delete_memory` | [官方] 删除记忆 | 保留 |
| `move_memory` | [推断] 移动到文件夹 | 保留 |
| `link_memories` | [官方] 建立记忆关联 | 保留 |
| `query_memory_links` | [推断] 查关联 | 保留 |
| `update_memory_link` | [推断] 更新关联 | 保留 |
| `delete_memory_link` | [推断] 删除关联 | 保留 |
| `update_user_profile` | [官方] 更新用户画像文档 | 保留（2026-09-09 改判：user.md 承载编码个性化上下文，Agent 自我进化入口） |
| `update_user_preferences` | [官方] 更新用户偏好 | 删除（源码注释自证为旧工具包兼容垫片，与画像工具重复，minibile 无旧调用方） |

### 对话管理（12：留 10 / 删 2）

| 工具 | 作用 | 建议 |
|---|---|---|
| `create_new_chat` | [官方] 新建会话 | 保留 |
| `list_chats` | [官方] 列全部会话 | 保留 |
| `find_chat` | [官方] 查找会话 | 保留 |
| `switch_chat` | [官方] 切换会话 | 保留 |
| `update_chat_title` | [官方] 改标题 | 保留 |
| `delete_chat` | [官方] 删会话 | 保留 |
| `get_chat_messages` | [官方] 取消息 | 保留 |
| `get_chat_messages_range` | [官方] 按区间取消息 | 保留 |
| `send_message_to_ai` | [官方] 向 AI 发消息 | 保留 |
| `send_message_to_ai_streaming` | [官方] 流式发消息 | 保留 |
| `start_chat_service` | [官方] 启动浮动窗口服务 | 删除（语音浮窗入口） |
| `stop_chat_service` | [官方] 停止浮动窗口服务 | 删除（同上） |

### 系统集成（14：留 3 / 删 11）

| 工具 | 作用 | 建议 |
|---|---|---|
| `device_info` | [官方] 读设备信息 | 保留（调试环境需要） |
| `execute_intent` | [官方] 发系统 Intent | 删除（手机控制） |
| `send_broadcast` | [推断] 发广播 | 删除（手机控制） |
| `get_system_setting` | [推断] 读系统设置 | 删除（手机控制） |
| `modify_system_setting` | [官方] 改系统设置 | 删除（手机控制） |
| `get_notifications` | [官方] 读通知栏 | 删除（手机监控） |
| `send_notification` | [推断] 发通知 | 删除（2026-09-09 定：任务完成提醒改用应用内提示） |
| `get_app_usage_time` | [推断] 读应用使用时长 | 删除（手机监控） |
| `list_installed_apps` | [推断] 列已装应用 | 删除（手机控制） |
| `install_app` | [官方] 安装 APK | 保留（2026-09-09 改判：minibox 开发闭环"构建→装机→验证"最后一公里；实现走 DebuggerSystemOperationTools pm install，经 Shizuku/Root shell 通道） |
| `uninstall_app` | [官方] 卸载应用 | 保留（2026-09-09 改判：同 install_app，闭环配套） |
| `start_app` | [官方] 启动应用 | 删除 |
| `stop_app` | [官方] 停止应用 | 删除 |
| `get_device_location` | [官方] 取定位 | 删除（隐私权限） |

### 浏览器自动化（24：全留）

> 2026-09-09 用户确认全保留。依据：Fl API Hub 的 Cloudflare Turnstile 半自动验证需要完整交互面（navigate/wait_for/handle_dialog/click）；将来 Web 项目 E2E 测试需要 click/type/console/network 全套；Playwright MCP 全工具面为业界主流（互联网校准）。代价：工具 schema 常驻上下文约 +2-5k token/对话（估算，T05 落地后实测）。

| 工具 | 作用 | 建议 |
|---|---|---|
| `browser_navigate` | [推断] 打开网页 | 保留 |
| `browser_snapshot` | [推断] 取页面结构 | 保留 |
| `browser_evaluate` | [推断] 执行 JS 取值 | 保留 |
| `browser_navigate_back` | [推断] 后退 | 保留 |
| `browser_close` | [推断] 关闭页面 | 保留 |
| `browser_close_all` | [推断] 关闭全部 | 保留 |
| `browser_tabs` | [推断] 标签管理 | 保留 |
| `browser_click` | [推断] 点击 | 保留（人机验证、E2E） |
| `browser_type` | [推断] 输入 | 保留（E2E） |
| `browser_fill_form` | [推断] 填表 | 保留（E2E） |
| `browser_select_option` | [推断] 选下拉 | 保留（E2E） |
| `browser_hover` | [推断] 悬停 | 保留 |
| `browser_drag` | [推断] 拖拽 | 保留 |
| `browser_press_key` | [推断] 按键 | 保留 |
| `browser_handle_dialog` | [推断] 处理弹窗 | 保留（验证弹窗） |
| `browser_file_upload` | [推断] 上传文件 | 保留 |
| `browser_wait_for` | [推断] 等待条件 | 保留（等验证通过） |
| `browser_resize` | [推断] 改视口 | 保留 |
| `browser_take_screenshot` | [推断] 页面截图 | 保留（查渲染结果） |
| `browser_console_messages` | [推断] 读控制台 | 保留（前端调试） |
| `browser_network_requests` | [推断] 读网络请求 | 保留（查接口） |
| `browser_run_code` | [推断] 跑 Playwright 代码 | 保留 |
| `get_page_info` | [官方] 取当前页信息 | 保留 |
| `manage_cookies` | [官方] 管理 Cookie | 保留（登录态保活） |

## 十三、179 工具全量清单（C：建议删除族，63 个）

### UI 自动化（10：留 1 / 删 9）

| 工具 | 作用 | 建议 |
|---|---|---|
| `tap` | [官方] 坐标点击 | 删除（手机控制） |
| `long_press` | [官方] 长按 | 删除 |
| `swipe` | [官方] 滑动 | 删除 |
| `click_element` | [官方] 按 resourceId 点击 | 删除 |
| `set_input_text` | [官方] 向输入框写文本 | 删除 |
| `press_key` | [官方] 按物理/虚拟键 | 删除 |
| `capture_screenshot` | [官方] 截屏返回路径 | 保留（2026-09-09 改判：minibox Android APP 开发需 Agent 读屏验证 Compose UI 渲染；原取舍点 2 已定为保留） |
| `run_ui_subagent` | [官方] 驱动 UI 子代理 | 删除（AutoGLM 手机操作） |
| `toast` | [推断] 弹 Toast | 删除 |
| `close_all_virtual_displays` | [官方] 关闭虚拟屏 | 删除（Root 虚拟显示） |

### 蓝牙（19：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `request_bluetooth_permission` | [推断] 申请蓝牙权限 | 删除 |
| `request_enable_bluetooth` | [推断] 打开蓝牙 | 删除 |
| `get_bluetooth_state` | [推断] 查蓝牙开关 | 删除 |
| `list_bluetooth_bonded_devices` | [推断] 列已配对设备 | 删除 |
| `scan_bluetooth_devices` | [推断] 扫描设备 | 删除 |
| `bluetooth_connect` | [推断] 经典蓝牙连接 | 删除 |
| `bluetooth_listen` | [推断] 监听连接 | 删除 |
| `bluetooth_accept` | [推断] 接受连接 | 删除 |
| `bluetooth_send` | [推断] 发送 | 删除 |
| `bluetooth_read` | [推断] 读取 | 删除 |
| `bluetooth_send_and_read` | [推断] 收发一体 | 删除 |
| `bluetooth_close` | [推断] 关闭 | 删除 |
| `bluetooth_ble_connect` | [推断] BLE 连接 | 删除 |
| `bluetooth_ble_discover_services` | [推断] 发现服务 | 删除 |
| `bluetooth_ble_read_characteristic` | [推断] 读特征 | 删除 |
| `bluetooth_ble_write_characteristic` | [推断] 写特征 | 删除 |
| `bluetooth_ble_write_and_read_characteristic` | [推断] 读写特征 | 删除 |
| `bluetooth_ble_subscribe_characteristic` | [推断] 订阅通知 | 删除 |
| `bluetooth_ble_read_notifications` | [推断] 读通知 | 删除 |

### 角色卡（10：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `list_character_cards_settings` | [推断] 列角色卡设置 | 删除（角色扮演） |
| `list_character_cards` | [推断] 列角色卡 | 删除 |
| `get_character_card` | [推断] 取单卡 | 删除 |
| `create_character_card` | [推断] 建卡 | 删除 |
| `update_character_card` | [推断] 改卡 | 删除 |
| `delete_character_card` | [推断] 删卡 | 删除 |
| `set_active_character_card` | [推断] 激活卡 | 删除 |
| `clear_active_character_card` | [推断] 取消激活 | 删除 |
| `import_character_card_from_tavern_json` | [推断] 导入 Tavern | 删除 |
| `export_character_card_to_tavern_json` | [推断] 导出 Tavern | 删除 |

### 工作流（10：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `get_all_workflows` | [官方] 列工作流 | 删除（通用自动化编排） |
| `create_workflow` | [官方] 建工作流 | 删除 |
| `get_workflow` | [官方] 查详情 | 删除 |
| `update_workflow` | [官方] 改 | 删除 |
| `patch_workflow` | [官方] 差异改 | 删除 |
| `enable_workflow` | [官方] 启用 | 删除 |
| `disable_workflow` | [官方] 停用 | 删除 |
| `delete_workflow` | [官方] 删 | 删除 |
| `trigger_workflow` | [官方] 触发 | 删除 |
| `trigger_tasker_event` | [官方] 触发 Tasker | 删除 |

> 2026-09-09 用户确认维持删除。依据：6 种触发器中 `intent`/`tasker`/`app_open`/`speech` 4 种为手机自动化场景，0 种编码场景（无 git push/文件变更/CI 失败触发）；编排职能由会话内 Agent 链式决策替代（可中途应变），定时职能由 GitHub Actions CI 替代（云端常驻，不受手机休眠影响）。删除范围 15 个源文件，speech 触发模板对语音识别子系统的交叉依赖一并消除。

### 音乐播放（8：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `music_play` | [推断] 播放 | 删除 |
| `music_play_queue` | [推断] 队列播放 | 删除 |
| `music_pause` | [推断] 暂停 | 删除 |
| `music_resume` | [推断] 继续 | 删除 |
| `music_stop` | [推断] 停止 | 删除 |
| `music_seek` | [推断] 跳转 | 删除 |
| `music_set_volume` | [推断] 音量 | 删除 |
| `music_status` | [推断] 播放状态 | 删除 |

### FFmpeg（3：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `ffmpeg_execute` | [官方] 执行 FFmpeg 命令 | 删除（音视频转码） |
| `ffmpeg_info` | [官方] 取媒体信息 | 删除 |
| `ffmpeg_convert` | [官方] 格式转换 | 删除 |

### 语音（3：全删）

| 工具 | 作用 | 建议 |
|---|---|---|
| `get_speech_services_config` | [推断] 读 TTS/STT 配置 | 删除 |
| `set_speech_services_config` | [推断] 写 TTS/STT 配置 | 删除 |
| `test_tts_playback` | [推断] 试听播报 | 删除 |

## 十四、179 汇总（脚本核算，非手算）

| 判定 | 数量 |
|---|---|
| 保留 | 101 |
| 删除 | 78 |
| 拆分待定 | 0 |
| 合计 | 179 |

校验结果：明细表 179 行、179 个唯一工具名、与扫描清单双向零差集（`missing=[]`、`extra=[]`）、无重复。

### 判定变更记录（2026-09-09 用户确认）

| 项 | 原判定 | 最终判定 | 依据 |
|---|---|---|---|
| 浏览器自动化 24 工具 | 留 3 / 删 21 | 全留 24 | Fl API Hub Turnstile 半自动验证需要完整交互面；Web 项目 E2E 前瞻；Playwright MCP 全工具面为业界主流（互联网校准） |
| `update_user_profile` | 删除 | 保留 | user.md 承载编码个性化上下文（通俗讲解、项目边界、环境限制），是 Agent 自我进化入口（源码证据：MemoryQueryToolExecutor 写 user.md） |
| `update_user_preferences` | 删除 | 维持删除 | 源码注释自证为旧工具包兼容垫片，与画像工具重复，minibile 无旧调用方 |
| 工作流 10 工具 | 全删 | 维持删除 | 触发器 4/6 为手机场景、0 编码场景；编排→会话 Agent 链式决策，定时→GitHub Actions CI |
| `send_notification` | 拆分待定 | 删除 | 任务完成提醒由应用内提示替代 |
| 截图 `capture_screenshot` + `ScreenCaptureService` | 删除 | 保留 | 取舍点 2 定稿：minibox Android APP 开发需读屏验证 Compose UI 渲染 |
| `install_app`/`uninstall_app` | 删除 | 保留 | 取舍点 3 定稿：开发闭环"构建→装机→验证"最后一公里；pm 命令走 Shizuku/Root 通道 |
| `QUERY_ALL_PACKAGES` 权限 | 随装卸应用待定 | 删除 | 唯一消费点 `list_installed_apps` 已定删；装卸应用走 pm 命令不依赖此权限 |
| 悬浮窗 `SYSTEM_ALERT_WINDOW` | 待定 | 保留 | 取舍点 4 定稿：浮动编码窗口支撑跨 App 查看进度 |
| 本地推理 `llm/mnn`+`llm/llama` | 待定 | 删除 | 取舍点 5 定稿：云端模型场景，体积收益 3.9M |
| 子包签名 `assets/jks.jks`/`pkcs12.keystore` | 待定 | 保留 | 取舍点 6 定稿：KeyStoreHelper 沙盒签名链路，删则 execute_sandbox_script_direct 断链 |
| `assets/shizuku.apk` 资产 | 删除 | 保留 | ShizukuInstaller.kt 第 22 行引用为启动器；Shizuku 工具链是终端/shell 核心依赖 |

> 上一轮初稿把 70 项笼统标为"拆分后保留"；经用户要求全量列出 179 项逐一判定后，全部定稿，无遗留待确认项。
