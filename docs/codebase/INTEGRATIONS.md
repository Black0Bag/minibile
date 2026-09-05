# External Integrations

> 实测生成，2026-09-03。所有条目均可追溯到源码。

## Core Sections (Required)

### 1) Integration Inventory

**LLM 服务商（26 个 Provider，`api/chat/llmprovider/`）** —— 全部走用户自配置 endpoint + API Key：

| System | Type | Purpose | Auth model | Criticality | Evidence |
|--------|------|---------|------------|-------------|----------|
| OpenAI Chat Completions | HTTP/SSE | 主流对话 | Bearer API Key | high | `OpenAIProvider.kt`（2758 行，churn 榜第 8） |
| OpenAI Responses API | HTTP/SSE | 新版 Responses 模式 | Bearer | high | `OpenAIResponsesProvider.kt` |
| Anthropic Claude | HTTP/SSE | 对话 + 1h prompt cache | `x-api-key` | high | `ClaudeProvider.kt` |
| Google Gemini | HTTP/SSE | 对话 + thinking config | API Key query/header | high | `GeminiProvider.kt`（2140 行） |
| DeepSeek | HTTP/SSE | 对话 | Bearer | high | `DeepseekProvider.kt` |
| Qwen / Doubao / Kimi / Mistral / xAI / OpenRouter / Nvidia / Novita / Nous Portal / 4Router / Mimo | HTTP/SSE | 兼容型对话服务 | Bearer | med | 各自 `*Provider.kt` |
| Codex | HTTP + OAuth | OpenAI Codex CLI 协议 + 配额查询 | **OAuth（加密存储）** | med | `CodexProvider.kt`、`CodexAuthPreferences.kt` |
| OpenCode | HTTP | 对话 | Bearer | low | `OpenCodeProvider.kt` |
| Ollama | HTTP（本地/局域网） | 本地模型服务 | 无/自定义 | med | `OllamaProvider.kt` |
| LM Studio | HTTP（OpenAI 兼容本地） | 本地模型服务 | 无 | med | 走 `OpenAIProvider` + `OPENAI_LOCAL` |
| MNN | **进程内 JNI** | 端侧推理（`:mnn` 模块） | 无 | med | `MNNProvider.kt` |
| llama.cpp | **进程内 JNI** | GGUF 端侧推理（`:llama` 模块） | 无 | med | `LlamaProvider.kt` |
| ToolPkg JS Provider | **QuickJS 进程内** | 插件自定义 provider | 插件自管 | med | `ToolPkgJsAiProviderService.kt` |

**语音服务**：

| System | Type | Purpose | Auth | Evidence |
|---|---|---|---|---|
| Sherpa-NCNN / Sherpa-MNN | 本地 ONNX | 中英文 STT（离线） | 无 | `SherpaSpeechProvider.kt`、`SherpaMnnSpeechProvider.kt` |
| Silero VAD | 本地 ONNX | 语音活动检测 | 无 | `OnnxSileroVad.kt` |
| OpenAI Whisper | HTTP | 云端 STT | Bearer | `OpenAISttProvider.kt` |
| Deepgram | HTTP | 云端 STT | API Key | `DeepgramSttProvider.kt` |
| 系统 TTS | Android API | 本机朗读 | 无 | `AccessibilityVoiceProvider.kt` |
| VITS/Piper | 本地 ONNX | 离线 TTS | 无 | `VitsVoiceProvider.kt` |
| OpenAI TTS / Realtime WS | HTTP / WebSocket | 云 TTS | Bearer | `OpenAIVoiceProvider.kt`、`OpenAIRealtimeVoiceProvider.kt` |
| MiniMax / SiliconFlow / Mimo / Doubao / 自定义 HTTP | HTTP | 云 TTS | API Key | 各自 `*VoiceProvider.kt` + `HttpVoiceProvider.kt` |

**扩展与平台服务**：

