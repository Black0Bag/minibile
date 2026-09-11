---
title: VibeCoding 专用 Agent 重构
status: in-progress
fork: https://github.com/Black0Bag/minibile
baseline: f323d6c5
created: 2026-09-04
---

# VibeCoding 专用 Agent 重构

## 原本状况

当前应用是覆盖对话、手机控制、语音、视觉、虚拟形象、市场、工作流、开发环境等领域的通用 Android Agent。编码能力已经存在，但工具、状态、权限和界面均围绕通用 Agent 叠加，缺少编码任务专用的 Plan/Build 硬门禁、运行时 TODO、Subagent、模型能力发现和检查点恢复。

## 大致意图

以当前 `main` 分支 `f323d6c5` 为不可变原始基线，把应用重构成只服务 VibeCoding 的专用编码 Agent。先建立统一编码任务执行内核，再依据可验证的能力矩阵删除所有非编码领域。

本重构不再同步上游，不与其他项目建立联系，不要求兼容现有本地数据。

## 期待结果

```text
项目工作区
	→ 用户手动 Plan
	→ 只读探索 + 网络研究 + 只读 Subagent
	→ 会话 TODO Dock 实时显示任务
	→ 用户手动 Build
	→ 编辑 + PRoot 构建测试 + 执行型 Subagent
	→ 暂停/卡顿检查点
	→ 继续并完成验证
	→ 提升唯一版本并推送 GitHub
	→ Actions 测试、固定签名构建并发布 Release
	→ Agent 跟踪修复至成功后通知用户安装
	→ 中文交付结果
```

最终应用不存在与 VibeCoding 无关的产品入口、权限、依赖和资源。

## 大致作用域

- Android/Kotlin/Compose 主应用
- Room 与 ObjectBox 数据模型
- 对话和工具执行链路
- PRoot 终端和工作区
- ToolPkg、MCP、Skill 扩展链路
- 模型 Provider 和设置界面
- Manifest、Gradle 模块、资源与本地化
- 唯一版本源、固定 APK 签名、GitHub Actions、Ruleset、Tag 与 Release
- CI、测试、开发文档

## 强制门禁

- 当前文件夹只是 L3 实施计划，不构成业务源码修改授权
- [`../../goal.md`](../../goal.md)、[`../../plan.md`](../../plan.md)、[`../../rules.md`](../../rules.md)、[`../../structure.md`](../../structure.md) 必须先通过检查并由用户确认
- 每个步骤开始前必须重新读取实际调用链，列出具体文件和验证命令
- 删除、移动、数据库重建、GitHub Secrets、Ruleset、Tag、Release 和签名密钥操作均需要该步骤的明确授权
- 每个发布批次必须从根 `VERSION` 推进版本，并通过 App/Tag/Release/APK 一致性门禁
- 每个步骤真正完成并验证后才在对应文档末尾增加 `[DONE]`

## 步骤索引

| 顺序 | 文档 | 目标 | 前置 |
|---|---|---|---|
| 00 | [`00_documentation_and_baseline_contract.md`](00_documentation_and_baseline_contract.md) | 固化文档、边界与基线契约 | 无 |
| 01 | [`01_build_environment_and_original_baseline.md`](01_build_environment_and_original_baseline.md) | GitHub 云构建、强制版本、固定签名与原始 Release 基线 | 00 |
| 02 | [`02_vibecoding_capability_matrix.md`](02_vibecoding_capability_matrix.md) | 全量能力矩阵与依赖图 | 01 |
| 03A | [`03a_vibecoding_task_engine_and_cloud_build_plan.md`](03a_vibecoding_task_engine_and_cloud_build_plan.md) | VibeCoding Skill 代码级状态机与统一云端构建发布工具修订计划 | 02 |
| 03 | [`03_session_mode_and_execution_context.md`](03_session_mode_and_execution_context.md) | 会话模式和执行上下文（并入 03A Phase A-C） | 01、02、03A |
| 04 | [`04_tool_policy_gate_and_readonly_terminal.md`](04_tool_policy_gate_and_readonly_terminal.md) | 不可绕过硬门禁 | 03 |
| 05 | [`05_session_todo_runtime.md`](05_session_todo_runtime.md) | 运行时 TODO 与 Dock | 03、04 |
| 06 | [`06_chinese_tool_activity_metadata.md`](06_chinese_tool_activity_metadata.md) | 中文工具动作视图 | 04、05 |
| 07 | [`07_model_capability_discovery.md`](07_model_capability_discovery.md) | 模型能力自动发现 | 03 |
| 08 | [`08_subagent_task_runtime.md`](08_subagent_task_runtime.md) | 子代理任务运行时 | 03、04、05 |
| 09 | [`09_pause_stall_checkpoint_resume.md`](09_pause_stall_checkpoint_resume.md) | 暂停、卡顿与恢复 | 05、08 |
| 10 | [`10_remove_non_coding_domains.md`](10_remove_non_coding_domains.md) | 分波删除通用领域 | 02、04、09 |
| 11 | [`11_vibecoding_ui_and_simplified_chinese.md`](11_vibecoding_ui_and_simplified_chinese.md) | 编码工作台与中文收敛 | 05-10 |
| 12 | [`12_cross_stack_end_to_end_acceptance.md`](12_cross_stack_end_to_end_acceptance.md) | 多技术栈端到端验收 | 01-11 |

## 当前状态

- 00 文档已创建并通过自动校验，L3 总计划已获用户批准
- 目标安装包 ID `io.github.black0bag.minibile` 和工作区根级 `.release-signing/` 已确认
- 工作区根级 `WORKSPACE.md` 已建立；签名目录已包含 keystore 与凭据
- 00 已完成 `[DONE]`
- 01 已完成 `[DONE]`：首个 GitHub Release `v0.0.2` 已发布，APK 已通过用户安装验收
- 02 已完成 `[DONE]`：能力矩阵定稿（2026-09-09），179 工具保留 101/删除 78，全部 7 个取舍点获用户确认，明细双向零差集核验通过
- 03A Phase A 实施中：用户已于 2026-09-12 确认开工；纯 Kotlin 领域模型、状态机转换守卫和 21 个 JVM 单测已落盘，尚未接数据库/UI/真实 GitHub，等待 PR #3 云端编译测试
- 03-12 将按 03A 的 Phase A-H 顺序重排
