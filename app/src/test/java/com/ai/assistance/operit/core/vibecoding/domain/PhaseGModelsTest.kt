package com.ai.assistance.operit.core.vibecoding.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RepairBudgetTest {
    @Test
    fun `budget allows code attempts within limit`() {
        val budget = RepairBudget(maxCodeAttempts = 2)
        assertTrue(budget.canAttempt(FailureCategory.CODE))
        val after1 = budget.consume(FailureCategory.CODE)
        assertTrue(after1.canAttempt(FailureCategory.CODE))
        val after2 = after1.consume(FailureCategory.CODE)
        assertFalse(after2.canAttempt(FailureCategory.CODE))
        assertTrue(after2.isExhausted)
    }

    @Test
    fun `budget allows infra retries within limit`() {
        val budget = RepairBudget(maxInfraRetries = 3)
        assertTrue(budget.canAttempt(FailureCategory.INFRASTRUCTURE))
        val after1 = budget.consume(FailureCategory.INFRASTRUCTURE)
        val after2 = after1.consume(FailureCategory.INFRASTRUCTURE)
        assertTrue(after2.canAttempt(FailureCategory.INFRASTRUCTURE))
        val after3 = after2.consume(FailureCategory.INFRASTRUCTURE)
        assertFalse(after3.canAttempt(FailureCategory.INFRASTRUCTURE))
        assertTrue(after3.isExhausted)
    }

    @Test
    fun `code and infra budgets are independent`() {
        val budget = RepairBudget(maxCodeAttempts = 1, maxInfraRetries = 2)
        val afterCode = budget.consume(FailureCategory.CODE)
        assertFalse(afterCode.canAttempt(FailureCategory.CODE))
        assertTrue(afterCode.canAttempt(FailureCategory.INFRASTRUCTURE))
        val afterInfra1 = afterCode.consume(FailureCategory.INFRASTRUCTURE)
        assertFalse(afterInfra1.canAttempt(FailureCategory.CODE))
        assertTrue(afterInfra1.canAttempt(FailureCategory.INFRASTRUCTURE))
    }
}

class FailureFingerprintTest {
    @Test
    fun `same error pattern produces same fingerprint`() {
        val fp1 = FailureFingerprint("hash1", "build", "llvm-strip error", FailureCategory.INFRASTRUCTURE)
        val fp2 = FailureFingerprint("hash1", "build", "llvm-strip error", FailureCategory.INFRASTRUCTURE)
        assertEquals(fp1.fingerprint, fp2.fingerprint)
    }

    @Test
    fun `different error patterns produce different fingerprints`() {
        val fp1 = FailureFingerprint("hash1", "build", "llvm-strip error", FailureCategory.INFRASTRUCTURE)
        val fp2 = FailureFingerprint("hash2", "test", "assertion failed", FailureCategory.CODE)
        assertFalse(fp1.fingerprint == fp2.fingerprint)
    }
}

class BuildAttemptTest {
    @Test
    fun `attempt lifecycle starts as initiated`() {
        val attempt = BuildAttempt(
            attemptId = "att-1",
            taskId = "task-1",
            runId = "run-1",
            sourceSha = "abc123",
            failureFingerprint = "hash1",
            failureCategory = FailureCategory.CODE,
            fixDescription = "fix null pointer",
            status = AttemptStatus.INITIATED,
        )
        assertEquals(AttemptStatus.INITIATED, attempt.status)
    }

    @Test
    fun `attempt with fix commit transitions to pr opened`() {
        val attempt = BuildAttempt(
            attemptId = "att-1",
            taskId = "task-1",
            runId = "run-1",
            sourceSha = "abc123",
            failureFingerprint = "hash1",
            failureCategory = FailureCategory.CODE,
            fixDescription = "fix null pointer",
            fixCommitSha = "def456",
            status = AttemptStatus.PR_OPENED,
        )
        assertEquals(AttemptStatus.PR_OPENED, attempt.status)
        assertEquals("def456", attempt.fixCommitSha)
    }
}

class RecoveryCheckpointTest {
    @Test
    fun `checkpoint captures recovery target`() {
        val cp = RecoveryCheckpoint(
            checkpointId = "cp-1",
            taskId = "task-1",
            buildRunId = "run-1",
            sourceSha = "abc123",
            headSha = "def456",
            recoveryTarget = RecoveryTarget.RESUME_REPAIR,
            snapshotJson = "{}",
        )
        assertEquals(RecoveryTarget.RESUME_REPAIR, cp.recoveryTarget)
        assertEquals("run-1", cp.buildRunId)
    }

    @Test
    fun `resume tracking checkpoint has no attempt`() {
        val cp = RecoveryCheckpoint(
            checkpointId = "cp-2",
            taskId = "task-1",
            buildRunId = "run-1",
            sourceSha = "abc123",
            headSha = "def456",
            recoveryTarget = RecoveryTarget.RESUME_TRACKING,
            snapshotJson = "{}",
        )
        assertEquals(null, cp.currentAttemptId)
    }
}

class SubagentTaskTest {
    @Test
    fun `subagent task starts as pending`() {
        val task = SubagentTask(
            subtaskId = "sub-1",
            parentSessionId = "sess-1",
            parentTaskId = "task-1",
            agentName = "research-agent",
            instruction = "search for best practices",
            mode = CodingSessionMode.PLAN,
            status = SubagentStatus.PENDING,
        )
        assertEquals(SubagentStatus.PENDING, task.status)
        assertEquals(CodingSessionMode.PLAN, task.mode)
    }

    @Test
    fun `subagent task completion sets result and completedAt`() {
        val task = SubagentTask(
            subtaskId = "sub-1",
            parentSessionId = "sess-1",
            parentTaskId = "task-1",
            agentName = "research-agent",
            instruction = "search for best practices",
            mode = CodingSessionMode.PLAN,
            status = SubagentStatus.COMPLETED,
            result = "found 3 relevant articles",
            completedAt = System.currentTimeMillis(),
        )
        assertEquals(SubagentStatus.COMPLETED, task.status)
        assertEquals("found 3 relevant articles", task.result)
    }

    @Test
    fun `subagent task failure sets error message`() {
        val task = SubagentTask(
            subtaskId = "sub-1",
            parentSessionId = "sess-1",
            parentTaskId = "task-1",
            agentName = "research-agent",
            instruction = "search for best practices",
            mode = CodingSessionMode.PLAN,
            status = SubagentStatus.FAILED,
            errorMessage = "network timeout",
        )
        assertEquals(SubagentStatus.FAILED, task.status)
        assertEquals("network timeout", task.errorMessage)
    }
}