| System | Type | Purpose | Auth | Criticality | Evidence |
|---|---|---|---|---|---|
| MCP（本地） | stdio 子进程（npx/uvx/node） | 工具服务 | env 变量 | high | `data/mcp/MCPLocalServer.kt` |
| MCP（远程） | HTTP/SSE | 工具服务 | Bearer / headers | med | `MCPRepository.kt` |
| Operit 市场 API | HTTP（`api.operit.app`） | 脚本/ToolPkg/Skill/MCP 市场 | 无（只读） | med | 实测 3 处 `https://api.operit.app` |
| GitHub API | HTTP + OAuth | 登录、Artifact 发布、Release 检查 | **OAuth broker** | med | `data/preferences/GitHubAuthPreferences.kt`、`util/GithubReleaseUtil.kt` |
| Hugging Face | HTTP | STT 模型下载（构建期 + 运行期） | 无 | med | `app/config/stt-model-assets.properties` |
| ModelScope | HTTP | 模型下载备选源 | 无 | low | 实测 2 处引用 |
| Shizuku | **Binder IPC** | ADB 级权限通道 | 用户授权 | high | `core/tools/system/ShizukuAuthorizer.kt` |
| Root (libsu) | **su 进程** | 完全系统权限 | 用户授权 | high | `core/tools/system/RootAuthorizer.kt` |
| 无障碍服务 | Android AccessibilityService | 读屏 + 模拟操作 | 用户授权 | high | `AccessibilityProviderInstaller.kt` |
| Shower Server | Binder + 视频流 | 虚拟显示、截图、事件注入 | Binder 广播 | med | `:showerclient` 模块、`agent/ShowerBinderReceiver.kt` |
| Tasker | Intent | 外部自动化互通 | 无（Intent） | low | `integrations/tasker/`（3 文件） |
| A2A（Agent-to-Agent） | HTTP server | 对外暴露 Agent 任务接口 | Bearer | med | `integrations/a2a/A2aHttpHandler.kt` |
| Web Chat / HTTP API | NanoHTTPD server（局域网） | 浏览器端对话 + 管理 API | **Bearer Token（必配）** | high | `integrations/http/WebChatHttpBridge.kt:2565-2589` |
| Intent 外部对话 | Broadcast Receiver | 其他 App 触发对话 | 无 | med | `integrations/intent/ExternalChatReceiver.kt` |
| 插件黑名单服务 | HTTP + SHA256 | 远程拉取恶意插件名单 | 无 | med | `data/security/PluginDenylistRepository.kt` |
| pollinations.ai | HTTP | 图片生成（示例脚本） | 无 | low | 实测 6 处引用 |

### 2) Data Stores

| Store | Role | Access layer | Key risk | Evidence |
|-------|------|--------------|----------|----------|
| **Room SQLite** `AppDatabase` v21 | 对话/消息/消息变体/Token 用量/模型统计 | `data/dao/`（5 DAO）→ `ChatHistoryManager` | 22 个手写迁移，任何 schema 改动必须新增迁移；`exportSchema = false` 意味着无 schema 快照可比对 | `data/db/AppDatabase.kt:20-28` |
| **ObjectBox** 多 store | 记忆图谱（Memory）、文档分块（DocumentChunk）、自动保存候选（MemoryAutoSaveCandidate） | `data/repository/MemoryRepository.kt` | 按 `profileId` 分目录（`objectbox` / `objectbox_<id>`），跨 profile 数据隔离靠路径；模型文件 `app/objectbox-models/default.json` 是 schema 真相源 | `data/db/ObjectBox.kt` |
| **hnswlib 向量索引** | 记忆语义检索的近邻索引 | `util/vector/VectorIndexManager.kt` | 索引与 ObjectBox 数据一致性需应用层保证；重建成本高 | `util/vector/` |
| **DataStore Preferences**（多个独立 store） | 38 类偏好：模型配置、主题、权限、语音、协议、Codex 用量、唤醒词等 | `data/preferences/`（38 文件） | `versionedPreferencesDataStore` 提供版本化迁移（`VersionedPreferencesDataStore.kt`）；**模型配置含 API Key 且未加密** | `ModelConfigManager.kt:32-33` |
| **EncryptedSharedPreferences** | 仅 Codex OAuth 凭据 | `CodexAuthPreferences.kt` | AES256_SIV(key) + AES256_GCM(value)，是全仓唯一加密存储 | `CodexAuthPreferences.kt:21-28` |
| **文件系统** | 工作区、备份、导出、资源池（图片/媒体/Skill zip）、终端 rootfs | `util/OperitPaths.kt`、`data/backup/`、`data/exporter/` | 通过 3 个 DocumentsProvider 对外暴露 | `provider/` 3 文件 |

### 3) Secrets and Credentials Handling

- **凭据来源**：
  - 运行期：用户在应用内输入 → DataStore（模型配置）或 EncryptedSharedPreferences（Codex OAuth）
  - 构建期：`local.properties`（签名密钥路径与口令），**已在 `.gitignore` 排除**（`.gitignore:8,20`）
