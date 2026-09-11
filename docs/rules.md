# 编码规范（Rules）

> 状态：L3 重构规则基线。
> 本文件对后续所有设计、代码、测试和文档生效。若临时需求与本规则冲突，必须先由用户明确修改规则，不能由 Agent 自行解释绕过。

## 默认规则（创建即生效）

- 以 [`goal.md`](goal.md) 定义的 VibeCoding 闭环为唯一产品边界
- 先核对事实，再形成结论；不确定的信息标记为“未知”并停止相关实现
- 新架构优先建立统一领域模型和单一事实源，不向现有巨类继续堆补丁
- Plan/Build、TODO、Subagent、暂停恢复必须共享同一套会话与任务上下文
- 删除功能之前先建立能力矩阵和反向依赖证据，不按目录名、类名或 UI 名称盲删
- 所有代码改动按最小可验证单元实施；一个单元只解决一个明确问题
- 当前项目不承担旧数据、旧数据库、旧设置或旧接口的兼容义务
- 当前 GitHub 源码是一次性原始基线；开始定制后，仓库文档、测试和构建证据是新系统的事实源
- 不与其他项目建立架构、业务、命名或记忆关联

- VibeCoding 主流程代码化：需求、探索、研究、计划、用户审批、实施、验证、文档、证据与发布必须由强类型状态机和转换守卫驱动，不得只依赖 Skill/Prompt 文本
- 固定 VibeCoding 状态机不是可配置的通用工作流；只允许配置项目构建模板、验证命令、风险策略和用户批准的远端目标
- 云端构建统一对外协议为 `cloud_build_release`；PLAN 仅允许 `inspect`/`prepare`/`status`，其余远端副作用 action 仅允许 BUILD + 当前 `planRevision` 审批
- 构建策略必须保存证据：Go/可承受轻量项目本地优先；Android、Flutter Android、NDK、大型 Gradle/Maven、Rust/C++、Docker/多架构默认 GitHub Actions；未知项目不得猜测
- 临时仓库默认 private、不自动删除；public、Secrets 写入、Workflow 覆盖、发布、清理必须显式审批，仓库/Tag/Release 删除永不自动执行
- 自动修复必须有失败分类、失败指纹和熔断预算；同 SHA 只用于基础设施瞬时故障重跑，代码变化必须产生新 SHA
- GitHub 账号凭据必须使用 Android Keystore 不可导出主密钥 + AES-GCM 信封加密，禁止复制已废弃的 `EncryptedSharedPreferences`，也禁止继续使用普通 `EnvPreferences` 存放 `GITHUB_TOKEN`；凭据永不进入 Prompt、日志、TODO、证据或检查点
- 首选 `git push` 上传；Git Data API 仅作为小型仓库降级通道；禁止用 Contents API 手工重建大型仓库
- 以 [`03a_vibecoding_task_engine_and_cloud_build_plan.md`](TODO/vibecoding_specialization_20260904/03a_vibecoding_task_engine_and_cloud_build_plan.md) 作为 T03-T12 修订实施基线

## 命名规范

### 领域名称

- 会话模式统一使用 `CodingSessionMode.PLAN` 和 `CodingSessionMode.BUILD`
- 当前编码任务清单统一称为 `SessionTodo`，不能称为计划文档
- 仓库内长期计划统一称为 `ImplementationPlan` 或文档计划，不能与 `SessionTodo` 混用
- 工具执行上下文统一称为 `ToolExecutionContext`
- 模式硬权限统一称为 `CodingModePolicy`
- 工具执行门禁统一称为 `ToolPolicyGate`
- 子代理任务统一称为 `SubagentTask`
- 可恢复执行点统一称为 `ExecutionCheckpoint`
- 模型能力统一称为 `ModelCapability`
- 工具显示信息统一称为 `ToolDisplayMetadata`

### Kotlin

- 类型、Composable 和文件名使用 PascalCase
- 函数、属性和参数使用 camelCase
- 常量使用 UPPER_SNAKE_CASE
- 布尔值以 `is`、`has`、`can` 或 `should` 开头
- 状态类型优先使用 sealed interface / sealed class 或 enum，禁止用无约束字符串表达核心状态
- 数据库字段和协议字段必须使用稳定英文 ID；简体中文只作为显示文本

