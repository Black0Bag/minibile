# 07 模型能力自动发现

## 旧实现情况

当前 `ModelListFetcher.kt` 可以适配多种模型列表响应，但主要产出模型 ID 和显示名称，没有形成上下文长度、最大输出、推理控制、支持参数和模态等统一能力模型。`ThinkingQualityMapping.kt` 主要依赖 Provider 与模型名规则匹配，模型配置 UI 无法完整呈现服务端实际能力。

## 意图修正

模型配置应尽可能读取 API 实际返回的所有可验证参数，并将“服务端已声明”“用户明确配置”和“未知”严格区分。界面只展示有证据支持的控件。

## 预期新实现

### 标准能力模型

`ModelCapability` 至少包含：

- Provider ID、endpoint 和模型 ID
- 上下文长度
- 最大输出 Token
- 支持参数集合
- 推理是否支持、是否强制、可选强度和默认强度
- 输入与输出模态
- 结构化输出、工具调用和流式能力
- 可选价格和知识截止时间
- 每个字段的来源与可信状态
- 原始响应摘要、获取时间和缓存版本

### 数据来源

按证据强度处理：

1. 当前 endpoint 的真实模型 API 响应
2. Provider 明确且稳定的官方能力接口
3. 用户在配置页明确填写的覆盖值
4. 没有证据时为“未知”

不得根据模型名称、发布时间或相似模型猜测能力。

### Provider 适配

- 原始响应先保存为诊断对象，再由 Provider 适配器标准化
- OpenRouter 等富元数据接口解析 `context_length`、`top_provider`、`supported_parameters`、`reasoning`、`architecture` 等已返回字段
- 只返回模型 ID 的 OpenAI 兼容端点，其余字段保持未知
- 自定义兼容端点只解析实际存在且类型正确的字段
- 单字段解析失败不污染其他已验证字段

### UI

- 模型列表直接显示上下文长度和能力摘要
- 最大输出输入框受已知服务端上限约束
- 推理强度控件严格按服务端选项生成
- 未知字段显示“未知”，并允许用户在高级设置中明确配置
- 显示来源与最后刷新时间
- 更换 Provider、endpoint 或 Key 后使对应缓存失效
- 提供手动刷新和原始响应诊断入口

## 作用域候选

- `api/chat/llmprovider/ModelListFetcher.kt`
- `api/chat/llmprovider/ModelListFetcher` 的返回模型
- `api/chat/llmprovider/ThinkingQualityMapping.kt`
- `data/model/ModelConfigData.kt`
- `data/preferences/ModelConfigManager.kt`
- `ui/features/settings/screens/ModelConfigScreen.kt`
- `ui/features/settings/sections/ModelApiSettingsSection.kt`
- `ui/features/settings/sections/ModelParametersSection.kt`
- Provider 连接测试与 Web Chat 模型数据接口

## 验证

- 保存真实且脱敏的 Provider 响应夹具
- 富元数据 Provider 能完整解析已提供字段
- 仅 ID Provider 不产生虚假能力
- 自定义端点的缺字段、错类型和额外字段行为明确
- 思考强度选项与服务端声明完全一致
- endpoint、Key、Provider 变化后缓存失效
- UI 能区分服务端值、用户值和未知值
- API Key 与原始敏感响应不进入日志或测试仓库

## 风险

- 不同 Provider 使用同名但不同语义的字段
- 模型能力会随路由和时间变化，缓存可能过期
- 上下文长度与最大输出可能同时受上游路由限制
- 用户覆盖值可能超过服务端上限
- 原始响应可能包含不适合持久化的字段

## 回滚

新能力模型和解析器独立于现有模型请求执行。初期只用于展示和诊断，验证稳定后再驱动请求参数。解析器与 UI 分独立提交；失败时可回退显示层而不改变现有模型配置协议。未知字段始终保持未知。