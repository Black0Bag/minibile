# 实施计划（Plan）

> 状态：L3 总计划草案，等待用户确认。
> 本文获用户确认后，才允许创建开发分支并修改业务源码。
> 详细步骤见 [`TODO/vibecoding_specialization_20260904/`](TODO/vibecoding_specialization_20260904/)。

## 里程碑

### 里程碑 0：冻结原始基线并建立 GitHub 云发布链路

目标：在任何业务源码改动之前，以 GitHub Actions 建立可重复、可追溯、可持续更新的签名 APK 发布基线，本地不安装 Android 构建工具链。

交付：

- 记录基线提交 `f323d6c5`、模块、路由、工具、Manifest 权限、依赖和 APK 体积
- 建立根 `VERSION` 唯一版本源和 `CHANGELOG.md`，将未定制基线定义为 `0.0.0`
- 由 Gradle 从唯一版本源派生 `versionName = 0.0.0` 与 `versionCode = 1`，并建立自动一致性校验
- 固定 GitHub Actions 中的 JDK 21、Android SDK、NDK 25.1.8937393、CMake、Node 和 Rust 工具链，校验三份外部依赖
- 建立版本门禁、JVM 单测、签名 Release APK 构建、APK 元数据/证书/SHA256 校验、Tag 与 GitHub Release 发布链路
- 生成专用于本项目的固定 Release 签名密钥，完成仓库外可恢复备份后仅通过 GitHub Actions Secrets 注入
- 发布首个 `v0.0.0`，产物为 `minibile-v0.0.0-arm64-v8a.apk` 及对应 SHA256 文件
- Agent 全程跟踪 GitHub Actions；瞬时故障重跑同一提交，任何修复提交先提升版本再推送，直到 Release 成功后通知用户安装

退出门禁：GitHub 上存在指向唯一提交的 `v0.0.0`（若首次代码修复已推进版本，则为有明确失败记录的下一版本）及校验通过的签名 APK；构建、测试、版本、签名和 Release 证据齐全。未通过不得进入业务重构。

### 里程碑 1：建立编码会话与统一工具硬门禁

目标：先建立整个新系统的安全地基，不先删功能。

交付：

- `CodingSessionMode` 会话级状态及持久化
- 仅用户 UI 可以触发的 Plan / Build 切换控件
- `ToolExecutionContext`，覆盖主代理、Subagent、QuickJS、ToolPkg、MCP、Skill、工作流和直接调用
- `CodingModePolicy` 与 `ToolPolicyGate`
- Plan 只读终端命令解析与拒绝机制
- 模式审计事件和并行会话隔离测试

退出门禁：所有已知工具执行路径均无法绕过 Plan；Build 仍能执行获准的编码工具。

### 里程碑 2：建立会话级 TODO 与简体中文运行视图

目标：让复杂任务的计划、当前动作和完成进度在 UI 中持续可见。

交付：

- `SessionTodo` 数据模型、DAO/Repository 和状态约束
- `todo_write` 与 `todo_read` 等价工具能力，协议名在实现阶段确定
- TODO 更新事件流
- 输入区附近的可折叠 TODO Dock
- 完成数/总数、当前项预览和完整列表
- `ToolDisplayMetadata` 及内置/包/MCP/Skill 工具中文显示
- 原始协议 ID 可在调试详情中查看

退出门禁：TODO 按会话隔离、实时更新、状态规则有效；核心工具不再以裸英文 ID 作为主显示。

### 里程碑 3：建立模型能力自动发现

目标：模型选择和参数界面由可验证的 Provider 元数据驱动，不靠模型名猜测。

交付：

- `ModelCapability` 标准模型
- 原始模型响应与标准化结果的分层存储
- Provider 级能力解析器
- 上下文长度、最大输出、支持参数、推理强度和输入模态展示
- 来源、可信状态和最后刷新时间
- endpoint、Key 或 Provider 变化时失效缓存
- 缺失字段显示“未知”

退出门禁：真实响应夹具覆盖至少一个完整元数据 Provider、一个仅返回模型 ID 的 Provider 和一个自定义兼容端点。

### 里程碑 4：建立 Subagent 任务系统

目标：主代理能把独立工作交给专业子代理，同时保持模式、权限和工作区边界。

交付：

- Agent 配置模型：名称、说明、模型、Prompt、工具权限、最大步骤、前台/后台能力
- `SubagentTask` 父子会话和父 TODO 关系
- 受控任务委派工具
- 前台等待、后台并行、完成通知、失败通知和取消
- 使用既有 `taskId` 继续同一子会话
- 默认无嵌套、无父 TODO 写权限
- Plan 只读上限和 Build 最小权限
- 子任务 UI 与父子会话导航