### 工具协议

- `read_file`、`package_proxy`、`super_admin:terminal` 等工具 ID 是稳定协议，不得为了中文化而改名
- UI 通过 `ToolDisplayMetadata` 映射为中文名称、动作、对象和结果
- 动态工具统一保留原始 `sourceId:toolId`，显示层解析来源和动作；无法识别时显示“来源 + 原始 ID”，不得编造翻译

## 代码风格

- 业务逻辑与 Compose UI 分离；Composable 不直接执行工具、数据库迁移或 Provider 请求
- 状态由上层持有并向下传递，事件向上传递
- 公共 Composable 提供 `modifier: Modifier = Modifier`，应用在根节点
- 新增关键 UI 组件必须提供简体中文 Preview，至少覆盖主要状态
- 避免继续扩大以下既有巨类：`EnhancedAIService`、`ChatViewModel`、`ToolRegistration`、`PackageManager`、`SystemToolPromptsInternal`
- 新内核采用小型、可测试组件，通过接口连接现有代码；迁移完成后再删除旧路径
- 配置集中定义，不把工具分类、模式权限或模型能力规则散落在 UI 和 Provider 中
- 生成文件和构建产物不得手工修改：`examples/*.js`、`app/src/main/assets/packages/*`、Web Chat assets、Generated Renderers

## 模式与权限规则

### 模式所有权

- Plan/Build 只能由用户通过明确的 UI 操作切换
- Agent、Subagent、ToolPkg、MCP、Skill、Prompt、工作流和任何工具都没有切换模式的 API
- 不向模型暴露 `set_mode`、`enter_build`、`exit_plan` 或语义等价工具
- 模式按会话持久化；并行会话独立，禁止全局模式布尔值
- 一次模型响应及其派生工具调用使用发送时捕获的模式快照；切换模式不追溯改变正在执行的调用
- 用户从 Build 切到 Plan 时，所有尚未开始的写操作立即失去执行资格；正在执行且不可安全中断的操作必须明确显示状态并在完成后进入 Plan

### Plan 允许能力

- 读取、列出和搜索工作区文件
- Git 状态、日志、差异、分支列表和其他经分类的只读查询
- 只读终端命令
- 互联网搜索、网页读取、依赖源码和文档研究
- 只读 Subagent
- 当前会话 `SessionTodo` 的创建和状态更新
- 向用户提问、展示分析和形成计划

### Plan 禁止能力

- 创建、编辑、删除、移动、覆盖或改权限的文件操作
- 终端中的重定向写入、管道到写入程序、`tee`、`sed -i`、脚本解释器写文件或其他间接写入
- 安装、更新或卸载依赖、软件、系统包和模型
- 改变 Git 工作树、索引、分支、标签、远端、stash 或提交历史
- 启动会修改项目或外部系统的构建、格式化、代码生成和部署任务
- 修改 Android 系统、其他 App、远端仓库、数据库或外部服务
- 调用执行型 Subagent
- 通过 QuickJS、ToolPkg、MCP、Skill、工作流、`package_proxy`、终端或直接 Executor 间接执行上述操作

### 硬门禁实现规则

- 工具是否出现在 Prompt 中只是用户体验优化，不是权限边界
- UI 是否隐藏按钮不是权限边界
- `ToolExecutionManager` 的检查不是唯一边界，因为存在大量 `AIToolHandler.executeTool()` 直接调用点
- 所有 Agent 发起的工具执行必须携带 `ToolExecutionContext`，至少包含：`sessionId`、`modeSnapshot`、`actorType`、`actorId`、`taskId`、`workspaceId`
- 所有执行来源必须在 Executor 激活前经过同一个 `ToolPolicyGate`
- 自动执行路径缺少上下文时 fail closed；用户在独立工具页面主动点击的本地操作使用明确的 `USER_DIRECT` 来源，不伪装成 Agent 调用
- 工具代理和包装器必须校验最终目标工具，而不只校验 `package_proxy`、Shell 或 MCP 外壳
- Plan 的 deny 规则是父会话和所有后代任务的硬上限；子代理只能进一步收紧，不能覆盖或放宽
- 权限判定、拒绝原因和调用来源必须写入可审计事件

