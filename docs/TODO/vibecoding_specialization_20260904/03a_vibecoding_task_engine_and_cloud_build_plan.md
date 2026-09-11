# 03A VibeCoding 任务引擎与云端构建发布工具 L3 修订计划

## 一、决策状态

- 用户于 2026-09-12 确认：把 `Vibe-coding-workflow-zh` 从提示词/Skill 文档升级为 App 内不可绕过的代码级流程。
- 产品唯一方向是 VibeCoding；不探索、不实现其他产品方向。
- 用户新增需求：除适合手机本地构建的项目（Go 默认本地）外，重量级构建统一交给 GitHub Actions；系统可创建已议定仓库或临时仓库，部署 CI，跟踪并修复到构建成功并发布 Release。
- 本文是 L3 计划修订和业务代码开工门禁，不构成业务源码修改授权。

## 二、本轮源码证据

1. 当前工作区已能随会话传递 `workspacePath`/`workspaceEnv`，证据入口包括 `EnhancedAIService.kt`、`ConversationService.kt`、`ChatEntity.kt`、`ChatDao.kt`。
2. 当前工具执行存在多入口：`ToolExecutionManager` 与大量直接 `AIToolHandler.executeTool()` 调用；因此流程门禁不能只放在 UI 或 Prompt。
3. 内置 `app/src/main/assets/packages/github.js` 当前只有 20 个工具，已有：仓库查询、Issue、PR、单文件提交、分支、本地编辑、终端；缺少：
   - 创建用户仓库
   - 工作流部署与 `workflow_dispatch`
   - Actions run/job 查询、日志下载、取消与失败重跑
   - Repository Secrets 公钥获取、LibSodium 加密写入
   - Release/资产查询与验收
4. 当前 GitHub 包读取 `EnvPreferences` 中的 `GITHUB_TOKEN`；`EnvPreferences.kt` 是普通 app-private `SharedPreferences`，不是加密凭据仓库。
5. 仓库已有旧加密实现参考：`CodexAuthPreferences.kt` 使用 `EncryptedSharedPreferences` + `MasterKey(AES256_GCM)`；但 2026-09-12 AndroidX 官方参考已将 `EncryptedSharedPreferences` 标记 deprecated，新实现不能复制该 API，应直接使用 Android Keystore 不可导出主密钥 + AES-GCM 信封加密保存 PAT。
6. 当前工作区已有 Android 云构建成功基线：`.github/workflows/android-release.yml`、版本门禁、签名、Release、失败日志跟踪和同提交瞬时故障重跑。

## 三、互联网校准（GitHub 官方文档）

校准日期：2026-09-12。

- 建仓：`POST /user/repos`；Fine-grained PAT 需要 `Administration: write`。
- 手动触发：`POST /repos/{owner}/{repo}/actions/workflows/{workflow_id}/dispatches`；workflow 必须含 `workflow_dispatch`，且触发入口必须存在于默认分支；需要仓库写访问与 `Actions: write`。
- 运行跟踪：Actions REST API 支持按 repo/workflow/head SHA 查询 run、job 和日志。
- 失败重跑：`POST /repos/{owner}/{repo}/actions/runs/{run_id}/rerun-failed-jobs`；Fine-grained PAT 需要 `Actions: write`。
- Secrets：必须先取仓库公钥，客户端用 LibSodium sealed box 加密后写入；Fine-grained PAT 需要 `Secrets: write`；API 永不回传明文。
- Release：发布与读取资产由 Releases REST API 完成；发布至少需要 `Contents: write`。若目标提交修改 workflow，还需要 `Workflows: write`。
- Workflow 内部优先使用仓库自带 `GITHUB_TOKEN`，并按 job 最小化 `permissions`；只有内置 token 无法覆盖的跨仓库/建仓操作才用 App 内账号凭据。
- GitHub 官方建议优先 Fine-grained PAT；长生命周期自动化可升级 GitHub App。当前为单人自用，第一版采用 Fine-grained PAT，保留 GitHub App 适配接口。

## 四、产品模型：固定 VibeCoding 状态机

