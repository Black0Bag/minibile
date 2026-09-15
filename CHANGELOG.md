# 更新日志
本文件记录每个可安装版本的实际交付内容。每个版本只对应一个 Git 提交、一个 Tag 和一个 GitHub Release。

## 0.4.1 - 2026-09-15

### 修复

- 修复 GitHub Actions runner 镜像更新导致的 Android SDK 安装失败：`setup-android` 默认安装的 `tools` 包已从新版 SDK 移除（"Failed to find package 'tools'"），改为 `packages: platform-tools`，其余包由后续 `sdkmanager --install` 安装。

## 0.5.0 - 2026-09-15

### 新功能

- Phase E 云端构建只读能力：新增纯 Kotlin `CloudBuildInspector`、`CloudBuildTemplateRegistry`、`CloudBuildModels`。
- `cloud_build_release` 只读动作（inspect/prepare/status）：
  - `inspect`：按项目文件识别技术栈（Android/Flutter/Rust/JVM/Go/Python/Node）、给出构建后端建议并匹配 CI 模板
  - `prepare`：生成仓库模式 + CI 模板 + 产物语义的只读发布计划预览，不写远端
  - `status`：解析远端 workflow run 查询结果摘要
- 8 个 CI 模板注册表（android-gradle-apk / flutter-android / node-web / python-package / jvm-gradle / jvm-maven / rust-binary / generic-command），来源锚定 03A 计划 §6.4。
- 权限预检只报告缺失权限与原因，不输出或回显任何 token。

### 测试

- 新增 7 个 JVM 单元测试：技术栈识别（Android/Go/未知）、prepare 只读预览、权限预检（无 token/部分权限/齐备）、status 摘要、空运行、模板注册表完整性。

## 0.4.0 - 2026-09-14

### 新功能

- Phase D 会话任务纵向切片：新增纯 Kotlin 运行时模型 `SessionTodo`（状态/优先级/排序/父任务/阻塞原因）与 `SessionTaskCoordinator`，把 VibeCoding 状态机与 TODO 清单串成“澄清 → 探索 → 研究 → 计划 → 审批 → Build → 验证 → 交付”可恢复执行链。
- 新增 `SessionTodoDao`、`SessionTodoMapper`、`SessionTodoRepository`：Room 会话级 TODO 持久化与 Flow 观察。
- Room 版本 22→23，新增 `MIGRATION_22_23`（仅 CREATE TABLE `vibecoding_session_todos` + INDEX，不修改现有表）。

### 测试

- 新增 5 个 JVM 单元测试：TODO 状态转换、主 TODO 唯一进行中约束、子任务不阻塞主任务、会话隔离、Mapper 往返一致性。

## 0.3.0 - 2026-09-12

### 新功能

- Phase C 统一工具硬门禁：新增 `ToolExecutionContext`、`ToolPolicyDecision`、`ToolPolicyGate` 纯 Kotlin 组件。
- `AIToolHandler.executeTool/executeToolAndStream` 新增可空 context 重载，在 Executor 激活前 fail-closed 判定；旧签名保持兼容。
- `ToolExecutionManager` 构建 `ToolRuntimeContext` 并预留 `vibecodingContext`；Phase C 默认传 null（不破坏现有调用），Phase D 接入真实会话模式快照后注入。

### 测试

- 新增 11 个 JVM 单元测试：null context 兼容、用户直连放行、PLAN 拒绝写/放行读、未知工具保守拒绝、BUILD 审批校验、过期审批拒绝、调用者类型限制、包只读工具放行、工具分类。

## 0.2.0 - 2026-09-12

### 新功能

- Phase B 数据库持久化：新增 4 张独立 Room 表（`vibecoding_tasks`、`vibecoding_research_records`、`vibecoding_validation_runs`、`vibecoding_build_runs`），带 `chats(id) ON DELETE CASCADE` 外键。
- 新增 `VibeCodingTaskDao`、`VibeCodingTaskMapper`（Domain ↔ Entity JSON 映射）和 5 个 Mapper 往返测试。
- Room 版本 21→22，新增 `MIGRATION_21_22`（仅 CREATE TABLE + INDEX，不修改现有表）。