退出门禁：Plan 子代理无法写入；后台任务无需轮询；并行任务不能同时修改重叠作用域。

### 里程碑 5：建立暂停、卡顿检测与检查点恢复

目标：将任务编排、网络流和工具副作用纳入统一生命周期，用户无需手工发送“继续”。

交付：

- 运行状态：运行中、正在暂停、已暂停、网络无响应、正在恢复、完成、失败、不可恢复
- 用户暂停/继续控制
- 流式无数据超时监测
- `ExecutionCheckpoint`
- 原子文件操作边界
- 前后台 Subagent 和 TODO 状态快照
- 新请求恢复时的 Token 成本提示
- 副作用幂等性与用户确认机制

退出门禁：暂停后不产生新副作用；可恢复任务继续同一 TODO/子任务；不可恢复时明确原因，不伪造 SSE 续传。

### 里程碑 6：建立 VibeCoding 能力矩阵并分波删除通用领域

目标：在新内核稳定后，根据“是否支撑编码闭环”而不是目录名称删除旧能力。

交付：

- 每个路由、工具、Provider、权限、Service、Receiver、资源、Gradle 模块和依赖的能力矩阵
- 分类：保留、拆分后保留、删除
- 首批明确删除领域：日常手机控制、语音、听说模型、视觉功能模型、虚拟形象
- 对同时服务开发场景的能力拆分用途，例如 ADB、Shizuku、截图、浏览器自动化和 APK 工具
- 每个删除波次独立提交、构建和真机验证
- 清理死代码、资源、权限、字符串、Manifest 组件和依赖

退出门禁：最终产品没有非编码入口；所有保留能力都能关联到一条已验证的 VibeCoding 使用场景。

### 里程碑 7：收敛简体中文产品界面和设置

目标：将原通用 Agent 的信息架构改造成专用编码 Agent 工作台。

交付：

- 主导航只呈现编码工作流相关入口
- 对话、项目、终端、TODO、Subagent、模型和设置之间的低摩擦导航
- 简体中文模式、状态、工具动作、错误和权限说明
- 移除已删除领域的设置、引导和文案
- 手机/平板适配和 Android 无障碍检查
- 是否物理删除其他 locale 资源由依赖检查结果决定；最终用户体验只要求简体中文完整

退出门禁：从首次启动到完成一次编码任务，全程无必须理解的裸英文协议 ID，无死入口或无效设置。

### 里程碑 8：全链路验收与文档收口

目标：用真实项目验证产品而不是只证明能编译。

交付：

- Android、Web、Python、Go、Node.js 等代表项目端到端任务
- Plan 只读与绕过测试
- Build 编辑、依赖安装、编译和测试
- TODO、Subagent、暂停恢复和模型能力联合测试
- APK 体积、启动性能、权限面和工具数量对比
- 更新 `goal.md`、`plan.md`、`rules.md`、`structure.md` 和正式开发文档
- 所有完成的步骤文档标记 `[DONE]`

退出门禁：满足 [`goal.md`](goal.md) 的全部成功标准。

## 任务拆解

### 任务依赖总图

```text
T00 文档与基线
	→ T01 GitHub 云构建、版本、签名与原始 Release 基线
	→ T02 能力矩阵初版
	→ T03 会话模式与执行上下文
	→ T04 统一工具硬门禁
	→ T05 运行时 TODO
	→ T06 中文工具元数据
	→ T07 模型能力发现
	→ T08 Subagent
	→ T09 暂停与恢复
	→ T10 通用领域分波删除
	→ T11 UI 与简体中文收敛
	→ T12 多项目端到端验收
```

T03 与 T04 是后续任务的安全前置。T05 是 T08 和 T09 的状态前置。T08 完成后才能验证包含子任务的暂停恢复。删除工作必须在新内核可用后进行，避免一边拆旧地基一边建立新状态模型。

### 所有发布批次的固定闭环

T01 建立发布链路后，每个后续可交付实施单元统一执行：

1. 在本地读取对应计划和源码，完成范围内修改与可执行的静态检查
2. 将根 `VERSION` 提升到严格大于历史版本的新 SemVer，并同步当前版本 `CHANGELOG.md`
3. 创建范围单一的 Git 提交并推送工作分支，提交 Pull Request；不把多个删除波次或独立功能塞进同一发布批次
4. GitHub Actions 在 PR 上校验版本历史并运行必要检查；通过后经 `main` Ruleset 合入 `main`
5. `main` 发布工作流再次校验版本，运行测试，使用固定证书构建 Release APK，核对 APK 元数据、签名和 SHA256
6. 全部成功后，CI 在当前提交创建 `v{VERSION}` Tag 和同名 GitHub Release，上传 `minibile-v{VERSION}-arm64-v8a.apk` 与校验文件
7. Agent 持续查询运行状态并读取失败日志；Runner/网络瞬时故障可重跑原提交，任何仓库修复都必须先提升版本后提交
8. 只有 Release 资产和版本映射全部校验通过后才通知用户下载安装；用户反馈的问题进入更高版本修复，不修改已发布版本

