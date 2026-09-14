package com.ai.assistance.operit.core.vibecoding.runtime

/**
 * 会话级结构化 TODO（Phase D 纵向切片运行时模型）。
 *
 * 与 Phase A 领域模型一致，本类保持纯 Kotlin、无 Android/IO 依赖，便于 JVM 单测。
 * TODO 是 Agent 任务编排的运行时事实，不是 Markdown 文档；状态转换由代码校验。
 */
enum class SessionTodoStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    CANCELLED,
}

enum class SessionTodoPriority {
    HIGH,
    MEDIUM,
    LOW,
}

data class SessionTodo(
    val id: String,
    val sessionId: String,
    val content: String,
    val status: SessionTodoStatus = SessionTodoStatus.PENDING,
    val priority: SessionTodoPriority = SessionTodoPriority.MEDIUM,
    val order: Int = 0,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = System.currentTimeMillis(),
    val parentTaskId: String? = null,
    val blockedReason: String? = null,
) {
    /** 同时只能有一项主 TODO 处于进行中（无父任务即主 TODO）。 */
    fun canStart(others: List<SessionTodo>): Boolean {
        if (status != SessionTodoStatus.PENDING) return false
        return others.none { it.parentTaskId == null && it.status == SessionTodoStatus.IN_PROGRESS }
    }

    fun start(): SessionTodo =
        copy(status = SessionTodoStatus.IN_PROGRESS, updatedAtEpochMillis = System.currentTimeMillis())

    fun complete(): SessionTodo =
        copy(status = SessionTodoStatus.COMPLETED, updatedAtEpochMillis = System.currentTimeMillis())

    fun cancel(reason: String): SessionTodo =
        copy(
            status = SessionTodoStatus.CANCELLED,
            blockedReason = reason,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )

    fun block(reason: String): SessionTodo =
        copy(
            blockedReason = reason,
            updatedAtEpochMillis = System.currentTimeMillis(),
        )
}