package com.ai.assistance.operit.data.repository

import com.ai.assistance.operit.core.vibecoding.runtime.SessionTaskCoordinator
import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodo
import com.ai.assistance.operit.data.dao.SessionTodoDao
import com.ai.assistance.operit.data.vibecoding.SessionTodoMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 会话级 TODO 仓库（Phase D 纵向切片持久化层）。
 *
 * 通过 [SessionTodoDao] 提供会话隔离的 TODO 流，并委托 [SessionTaskCoordinator]
 * 完成状态机与 TODO 清单的编排；DAO 映射由 [SessionTodoMapper] 完成。
 */
class SessionTodoRepository(
    private val dao: SessionTodoDao,
) {
    val coordinator = SessionTaskCoordinator()

    fun observeBySession(sessionId: String): Flow<List<SessionTodo>> =
        dao.observeBySession(sessionId).map { entities ->
            entities.map(SessionTodoMapper::toDomain)
        }

    suspend fun replaceBySession(sessionId: String, todos: List<SessionTodo>) {
        dao.replaceBySession(sessionId, todos.map(SessionTodoMapper::toEntity))
    }

    suspend fun getBySession(sessionId: String): List<SessionTodo> =
        dao.getBySession(sessionId).map(SessionTodoMapper::toDomain)
}