已发布版本不可原地回滚。需要撤销功能时，通过 `git revert` 形成新提交并发布更高版本，使版本历史和安装升级保持单调。

### T00：核心文档和任务文档

目标：使 L3 重构可追溯。

输入：用户确认的产品边界、现有代码库分析、OpenCode 官方文档与源码参考。

输出：四份核心文档和 `docs/TODO/vibecoding_specialization_20260904/`。

负责人：Agent 编写，用户审批。

影响范围：仅 `docs/`。

验证：`ensure_core_docs.py --check-only`、链接检查、Git 状态检查。

### T01：GitHub 云构建、强制版本与原始 Release 基线

目标：证明原始业务源码可由 GitHub Actions 重复测试、固定签名构建并发布，同时把版本一致性变成不可绕过的自动门禁。

输入：当前 Git 基线、外部依赖下载脚本、现有 Android Build/Test Workflows，以及用户指定的初始版本 `0.0.0`。

输出：

- 根 `VERSION` 与 `CHANGELOG.md`
- Gradle 单一版本派生和版本校验脚本/测试
- 安装包 ID `io.github.black0bag.minibile`；保留现有源码 `namespace`，逐项迁移真正依赖安装包 ID 的脚本和测试
- 工作区根级 `../.release-signing/minibile-release.jks`、仅本地凭据、校验值、公开证书指纹和恢复说明
- 自动测试、签名构建、APK 元数据/签名/SHA256 校验和 GitHub Release Workflow
- `v0.0.0` 或按失败修复规则递增后的首个成功 Release、测试日志和基线指标

负责人：Agent 本地修改、提交、推送、轮询 Actions、读取失败日志并修复到成功；用户只在 Release 成功后下载安装烟测。包身份和本地密钥位置已确认；生成密钥及写入 GitHub Secrets 仍需 T01 明确实施授权。

影响范围：`VERSION`、`CHANGELOG.md`、`app/build.gradle.kts`、依赖安装包 ID 的测试/调试脚本、`.github/workflows/`、版本/发布校验脚本、工作区根级 `.release-signing/`、GitHub Actions Secrets、Git Tag 和 GitHub Release；不安装本地 JDK/Android SDK/NDK/CMake。

验证：版本格式与严格递增、版本到 `versionCode` 映射、历史 Tag 冲突、JVM 单测、签名 Release APK 构建、`apkanalyzer`/`aapt` 元数据、`apksigner` 证书、SHA256、Tag 提交和 Release 资产全部一致。

### T02：全量能力矩阵与反向依赖图

目标：将所有功能按实际 VibeCoding 用途分类。

输入：179 个工具、所有路由、Manifest 组件、Gradle 模块、依赖和资源。

输出：可审计矩阵，每项有代码入口、反向依赖、保留理由或删除理由。

负责人：Agent 分析，用户确认产品取舍。

影响范围：文档，不改业务源码。

验证：矩阵覆盖数量与源码扫描数量一致。

### T03：会话模式与执行上下文

目标：把模式从 UI 偏好提升为所有自动执行都携带的会话事实。

输入：当前 Chat、RuntimeSlot、工具调用和并行会话实现。

输出：`CodingSessionMode`、持久化、用户切换 UI、`ToolExecutionContext` 和上下文传播测试。

影响候选：Chat 实体/Repository、ChatRuntime、ChatViewModel、ToolInvocation 和工具桥。

验证：两个并行会话处于不同模式时互不串扰；Agent 无切换 API。

### T04：统一工具硬门禁

目标：所有自动工具调用在执行前经过同一策略判定。

输入：T03 上下文、`AIToolHandler`、`ToolExecutionManager`、QuickJS/MCP/ToolPkg/工作流执行入口。

输出：`CodingModePolicy`、`ToolPolicyGate`、最终目标解析、只读终端策略和审计事件。

验证：绕过测试矩阵全部拒绝；权限异常 fail closed。

### T05：运行时 TODO

目标：建立会话级结构化任务进度。

输入：OpenCode `SessionTodo`/`todowrite`/TODO Dock 参考、当前 Room 和 Compose 聊天输入区。