旧通用工作流继续按 T02 决策删除。新引擎不是任意节点编辑器，而是固定、强类型、不可跳步的编码任务状态机：

```text
NEW
  -> CLARIFYING
  -> EXPLORING
  -> RESEARCHING
  -> PLANNING
  -> WAITING_PLAN_APPROVAL
  -> READY_TO_BUILD
  -> IMPLEMENTING
  -> VALIDATING
  -> DOCUMENTING
  -> REVIEWING
  -> LOCAL_BUILD | CLOUD_BUILD
  -> RELEASING
  -> COMPLETED
```

异常状态：`PAUSE_REQUESTED`、`PAUSED`、`STALLED`、`BLOCKED`、`FAILED`、`RECOVERING`、`CANCELLED`。

阶段转换由代码校验前置条件，不以模型文本判断：

| 转换 | 硬条件 |
|---|---|
| CLARIFYING -> EXPLORING | 目标、做/不做边界、验收标准、工作区齐全 |
| RESEARCHING -> PLANNING | 有来源记录；网络失败则显式 BLOCKED 或用户批准仅用本地证据 |
| PLANNING -> WAITING_PLAN_APPROVAL | 有版本化计划、风险等级、文件范围、验证和回滚方案 |
| WAITING_PLAN_APPROVAL -> READY_TO_BUILD | 用户批准当前 `planRevision`；计划变化立即使批准失效 |
| READY_TO_BUILD -> IMPLEMENTING | 用户手动切换 BUILD，模式快照有效 |
| IMPLEMENTING -> VALIDATING | 修改事件和影响范围已记录 |
| VALIDATING -> DOCUMENTING | 必要测试真实通过；失败回到 IMPLEMENTING |
| REVIEWING -> LOCAL_BUILD/CLOUD_BUILD | 构建策略代码判定并向用户显示依据 |
| RELEASING -> COMPLETED | Tag、Release、产物、校验和/签名（适用时）均验证通过 |

## 五、核心实体

```text
VibeCodingTask
RequirementSpec
ResearchRecord
TaskPlan + PlanRevision + PlanStep
ApprovalRecord
ToolExecutionContext
SessionTodo
EvidenceRecord
ValidationRun
DocumentUpdateRecord
BuildStrategyDecision
CloudBuildProfile
CloudBuildRun
CloudBuildAttempt
CloudArtifact
ReleaseEvidence
ExecutionCheckpoint
TaskEvent
```

所有实体按 `taskId`、`sessionId`、`workspaceId` 关联；关键状态使用数据库单一事实源和事件流驱动 UI。

## 六、统一云端工具

### 6.1 对外只暴露一个工具

稳定协议 ID：`cloud_build_release`

通过 `action` 参数承载受控生命周期，而不是暴露十几个 GitHub 低级工具：

```text
inspect      只读识别技术栈、成本和所需 Secrets（PLAN 可用）
prepare      生成仓库/CI/发布计划，不修改远端（PLAN 可用）
start        经批准后创建/选择仓库、上传源码、部署 CI、触发构建（BUILD）
status       查询 run/job/日志/产物/Release（PLAN、BUILD 可用）
resume       Agent 修复代码后推送新 revision 并继续闭环（BUILD）
retry        仅对已证明为 Runner/网络瞬时故障的同 SHA 重跑（BUILD）
cancel       取消运行，不删除仓库（用户或获准 Agent）
cleanup      删除/归档临时仓库；高危，必须用户逐次确认
```

它是一个工具，但内部由小组件实现，禁止做成不可测试的巨型函数。

### 6.2 构建策略

| 技术栈/条件 | 默认后端 |
|---|---|
| Go，中小项目且工具链已存在 | LOCAL；失败原因是缺工具链/资源时可切 CLOUD |
| Python/Node/Java 小项目，依赖已缓存且预计资源可承受 | LOCAL 优先 |
| Android APK/AAB、Flutter Android | CLOUD 默认 |
| Rust/C++/NDK、大型 Gradle/Maven、Docker、多架构 | CLOUD 默认 |
| 未知项目 | `inspect` 后向用户展示依据，不猜测 |