### 只读终端规则

- 不能仅靠命令字符串关键字黑名单判定只读，因为 shell 可以组合和间接写入
- Plan 终端应使用专用只读执行策略：受控命令/参数分类、禁止重定向和命令替换的写路径，并限制工作目录与环境副作用
- 无法证明命令只读时一律拒绝，并提示用户手动切换 Build
- 只读命令的首批范围至少覆盖项目检查所需的 `pwd`、`ls`、`find`、`grep`/`rg`、`cat`、`head`、`tail`、`sed -n`、`wc`、`stat`、`file`、`git status/log/diff/show/branch --list` 和构建工具的纯查询参数
- 具体命令语法必须由解析器和测试确定，本规则不把上述示例视为自动放行实现

## 运行时 TODO 规则

- `SessionTodo` 是会话运行时结构化数据，不是 Markdown 文件
- 仓库 `docs/plan.md` 和 `docs/TODO/` 负责长期工程追踪，不出现在普通任务 TODO Dock 中
- 复杂任务或包含三个以上独立步骤的任务应主动创建 TODO；简单问答不创建
- TODO 数据至少包含稳定 ID、内容、状态、优先级、顺序、所属会话和更新时间
- 状态至少包括：`PENDING`、`IN_PROGRESS`、`COMPLETED`、`CANCELLED`
- 有未完成工作时只能有一项主 TODO 为 `IN_PROGRESS`
- 开始工作前立即更新为进行中，完成并通过必要验证后立即更新为已完成；禁止最后批量补状态
- 工作受阻时保持当前项进行中，并新增明确的阻塞处理项或记录阻塞原因
- 新用户指令到达时，先合并或调整 TODO，再继续执行
- TODO 更新写入会话存储并发布事件；UI 订阅事件实时更新，不从模型文本正则解析状态
- TODO Dock 位于聊天输入区附近，支持折叠、展开、完成数/总数、当前任务预览和完整列表
- 切换会话时只显示对应会话 TODO；全部完成或列表为空时 Dock 自动收起
- 默认只有主代理维护父会话 TODO；Subagent 维护自己的任务状态，不直接改写父清单，除非通过受控的父任务同步接口

## Subagent 规则

- Subagent 是专业任务执行器，不等同于角色卡、多角色聊天或手机 UI 子代理
- 每个 Subagent 任务创建独立子会话，记录 `parentSessionId` 和 `parentTaskId`
- 主代理通过受控 `task` 能力委派，不直接共享可变运行状态
- 支持前台等待和后台并行；后台任务完成、失败或取消时主动向父会话发送结构化事件，禁止主代理轮询
- 支持用既有 `taskId` 继续同一个子会话，不能为“继续”悄悄新建重复任务
- 默认禁止 Subagent 修改父会话 TODO，也默认禁止继续创建嵌套 Subagent；只有用户明确配置才放开深度
- Plan 模式只能调用只读 Subagent，且父会话 deny 是不可突破上限
- Build 模式的执行型 Subagent 仍受工作区边界、用户工具审批和任务授权范围限制
- 子代理不得同时修改重叠文件；调度器在并行前检查声明的文件/模块作用域
- 子代理结果必须返回状态、摘要、证据、变更范围和未完成项，不只返回自然语言结论

## 暂停、卡顿与恢复规则

- “暂停”必须暂停任务编排，不等同于把 HTTP Socket 永久挂起
- 用户可以随时请求暂停；Agent 无权自动取消用户暂停状态
- 暂停后不启动新工具、不派生新 Subagent、不推进新 TODO
- 已开始的原子文件写入不得中途截断；完成当前原子操作后进入暂停
- 可安全取消的网络请求和长任务应取消并记录检查点
- `ExecutionCheckpoint` 至少记录：会话、模式快照、当前 TODO、已完成步骤、进行中的工具/子任务、最后可靠消息位置、可继续动作和失败原因
- 网络卡顿与用户暂停是不同状态：卡顿由超时监测识别，暂停由用户控制
- UI 必须显示“正在暂停”“已暂停”“网络无响应”“可继续”或“不可恢复及原因”
- 恢复时从结构化检查点继续，不能伪造底层 SSE 的原地续传
- 若恢复需要新模型请求，必须在 UI 明示会重新请求及可能产生额外 Token
- 已产生副作用的工具在恢复前必须检查幂等性；无法证明安全时请求用户确认