输出：TODO 数据、Repository、工具、事件流和 Dock UI。

验证：状态机、实时更新、会话切换、折叠展开、全部完成后收起。

### T06：简体中文工具元数据

目标：用户无需理解协议 ID 即可知道 Agent 正在做什么。

输入：工具注册、ToolPkg/MCP/Skill 元数据、现有工具渲染组件。

输出：中文显示目录、参数摘要器、动态工具显示规则和调试详情。

验证：内置、`package_proxy`、MCP、ToolPkg、Skill 和未知工具六类样例。

### T07：模型能力自动发现

目标：以 Provider 真实元数据驱动配置界面。

输入：`ModelListFetcher`、`ModelConfigData`、`ThinkingQualityMapping` 和真实 API 响应。

输出：标准能力模型、Provider 解析器、缓存、刷新和自适应 UI。

验证：真实响应夹具、未知字段、Key/endpoint 变化和推理选项。

### T08：Subagent

目标：支持受控任务委派、并行和继续。

输入：T03/T04/T05、当前多会话与并行运行时、OpenCode Task 工具参考。

输出：Agent 配置、父子会话、任务工具、后台作业、结果事件和 UI。

验证：只读上限、任务恢复、深度限制、取消、后台通知和文件作用域冲突。

### T09：暂停、卡顿和恢复

目标：为主任务及其子任务建立可恢复生命周期。

输入：TODO、Subagent、流式 Provider、工具执行和取消实现。

输出：状态机、检查点、无数据监测、暂停/继续 UI 和恢复策略。

验证：网络卡顿、用户暂停、工具原子性、重复副作用和恢复 Token 提示。

### T10：通用能力删除

目标：分波移除所有不服务 VibeCoding 的产品领域。

输入：T02 最终矩阵和稳定的新内核。

输出：多个独立删除提交；每个提交有专门的 TODO 步骤补充文档。

首批领域：

- 手机日常控制与 UI 自动化产品功能
- TTS、STT、唤醒词和语音界面
- 图像、音频、视频识别功能模型
- DragonBones、MMD、FBX、glTF 等虚拟形象
- 蓝牙、音乐、通话、短信、位置等日常工具

注意：编码调试共用能力必须先拆分，不得与通用产品功能一起误删。

验证：每波编译、相关测试、Manifest/路由/资源/依赖残留扫描和真机烟测。

### T11：UI 与中文收敛

目标：从功能堆叠界面变为编码任务工作台。

输入：删除后的实际能力、新运行时状态和中文元数据。

输出：最终导航、聊天输入区、任务 Dock、Subagent 面板、模型设置和诊断界面。

验证：手机/平板、深色/浅色、TalkBack、48dp 触控、长文本和错误状态。

### T12：端到端验收

目标：证明不同项目类型都能完成 VibeCoding 闭环。

输入：最终候选 APK。

输出：代表项目验收日志、对比指标和发布前清单。

验证项目至少包括：Android、Web、Python、Go、Node.js；Java/Flutter/TypeScript 用模板和工具链覆盖补充验证。

## 验收节点

### 验收点 A：文档门禁

- 四份核心文档通过结构检查
- L3 TODO 索引和步骤齐全
- 用户明确同意本计划
- 未修改业务源码

### 验收点 B：云构建、版本与原始发布基线

- 根 `VERSION` 为 `0.0.0`，Gradle 最终解析为 `versionName = 0.0.0`、`versionCode = 1`，About 页面继续从安装包元数据显示同一版本
- GitHub Actions 的版本门禁、JVM 测试和签名 Release APK 构建成功
- APK 元数据、固定证书 SHA-256 指纹、SHA256 文件、Tag 目标提交、Release 名称和资产文件名一致
- GitHub Release 可下载；用户完成安装、启动、基础对话、工作区和终端烟测
- 基线指标与全部失败尝试留档，历史失败版本不被复用

### 验收点 C：模式硬门禁

- 用户手动切换生效
- Agent 无模式切换工具或 API
- Plan 对主代理、子代理、QuickJS、MCP、ToolPkg、工作流和直接调用均不可写
- 并行会话模式隔离

### 验收点 D：任务运行时

- TODO Dock 实时工作
- 中文工具动作可读
- 子代理能前台/后台执行并回报
- 暂停与恢复覆盖主任务和子任务

### 验收点 E：模型配置

- 模型上下文、输出和推理选项按真实元数据呈现
- 未提供字段明确显示未知
- 无基于模型名称的能力猜测

### 验收点 F：功能删除

