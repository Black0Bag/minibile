# 05 会话级 TODO 运行时

## 旧实现情况

当前项目没有 `todowrite`、`todoread` 或等价会话任务列表。仓库里的 `docs/TODO/` 是开发计划文档，不是应用运行时 UI。

OpenCode 官方源码已确认其 TODO 形态：

- 按 `sessionID` 存数据库
- 结构包含内容、状态、优先级和位置
- 更新后发布 `todo.updated` 事件
- 输入框上方显示可折叠 `SessionTodoDock`
- Dock 展示完成数/总数、当前任务和完整列表
- 切换会话时只显示该会话数据

## 意图修正

实现同类产品形态，但使用本项目 Kotlin、Room、Flow 和 Compose 架构。TODO 是 Agent 任务编排的运行时事实，不依赖 Markdown 或自然语言解析。

## 预期新实现

### 数据

`SessionTodo` 至少包含：

- 稳定 ID
- `chatId`
- 任务内容
- `PENDING`、`IN_PROGRESS`、`COMPLETED`、`CANCELLED`
- `HIGH`、`MEDIUM`、`LOW`
- 排序位置
- 创建与更新时间
- 可选父任务/Subagent 关联
- 可选阻塞原因

### 工具

- 主代理读取当前会话 TODO
- 主代理以完整结构化列表更新 TODO
- 参数 schema 拒绝未知状态和非法优先级
- Plan 与 Build 均可更新 TODO，因为该操作只改变应用任务状态，不改变项目
- 默认对子代理禁用父会话 TODO 写入

### UI

- Dock 位于聊天输入区附近，不占用消息正文
- 支持折叠/展开
- 折叠态显示完成数/总数和当前项
- 进行中项有清晰动态状态，完成/取消项划除
- 所有内容为自然简体中文
- 全部完成或列表为空时自动收起
- 会话切换不重播已有动画，不泄露其他会话 TODO
- 支持 TalkBack 状态描述和至少 48dp 操作目标

## 作用域候选

- Room 实体、DAO 和 Database
- 新 TODO Repository/Service
- 工具注册、Prompt 和结果类型
- `InputProcessingState` 或新的统一任务状态
- `AgentChatInputSection.kt`
- `ClassicChatInputSection.kt`
- ChatViewModel/Service 的会话切换流
- 简体中文资源和 Preview

## 验证

- 数据 schema 与状态机单测
- 同时只允许一个主项进行中
- 开始前更新、验证后完成，禁止批量伪完成
- TODO 更新事件实时到 UI
- 两个会话数据隔离
- 进出会话 Dock 行为正确
- 列表为空或全完成后收起
- Plan 模式允许 TODO、仍拒绝项目写入
- Subagent 不能直接改父清单
- Compose Preview 覆盖空、待处理、进行中、完成、取消和长文本

## 风险

- 把模型输出文本当状态源会造成错乱
- 用 DataStore 保存列表不适合关系、排序和会话查询
- TODO 与聊天消息生命周期不同步可能留下孤儿数据
- 双输入样式需要共享同一组件，不能复制两份逻辑

## 回滚

TODO 数据表、服务、工具和 UI 分独立提交。若 UI 有问题可先隐藏 Dock 但保留经过测试的数据层；最终发布前不允许长期保留不可见的半成品。数据无需兼容，回滚后清除应用数据。