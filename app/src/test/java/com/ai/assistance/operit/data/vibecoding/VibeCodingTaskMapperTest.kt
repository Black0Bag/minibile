package com.ai.assistance.operit.data.vibecoding

import com.ai.assistance.operit.core.vibecoding.domain.ApprovalRecord
import com.ai.assistance.operit.core.vibecoding.domain.BuildBackend
import com.ai.assistance.operit.core.vibecoding.domain.BuildFailureCategory
import com.ai.assistance.operit.core.vibecoding.domain.BuildRunEvidence
import com.ai.assistance.operit.core.vibecoding.domain.BuildRunStatus
import com.ai.assistance.operit.core.vibecoding.domain.BuildStrategyDecision
import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.DocumentationDecision
import com.ai.assistance.operit.core.vibecoding.domain.PlanStep
import com.ai.assistance.operit.core.vibecoding.domain.RequirementSpec
import com.ai.assistance.operit.core.vibecoding.domain.ResearchEvidenceStatus
import com.ai.assistance.operit.core.vibecoding.domain.ResearchRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReviewRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReleaseEvidence
import com.ai.assistance.operit.core.vibecoding.domain.TaskPlanRevision
import com.ai.assistance.operit.core.vibecoding.domain.ValidationRun
import com.ai.assistance.operit.core.vibecoding.domain.ValidationStatus
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingActorType
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingRiskLevel
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTask
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTaskStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeCodingTaskMapperTest {
    @Test
    fun `round trip preserves all fields`() {
        val original = VibeCodingTask(
            id = "task-1",
            sessionId = "chat-1",
            workspaceId = "workspace-1",
            stage = VibeCodingTaskStage.WAITING_PLAN_APPROVAL,
            mode = CodingSessionMode.PLAN,
            requirement = RequirementSpec(
                goal = "implement feature",
                inScope = listOf("src/main"),
                outOfScope = listOf("tests"),
                acceptanceCriteria = listOf("compiles"),
            ),
            localEvidenceFallbackApproved = true,
            plan = TaskPlanRevision(
                revision = 3,
                summary = "do the thing",
                riskLevel = VibeCodingRiskLevel.L2,
                affectedPaths = listOf("src/Main.kt"),
                steps = listOf(PlanStep("1", "write code", "run tests")),
                rollbackPlan = "git revert",
            ),
            approval = ApprovalRecord(
                planRevision = 3,
                actorType = VibeCodingActorType.USER,
                approvedAtEpochMillis = 1000L,
            ),
            changedPaths = setOf("src/Main.kt", "src/Other.kt"),
            validationRuns = listOf(
                ValidationRun("test-1", ValidationStatus.PASSED, "./gradlew test", "all passed"),
            ),
            documentationDecision = DocumentationDecision(
                required = true,
                updatedPaths = listOf("docs/structure.md"),
                rationale = "new module",
            ),
            review = ReviewRecord(evidence = "diff reviewed"),
            buildStrategy = BuildStrategyDecision(BuildBackend.CLOUD, "heavy build"),
            buildRuns = listOf(
                BuildRunEvidence("run-1", "abc", BuildRunStatus.SUCCEEDED, "build ok"),
            ),
            releaseEvidence = ReleaseEvidence.Published(
                tag = "v1.0.0",
                sourceSha = "abc",
                assetNames = listOf("app.apk"),
                digestVerified = true,
                signatureRequired = true,
                signatureVerified = true,
            ),
            recoveryTarget = VibeCodingTaskStage.IMPLEMENTING,
        )

        val entity = VibeCodingTaskMapper.toEntity(original)
        val researchEntities = original.researchRecords.map { VibeCodingTaskMapper.toEntity(it, original.id) }
        val validationEntities = original.validationRuns.map { VibeCodingTaskMapper.toEntity(it, original.id) }
        val buildEntities = original.buildRuns.map { VibeCodingTaskMapper.toEntity(it, original.id) }

        val restored = VibeCodingTaskMapper.toDomain(entity, researchEntities, validationEntities, buildEntities)

        assertEquals(original.id, restored.id)
        assertEquals(original.sessionId, restored.sessionId)
        assertEquals(original.workspaceId, restored.workspaceId)
        assertEquals(original.stage, restored.stage)
        assertEquals(original.mode, restored.mode)
        assertEquals(original.requirement, restored.requirement)
        assertEquals(original.localEvidenceFallbackApproved, restored.localEvidenceFallbackApproved)
        assertEquals(original.plan, restored.plan)
        assertEquals(original.approval, restored.approval)
        assertEquals(original.changedPaths, restored.changedPaths)
        assertEquals(original.validationRuns, restored.validationRuns)
        assertEquals(original.documentationDecision, restored.documentationDecision)
        assertEquals(original.review, restored.review)
        assertEquals(original.buildStrategy, restored.buildStrategy)
        assertEquals(original.buildRuns, restored.buildRuns)
        assertEquals(original.releaseEvidence, restored.releaseEvidence)
        assertEquals(original.recoveryTarget, restored.recoveryTarget)
    }

    @Test
    fun `empty task round trips with nulls`() {
        val original = VibeCodingTask(
            id = "task-2",
            sessionId = "chat-2",
            workspaceId = "ws-2",
        )

        val entity = VibeCodingTaskMapper.toEntity(original)
        val restored = VibeCodingTaskMapper.toDomain(entity, emptyList(), emptyList(), emptyList())

        assertEquals(original.id, restored.id)
        assertEquals(VibeCodingTaskStage.NEW, restored.stage)
        assertEquals(CodingSessionMode.PLAN, restored.mode)
        assertNull(restored.requirement)
        assertNull(restored.plan)
        assertNull(restored.approval)
        assertTrue(restored.changedPaths.isEmpty())
        assertTrue(restored.researchRecords.isEmpty())
        assertTrue(restored.validationRuns.isEmpty())
        assertNull(restored.recoveryTarget)
    }

    @Test
    fun `research record round trip`() {
        val original = ResearchRecord(
            query = "Kotlin sealed classes",
            source = "https://kotlinlang.org",
            conclusion = "exhaustive when",
            status = ResearchEvidenceStatus.VERIFIED,
        )

        val entity = VibeCodingTaskMapper.toEntity(original, "task-1")
        val restored = entity.toDomain()

        assertEquals(original, restored)
    }

    @Test
    fun `build run with failure category round trips`() {
        val original = BuildRunEvidence(
            runId = "run-1",
            sourceSha = "abc",
            status = BuildRunStatus.FAILED,
            evidence = "compile error",
            failureCategory = BuildFailureCategory.CODE,
        )

        val entity = VibeCodingTaskMapper.toEntity(original, "task-1")
        val restored = entity.toDomain()

        assertEquals(original, restored)
        assertEquals(BuildFailureCategory.CODE, restored.failureCategory)
    }

    @Test
    fun `not required release evidence round trips`() {
        val original = ReleaseEvidence.NotRequired("local only")
        val task = VibeCodingTask(
            id = "task-3",
            sessionId = "chat-3",
            workspaceId = "ws-3",
            releaseEvidence = original,
        )

        val entity = VibeCodingTaskMapper.toEntity(task)
        val restored = VibeCodingTaskMapper.toDomain(entity, emptyList(), emptyList(), emptyList())

        assertNotNull(restored.releaseEvidence)
        assertTrue(restored.releaseEvidence is ReleaseEvidence.NotRequired)
        assertEquals(original, restored.releaseEvidence)
    }
}