- 能力矩阵中的删除项无源码、路由、Manifest、权限、依赖和资源残留
- 保护性保留的编码能力均有实际使用场景验证
- APK 体积和权限面相较基线显著下降

### 验收点 G：最终产品

- 所有目标项目类型可创建或导入
- 完成 Plan → TODO → 用户切 Build → 修改 → 编译测试 → Subagent → 暂停恢复 → 交付
- UI 核心流程为自然简体中文
- 无非 VibeCoding 产品入口

## 风险与回滚

### 总体风险

| 风险 | 证据 | 控制方式 |
|---|---|---|
| 工具权限可绕过 | `AIToolHandler.executeTool()` 有大量直接调用点 | 先建执行上下文与底层门禁，专门绕过测试 |
| 并行会话串模式 | 当前没有会话级 Coding 模式 | 模式随会话和调用快照传播，不用全局状态 |
| Subagent 越权 | OpenCode 官方源码也有专门越权回归测试 | 父会话 deny 作为硬上限，只能收紧 |
| TODO 与文档混淆 | 当前无运行时 TODO | 独立数据库与事件流，明确文档边界 |
| SSE 无法原地续传 | HTTP 流式协议事实 | 以检查点恢复，不声称恢复同一 Socket |
| 大规模删除误伤开发能力 | ADB、截图、浏览器等具有双重用途 | 能力矩阵按用途拆分，逐波删除 |
| 数据结构大改 | 用户不要求旧数据兼容 | 使用干净安装作为里程碑前置，不写无用迁移 |
| 原始项目尚无构建/发布基线 | 当前 Actions 无运行记录，只上传临时 Artifact，且无签名 Secrets/Tag/Release | T01 建立云测试、固定签名和 Release 后阻断所有业务改动 |
| 版本漂移或版本复用 | 当前 Gradle 硬编码 `1.12.1+3`/`46`，仓库无唯一版本源 | 根 `VERSION` + 历史 Tag + APK 元数据的 CI 强校验；一版本只绑定一提交 |
| 签名丢失导致无法升级 | Runner Debug key 不稳定，现有子包 keystore 不可信任为发布身份 | 专用密钥、仓库外可恢复备份、Secrets 注入和证书指纹门禁 |
| 工作区删除导致签名身份丢失 | 目标包已改为 `io.github.black0bag.minibile`，无需卸载当前 App；但工作区和 `.release-signing/` 仍位于当前 App 私有目录 | 不清除/卸载当前 App；迁移或归档工作区时必须包含隐藏签名目录，并用校验值验证完整性 |
| GitHub 网络/Runner 瞬时失败 | 当前网络曾出现 443 超时 | 同一提交原样重跑；只有仓库内容变化时提升版本，失败不创建 Release |
| 核心巨类缺乏测试 | 代码库分析已确认 | 新内核外置为小组件，先测再接入 |
| Provider 元数据不统一 | `/models` 格式因厂商而异 | Provider 适配器 + 来源/未知状态 |

### 回滚策略

- 原始不可变基线为提交 `f323d6c5`
- 不使用破坏性的 `git reset --hard` 或覆盖用户改动
- 每个任务单元使用独立提交，回滚使用 `git revert <commit>`
- 每个删除领域使用独立提交，不能和新内核功能混在一起
- 数据不要求兼容；数据库结构变化后的回滚方式是卸载/清除应用数据后安装目标版本，不编写旧数据降级迁移
- GitHub Actions 工具链版本、Workflow SHA 和运行 URL 写入对应步骤证据；本地不安装 Android 构建环境
- 已发布的 Tag、Release 和资产不覆盖、不移动、不删除；代码回滚使用 `git revert` 形成更高版本的新 Release
- 未发布且尚未创建 Tag 的失败提交保留 Actions 日志；若只属 Runner/网络瞬时故障可重跑同一提交，若需修改仓库则提升版本后提交修复
- 若某阶段无法通过退出门禁，停止后续任务，保留日志并回退该阶段提交
- 并行 Subagent 不接触同一文件作用域；发生冲突时取消其中一个任务，不自动合并不明改动

### L3 实施授权边界

用户确认本文，只代表同意总体路线。每个里程碑开始前仍需：

1. 读取对应 TODO 步骤和目标代码完整调用链
2. 给出该步骤的具体文件清单和验证命令
3. 获得涉及文件删除、移动、数据库重建、签名密钥、GitHub Secrets、Ruleset、Tag 或 Release 的明确授权
4. 对拟发布改动明确版本提升类型，并确认新版本未被任何提交使用
5. 实施后通过 GitHub Actions 和 Release 提交证据，再进入下一步骤

未经上述门禁，不得把总体授权扩大解释为任意删改整个仓库。