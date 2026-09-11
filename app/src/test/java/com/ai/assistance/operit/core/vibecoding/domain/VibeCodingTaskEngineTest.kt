package com.ai.assistance.operit.core.vibecoding.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VibeCodingTaskEngineTest {
    @Test
    fun `happy path reaches completed only with current approval and complete evidence`() {
        var task = newTask()
        task = task.move(VibeCodingTaskStage.CLARIFYING)
        task = task.recordRequirement(completeRequirement())
        task = task.move(VibeCodingTaskStage.EXPLORING)
        task = task.move(VibeCodingTaskStage.RESEARCHING)
        task = task.recordResearch(verifiedResearch())
        task = task.move(VibeCodingTaskStage.PLANNING)
        task = task.revisePlan(completePlan(1))
        task = task.move(VibeCodingTaskStage.WAITING_PLAN_APPROVAL)
        task = task.approvePlan()
        task = task.move(VibeCodingTaskStage.READY_TO_BUILD)
        task = task.setMode(CodingSessionMode.BUILD)
        task = task.move(VibeCodingTaskStage.IMPLEMENTING)
        task = task.recordChangedPaths(setOf("src/Main.kt"))
        task = task.move(VibeCodingTaskStage.VALIDATING)
        task = task.recordValidation(passedValidation())
        task = task.move(VibeCodingTaskStage.DOCUMENTING)
        task = task.recordDocumentation()
        task = task.move(VibeCodingTaskStage.REVIEWING)
        task = task.recordReview()
        task = task.recordBuildStrategy(BuildBackend.CLOUD)
        task = task.move(VibeCodingTaskStage.CLOUD_BUILD)
        task = task.recordBuildRun(successfulBuild())
        task = task.move(VibeCodingTaskStage.RELEASING)
        task = task.recordRelease(completeRelease())
        task = task.move(VibeCodingTaskStage.COMPLETED)

        assertEquals(VibeCodingTaskStage.COMPLETED, task.stage)
        assertEquals(CodingSessionMode.BUILD, task.mode)
        assertTrue(task.releaseEvidence?.isComplete() == true)
    }

    @Test
    fun `new task rejects every forward jump except clarifying`() {
        val task = newTask()
        val forbidden =
            VibeCodingTaskStage.entries.filterNot {
                it in setOf(
                    VibeCodingTaskStage.NEW,
                    VibeCodingTaskStage.CLARIFYING,
                    VibeCodingTaskStage.CANCELLED,
                )
            }

        forbidden.forEach { target ->
            val result = VibeCodingTaskEngine.transitionTo(task, target, VibeCodingActorType.MAIN_AGENT)
            assertRejected(result, target.name)
        }
    }

    @Test
    fun `incomplete requirement blocks exploration`() {
        val task = newTask().move(VibeCodingTaskStage.CLARIFYING)
        val incomplete = RequirementSpec("", listOf("src"), listOf("docs"), listOf("tests pass"))

        val recordResult = VibeCodingTaskEngine.recordRequirement(task, incomplete)
        assertRejected(recordResult, VibeCodingRejectionCode.INCOMPLETE_REQUIREMENTS)

        val transitionResult =
            VibeCodingTaskEngine.transitionTo(task, VibeCodingTaskStage.EXPLORING, VibeCodingActorType.MAIN_AGENT)
        assertRejected(transitionResult, VibeCodingRejectionCode.INCOMPLETE_REQUIREMENTS)
    }

    @Test
    fun `planning requires verified research or explicit user fallback`() {
        val researching =
            newTask()
                .move(VibeCodingTaskStage.CLARIFYING)
                .recordRequirement(completeRequirement())
                .move(VibeCodingTaskStage.EXPLORING)
                .move(VibeCodingTaskStage.RESEARCHING)
                .recordResearch(
                    ResearchRecord(
                        query = "official API",
                        source = null,
                        conclusion = "network unavailable",
                        status = ResearchEvidenceStatus.UNAVAILABLE,
                    ),
                )

        val blocked =
            VibeCodingTaskEngine.transitionTo(
                researching,
                VibeCodingTaskStage.PLANNING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(blocked, VibeCodingRejectionCode.MISSING_RESEARCH_EVIDENCE)

        val agentFallback =
            VibeCodingTaskEngine.approveLocalEvidenceFallback(researching, VibeCodingActorType.MAIN_AGENT)
        assertRejected(agentFallback, VibeCodingRejectionCode.USER_ONLY)

        val approvedFallback =
            VibeCodingTaskEngine.approveLocalEvidenceFallback(researching, VibeCodingActorType.USER).acceptedTask()
        assertEquals(VibeCodingTaskStage.PLANNING, approvedFallback.move(VibeCodingTaskStage.PLANNING).stage)
    }

    @Test
    fun `only user can approve a plan`() {
        val waiting = waitingForApprovalTask()

        val agentApproval =
            VibeCodingTaskEngine.approvePlan(
                waiting,
                VibeCodingActorType.MAIN_AGENT,
                approvedAtEpochMillis = 1L,
            )
        assertRejected(agentApproval, VibeCodingRejectionCode.USER_ONLY)
        assertNull(agentApproval.task().approval)
    }

    @Test
    fun `revising plan invalidates prior approval`() {
        val waiting = waitingForApprovalTask().approvePlan()
        assertNotNull(waiting.approval)

        val revised = waiting.revisePlan(completePlan(2))
        assertNull(revised.approval)
        assertEquals(2L, revised.plan?.revision)

        val result =
            VibeCodingTaskEngine.transitionTo(
                revised,
                VibeCodingTaskStage.READY_TO_BUILD,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(result, VibeCodingRejectionCode.APPROVAL_REQUIRED)
    }

    @Test
    fun `agent cannot switch session mode`() {
        val ready = waitingForApprovalTask().approvePlan().move(VibeCodingTaskStage.READY_TO_BUILD)

        VibeCodingActorType.entries
            .filterNot { it == VibeCodingActorType.USER }
            .forEach { actor ->
                val result = VibeCodingTaskEngine.setMode(ready, CodingSessionMode.BUILD, actor)
                assertRejected(result, VibeCodingRejectionCode.USER_ONLY)
                assertEquals(CodingSessionMode.PLAN, result.task().mode)
            }
    }

    @Test
    fun `implementation cannot start while mode remains plan`() {
        val ready = waitingForApprovalTask().approvePlan().move(VibeCodingTaskStage.READY_TO_BUILD)

        val result =
            VibeCodingTaskEngine.transitionTo(
                ready,
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(result, VibeCodingRejectionCode.BUILD_MODE_REQUIRED)
    }

    @Test
    fun `forged build mode without current approval cannot enter implementation`() {
        val forged =
            newTask().copy(
                stage = VibeCodingTaskStage.READY_TO_BUILD,
                mode = CodingSessionMode.BUILD,
                plan = completePlan(2),
                approval = ApprovalRecord(1, VibeCodingActorType.USER, 1L),
            )

        val result =
            VibeCodingTaskEngine.transitionTo(
                forged,
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(result, VibeCodingRejectionCode.APPROVAL_STALE)
    }

    @Test
    fun `validation failure returns to implementation but cannot advance to documentation`() {
        var task = implementingTask().recordChangedPaths(setOf("src/Main.kt"))
        task = task.move(VibeCodingTaskStage.VALIDATING)
        task =
            task.recordValidation(
                ValidationRun(
                    id = "test-1",
                    status = ValidationStatus.FAILED,
                    command = "./gradlew test",
                    evidence = "1 test failed",
                ),
            )

        val documentResult =
            VibeCodingTaskEngine.transitionTo(
                task,
                VibeCodingTaskStage.DOCUMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(documentResult, VibeCodingRejectionCode.VALIDATION_FAILED)

        val returned = task.move(VibeCodingTaskStage.IMPLEMENTING)
        assertEquals(VibeCodingTaskStage.IMPLEMENTING, returned.stage)
    }

    @Test
    fun `documentation stage requires update paths or explicit no-update rationale`() {
        val documenting = validatedTask().move(VibeCodingTaskStage.DOCUMENTING)

        val invalid =
            VibeCodingTaskEngine.recordDocumentationDecision(
                documenting,
                DocumentationDecision(required = true, updatedPaths = emptyList(), rationale = "must update"),
            )
        assertRejected(invalid, VibeCodingRejectionCode.DOCUMENTATION_REQUIRED)

        val noUpdate =
            VibeCodingTaskEngine.recordDocumentationDecision(
                documenting,
                DocumentationDecision(required = false, rationale = "pure runtime probe; no docs changed"),
            ).acceptedTask()
        assertEquals(VibeCodingTaskStage.REVIEWING, noUpdate.move(VibeCodingTaskStage.REVIEWING).stage)
    }

    @Test
    fun `recorded build strategy prevents switching to another backend`() {
        var reviewing = validatedTask().move(VibeCodingTaskStage.DOCUMENTING).recordDocumentation()
        reviewing = reviewing.move(VibeCodingTaskStage.REVIEWING).recordReview()
        reviewing = reviewing.recordBuildStrategy(BuildBackend.CLOUD)

        val localResult =
            VibeCodingTaskEngine.transitionTo(
                reviewing,
                VibeCodingTaskStage.LOCAL_BUILD,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(localResult, VibeCodingRejectionCode.BUILD_BACKEND_MISMATCH)
        assertEquals(VibeCodingTaskStage.CLOUD_BUILD, reviewing.move(VibeCodingTaskStage.CLOUD_BUILD).stage)
    }

    @Test
    fun `failed build must be classified and only code failure can return to implementation`() {
        var cloud = reviewingTask(BuildBackend.CLOUD).move(VibeCodingTaskStage.CLOUD_BUILD)

        val unclassified =
            VibeCodingTaskEngine.recordBuildRun(
                cloud,
                BuildRunEvidence("run-1", "abc", BuildRunStatus.FAILED, "failed", failureCategory = null),
            )
        assertRejected(unclassified, VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED)

        cloud =
            cloud.recordBuildRun(
                BuildRunEvidence(
                    runId = "run-1",
                    sourceSha = "abc",
                    status = BuildRunStatus.FAILED,
                    evidence = "runner shutdown",
                    failureCategory = BuildFailureCategory.INFRASTRUCTURE,
                ),
            )
        val infraReturn =
            VibeCodingTaskEngine.transitionTo(
                cloud,
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(infraReturn, VibeCodingRejectionCode.CODE_FAILURE_REQUIRED)

        val codeFailure =
            cloud.copy(buildRuns = emptyList()).recordBuildRun(
                BuildRunEvidence(
                    runId = "run-2",
                    sourceSha = "def",
                    status = BuildRunStatus.FAILED,
                    evidence = "Kotlin compile error",
                    failureCategory = BuildFailureCategory.CODE,
                ),
            )
        assertEquals(VibeCodingTaskStage.IMPLEMENTING, codeFailure.move(VibeCodingTaskStage.IMPLEMENTING).stage)
    }

    @Test
    fun `completion requires digest and required signature verification`() {
        val releasing = successfulCloudBuildTask().move(VibeCodingTaskStage.RELEASING)
        val incomplete =
            ReleaseEvidence.Published(
                tag = "v1.0.0",
                sourceSha = "abc",
                assetNames = listOf("app.apk"),
                digestVerified = true,
                signatureRequired = true,
                signatureVerified = false,
            )

        val recordResult = VibeCodingTaskEngine.recordReleaseEvidence(releasing, incomplete)
        assertRejected(recordResult, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED)

        val completeResult =
            VibeCodingTaskEngine.recordReleaseEvidence(releasing, completeRelease()).acceptedTask()
        assertEquals(VibeCodingTaskStage.COMPLETED, completeResult.move(VibeCodingTaskStage.COMPLETED).stage)
    }

    @Test
    fun `release source sha must match latest successful build`() {
        val releasing = successfulCloudBuildTask().move(VibeCodingTaskStage.RELEASING)
        val mismatched = completeRelease().copy(sourceSha = "different-sha")

        val result = VibeCodingTaskEngine.recordReleaseEvidence(releasing, mismatched)

        assertRejected(result, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED)
    }

    @Test
    fun `local build may explicitly record that release is not required`() {
        var local = reviewingTask(BuildBackend.LOCAL).move(VibeCodingTaskStage.LOCAL_BUILD)
        local = local.recordBuildRun(successfulBuild()).move(VibeCodingTaskStage.RELEASING)
        local =
            VibeCodingTaskEngine.recordReleaseEvidence(
                local,
                ReleaseEvidence.NotRequired("local verification only"),
            ).acceptedTask()

        assertEquals(VibeCodingTaskStage.COMPLETED, local.move(VibeCodingTaskStage.COMPLETED).stage)
    }

    @Test
    fun `cloud build cannot replace published release evidence with not-required marker`() {
        val releasing = successfulCloudBuildTask().move(VibeCodingTaskStage.RELEASING)
        val result =
            VibeCodingTaskEngine.recordReleaseEvidence(
                releasing,
                ReleaseEvidence.NotRequired("skip release"),
            )

        assertRejected(result, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED)
    }

    @Test
    fun `only user can pause or cancel`() {
        val implementing = implementingTask()

        val agentPause =
            VibeCodingTaskEngine.transitionTo(
                implementing,
                VibeCodingTaskStage.PAUSE_REQUESTED,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(agentPause, VibeCodingRejectionCode.USER_ONLY)

        val agentCancel =
            VibeCodingTaskEngine.transitionTo(
                implementing,
                VibeCodingTaskStage.CANCELLED,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(agentCancel, VibeCodingRejectionCode.USER_ONLY)
    }

    @Test
    fun `recovery cannot restore write stage after user switches back to plan`() {
        var task = implementingTask()
        task =
            VibeCodingTaskEngine.transitionTo(
                task,
                VibeCodingTaskStage.PAUSE_REQUESTED,
                VibeCodingActorType.USER,
            ).acceptedTask()
        task = task.move(VibeCodingTaskStage.PAUSED)
        task = task.setMode(CodingSessionMode.PLAN)
        task = task.move(VibeCodingTaskStage.RECOVERING)

        val result =
            VibeCodingTaskEngine.transitionTo(
                task,
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )
        assertRejected(result, VibeCodingRejectionCode.BUILD_MODE_REQUIRED)
        assertEquals(VibeCodingTaskStage.RECOVERING, result.task().stage)
    }

    @Test
    fun `recovery to write stage rejects stale approval even in build mode`() {
        val recovering =
            implementingTask().copy(
                stage = VibeCodingTaskStage.RECOVERING,
                recoveryTarget = VibeCodingTaskStage.IMPLEMENTING,
                plan = completePlan(2),
                approval = ApprovalRecord(1, VibeCodingActorType.USER, 1L),
            )

        val result =
            VibeCodingTaskEngine.transitionTo(
                recovering,
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingActorType.MAIN_AGENT,
            )

        assertRejected(result, VibeCodingRejectionCode.APPROVAL_STALE)
    }

    @Test
    fun `two task values remain isolated`() {
        val first = waitingForApprovalTask(taskId = "task-a", sessionId = "chat-a").approvePlan()
        val second = waitingForApprovalTask(taskId = "task-b", sessionId = "chat-b")

        assertNotNull(first.approval)
        assertNull(second.approval)
        assertEquals("chat-a", first.sessionId)
        assertEquals("chat-b", second.sessionId)
        assertFalse(first == second)
    }

    private fun newTask(
        taskId: String = "task-1",
        sessionId: String = "chat-1",
    ): VibeCodingTask =
        VibeCodingTaskEngine.createTask(taskId, sessionId, "workspace-1").acceptedTask()

    private fun waitingForApprovalTask(
        taskId: String = "task-1",
        sessionId: String = "chat-1",
    ): VibeCodingTask {
        var task = newTask(taskId, sessionId).move(VibeCodingTaskStage.CLARIFYING)
        task = task.recordRequirement(completeRequirement())
        task = task.move(VibeCodingTaskStage.EXPLORING)
        task = task.move(VibeCodingTaskStage.RESEARCHING)
        task = task.recordResearch(verifiedResearch())
        task = task.move(VibeCodingTaskStage.PLANNING)
        task = task.revisePlan(completePlan(1))
        return task.move(VibeCodingTaskStage.WAITING_PLAN_APPROVAL)
    }

    private fun implementingTask(): VibeCodingTask {
        var task = waitingForApprovalTask().approvePlan().move(VibeCodingTaskStage.READY_TO_BUILD)
        task = task.setMode(CodingSessionMode.BUILD)
        return task.move(VibeCodingTaskStage.IMPLEMENTING)
    }

    private fun validatedTask(): VibeCodingTask {
        var task = implementingTask().recordChangedPaths(setOf("src/Main.kt"))
        task = task.move(VibeCodingTaskStage.VALIDATING)
        return task.recordValidation(passedValidation())
    }

    private fun reviewingTask(backend: BuildBackend): VibeCodingTask {
        var task = validatedTask().move(VibeCodingTaskStage.DOCUMENTING).recordDocumentation()
        task = task.move(VibeCodingTaskStage.REVIEWING).recordReview()
        return task.recordBuildStrategy(backend)
    }

    private fun successfulCloudBuildTask(): VibeCodingTask {
        var task = reviewingTask(BuildBackend.CLOUD).move(VibeCodingTaskStage.CLOUD_BUILD)
        return task.recordBuildRun(successfulBuild())
    }

    private fun completeRequirement() =
        RequirementSpec(
            goal = "implement state machine",
            inScope = listOf("core/vibecoding/domain"),
            outOfScope = listOf("database", "real GitHub"),
            acceptanceCriteria = listOf("JVM tests pass"),
        )

    private fun verifiedResearch() =
        ResearchRecord(
            query = "Kotlin sealed classes state machine",
            source = "https://kotlinlang.org/docs/sealed-classes.html",
            conclusion = "sealed hierarchies provide exhaustive closed state handling",
            status = ResearchEvidenceStatus.VERIFIED,
        )

    private fun completePlan(revision: Long) =
        TaskPlanRevision(
            revision = revision,
            summary = "add pure state machine",
            riskLevel = VibeCodingRiskLevel.L3,
            affectedPaths = listOf("core/vibecoding/domain"),
            steps = listOf(PlanStep("1", "implement models", "run JVM tests")),
            rollbackPlan = "git revert the Phase A commit",
        )

    private fun passedValidation() =
        ValidationRun(
            id = "test-1",
            status = ValidationStatus.PASSED,
            command = "./gradlew :app:testDebugUnitTest",
            evidence = "all tests passed",
        )

    private fun successfulBuild() =
        BuildRunEvidence(
            runId = "run-1",
            sourceSha = "abc",
            status = BuildRunStatus.SUCCEEDED,
            evidence = "build passed",
        )

    private fun completeRelease() =
        ReleaseEvidence.Published(
            tag = "v1.0.0",
            sourceSha = "abc",
            assetNames = listOf("app.apk", "app.apk.sha256"),
            digestVerified = true,
            signatureRequired = true,
            signatureVerified = true,
        )

    private fun VibeCodingTask.move(target: VibeCodingTaskStage): VibeCodingTask =
        VibeCodingTaskEngine.transitionTo(this, target, VibeCodingActorType.MAIN_AGENT).acceptedTask()

    private fun VibeCodingTask.recordRequirement(requirement: RequirementSpec): VibeCodingTask =
        VibeCodingTaskEngine.recordRequirement(this, requirement).acceptedTask()

    private fun VibeCodingTask.recordResearch(record: ResearchRecord): VibeCodingTask =
        VibeCodingTaskEngine.recordResearch(this, record).acceptedTask()

    private fun VibeCodingTask.revisePlan(plan: TaskPlanRevision): VibeCodingTask =
        VibeCodingTaskEngine.revisePlan(this, plan).acceptedTask()

    private fun VibeCodingTask.approvePlan(): VibeCodingTask =
        VibeCodingTaskEngine.approvePlan(this, VibeCodingActorType.USER, 1L).acceptedTask()

    private fun VibeCodingTask.setMode(mode: CodingSessionMode): VibeCodingTask =
        VibeCodingTaskEngine.setMode(this, mode, VibeCodingActorType.USER).acceptedTask()

    private fun VibeCodingTask.recordChangedPaths(paths: Set<String>): VibeCodingTask =
        VibeCodingTaskEngine.recordChangedPaths(this, paths).acceptedTask()

    private fun VibeCodingTask.recordValidation(run: ValidationRun): VibeCodingTask =
        VibeCodingTaskEngine.recordValidation(this, run).acceptedTask()

    private fun VibeCodingTask.recordDocumentation(): VibeCodingTask =
        VibeCodingTaskEngine.recordDocumentationDecision(
            this,
            DocumentationDecision(
                required = true,
                updatedPaths = listOf("docs/structure.md"),
                rationale = "new domain boundary",
            ),
        ).acceptedTask()

    private fun VibeCodingTask.recordReview(): VibeCodingTask =
        VibeCodingTaskEngine.recordReview(this, ReviewRecord(evidence = "diff reviewed")).acceptedTask()

    private fun VibeCodingTask.recordBuildStrategy(backend: BuildBackend): VibeCodingTask =
        VibeCodingTaskEngine.recordBuildStrategy(
            this,
            BuildStrategyDecision(backend, "toolchain and resource evidence"),
        ).acceptedTask()

    private fun VibeCodingTask.recordBuildRun(run: BuildRunEvidence): VibeCodingTask =
        VibeCodingTaskEngine.recordBuildRun(this, run).acceptedTask()

    private fun VibeCodingTask.recordRelease(evidence: ReleaseEvidence): VibeCodingTask =
        VibeCodingTaskEngine.recordReleaseEvidence(this, evidence).acceptedTask()

    private fun VibeCodingDecision.acceptedTask(): VibeCodingTask {
        assertTrue("expected accepted decision but got $this", this is VibeCodingDecision.Accepted)
        return (this as VibeCodingDecision.Accepted).task
    }

    private fun VibeCodingDecision.task(): VibeCodingTask =
        when (this) {
            is VibeCodingDecision.Accepted -> task
            is VibeCodingDecision.Rejected -> task
        }

    private fun assertRejected(
        decision: VibeCodingDecision,
        expectedCode: VibeCodingRejectionCode,
    ) {
        assertTrue("expected rejection but got $decision", decision is VibeCodingDecision.Rejected)
        assertEquals(expectedCode, (decision as VibeCodingDecision.Rejected).code)
    }

    private fun assertRejected(
        decision: VibeCodingDecision,
        targetLabel: String,
    ) {
        assertTrue("expected $targetLabel to be rejected but got $decision", decision is VibeCodingDecision.Rejected)
    }
}