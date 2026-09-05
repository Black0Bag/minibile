# 03 会话模式与执行上下文

## 旧实现情况

当前应用没有 VibeCoding 专用的 Plan/Build 会话模式。工具可见性、权限设置和 `ToolExposureMode.FULL/CLI` 各自解决局部问题，不能表达“此会话只能只读”。`AIToolHook` 只接收 `AITool`，没有会话、模式、任务、调用者和工作区信息。

## 意图修正

建立所有编码任务状态共享的会话级模式与不可丢失的执行上下文。模式由用户拥有，Agent 只能读取。

## 预期新实现

- `CodingSessionMode`：`PLAN`、`BUILD`
- 模式按聊天会话持久化，并支持并行会话隔离
- 聊天输入区提供用户手动切换控件
- 模型请求创建时捕获不可变 `modeSnapshot`
- `ToolExecutionContext` 至少包含：
  - `sessionId`
  - `modeSnapshot`
  - `actorType` 和 `actorId`
  - `taskId`
  - `workspaceId`
  - `invocationSource`
- 上下文贯穿主代理、直接工具、包代理、QuickJS、MCP、ToolPkg、Skill、工作流和 Subagent
- 用户直接点击工具页的操作使用 `USER_DIRECT` 来源，与 Agent 自动操作分离

## 作用域候选

实施前必须详细阅读并最终确定：

- `data/model/ChatEntity.kt`
- `data/db/AppDatabase.kt`
- `api/chat/ChatRuntimeSlot.kt`
- `api/chat/ChatRuntimeHolder.kt`
- `data/model/ToolInvocation.kt` 所在文件
- `api/chat/EnhancedAIService.kt`
- `api/chat/enhance/ToolExecutionManager.kt`
- `core/tools/AIToolHandler.kt`
- `ui/features/chat/viewmodel/ChatViewModel.kt`
- 聊天输入区两套样式组件

## 验证

- 两个会话可分别处于 Plan 和 Build，互不串扰
- 重启应用后每个会话模式保持一致
- Agent 可读取模式但不存在模式写入工具
- 一次响应执行期间使用发送时模式快照
- Build 切 Plan 后尚未开始的写调用被拒绝
- 缺少执行上下文的自动调用不能执行

## 风险

- 模式存到全局偏好会导致并行会话串扰
- 模式切换与正在执行工具之间存在竞态
- 改 `ChatEntity` 牵涉 Room schema；本项目不兼容旧数据，可选择干净重建，但必须由用户确认
- 直接工具页面与 Agent 自动调用如果不区分，会错误限制用户手动操作

## 回滚

模式与上下文作为独立提交。若上下文无法覆盖全部执行路径，停止进入步骤 04；通过 `git revert` 回退该提交，数据库测试使用清除应用数据恢复原始基线。