## 模型能力规则

- 模型能力来自服务端响应、Provider 官方接口或用户明确配置，不能根据模型名称猜测
- `ModelCapability` 至少区分：上下文长度、最大输出、支持参数、推理控制、输入模态、来源、获取时间和可信状态
- 服务端没有返回的字段显示“未知”，不填虚假默认值
- 原始 `/models` 响应与标准化能力分离，便于诊断 Provider 格式差异
- Provider 适配器只解析自身有证据的字段，不在公共 UI 中散落格式判断
- 思考强度控件只在模型明确声明支持时显示，并严格使用服务端支持的可选值
- 视觉、音频和视频模态不作为产品功能保留；服务端即使声明支持，也不自动启用对应 UI
- 能力缓存必须可刷新并显示最后更新时间；更换 endpoint、Key 或 Provider 后缓存失效

## 简体中文与可观察性规则

- 用户可见的模式、TODO、工具动作、Subagent 状态、权限拒绝、恢复状态和错误信息使用自然简体中文
- 不直接把协议 ID 作为主标题显示；原始 ID 放在可展开的调试详情中
- 中文显示必须描述“正在做什么”和“作用对象”，例如“读取文件：docs/goal.md”，不能只显示“文件工具”
- `package_proxy` 显示最终目标包和工具，例如“调用扩展工具：super_admin / 终端执行”
- 动态 ToolPkg、MCP 和 Skill 提供显示元数据；缺少中文名称时显示来源与原始 ID，明确标注“未本地化”
- 用户界面只保留简体中文产品体验；协议、源码标识和必要技术日志继续使用稳定英文
- 删除其他语言资源前，先确认构建、第三方协议和代码生成不依赖这些资源目录

## 错误处理

- 禁止静默吞错、伪造成功或把未知状态显示为完成
- 错误必须包含可定位上下文：会话、模式、工具、任务、阶段和原始原因
- 权限检查异常 fail closed，不允许工具继续执行
- 模型能力解析失败只将对应字段标记未知，不污染其他已验证字段
- TODO 只有在真实工作和验证均完成后才能标记完成
- 子代理失败必须通知父会话并保留子会话证据
- 恢复不可行时明确说明原因，不自动执行可能重复产生副作用的操作
- 允许显式、用户可见且经过设计的错误恢复；禁止无提示的隐式降级

## 版本与发布规则

### 唯一版本源

- 仓库根目录 `VERSION` 是发布版本的唯一事实源，格式严格为无前导零的 `MAJOR.MINOR.PATCH`；当前定制起始版本固定为 `0.0.0`
- 初期不使用预发布后缀或构建元数据；确有需要时必须先修改本规则和校验脚本
- Gradle 必须读取 `VERSION` 生成 App `versionName`，禁止在 `app/build.gradle.kts` 或其他文件重复硬编码版本名
- App `versionCode` 必须由同一 SemVer 确定性生成：`major * 1_000_000 + minor * 1_000 + patch + 1`；`minor` 和 `patch` 必须小于 `1000`，结果不得超过 Android 上限
- 因此 `0.0.0` 对应 `versionName = 0.0.0`、`versionCode = 1`；仓库版本、App 版本、Tag、Release 和 APK 文件名形成可机器校验的一一映射

### 版本推进

