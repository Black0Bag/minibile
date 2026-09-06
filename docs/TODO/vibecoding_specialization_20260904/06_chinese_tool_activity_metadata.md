# 06 简体中文工具活动元数据

## 旧实现情况

默认中文资源已有大量工具操作描述，但聊天 UI 仍直接显示 `read_file_part`、`edit_file`、`package_proxy` 或 `super_admin:terminal` 等协议 ID。`CustomXmlRenderer.resolveToolDisplayNameForRender()` 和 `ToolDisplayComponents.normalizeToolDisplayForStrictProxy()` 当前主要处理代理工具拆包，不提供统一中文显示目录。

## 意图修正

协议标识保持稳定，用户界面通过独立元数据层显示“谁正在对什么做什么”。中文化只作用于展示，不破坏 Prompt、ToolPkg、MCP 或脚本 API。

## 预期新实现

`ToolDisplayMetadata` 至少包含：

- 原始协议 ID
- 来源类型与来源名称
- 简体中文名称
- 进行中动作模板
- 完成动作模板
- 失败动作模板
- 可安全展示的参数摘要器
- 图标/类别
- 是否已本地化

显示示例：

| 原始调用 | 主显示 |
|---|---|
| `read_file_part` | `读取文件片段：docs/goal.md（第 1-100 行）` |
| `edit_file` | `修改文件：app/.../Foo.kt` |
| `super_admin:terminal` | `在 Ubuntu 终端执行命令` |
| `package_proxy` → `extended_memory_tools:create_memory` | `调用记忆工具：创建项目记忆` |
| 未知 MCP 工具 | `调用 MCP：server / raw_tool_id（未本地化）` |

原始 ID、完整参数和原始输出只放在可展开调试详情中。API Key、Token、Cookie、文件全文等敏感数据不得进入摘要。

## 作用域候选

- 新 ToolDisplayCatalog/Resolver
- `CustomXmlRenderer.kt`
- `ToolDisplayComponents.kt`
- `InputProcessingState.ExecutingTool`
- 工具权限弹窗和操作描述注册
- ToolPkg/MCP/Skill 元数据解析
- `package_proxy` 最终目标解析
- 简体中文资源和 UI 测试

## 验证

- 内置工具、包工具、MCP、ToolPkg、Skill、未知动态工具各有测试
- 进行中/完成/失败三种状态文本准确
- 路径、行号、命令目标等摘要正确
- 敏感参数不显示
- 原始协议 ID 不变，现有工具调用测试继续通过
- 长名称和窄屏不溢出
- TalkBack 能读出动作与状态

## 风险

- 把协议 ID 改名会导致工具不可调用
- 根据 ID 生硬翻译会误导用户
- 动态工具缺少中文元数据
- 参数摘要可能泄露私密内容

## 回滚

元数据层是纯展示适配。若某类动态工具无法正确解析，显示明确“未本地化”的来源和原始 ID，而不是错误翻译；该行为是显式未知状态，不是静默降级。提交可独立回退，不改协议和数据。