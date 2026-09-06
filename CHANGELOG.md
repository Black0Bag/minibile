# 更新日志

本文件记录每个可安装版本的实际交付内容。每个版本只对应一个 Git 提交、一个 Tag 和一个 GitHub Release。

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