- 每次推送到 `main` 的新发布批次都必须先递增 `VERSION` 并同步新增对应 `CHANGELOG.md` 章节；没有版本变化的发布批次由 CI 拒绝
- 同一提交因 GitHub Runner 或网络瞬时故障而重新运行 Workflow，可以保持原版本；只要源码、构建脚本、Workflow 或文档发生任何修复并形成新提交，就必须使用更高且从未使用的新版本
- 一个 SemVer 只能绑定一个 Git commit；禁止移动已创建的版本 Tag、覆盖已发布资产、让同一版本代表不同源码，或把版本号降回历史值
- `0.y.z` 处于初始开发期：新增功能通常提升 minor，修复或文档/CI 调整提升 patch，破坏性阶段变更由发布前计划明确；无论类别如何，新值必须严格大于上一批次

### GitHub Actions 与 Release

- GitHub Actions 是本仓 Android 测试、构建、签名和发布的唯一权威环境；本地只负责源码编辑、静态检查、版本推进、Git 提交与推送
- 每次 `main` 推送依次执行版本门禁、JVM 测试、Release APK 构建、APK 元数据校验、签名校验、SHA256 生成和 GitHub Release 发布；任一前置失败均不得发布
- Git Tag 与 Release 名称统一为 `v{VERSION}`，发布 APK 文件名统一为 `minibile-v{VERSION}-arm64-v8a.apk`，并附带同名 `.sha256` 校验文件
- Release Notes 必须来自当前版本的 `CHANGELOG.md` 章节；不得发布空说明或与当前提交不符的自动拼接内容
- 发布 Job 单独使用最小 `contents: write` 权限；测试和构建 Job 保持 `contents: read`，第三方 Action 固定到完整提交 SHA
- Agent 必须持续读取 GitHub Actions 状态和失败日志，区分瞬时故障与代码故障；同一提交可重跑，代码修复则提升版本后提交，直到 Release 中存在校验通过的 APK 才报告成功

### 签名密钥

- 自定义应用使用一套独立、固定、长期有效的 Release 签名密钥；仓库现有 `assets/jks.jks` 和 `pkcs12.keystore` 仅用于子包调试签名，严禁复用为 App Release 密钥
- Release 私钥、keystore Base64、store password、alias password 只存于用户批准的仓库外备份和 GitHub Actions Secrets，绝不写入 Git、文档、Artifact 或日志
- 首次发布前必须验证离线备份可恢复，并记录不敏感的 SHA256 与签名证书 SHA-256 指纹；无法证明密钥可恢复时禁止发布
- 每个 Release APK 必须由 CI 使用固定证书签名，并通过 `apksigner verify --verbose --print-certs`；证书指纹不等于已登记指纹时发布失败
- 自定义 App 的安装包 ID 固定为 `io.github.black0bag.minibile`，与当前 `com.ai.assistance.operit` 并行安装；Android `namespace` 和 Kotlin 源码包暂时保留 `com.ai.assistance.operit`，禁止为了改安装身份做全仓包声明迁移
- 依赖安装包 ID 的测试、脚本、广播 action/component 和外部目录必须逐项迁移；动态使用 `context.packageName`、`BuildConfig.APPLICATION_ID` 或 Manifest `${applicationId}`，禁止无差别全局字符串替换
- Release keystore 固定保存在 Git 仓库外的工作区根级 `../.release-signing/minibile-release.jks`，目录权限 `700`、文件权限 `600`；复制工作区时必须包含该隐藏目录
- 工作区仍属于当前 Operit App 的私有数据；清除数据、卸载当前 App 或删除工作区会删除 keystore，GitHub Secrets 也不是可下载备份，该风险由工作区根级 `../WORKSPACE.md` 持续记录
- 包身份一经首个 Release 发布即冻结；从首个自定义版本起，后续版本必须使用同一 `applicationId` 和同一签名直接覆盖升级，修改身份需作为新的 L3 迁移任务

## 日志与注释

- 日志统一使用 `AppLogger`，不新增直接 `android.util.Log`
- 模式切换、策略判定、工具拒绝、TODO 更新、子代理生命周期和检查点恢复均记录结构化日志
- 日志不得记录 API Key、Token、Cookie、完整敏感请求或用户私有源码内容
- URL 和请求日志使用现有脱敏设施
- 注释解释“为什么”和安全边界，不复述代码动作
- 临时调试日志在功能完成前删除或降级为受控诊断日志

## 安全与隐私

