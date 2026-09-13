package com.ai.assistance.operit.core.vibecoding.policy

/**
 * 工具策略判定结果。
 *
 * - [Allow]：允许在 Executor 激活前继续。
 * - [Deny]：拒绝执行，携带稳定的拒绝码与可读原因，供日志、UI 和审计使用。
 */
sealed interface ToolPolicyDecision {
    data object Allow : ToolPolicyDecision

    data class Deny(
        val code: ToolPolicyRejectionCode,
        val reason: String,
    ) : ToolPolicyDecision
}

/** 统一拒绝码。 */
enum class ToolPolicyRejectionCode {
    MISSING_CONTEXT,           // 缺少执行上下文（fail-closed）
    PLAN_WRITE_DENIED,         // PLAN 模式下尝试写操作
    BUILD_APPROVAL_REQUIRED,   // BUILD 模式缺少当前计划审批
    APPROVAL_STALE,            // 计划已变化，旧审批失效
    ACTOR_NOT_ALLOWED,         // 调用者类型不被允许
    UNKNOWN_TOOL,              // 工具未注册或不可判定
}