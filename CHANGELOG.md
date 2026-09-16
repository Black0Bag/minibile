# 更新日志
本文件记录每个可安装版本的实际交付内容。每个版本只对应一个 Git 提交、一个 Tag 和一个 GitHub Release。

## 0.8.0 - 2026-09-16

### 删除

- Phase H Wave 1：删除工作流模块（15 源文件 + Manifest 组件 + 权限）
- 删除 QUERY_ALL_PACKAGES 权限
- 清理 10 个文件中的工作流引用（AIForegroundService, OperitApplication, PluginRegistry, SystemToolPromptsInternal, ToolRegistration, ToolGetter, JsTools, PackageManager, PackageManagerToolPkgFacade, ToolPolicyGate）

## 0.7.0 - 2026-09-16

### 新功能

- Phase G 恢复、Subagent 与修复循环：新增 BuildAttempt、FailureFingerprint、RepairBudget、RecoveryCheckpoint、SubagentTask 领域模型。
- 数据库迁移 23→24：新增 3 张表（vibecoding_build_attempts、vibecoding_subagent_tasks、vibecoding_recovery_checkpoints），非破坏性迁移。
- VibeCodingTaskDao 新增 12 个 DAO 方法（build attempts、subagent tasks、recovery checkpoints 的 CRUD）。

### 测试

- 新增 12 个 JVM 单元测试：RepairBudget 熔断逻辑（3）、失败指纹去重（2）、BuildAttempt 生命周期（2）、RecoveryCheckpoint 恢复目标（2）、SubagentTask 状态转换（3）。

## 0.6.3 - 2026-09-16

### 修复

- 修复 GitHub runner 镜像 NDK 27 llvm-strip 无法处理 terminal 模块 libsudo.so 的问题：在 app/build.gradle.kts 的 android.packaging.jniLibs 中添加 `keepDebugSymbols += "**/libsudo.so"`，跳过 strip（PR #13 合并到 main 42cf2076）。

## 0.6.2 - 2026-09-16

### 修复

- 同上（VERSION 0.6.2 + CHANGELOG 0.6.2 分两次推送导致 release_version 检查失败，已合并到 0.6.3 同一 commit）。

## 0.6.1 - 2026-09-16

### 新功能

- Phase F 第二部分：`GitHubCloudWriteClient.putSecretViaWorkflow`（通过 workflow_dispatch 触发设置 Secret，方案 B，不在 App 侧加密）。
- `isValidSecretName` 校验（字母数字下划线，不以 GITHUB_ 开头）。
- F5 端到端验证：vc-node-demo 仓库 set-secret.yml workflow_dispatch 成功设置 VC_DEMO_TEST Secret。

### 测试

- 新增 2 个 JVM 单元测试：Secret 名称校验（合法/非法）。

## 0.6.0 - 2026-09-15

### 新功能

- Phase F 云端写入闭环（第一部分）：新增 `GitHubCredentialStore`（Android Keystore 不可导出 AES-256-GCM 主密钥 + 信封加密，密文/IV/版本存 app-private SharedPreferences，不复用已废弃的 EncryptedSharedPreferences）。
- 新增 `GitHubCloudWriteClient`（OkHttp）：建仓、单文件上传（Contents API）、workflow dispatch、run 查询、Release 验收；token 仅从凭据仓读取，绝不写入日志/结果/证据。
- 新增 `CloudWriteModels`：仓库创建/上传条目/dispatch/Release 验收模型。

### 测试

- 新增 7 个 JVM 单元测试：凭据信封编解码、路径/仓库名校验、Release 验收完整性。

## 0.4.1 - 2026-09-15

### 修复

- 修复 GitHub Actions runner 镜像更新导致的 Android SDK 安装失败：`setup-android` 默认安装的 `tools` 包已从新版 SDK 移除（"Failed to find package 'tools'"），改为 `packages: platform-tools`，其余包由后续 `sdkmanager --install` 安装。

## 0.5.0 - 2026-09-15

### 新功能

- 03A Phase E cloud_build_release 只读动作：CloudBuildInspector + CloudBuildTemplateRegistry（8 个 CI 模板）+ 权限预检。
- 新增 7 个 JVM 单元测试覆盖模板注册、权限判定和 run 摘要解析。
- GitHub Actions 工作流：PR Check（Fast checks + Android JVM tests + Android build + Candidate checks）、Android Release（Signed Android release + Release metadata + Publish GitHub Release）。

### 修复

- 修复 GitHub Actions runner 镜像更新导致的 Android SDK 安装失败：`setup-android` 默认安装的 `tools` 包已从新版 SDK 移除（"Failed to find package 'tools'"），改为 `packages: platform-tools`，其余包由后续 `sdkmanager --install` 安装。

## 0.4.0 - 2026-09-14

### 新功能

- 03A Phase D 会话任务纵向切片：SessionTodo + SessionTaskCoordinator + Room 持久化（22→23 张表）。
- 新增 5 个 JVM 单元测试覆盖任务协调器与持久化往返。
- 新增 `TaskLifecycleValidator` 状态机转换守卫。

## 0.3.0 - 2026-09-12

### 新功能

- 03A Phase C 工具硬门禁：ToolExecutionContext + ToolPolicyGate。
- 新增 5 个 JVM 单元测试覆盖工具执行权限判定。

## 0.2.0 - 2026-09-12

### 新功能

- 03A Phase B Room 持久化：4 张 VibeCoding 任务表 + DAO + Mapper。
- 新增 6 个 JVM 单元测试覆盖 DAO 往返与 Mapper 一致性。

## 0.1.0 - 2026-09-12

### 新功能

- 03A Phase A 纯 Kotlin 领域模型：VibeCodingTask、TaskSession、TaskAttempt、VibeCodingPlan、VibeCodingRule。
- 新增 21 个 JVM 单元测试覆盖状态机转换守卫和领域逻辑。