- API Key、OAuth Token 和终端凭据不得写入源码、文档、测试夹具或普通日志
- 新模型配置应使用加密存储；现有数据无需迁移时可以直接重建存储结构
- Plan 权限规则不依赖模型遵守 Prompt，必须由本地代码执行
- MCP、ToolPkg 和 Skill 属于不受信输入，其能力按最终工具效果分类并经过同一门禁
- 工作区外路径默认拒绝；用户明确授权后按会话记录范围
- Subagent 只获得完成任务所需最小权限和最小上下文
- 用户直接操作与 Agent 自动操作在审计记录中明确区分

## 测试与回归要求

- GitHub Actions 尚未在原始提交派生的 `0.0.0` 版本上完成版本校验、JVM 测试、固定证书签名构建和 Release 发布前，不开始业务源码删改；本地无需安装 Android 构建工具链
- 改造前记录原始构建、测试、APK 体积、模块数、权限数、路由数和工具数基线
- 每个发布批次必须测试版本递增、版本格式、历史 Tag 冲突、CHANGELOG 章节、APK `versionName`/`versionCode`、Tag 目标提交、Release 名称、APK 文件名、SHA256 和签名证书指纹
- 每个实施单元至少覆盖主流程、一个拒绝/失败分支和一个模式隔离场景
- Plan 门禁必须包含绕过测试：直接 `AIToolHandler`、`package_proxy`、终端、QuickJS、MCP、ToolPkg、工作流、Subagent、并行会话
- TODO 必须测试状态顺序、会话隔离、事件更新、切换会话和完成后收起
- Subagent 必须测试父子关系、权限继承、深度限制、后台通知、取消和继续既有任务
- 暂停恢复必须测试原子写、网络卡顿、重复副作用、前后台子任务和模式切换
- 模型能力必须使用保存的真实响应夹具测试不同 Provider 和缺失字段
- 中文显示必须测试内置工具、包代理、MCP、ToolPkg 和未知动态工具
- 每波删除后运行编译、相关单测和真机烟测，确认无残留入口、权限和依赖
- 测试结果必须写入对应 `docs/TODO` 步骤文档，完成后加 `[DONE]`

## 文档规则

- `docs/goal.md` 记录长期产品目标和边界
- `docs/plan.md` 记录经用户确认的 L3 总实施计划
- `docs/rules.md` 记录长期工程铁律
- `docs/structure.md` 记录当前架构事实和目标边界
- `docs/TODO/vibecoding_specialization_20260904/` 记录本次大型改造的可执行步骤
- `SessionTodo` 只属于应用运行时，不写入上述 Markdown 文档
- 文档不得把候选方案写成已经实现的事实
- 每个步骤文档写明旧实现、目标、作用域、验证、风险和回滚；真正完成后才加 `[DONE]`

## 禁止事项（反模式）

- 禁止 Agent 自动切换 Plan/Build
- 禁止仅靠 Prompt、工具隐藏或 UI 开关实现 Plan 安全
- 禁止使用全局模式变量导致并行会话串扰
- 禁止子代理扩大父会话权限
- 禁止把运行时 TODO 实现为 Markdown 文件
- 禁止批量把 TODO 标记完成
- 禁止把协议工具 ID 直接翻译或改名
- 禁止根据模型名称猜上下文和思考强度
- 禁止声称 SSE 可以在任意断点原地续传
- 禁止按目录整块删除同时服务编码场景的底层能力
- 禁止为了快速通过编译保留空壳、死路由或无行为占位实现
- 禁止在一个提交中混合多个删除波次、数据库重建和核心新功能
- 禁止手工维护多份版本号、无版本递增发布、复用或移动版本 Tag、覆盖已发布 APK，或让同一版本绑定多个提交
- 禁止使用 Runner 临时 Debug 密钥或仓库现有子包测试密钥签署 Release APK
- 禁止把 Release 私钥、keystore、口令、Base64 内容或未脱敏 Secret 输出到 Git、缓存、Artifact、日志或文档
- 禁止在测试、签名、版本或产物校验未全部通过时创建 GitHub Release
- 禁止在未获 L3 计划确认前修改业务源码