判定输入：项目标识文件、依赖锁、预计下载量、磁盘/内存、历史构建时长、是否需要 SDK/NDK/CMake/Docker、用户强制选择。判定结果持久化为 `BuildStrategyDecision`。

### 6.3 仓库模式

1. `EXISTING`：使用对话已确认的现有仓库。
2. `DEDICATED`：首次创建长期私有项目仓库，后续复用。
3. `TEMPORARY`：默认创建 private 临时仓库，命名 `vc-build-{task}-{timestamp}`，写入用途和任务 ID；完成后只标记“待清理”，不自动删除。

建仓、选择公开可见性、覆盖 workflow、写 Secrets、合并、发布、清理均生成显式审批记录。临时仓库默认 private；public 必须用户明确选择。

### 6.4 CI 模板注册表

`CloudBuildTemplateRegistry` 第一阶段支持：

- `android-gradle-apk`
- `flutter-android`
- `node-web`
- `python-package`
- `jvm-gradle`
- `jvm-maven`
- `rust-binary`
- `generic-command`

每个模板声明：检测规则、runner、工具链版本、缓存键、测试命令、构建命令、产物 glob、签名需求、Release 规则。第三方 Actions 固定完整 commit SHA；模板有内部版本号和来源证据。

### 6.5 完整执行闭环

```text
inspect workspace
-> 决定 local/cloud
-> 检查 GitHub 身份与权限（不输出 token）
-> 选择/创建 private repo
-> 生成 .gitignore + workflow + release manifest
-> 展示远端写入计划并取得当前 planRevision 审批
-> 上传源码（首选 git push；小仓网络故障才允许 Git Data API fallback）
-> workflow_dispatch
-> 按 runId/headSha 跟踪 job
-> 拉取失败步骤与收窄日志
-> 分类：INFRA / CODE / CONFIG / SECRET / QUOTA / UNKNOWN
-> INFRA：同 SHA 重跑
-> CODE：回到 IMPLEMENTING，由 Agent 修复、验证、提交新 revision
-> CONFIG/SECRET：请求所需配置，不猜值
-> 相同失败指纹连续出现触发熔断
-> 测试和构建通过
-> 校验产物名称、大小、digest、版本、签名（适用时）
-> workflow 使用最小 GITHUB_TOKEN 创建 Release
-> App 复核 Release 和资产
-> 生成 ReleaseEvidence
-> COMPLETED
```

### 6.6 “直到成功”的安全定义

系统默认持续修复而不是一次失败就退出，但不能无限烧 Actions 配额：

- Runner/网络瞬时故障：同 SHA 可自动重跑，默认最多 3 次。
- 代码失败：Agent 可修复并提交新 revision；每次必须有差异和新失败指纹/进展。
- 相同失败指纹连续 2 次、总代码修复 5 次、Actions 配额不足、凭据不足、签名缺失、需求矛盾时进入 `BLOCKED`，向用户报告证据并等待决策。
- 用户可选择继续增加尝试预算；不伪造“无限重试”。
- 非幂等操作使用 idempotency key：`taskId + planRevision + sourceSha + templateVersion`。

### 6.7 产物与 Android 签名语义

`Release` 是 GitHub 的交付容器，不代表其中 Android APK 自动具备长期升级身份。工具必须明确区分：

1. `TEST_ARTIFACT`：临时验证使用 debug/test 签名或无签名产物；可上传 GitHub Release，但 UI 必须标注“测试产物，不保证覆盖升级”，不得声称正式发布。
2. `PROJECT_RELEASE`：长期项目使用该项目独立、固定的 release keystore；首次生成/导入、仓库外备份、证书指纹确认和写入 GitHub Secrets 均需用户明确授权。后续版本必须复用同一签名身份。
3. `EXISTING_SIGNING`：已有项目只引用用户确认的现有 GitHub Secrets 名称；App 不读取 Secrets 明文，构建后验证 APK 证书指纹与项目记录一致。

临时仓库默认 `TEST_ARTIFACT`；没有可恢复的固定密钥时不得自动升级为 `PROJECT_RELEASE`。每个技术栈的 Release 资产必须声明 `artifactKind`、版本、digest、是否签名、签名身份（仅公开指纹）和可安装/可升级语义。