### 测试

- 5 个 JVM 单元测试：完整任务往返、空任务往返、研究记录往返、构建失败分类往返、NotRequired Release 往返。

## 0.1.0 - 2026-09-12

### 新功能

- 新增 Phase A 纯 Kotlin `VibeCodingTaskEngine`：将需求澄清、源码探索、联网校准、版本化计划、用户审批、实施、验证、文档、审查、构建和发布固化为不可跳步的状态机。
- 新增用户独占的 PLAN/BUILD 模式与计划审批守卫；计划 revision 变化后旧审批立即失效，恢复写阶段也必须重新验证当前模式和审批。
- 新增结构化需求、研究、验证、文档、构建策略、构建运行和 Release 证据模型；云构建必须提供与成功构建 SHA 一致的已发布 Release 证据。

### 测试

- 新增 21 个 JVM 单元测试，覆盖完整主路径、非法跳阶段、联网证据降级、Agent 越权、过期审批、验证失败回退、构建后端一致性、构建失败分类、Release digest/签名/SHA 和并行任务隔离。

### 文档

- 将 Vibe-coding-workflow-zh 的关键规则提升为代码级唯一产品主线，并新增 `cloud_build_release` 重量级云端构建发布工具 L3 计划。

## 0.0.3 - 2026-09-09

### 文档

- T02 能力矩阵定稿：179 个内置工具逐一判定（保留 101 / 删除 78），全部 7 个取舍点获用户确认。
- 判定变更记录附源码证据：浏览器自动化 24 工具全保留、`update_user_profile` 保留、`capture_screenshot`/`install_app`/`uninstall_app` 改判保留、`QUERY_ALL_PACKAGES` 删除、本地推理模块删除、子包签名与 `assets/shizuku.apk` 修正为保留。
- 步骤文档与索引同步：02 标记 [DONE]，进入 T03。

## 0.0.2 - 2026-09-06

### 修复

- 修复 `RawSnapshotBackupManagerTest`：备份包名前缀已迁移到 `io.github.black0bag.minibile`，测试断言同步更新。
- 修复 `XaiProviderReasoningTest.defaultConfigUsesTheOfficialXaiEndpointAndModel`：`ModelListFetcher.getModelsListUrl` 内部调用 `AppLogger.d`，在纯 JVM 测试环境触发 `android.util.Log` stub 异常；测试中关闭 `enableSystemLog`。
- 修复 `ReleasedProviderModelKeyDecoder`：`decode` 在 `known` 为 null 时未校验 `providerAlias` 是否在已知 alias 集合中，导致 `FILE_BINDING` 被错误解码；新增 `require` 拒绝未知 provider alias。

## 0.0.1 - 2026-09-06

### 修复

- 修复上游遗留的 `XaiProviderReasoningTest`：它引用了已被删除的 `XaiReasoningMapper` 与 `xaiModelSupportsReasoningEffort`，导致 `:app:compileDebugUnitTestKotlin` 编译失败；改为通过现行 `ThinkingQualityMappingRegistry` 与 `ModelThinkingConfigDefaults` 校验 Grok 的 `reasoning_effort` 契约。

## 0.0.0 - 2026-09-05

### 基线

- 建立 minibile 独立定制版本线，初始版本为 `0.0.0`。
- 将 Android 安装包 ID 设为 `io.github.black0bag.minibile`，与原始应用并行安装。

### 工程

- 建立仓库唯一版本源、确定性 Android `versionCode` 和版本一致性检查。
- 建立 GitHub Actions 测试、固定证书签名、APK 校验和 GitHub Release 发布链路。
- 建立工作区外签名资产索引，不将私钥或凭据提交到 Git。

### 文档

- 建立 VibeCoding 专用 Agent 的目标、架构、规则和分步实施计划。
- 建立代码库全景分析和工作区根目录索引。
