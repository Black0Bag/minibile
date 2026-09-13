package com.ai.assistance.operit.core.vibecoding.runtime

import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.PlanStep
import com.ai.assistance.operit.core.vibecoding.domain.RequirementSpec
import com.ai.assistance.operit.core.vibecoding.domain.ResearchEvidenceStatus
import com.ai.assistance.operit.core.vibecoding.domain.ResearchRecord
import com.ai.assistance.operit.core.vibecoding.domain.TaskPlanRevision
import com.ai.assistance.operit.core.vibecoding.domain.ValidationRun
import com.ai.assistance.operit.core.vibecoding.domain.ValidationStatus
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingRiskLevel
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTaskStage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionTaskCoordinatorTest {

    private val coordinator = SessionTaskCoordinator()

    @Test
    fun `vertical slice drives full lifecycle and todos`() {
        // 澄清 -> 探索 -> 研究 -> 计划 -> 审批 -> Build -> 验证 -> 交付
        val created = coordinator.createSession("task-1", "chat-1", "workspace-1")
        assertTrue(created is SessionTaskCoordinator.Result.Success)
        var (task, todos) = created as SessionTaskCoordinator.Result.Success
        assertEquals(8, todos.size)
        assertTrue(todos.all { it.status == SessionTodoStatus.PENDING })

        // NEW -> CLARIFYING
        val clarifying =
            coordinator.transition(task, VibeCodingTaskStage.CLARIFYING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = clarifying.task
        todos = clarifying.todos

        // 澄清需求
        val clarified =
            coordinator.recordRequirement(
                task,
                RequirementSpec(
                    goal = "implement vertical slice",
                    inScope = listOf("core/vibecoding/runtime"),
                    outOfScope = listOf("database"),
                    acceptanceCriteria = listOf("JVM tests pass"),
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = clarified.task
        todos = clarified.todos
        assertEquals(SessionTodoStatus.COMPLETED, todos.first { it.content == "澄清需求" }.status)

        // 探索
        val explored =
            coordinator.transition(task, VibeCodingTaskStage.EXPLORING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = explored.task
        todos = explored.todos
        assertEquals(SessionTodoStatus.IN_PROGRESS, todos.first { it.content == "探索代码" }.status)

        // 研究
        val researched =
            coordinator.transition(task, VibeCodingTaskStage.RESEARCHING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = researched.task
        todos = researched.todos
        val researchDone =
            coordinator.recordResearch(
                task,
                ResearchRecord(
                    query = "official api",
                    source = "https://example.com",
                    conclusion = "verified",
                    status = ResearchEvidenceStatus.VERIFIED,
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = researchDone.task
        todos = researchDone.todos

        // 计划
        val planned =
            coordinator.transition(task, VibeCodingTaskStage.PLANNING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = planned.task
        todos = planned.todos
        val planRevised =
            coordinator.revisePlan(
                task,
                TaskPlanRevision(
                    revision = 1,
                    summary = "vertical slice",
                    riskLevel = VibeCodingRiskLevel.L3,
                    affectedPaths = listOf("core/vibecoding/runtime"),
                    steps = listOf(PlanStep("1", "implement", "run JVM tests")),
                    rollbackPlan = "git revert",
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = planRevised.task
        todos = planRevised.todos

        // 等待审批
        val waiting =
            coordinator.transition(task, VibeCodingTaskStage.WAITING_PLAN_APPROVAL, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = waiting.task
        todos = waiting.todos

        // 审批
        val approved =
            coordinator.approvePlan(task, approvedAtEpochMillis = 1L, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = approved.task
        todos = approved.todos
        assertEquals(SessionTodoStatus.COMPLETED, todos.first { it.content == "等待用户审批" }.status)

        // READY_TO_BUILD -> BUILD 模式 -> IMPLEMENTING
        val ready =
            coordinator.transition(task, VibeCodingTaskStage.READY_TO_BUILD, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = ready.task
        todos = ready.todos
        val buildMode =
            coordinator.setMode(task, CodingSessionMode.BUILD, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = buildMode.task
        todos = buildMode.todos
        val implementing =
            coordinator.transition(task, VibeCodingTaskStage.IMPLEMENTING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = implementing.task
        todos = implementing.todos
        val changed =
            coordinator.recordChangedPaths(task, setOf("src/Main.kt"), todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = changed.task
        todos = changed.todos

        // 验证
        val validating =
            coordinator.transition(task, VibeCodingTaskStage.VALIDATING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = validating.task
        todos = validating.todos
        val validation =
            coordinator.recordValidation(
                task,
                ValidationRun(
                    id = "test-1",
                    status = ValidationStatus.PASSED,
                    command = "./gradlew test",
                    evidence = "all passed",
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = validation.task
        todos = validation.todos
        assertEquals(SessionTodoStatus.COMPLETED, todos.first { it.content == "验证结果" }.status)

        // DOCUMENTING -> REVIEWING -> CLOUD_BUILD -> RELEASING -> COMPLETED
        val documenting =
            coordinator.transition(task, VibeCodingTaskStage.DOCUMENTING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = documenting.task
        todos = documenting.todos
        val docsRecorded =
            coordinator.recordDocumentation(
                task,
                com.ai.assistance.operit.core.vibecoding.domain.DocumentationDecision(
                    required = true,
                    updatedPaths = listOf("docs/structure.md"),
                    rationale = "new runtime boundary",
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = docsRecorded.task
        todos = docsRecorded.todos

        val reviewing =
            coordinator.transition(task, VibeCodingTaskStage.REVIEWING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = reviewing.task
        todos = reviewing.todos
        val reviewed =
            coordinator.recordReview(
                task,
                com.ai.assistance.operit.core.vibecoding.domain.ReviewRecord(evidence = "diff reviewed"),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = reviewed.task
        todos = reviewed.todos
        val strategy =
            coordinator.recordBuildStrategy(
                task,
                com.ai.assistance.operit.core.vibecoding.domain.BuildBackend.CLOUD,
                reason = "android toolchain requires cloud",
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = strategy.task
        todos = strategy.todos

        val cloudBuild =
            coordinator.transition(task, VibeCodingTaskStage.CLOUD_BUILD, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = cloudBuild.task
        todos = cloudBuild.todos
        val buildRun =
            coordinator.recordBuildRun(
                task,
                com.ai.assistance.operit.core.vibecoding.domain.BuildRunEvidence(
                    runId = "run-1",
                    sourceSha = "abc",
                    status = com.ai.assistance.operit.core.vibecoding.domain.BuildRunStatus.SUCCEEDED,
                    evidence = "build passed",
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = buildRun.task
        todos = buildRun.todos

        val releasing =
            coordinator.transition(task, VibeCodingTaskStage.RELEASING, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        task = releasing.task
        todos = releasing.todos
        val release =
            coordinator.recordReleaseEvidence(
                task,
                com.ai.assistance.operit.core.vibecoding.domain.ReleaseEvidence.Published(
                    tag = "v1.0.0",
                    sourceSha = "abc",
                    assetNames = listOf("app.apk", "app.apk.sha256"),
                    digestVerified = true,
                    signatureRequired = true,
                    signatureVerified = true,
                ),
                todos,
                "chat-1",
            ) as SessionTaskCoordinator.Result.Success
        task = release.task
        todos = release.todos

        val completed =
            coordinator.transition(task, VibeCodingTaskStage.COMPLETED, todos, "chat-1")
                as SessionTaskCoordinator.Result.Success
        assertEquals(VibeCodingTaskStage.COMPLETED, completed.task.stage)
        assertEquals(
            SessionTodoStatus.COMPLETED,
            completed.todos.first { it.content == "交付与文档" }.status,
        )
    }

    @Test
    fun `session todos are isolated per session`() {
        val a = coordinator.createSession("task-a", "chat-a", "ws") as SessionTaskCoordinator.Result.Success
        val b = coordinator.createSession("task-b", "chat-b", "ws") as SessionTaskCoordinator.Result.Success
        assertTrue(a.todos.all { it.sessionId == "chat-a" })
        assertTrue(b.todos.all { it.sessionId == "chat-b" })
        assertTrue(a.todos.none { it.id == b.todos.first().id })
    }
}