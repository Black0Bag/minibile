# 项目架构（Structure）

> 状态：L3 重构准备阶段。
> 本文严格区分“当前事实”和“目标架构”。未标注为目标的内容均来自当前源码；目标部分尚未实现。

## 目录结构

当前仓库是 Android 原生多模块项目，核心目录如下：

```text
minibile/
	app/                         主 Android 应用
		config/                    构建期资源配置
		objectbox-models/          ObjectBox 模型定义
		src/main/
			aidl/                   AIDL 接口
			assets/                 内置资源、插件包、项目模板
			cpp/                    App 自有 JNI/CMake 代码
			java/com/ai/assistance/operit/
			res/                    Android 资源与多语言文本
	avator/
		dragonbones/              DragonBones 原生渲染模块
		fbx/                      FBX 原生渲染模块
		mmd/                      MMD 原生渲染模块
	ci/                          CI 脚本和测试
	cmake/                       公共 CMake 拉取逻辑
	docs/
		codebase/                 代码库分析资料
		doc-src/                  当前项目正式开发文档
		TODO/                     大型改造的分步计划
	examples/                    JavaScript/TypeScript 插件源码与产物
	gradle/                      Gradle Wrapper 与版本目录
	llm/
		llama/                    llama.cpp 本地推理模块
		mnn/                      MNN 本地推理模块
	quickjs/                     QuickJS JNI 运行时
	showerclient/                虚拟显示客户端模块
	terminal/                    OperitTerminalCore Git 子模块
	tools/                       构建、调试与开发辅助工具
	web-chat/                    React + Vite Web Chat 前端
```

证据：根 [`settings.gradle.kts`](../settings.gradle.kts)、[`Repo_Arch_Basic.md`](../Repo_Arch_Basic.md) 和 [`docs/codebase/STRUCTURE.md`](codebase/STRUCTURE.md)。

## 模块划分

### 当前 Gradle 模块

| 模块 | 当前职责 | VibeCoding 目标中的初始分类 |
|---|---|---|
| `:app` | 对话、工具、工作区、设置、数据和全部业务 UI | 保留并重构 |
| `:terminal` | Ubuntu 24.04 PRoot/chroot 终端 | 必须保留 |
| `:quickjs` | ToolPkg 和脚本执行运行时 | 保留，属于扩展与代码工具能力 |
| `:llama` | llama.cpp 本地模型推理 | 待能力矩阵评估，不能在无证据时删除 |
| `:mnn` | MNN 本地模型、语音相关原生能力 | 按源码依赖拆分评估，禁止整块误删 |
| `:showerclient` | 虚拟显示与手机 UI 控制 | 删除候选 |
| `:dragonbones` | DragonBones 虚拟形象 | 删除候选 |
| `:mmd` | MMD 虚拟形象 | 删除候选 |
| `:fbx` | FBX 虚拟形象 | 删除候选 |

“删除候选”不等于已授权删除。每个模块必须先完成反向依赖扫描、构建基线和专门步骤计划。

### 安装身份与源码命名空间

| 项目 | 当前事实 | 已确认目标 |
|---|---|---|
| Release `applicationId` | `com.ai.assistance.operit` | `io.github.black0bag.minibile`，与原始 App 并行安装 |
| Android `namespace` | `com.ai.assistance.operit` | 暂时保留，不因安装身份变化做全仓源码迁移 |
| Kotlin 包声明 | 主要位于 `com.ai.assistance.operit.*` | 暂时保留 |
| 动态安装包引用 | 多处使用 `context.packageName`、`BuildConfig.APPLICATION_ID`、`${applicationId}` | 保持动态 |
| 静态安装包引用 | Android JS 测试、ADB/ToolPkg 脚本、广播 action/component、外部目录和开发文档存在硬编码 | T01 逐项迁移和测试，禁止全局字符串替换 |
| 本地签名资产 | 尚无自定义 App Release 密钥 | 工作区根级 `.release-signing/`，Git 仓库外 |

源码命名空间与 Android 安装包 ID 是两件事。T01 只改变安装身份及其真实依赖，不重命名 1100 多个 Kotlin 源文件。