## 七、安全模型

1. 新建 `GitHubCredentialStore`：使用 Android Keystore 生成不可导出的 AES-GCM 主密钥，凭据密文/IV/版本写入 app-private 存储；不复制已废弃的 `EncryptedSharedPreferences`。禁止继续把 `GITHUB_TOKEN` 明文放入 `EnvPreferences`。
2. Token 永不进入 Prompt、工具结果、日志、TODO、证据包、检查点或 Git。
3. 第一版 Fine-grained PAT 权限按功能预检：
   - 建仓：Administration write
   - 源码/workflow/Release：Contents write + Workflows write
   - Actions：Actions read/write
   - Secrets：Secrets write
4. Workflow 内只用最小 `GITHUB_TOKEN`；构建 job `contents: read`，发布 job 才 `contents: write`。
5. Secrets 客户端用仓库公钥 + LibSodium sealed box 加密；不提供读取明文功能。
6. 外部仓库写操作只在 BUILD 且审批的 `planRevision` 下执行；PLAN 中 `inspect/status/prepare` 只读。
7. 禁止上传 `.git`、构建缓存、私钥、`.env`、签名文件和用户未批准的大文件；上传前运行 secret scan 和大小清单。
8. 仓库删除、Release 删除、Tag 移动、force push 永不自动执行。

## 八、上传与网络降级

- 首选：终端 `git push`，保留提交图和大仓效率。
- 降级：GitHub Git Database API（blob/tree/commit/ref），仅用于小型源码快照且受 API 限制。
- 禁止用 Contents API 手工重建大型仓库：本项目 T02 已实测发生长内容 typo 与本地/远端分叉。
- 两条通道均失败时进入 `BLOCKED_NETWORK`，不假装已上传。
- 上传前生成 manifest（相对路径、大小、SHA-256），上传后抽样/全量复核 tree SHA。

## 九、分阶段实施

### Phase A：任务状态机纯领域层

- 新建状态、实体、转换守卫、事件模型和纯 JVM 单测。
- 不接数据库、不接 UI、不执行工具。

### Phase B：持久化与会话模式

- 按会话持久化 task/mode/plan revision/approval。
- 允许不兼容数据库重建，但实施前再次获得数据库明确授权。

### Phase C：统一工具上下文与硬门禁

- `ToolExecutionContext` 贯穿主代理、QuickJS、ToolPkg、MCP、Skill、Subagent。
- `ToolPolicyGate` 在 Executor 激活前 fail closed。

### Phase D：需求、研究、计划、TODO、证据纵向切片

- 跑通“澄清 -> 探索 -> 研究 -> 计划 -> 审批 -> Build -> 验证 -> 六段式交付”。

### Phase E：云端工具只读能力

- `cloud_build_release(action=inspect|prepare|status)`。
- 技术栈识别、模板预览、权限预检、运行/Release 查询；不写远端。

### Phase F：云端工具写入闭环

- 安全凭据仓、建仓、上传、workflow、dispatch、跟踪、日志、重跑、Secrets、Release 验收。
- 先用无 Secrets 的小型 Node/Java 样例，再做 Android 签名样例。

### Phase G：恢复、Subagent 与修复循环

- 持久化 `CloudBuildRun` 和 attempt；App 被杀后按 runId/headSha 恢复。
- Agent 修复循环受尝试预算、失败指纹和审批版本约束。

### Phase H：删除非 VibeCoding 能力并收敛 UI

- 新内核与云构建闭环稳定后，按 T02 矩阵分波删除通用能力。

## 十、验证矩阵

### 状态机

- 非法跳阶段全部拒绝。
- 计划修改使旧审批失效。
- 两会话/两任务状态互不串扰。
- Agent 无模式切换 API。

### 云端工具

