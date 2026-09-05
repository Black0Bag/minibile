# 10 分波删除非编码领域

## 旧实现情况

当前应用同时承担通用手机助手和开发工作台职责。路由、Manifest、权限、工具、Provider、资源和 Gradle 模块存在跨领域共享，直接按目录删除会误伤 Android/Web 开发能力。

## 意图修正

在新编码任务执行内核稳定后，以步骤 02 的能力矩阵为唯一删除依据，彻底清除不服务 VibeCoding 的产品领域。删除意味着源码、入口、权限、资源、依赖和文档均消失，不使用隐藏开关保留空壳。

## 预期新实现

### 已确认删除领域

- 以控制其他手机 App 为目的的 UI 自动化产品功能
- 通话、短信、蓝牙、音乐、位置和日常生活控制
- TTS、STT、语音唤醒、连续语音对话和系统默认语音助理
- 图像、音频和视频识别功能模型分类
- 虚拟形象、Waifu、DragonBones、MMD、FBX、glTF/GLB、WebP/MP4 形象播放
- 与上述领域专用的设置、工具箱页面、Service、Receiver、权限、资源和依赖

### 保护性保留并拆分评估

这些能力可能服务编码，不得随同名通用功能误删：

- ADB/Shizuku：Android 应用安装、Logcat、测试和调试
- 截图：网页/应用开发的视觉验收；不等于保留视觉模型
- 浏览器自动化：Web 开发预览和验收
- APK 解析/安装：Android 开发和测试
- 文件下载、网络请求和 GitHub：依赖与源码获取
- QuickJS、ToolPkg、MCP、Skill：编码扩展
- 记忆：项目规则和编码经验
- FFmpeg/文档转换：只有能力矩阵证明编码流程需要时保留
- Web Chat、A2A、Intent、Tasker、工作流：逐项用真实编码场景决定
- MNN/llama.cpp：按文本编码模型价值、构建成本和依赖耦合决定

### 删除波次

具体文件清单在步骤 02 用户确认后生成。原则顺序：

1. 用户入口和功能模型分类
2. Agent 工具注册与 Prompt
3. 业务 Service/Repository/UI
4. Manifest 组件与 Android 权限
5. 资源和字符串
6. 生产依赖与 Gradle 模块
7. 原生代码、assets 和构建脚本
8. 测试、文档和 CI 残留

每个产品领域单独提交，不在一个提交中混删多个领域。

## 作用域候选

全仓，但每个波次必须收窄到能力矩阵批准的文件。重点包括：

- `ScreenRouteRegistry.kt`
- `ToolRegistration.kt` 与工具 Prompt/JS/类型/示例契约
- `FunctionType.kt`
- `AndroidManifest.xml`
- `app/build.gradle.kts`、`settings.gradle.kts`
- `api/speech`、`api/voice`
- `core/avatar`、`avator/`、`showerclient/`
- 手机控制相关 defaultTool/system/agent 代码
- settings/toolbox/assistant/widget/floating 等 UI
- `res/values*` 和 assets

## 验证

每波必须完成：

- 反向依赖搜索无遗留引用
- Gradle 配置可解析
- Kotlin 编译与相关单测
- APK 构建和真机启动
- 路由、设置、工具列表无死入口
- Manifest 无无效组件和多余权限
- 资源、依赖和原生库无孤儿
- 保留的 Android/Web 开发场景仍可运行
- APK 体积、权限数、工具数和模块数与基线对比

## 风险

- 删除共享底层能力导致编码验证退化
- 工具协议七处契约未同步
- 删除多语言资源造成资源解析失败
- 原生 Gradle 模块移除后 CMake 或打包任务残留
- 删除数据库实体后 DAO、备份和导出仍引用
- 一次改动过大导致无法定位回归

## 回滚

每个领域独立提交并记录提交 SHA、构建结果和真机结果。失败使用 `git revert` 回退该领域提交，不使用破坏性重置。数据库无需向旧版本兼容；回退 APK 后通过清除应用数据恢复。未通过验证的删除波次不得与后续波次合并。