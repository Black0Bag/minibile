package com.ai.assistance.operit.data.vibecoding

import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodo
import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodoPriority
import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodoStatus
import com.ai.assistance.operit.data.model.SessionTodoEntity

/** Maps between pure runtime [SessionTodo] and Room [SessionTodoEntity]. */
object SessionTodoMapper {
    fun toEntity(todo: SessionTodo): SessionTodoEntity =
        SessionTodoEntity(
            id = todo.id,
            sessionId = todo.sessionId,
            content = todo.content,
            status = todo.status.name,
            priority = todo.priority.name,
            order = todo.order,
            createdAt = todo.createdAtEpochMillis,
            updatedAt = todo.updatedAtEpochMillis,
            parentTaskId = todo.parentTaskId,
            blockedReason = todo.blockedReason,
        )

    fun toDomain(entity: SessionTodoEntity): SessionTodo =
        SessionTodo(
            id = entity.id,
            sessionId = entity.sessionId,
            content = entity.content,
            status = SessionTodoStatus.valueOf(entity.status),
            priority = SessionTodoPriority.valueOf(entity.priority),
            order = entity.order,
            createdAtEpochMillis = entity.createdAt,
            updatedAtEpochMillis = entity.updatedAt,
            parentTaskId = entity.parentTaskId,
            blockedReason = entity.blockedReason,
        )
}