- PLAN 下 `inspect/prepare/status` 允许，`start/resume/retry/cleanup` 拒绝。
- 无 token、权限不足、token 过期、repo 重名、默认分支无 workflow、Secrets 未配置均给出精确错误。
- 新建 private 临时仓库，部署 workflow，触发并绑定正确 runId/headSha。
- 代码失败能拉取目标 job 日志并生成稳定失败指纹。
- Runner 瞬时故障同 SHA 重跑；代码变化使用新 SHA。
- App 重启后恢复跟踪，不能重复建仓/重复发布。
- 同一 source SHA/template/version 不能生成两个互相冲突的 Release。
- Release 资产 digest、版本、签名（Android）和 workflow run 证据一致。

### 代表项目

- Go：本地构建成功；资源不足时可转云端。
- Android：云端签名 APK + Release。
- Flutter Android：云端 APK + Release。
- Node/Web：测试、构建产物、浏览器预览证据。
- Java/Gradle：测试 + jar/apk（按项目）Release。
- 一个故意失败项目：Agent 修复后第二次 run 成功。

## 十一、预计影响范围

第一批业务实现候选（最终在各 Phase 开工前再精确确认）：

- 新目录 `core/vibecoding/domain/`
- 新目录 `core/vibecoding/runtime/`
- 新目录 `core/vibecoding/policy/`
- 新目录 `core/vibecoding/evidence/`
- 新目录 `core/vibecoding/cloudbuild/`
- `ChatEntity.kt`、`ChatDao.kt`、`AppDatabase.kt`
- `ChatRuntimeSlot.kt`、`ChatRuntimeHolder.kt`
- `ToolExecutionManager.kt`、`AIToolHandler.kt`
- `ChatViewModel.kt` 与输入区 UI
- `EnvPreferences.kt`（迁出 GitHub token，不删除通用 env 能力）
- 新 `GitHubCredentialStore.kt`
- GitHub REST client（原 `github.js` 只作为低级包保留；云构建内核不依赖模型连续调用 20+ 低级工具）
- CI 模板资源、测试夹具、中文 UI 文案

## 十二、回滚

- 每个 Phase 独立分支、独立版本、独立 PR 和 Release。
- Phase A 纯领域层可直接 `git revert`，不影响旧流程。
- 数据库 Phase 回滚通过更高版本 revert + 清除 App 数据；不编写旧数据降级迁移。
- 云端写入功能先 feature-disabled，只读验收通过后再启用 `start`。
- 远端创建的测试仓库默认 private，不自动删除；回滚时禁用工具并保留仓库证据，删除须用户确认。
- 任何凭据泄漏迹象立即禁用工具、撤销 token、停止发布；不在日志中复述凭据。
- 若云构建闭环不稳定，本地 Go/轻量构建保持可用，重量级构建显示“云端构建暂不可用”，不得静默退回手机强行编译。

## 十三、开工与实施状态

- 2026-09-12：用户明确确认 Phase A 按计划开工。
- Phase A 当前范围：纯 Kotlin 领域模型、状态机转换守卫和 JVM 单测；不改数据库、不接 UI、不注册工具、不访问真实 GitHub/Secrets。
- 当前实现分支：`feature/vibecoding-task-engine-phase-a`；PR #3 的 Fast checks、Android JVM tests、Android build 和 Candidate checks 已全部通过。
- Phase A 已实现：固定阶段与异常状态、结构化需求/研究/计划/审批/验证/文档/审查/构建/Release 证据、用户独占模式和审批、旧审批失效、构建后端与 SHA 一致性、恢复写权限复核；共 21 个 JVM 单测。
- 尚未合并/发布；合并后由 Android Release 生成 v0.1.0，并在 Release 成功后标记 Phase A `[DONE]`。
- Phase B 数据库改造仍需再次明确授权。

Phase A 已确认的门禁：

1. 固定 VibeCoding 状态机代替“仅靠 Skill 提示词”的方向。
2. 单一工具 ID `cloud_build_release` + `action` 生命周期接口。
3. 临时仓库默认 private、不自动删除。
4. 第一版账号模式为 Fine-grained PAT，加密存储；后续可升级 GitHub App。
5. 自动修复有熔断预算，不承诺无限消耗 Actions。
6. Phase A 先只写纯领域模型与测试，不改数据库、不调用真实 GitHub。
