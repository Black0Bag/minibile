package com.ai.assistance.operit.core.vibecoding.policy

import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingActorType

/**
 * 每次工具调用都携带的执行上下文。
 *
 * 与 Phase A 领域模型一致，本类保持纯 Kotlin、无 Android/依赖，便于 JVM 单测。
 * 通过 [ToolExecutionContext] 把「会话、模式、调用者、任务、工作区、审批」绑定到单次调用，
 * 使 [ToolPolicyGate] 能在 Executor 激活前以 fail-closed 方式做统一判定。
 */
data class ToolExecutionContext(
    val sessionId: String,
    val modeSnapshot: CodingSessionMode,
    val actorType: VibeCodingActorType,
    val actorId: String? = null,
    val taskId: String? = null,
    val workspaceId: String? = null,
    val invocationSource: ToolInvocationSource = ToolInvocationSource.MAIN_AGENT,
    val approvedPlanRevision: Long? = null,
    val currentPlanRevision: Long? = null,
)

/** 工具调用来源：区分 Agent 自动调用与用户/子系统直接调用。 */
enum class ToolInvocationSource {
    USER_DIRECT,
    MAIN_AGENT,
    SUBAGENT,
    TOOLPKG,
    MCP,
    QUICKJS,
    WORKFLOW,
    SKILL,
}

/** 调用是否携带有效的执行上下文。 */
fun ToolExecutionContext?.isContextual(): Boolean = this != null