### 当前 App 包边界

| 包 | 当前职责 | 目标处理原则 |
|---|---|---|
| `api/chat` | 对话编排、流式处理和模型 Provider | 重构为编码任务执行主链路 |
| `core/tools` | 179 个工具的注册、调度和执行 | 收敛为编码工具集，并增加硬权限门禁 |
| `core/tools/javascript` | QuickJS、Java Bridge、脚本工具 | 保留编码相关扩展能力 |
| `core/tools/packTool` | ToolPkg 解析、Hook、UI 和 Provider 扩展 | 保留，但必须受会话模式硬权限约束 |
| `core/workflow` | 通用可视化工作流 | 是否服务 VibeCoding 需在能力矩阵中判定 |
| `data/db`、`data/dao` | Room 和 ObjectBox 持久化 | 允许不兼容重建；不承担历史数据迁移义务 |
| `data/repository` | 对话、记忆、工作流等数据访问 | 按目标领域拆分 |
| `integrations` | HTTP、A2A、Intent、Tasker 等外部入口 | 按是否支撑 VibeCoding 逐项评估 |
| `services` | 对话服务、前台服务、悬浮窗、通知等 | 保留任务运行必需部分，删除通用入口候选 |
| `ui/features/chat` | 主对话、工作区与运行过程 UI | 目标产品主界面 |
| `ui/features/settings` | 模型、工具、主题和通用功能设置 | 收敛为编码 Agent 设置 |
| `ui/features/toolbox` | 18 个独立工具页 | 建立能力矩阵后逐项保留或删除 |
| `ui/features/packages` | ToolPkg、MCP、Skill 和市场 | 本地扩展能力与远程市场必须拆开评估 |
| `util` | 流、向量、文档、媒体等公共能力 | 按调用事实保留，禁止按目录名整块删除 |

## 数据流/调用关系

### 当前对话与工具执行链路

```text
用户输入
	→ ChatViewModel / ChatServiceCore
	→ EnhancedAIService
	→ AIServiceFactory / 具体模型 Provider
	→ 流式响应解析
	→ ToolExecutionManager
	→ ToolPermissionSystem
	→ AIToolHandler / ToolExecutor
	→ 工具结果回注对话
	→ Room 保存消息与状态
```

关键事实：

- `EnhancedAIService` 负责工具暴露和对话循环
- `ToolExecutionManager.executeInvocations()` 负责主对话生成的工具权限检查和批量执行
- `AIToolHandler.executeTool()` 还有大量直接调用点，包括 QuickJS、工作流、MCP、工作区和多个 UI 页面
- `AIToolHandler.executeTool()` 当前只执行 Hook 拦截、参数校验和 Executor 调用，不统一执行 `ToolPermissionSystem.checkToolPermission()`
- `AIToolHook` 当前只接收 `AITool`，没有 `sessionId`、模式、调用者或任务上下文

因此，目标 Plan 模式不能只依赖提示词、工具隐藏或 `ToolExecutionManager`。必须把模式策略落实到所有执行路径共同经过的底层门禁。

### 目标 VibeCoding 主流程

```text
用户绑定项目工作区
	→ 用户手动选择 Plan 模式
	→ 只读代码/终端/网络探索，可委派只读 Subagent
	→ Agent 创建并实时维护会话级 TODO
	→ UI 展示计划进度，用户审核方案
	→ 用户手动切换 Build 模式
	→ 写代码、运行构建测试、调用执行型 Subagent
	→ 发生等待或网络异常时形成可恢复检查点
	→ 继续执行 TODO，逐项验证后完成
	→ 输出变更、证据、风险并回填项目文档
```

### 目标编码任务执行内核

下列组件是目标设计，尚未实现：

