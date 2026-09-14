package com.ai.assistance.operit.data.vibecoding

import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodo
import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodoPriority
import com.ai.assistance.operit.core.vibecoding.runtime.SessionTodoStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTodoMapperTest {

    @Test
    fun `round trip preserves all fields`() {
        val original =
            SessionTodo(
                id = "todo-1",
                sessionId = "chat-1",
                content = "Implement feature",
                status = SessionTodoStatus.IN_PROGRESS,
                priority = SessionTodoPriority.HIGH,
                order = 3,
                createdAtEpochMillis = 1000L,
                updatedAtEpochMillis = 2000L,
                parentTaskId = "task-1",
                blockedReason = null,
            )

        val entity = SessionTodoMapper.toEntity(original)
        val restored = SessionTodoMapper.toDomain(entity)

        assertEquals(original.id, restored.id)
        assertEquals(original.sessionId, restored.sessionId)
        assertEquals(original.content, restored.content)
        assertEquals(original.status, restored.status)
        assertEquals(original.priority, restored.priority)
        assertEquals(original.order, restored.order)
        assertEquals(original.createdAtEpochMillis, restored.createdAtEpochMillis)
        assertEquals(original.updatedAtEpochMillis, restored.updatedAtEpochMillis)
        assertEquals(original.parentTaskId, restored.parentTaskId)
        assertEquals(original.blockedReason, restored.blockedReason)
        assertEquals(original, restored)
    }

    @Test
    fun `default todo round trips with nulls`() {
        val original =
            SessionTodo(
                id = "todo-2",
                sessionId = "chat-2",
                content = "Explore code",
            )

        val entity = SessionTodoMapper.toEntity(original)
        val restored = SessionTodoMapper.toDomain(entity)

        assertEquals(SessionTodoStatus.PENDING, restored.status)
        assertEquals(SessionTodoPriority.MEDIUM, restored.priority)
        assertNull(restored.parentTaskId)
        assertNull(restored.blockedReason)
        assertEquals(original, restored)
    }

    @Test
    fun `blocked reason is preserved`() {
        val original =
            SessionTodo(
                id = "todo-3",
                sessionId = "chat-3",
                content = "Research",
            ).start().block("network stall")

        val restored = SessionTodoMapper.toDomain(SessionTodoMapper.toEntity(original))

        assertEquals(SessionTodoStatus.IN_PROGRESS, restored.status)
        assertEquals("network stall", restored.blockedReason)
        assertTrue(restored.updatedAtEpochMillis >= restored.createdAtEpochMillis)
    }
}