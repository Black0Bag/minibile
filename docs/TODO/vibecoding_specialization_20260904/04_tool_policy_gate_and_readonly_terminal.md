# 04 工具硬门禁与只读终端

## 旧实现情况

`ToolExecutionManager` 会为主对话工具调用执行权限检查，但 `AIToolHandler.executeTool()` 还有 QuickJS、工作流、MCP、工作区和 UI 等大量直接调用点。现有 Hook 没有会话上下文。仅隐藏写工具或修改 Prompt 无法防止间接调用。

## 意图修正

所有 Agent 发起的工具行为在真正激活 Executor 前经过同一个 fail-closed 策略门禁，并按最终副作用而不是包装器名称判定权限。

## 预期新实现

- `CodingModePolicy`：以模式和工具效果返回允许、需确认或拒绝
- `ToolPolicyGate`：统一执行入口
- 工具能力分类：读取、搜索、网络读取、会话 TODO、文件写入、环境修改、Git 修改、外部副作用等
- `package_proxy`、MCP、ToolPkg 和脚本工具解析到最终目标后判定
- Plan 只读终端使用专门的命令解析和允许规则
- 模式拒绝优先于普通工具审批；用户不能在权限弹窗中把 Plan 拒绝永久改成允许
- 判定结果记录会话、调用者、目标工具、规则和拒绝原因
- 子代理继承父会话 deny 上限

## 只读终端边界

Plan 下终端只用于观察，不用于构建或改变环境。实现不能只搜索 `rm`、`>` 等关键字，因为 shell 有别名、命令替换、解释器和间接写入。

首批要验证的只读场景：

- 路径和目录：`pwd`、`ls`
- 文件读取：`cat`、`head`、`tail`、`sed -n`
- 搜索统计：`find`、`grep`、`rg`、`wc`
- 元数据：`stat`、`file`
- Git 读取：`status`、`log`、`diff`、`show`、`branch --list`
- 工具版本与帮助等纯查询

命令组合、管道、子 shell、重定向、环境变量赋值和脚本解释器必须有语法级测试。无法证明无副作用即拒绝并提示用户切 Build。

## 作用域候选

- `core/tools/AIToolHandler.kt`
- `core/tools/AIToolHook.kt`
- `api/chat/enhance/ToolExecutionManager.kt`
- `core/tools/ToolRegistration.kt`
- `core/tools/defaultTool/standard/StandardTerminalCommandExecutor.kt`
- `core/tools/defaultTool/standard/StandardShellToolExecutor.kt`
- `core/tools/javascript/`
- `data/mcp/`
- `core/tools/packTool/`
- `core/workflow/WorkflowExecutor.kt`
- 工具权限 UI 与数据存储

## 验证

建立表驱动绕过测试，至少覆盖：

- 主代理直接写文件
- `package_proxy` 包装写工具
- Shell 重定向和脚本写入
- QuickJS 调用写工具
- MCP 和 ToolPkg 动态工具
- 工作流执行写工具
- 直接 `AIToolHandler.executeTool()`
- Plan 子代理试图提升权限
- Plan/Build 并行会话
- 权限系统抛异常时 fail closed

## 风险

- 门禁下沉后可能拦截应用内部合法维护任务
- Shell 只读判定若设计不严会形成逃逸面
- 动态工具没有足够元数据时无法准确分类
- 上下文传播遗漏会导致功能被拒绝或越权

## 回滚

先以审计模式记录判定但不拦截，通过测试覆盖全部来源后再启用拒绝。审计模式只用于开发验证，不进入发布版本。门禁启用和调用点迁移分成独立提交，可逐个 `git revert`。