| 组件 | 职责 |
|---|---|
| `CodingSessionMode` | 会话级 `PLAN` / `BUILD` 状态，只接受用户 UI 操作改变 |
| `ToolExecutionContext` | 携带 session、task、actor、mode 和工作区信息贯穿所有工具调用 |
| `CodingModePolicy` | 定义不同模式允许的能力和终端命令类别 |
| `ToolPolicyGate` | Executor 激活前的统一硬门禁，所有调用来源必须经过 |
| `SessionTodo` | 会话级结构化 TODO、顺序、优先级和状态 |
| `TodoEventBus` | TODO 更新实时推送到 UI |
| `SubagentTask` | 父子会话、任务委派、状态、结果和取消关系 |
| `ExecutionCheckpoint` | 中断、暂停、网络卡顿后的可恢复状态 |
| `ModelCapability` | 模型上下文、输出限制、推理强度和支持参数的标准化模型 |
| `ToolDisplayCatalog` | 协议工具 ID 与简体中文显示信息分离 |

## 依赖与外部接口

### VibeCoding 必需能力边界

用户已确认：只要可能服务 VibeCoding，就不能因“看起来通用”而删除。以下能力暂列为必需或保护性保留：

- 多项目类型工作区：Android、Web、Python、Go、Node.js、Java、Flutter、TypeScript 等
- PRoot Ubuntu 终端及构建、测试、包管理能力
- 文件读取、搜索、创建、差异编辑、移动、归档和下载
- Git 与 GitHub 协作
- 网络搜索、网页读取、依赖文档研究和浏览器预览
- 记忆库，用于编码经验和项目知识
- MCP、Skill、ToolPkg，本地与远程编码工具扩展
- Logcat、APK 安装和 Android 调试等开发能力
- 模型 Provider、模型列表、参数配置和能力探测

### 已明确排除的产品领域

- 以操作其他手机 App 为目的的 UI 自动化
- 日常生活手机控制、通话、短信、蓝牙、音乐和位置能力
- 语音输入输出、唤醒词、TTS、STT
- 图像、音频和视频识别功能模型
- 虚拟形象和 Waifu 交互

注意：截图、ADB、Shizuku、浏览器自动化、APK 工具等能力可能同时用于 Android/Web 开发验证。删除前必须按“用途”而不是按“底层技术”分类。

### 外部接口

| 接口 | 当前用途 | 目标原则 |
|---|---|---|
| LLM Provider API | 对话与功能模型 | 保留文本编码模型，增加能力协商 |
| GitHub API/OAuth | 仓库、发布和账号 | 保留编码协作所需部分 |
| Web 搜索/网页访问 | 技术资料校准 | 必须保留 |
| MCP | 外部编码工具 | 必须保留并纳入模式权限 |
| ToolPkg/Skill | 扩展工具和流程 | 必须保留并纳入模式权限 |
| PRoot/终端 | 编译测试 | 必须保留 |
| Web Chat、A2A、Intent、Tasker | 通用外部入口 | 待目标工作流证据判定 |

## 关键入口文件

| 入口 | 作用 |
|---|---|
| `app/src/main/java/com/ai/assistance/operit/core/application/OperitApplication.kt` | 应用初始化 |
| `app/src/main/java/com/ai/assistance/operit/ui/main/MainActivity.kt` | Android 主 Activity |
| `app/src/main/java/com/ai/assistance/operit/ui/main/OperitApp.kt` | Compose 应用根 |
| `app/src/main/java/com/ai/assistance/operit/ui/main/screens/ScreenRouteRegistry.kt` | 宿主导航单一注册源 |
| `app/src/main/java/com/ai/assistance/operit/ui/features/chat/viewmodel/ChatViewModel.kt` | 聊天与工作区 UI 状态 |
| `app/src/main/java/com/ai/assistance/operit/api/chat/EnhancedAIService.kt` | 对话与 Agent 循环中枢 |
| `app/src/main/java/com/ai/assistance/operit/api/chat/enhance/ToolExecutionManager.kt` | LLM 工具调用解析、权限和执行 |
| `app/src/main/java/com/ai/assistance/operit/core/tools/AIToolHandler.kt` | 全局工具注册与直接执行入口 |
| `app/src/main/java/com/ai/assistance/operit/core/tools/ToolRegistration.kt` | 内置工具注册 |
| `app/src/main/java/com/ai/assistance/operit/ui/permissions/ToolPermissionSystem.kt` | 当前工具审批系统 |
| `app/src/main/java/com/ai/assistance/operit/data/db/AppDatabase.kt` | Room 数据库与迁移 |
| `app/src/main/java/com/ai/assistance/operit/ui/features/chat/webview/workspace/WorkspaceManager.kt` | 项目工作区管理 |
| `app/build.gradle.kts` | 当前 Android 版本、签名、变体和构建任务 |
| `.github/workflows/android-build.yml` | 当前 GitHub Android 构建与临时 Artifact 上传 |
| `.github/workflows/android-tests.yml` | 当前 GitHub JVM 测试 |

