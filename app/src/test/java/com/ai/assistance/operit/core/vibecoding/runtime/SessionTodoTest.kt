package com.ai.assistance.operit.core.vibecoding.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTodoTest {

    @Test
    fun `todo starts pending and can transition to in progress`() {
        val todo = newTodo()
        assertEquals(SessionTodoStatus.PENDING, todo.status)
        val started = todo.start()
        assertEquals(SessionTodoStatus.IN_PROGRESS, started.status)
    }

    @Test
    fun `only one main todo can be in progress at a time`() {
        val first = newTodo("a", order = 1).start()
        val second = newTodo("b", order = 2)
        assertFalse(second.canStart(listOf(first)))
        assertTrue(first.canStart(emptyList()))
    }

    @Test
    fun `sub todo does not block another main todo start`() {
        val main = newTodo("a", order = 1).start()
        val sub = newTodo("a-1", order = 1, parentTaskId = "a")
        val other = newTodo("b", order = 2)
        // 主 TODO 进行中会阻塞其他主 TODO
        assertFalse(other.canStart(listOf(main)))
        // 但子 TODO 自身进行中不会阻塞其他主 TODO
        val subStarted = sub.start()
        assertTrue(other.canStart(listOf(subStarted)))
    }

    @Test
    fun `completed and cancelled are terminal states`() {
        val completed = newTodo().complete()
        assertEquals(SessionTodoStatus.COMPLETED, completed.status)
        val cancelled = newTodo().cancel("out of scope")
        assertEquals(SessionTodoStatus.CANCELLED, cancelled.status)
        assertEquals("out of scope", cancelled.blockedReason)
    }

    @Test
    fun `block records reason without changing status`() {
        val todo = newTodo().start().block("network stall")
        assertEquals(SessionTodoStatus.IN_PROGRESS, todo.status)
        assertEquals("network stall", todo.blockedReason)
    }

    private fun newTodo(
        id: String = "todo-1",
        order: Int = 1,
        parentTaskId: String? = null,
    ) = SessionTodo(
        id = id,
        sessionId = "chat-1",
        content = "Implement feature",
        order = order,
        parentTaskId = parentTaskId,
    )
}