- **硬编码检查结果**：
  - ✅ 未发现硬编码的第三方 API Key（生产 Kotlin 代码中的域名均为默认 endpoint 或文档链接）
  - ⚠️ **`app/src/main/assets/` 下有两个 keystore 被 Git 跟踪**：`jks.jks`（2656 B）、`pkcs12.keystore`（2688 B）
    - 用途：`core/subpack/KeyStoreHelper.kt:180` —— 给"子包/APK 重打包"功能签名用的**通用调试 keystore，口令硬编码为 `"android"`**
    - 风险级别：低（不是发布签名密钥，仅用于应用内生成的子 APK），但 `.gitignore` 里明确写了 `*.keystore` 和 `*.jks`，这两个文件是历史强制入库的例外
- **轮换机制**：Release/Nightly 构建支持 **APK 签名密钥轮换**（`app/build.gradle.kts:36-90` `ApkRotationSigningConfig`，需要 `APK_ROTATION_LINEAGE_FILE` + build-tools 35.0.0 的 apksigner）
- **Web Chat / HTTP API 鉴权**：强制 Bearer Token，**未配置 Token 时直接拒绝服务**（`WebChatHttpBridge.kt:2566-2571` 返回 "Bearer token not configured"），这是正确的默认安全姿态

### 4) Reliability and Failure Behavior

- **重试/退避**：`LlmRetryPolicy`（`api/chat/llmprovider/LlmRetryPolicy.kt`）
  ```kotlin
  MAX_RETRY_ATTEMPTS = 5
  RETRY_BASE_DELAY_MS = 1000L
  nextDelayMs(n) = 1000 * (1 shl (n-1))   // 指数退避：1s, 2s, 4s, 8s, 16s
  ```
- **限流**：`SlidingWindowRateLimiter` + `RateLimiterRegistry`（按配置的 `request_limit_per_minute`）；`RequestConcurrencyRegistry` 控制 `max_concurrent_requests`
- **超时**：OkHttp 客户端级配置 + `JsTimeoutConfig.kt`（脚本执行超时）+ `ToolExecutionLimits.kt`（工具执行上限）
- **熔断器**：无
- **Fallback / 降级**：**按 `AGENTS.md` 明令禁止**。实测代码风格与之一致 —— 失败即抛异常或返回 `ToolResult(success=false)`，不做静默降级
  - 唯一的"多路尝试"是 `KeyStoreHelper.getOrCreateKeystore()`（PKCS12 → JKS → assets → 默认路径），属于历史代码，与 AGENTS.md 精神冲突
- **密钥池**：`ApiKeyProvider` + `ApiKeyPoolAvailabilityTester` 支持多 Key 轮换（这是可用性设计而非降级）

### 5) Observability for Integrations

- **外部调用日志**：有。`AppLogger`（357 文件）+ OkHttp `logging-interceptor` + `HttpLogSanitizer` 脱敏包装
- **Token 计量**：`TokenTrackingAIService` 装饰器 → `TokenUsageRepository` → Room 表（`TokenUsageRecordEntity` / `TokenStatsModelEntity`），UI 在 `ui/features/tokenstats/`（9 文件）
- **性能埋点**：`core/chat/logMessageTiming` / `messageTimingNow`（对话各阶段耗时）
- **崩溃与 ANR**：`GlobalExceptionHandler` + `CrashReportActivity` + `AnrMonitor` + `CrashRecoveryState`
- **工具执行可观测**：`ToolProgressBus.kt`（工具进度事件总线）、`JsExecutionTrace.kt`（脚本执行追踪）
- **缺失的可见性**：
  - 无分布式追踪/APM（单机应用，合理）
  - 无外部调用成功率/延迟指标聚合（只有单次日志与 Token 累计）
  - `exportSchema = false` 导致 Room schema 演进无法自动 diff 校验
  - MCP 子进程的健康检查依赖 `server_status.json` 缓存，非实时

### 6) Evidence

- `app/src/main/java/com/ai/assistance/operit/api/chat/llmprovider/`（26 provider + 限流 + 重试 + 装饰器）
- `app/src/main/java/com/ai/assistance/operit/api/{speech,voice}/`（STT 4 + TTS 12）
- `app/src/main/java/com/ai/assistance/operit/data/db/{AppDatabase,ObjectBox}.kt`
- `app/src/main/java/com/ai/assistance/operit/data/preferences/{ModelConfigManager,CodexAuthPreferences,ApiPreferences}.kt`
- `app/src/main/java/com/ai/assistance/operit/integrations/`（HTTP / A2A / Intent / Tasker）
- `app/src/main/java/com/ai/assistance/operit/core/subpack/KeyStoreHelper.kt:150-190`
- `app/src/main/java/com/ai/assistance/operit/data/security/PluginDenylistRepository.kt`
- `app/config/stt-model-assets.properties`、`.gitignore`
- 实测：`git ls-files app/src/main/assets/jks.jks app/src/main/assets/pkcs12.keystore` → 两文件均被跟踪