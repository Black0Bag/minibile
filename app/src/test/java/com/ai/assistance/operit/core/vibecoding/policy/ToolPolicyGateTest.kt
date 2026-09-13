package com.ai.assistance.operit.core.vibecoding.policy

import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingActorType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolPolicyGateTest {

    private val planContext = ToolExecutionContext(
        sessionId = "s1",
        modeSnapshot = CodingSessionMode.PLAN,
        actorType = VibeCodingActorType.MAIN_AGENT,
        actorId = "main",
    )

    private val buildContext = ToolExecutionContext(
        sessionId = "s1",
        modeSnapshot = CodingSessionMode.BUILD,
        actorType = VibeCodingActorType.MAIN_AGENT,
        actorId = "main",
        approvedPlanRevision = 1L,
        currentPlanRevision = 1L,
    )

    @Test
    fun `null context is allowed for backwards compatibility`() {
        assertTrue(ToolPolicyGate.evaluate("write_file", null) is ToolPolicyDecision.Allow)
    }

    @Test
    fun `user direct call is allowed regardless of mode`() {
        val userContext = planContext.copy(
            invocationSource = ToolInvocationSource.USER_DIRECT,
            actorType = VibeCodingActorType.USER,
        )
        assertTrue(ToolPolicyGate.evaluate("write_file", userContext) is ToolPolicyDecision.Allow)
    }

    @Test
    fun `plan mode denies write tools`() {
        val decision = ToolPolicyGate.evaluate("write_file", planContext)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.PLAN_WRITE_DENIED, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `plan mode allows read tools`() {
        assertTrue(ToolPolicyGate.evaluate("read_file", planContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("list_files", planContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("grep_code", planContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("visit_web", planContext) is ToolPolicyDecision.Allow)
    }

    @Test
    fun `plan mode denies unknown tools conservatively`() {
        val decision = ToolPolicyGate.evaluate("some_new_writing_tool", planContext)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.PLAN_WRITE_DENIED, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `build mode requires approval`() {
        val noApproval = buildContext.copy(approvedPlanRevision = null)
        val decision = ToolPolicyGate.evaluate("write_file", noApproval)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.BUILD_APPROVAL_REQUIRED, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `build mode rejects stale approval`() {
        val stale = buildContext.copy(currentPlanRevision = 2L)
        val decision = ToolPolicyGate.evaluate("write_file", stale)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.APPROVAL_STALE, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `build mode allows write with current approval`() {
        assertTrue(ToolPolicyGate.evaluate("write_file", buildContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("edit_file", buildContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("execute_shell", buildContext) is ToolPolicyDecision.Allow)
    }

    @Test
    fun `disallowed actor types are rejected`() {
        val toolActor = buildContext.copy(actorType = VibeCodingActorType.TOOL)
        val decision = ToolPolicyGate.evaluate("read_file", toolActor)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.ACTOR_NOT_ALLOWED, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `package read tools are allowed in plan`() {
        assertTrue(ToolPolicyGate.evaluate("github:get_file_content", planContext) is ToolPolicyDecision.Allow)
        assertTrue(ToolPolicyGate.evaluate("github:search_repositories", planContext) is ToolPolicyDecision.Allow)
    }

    @Test
    fun `empty tool name is rejected`() {
        val decision = ToolPolicyGate.evaluate("  ", planContext)
        assertTrue(decision is ToolPolicyDecision.Deny)
        assertEquals(ToolPolicyRejectionCode.UNKNOWN_TOOL, (decision as ToolPolicyDecision.Deny).code)
    }

    @Test
    fun `isWriteTool classifies conservatively`() {
        assertTrue(ToolPolicyGate.isWriteTool("write_file"))
        assertTrue(ToolPolicyGate.isWriteTool("edit_file"))
        assertTrue(ToolPolicyGate.isWriteTool("execute_shell"))
        assertFalse(ToolPolicyGate.isWriteTool("read_file"))
        assertFalse(ToolPolicyGate.isWriteTool("github:get_file_content"))
        assertTrue(ToolPolicyGate.isWriteTool("unknown_tool"))
    }
}