## 高风险模块

| 模块 | 风险 | 目标防护 |
|---|---|---|
| 工具执行与权限 | 存在多个直接执行入口，单点 UI 限制可被绕过 | 引入带会话上下文的统一底层门禁 |
| Plan/Build 模式 | 并行会话可能处于不同模式，全局开关会串状态 | 模式必须按会话保存并随调用上下文传播 |
| Subagent | 子代理可能扩大父会话权限 | 父会话 deny 规则作为硬上限，子代理只能进一步收紧 |
| 运行时 TODO | 与仓库计划文档混淆会导致错误持久化和 UI 设计 | 使用会话数据库、事件流和专用 Dock；不写 Markdown |
| 暂停与继续 | SSE 连接不能保证原地续传 | 先定义检查点语义，再选择挂起或恢复策略 |
| 大规模删除 | 功能共享工具、权限、资源和原生模块 | 先建能力矩阵和反向依赖图，分波删除 |
| 模型能力自适应 | Provider 元数据结构不统一 | 建立证据来源和置信状态，未知必须显式显示未知 |
| 中文显示 | 改协议工具名会破坏 ToolPkg/MCP/Prompt 合约 | 协议 ID 保持不变，只本地化显示元数据 |
| 当前测试覆盖 | 主对话链路与工具底层门禁缺少充分测试 | 先补策略和权限测试，再动核心链路 |
| 版本与发布 | `versionName`/`versionCode` 当前硬编码，Tag、Release 和唯一版本源均不存在 | 根 `VERSION` 为单一事实源，CI 校验 App、Tag、Release 和 APK 名称一致 |
| APK 签名 | 当前 CI 未配置仓库 Secret，Release 签名不可复现；Runner 临时 Debug 密钥不能作为长期更新身份 | 使用专用固定密钥和 GitHub Actions Secrets，公开证书指纹但绝不提交私钥或口令 |
| 云端交付 | 当前 Workflow 仅上传保留 14 天的 Artifact，`GITHUB_TOKEN` 默认只读 | 测试成功后才创建不可复用的 Tag/Release 并上传签名 APK，发布 Job 仅授予 `contents: write` |

## 当前验证基线

- 当前主仓源码没有已跟踪的业务代码改动，本地 `HEAD` 与 GitHub `main` 均为 `f323d6c50fa661837fad06d4618462861779b562`
- 外部二进制依赖已下载并部署，但被 `.gitignore` 排除
- 当前本地 Linux 未安装 JDK、Android SDK、NDK 和 CMake；用户已决定不在本地建立 Android 编译环境
- GitHub Actions 已启用，但当前没有运行记录；`android-build.yml` 只构建并上传保留 14 天的 Artifact，不创建 Release
- 当前仓库没有 Tag、GitHub Release、Actions Secrets、Actions Variables 或 Ruleset，默认 `GITHUB_TOKEN` 权限为只读
- `app/build.gradle.kts` 当前硬编码 `versionName = "1.12.1+3"` 与 `versionCode = 46`；目标定制版本尚未建立，用户指定起点为 `0.0.0`
- 因此当前尚未建立可重复、可持续更新、版本一致的签名 APK 发布基线

详细证据见 [`codebase/ARCHITECTURE.md`](codebase/ARCHITECTURE.md)、[`codebase/FEATURES.md`](codebase/FEATURES.md) 和 [`codebase/EXTERNAL_DEPS.md`](codebase/EXTERNAL_DEPS.md)。