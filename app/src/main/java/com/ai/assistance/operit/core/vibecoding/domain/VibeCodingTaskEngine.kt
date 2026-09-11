package com.ai.assistance.operit.core.vibecoding.domain

/** Pure domain state machine. It performs no I/O and owns no persistence or UI state. */
object VibeCodingTaskEngine {
    private val activeStages =
        setOf(
            VibeCodingTaskStage.CLARIFYING,
            VibeCodingTaskStage.EXPLORING,
            VibeCodingTaskStage.RESEARCHING,
            VibeCodingTaskStage.PLANNING,
            VibeCodingTaskStage.WAITING_PLAN_APPROVAL,
            VibeCodingTaskStage.READY_TO_BUILD,
            VibeCodingTaskStage.IMPLEMENTING,
            VibeCodingTaskStage.VALIDATING,
            VibeCodingTaskStage.DOCUMENTING,
            VibeCodingTaskStage.REVIEWING,
            VibeCodingTaskStage.LOCAL_BUILD,
            VibeCodingTaskStage.CLOUD_BUILD,
            VibeCodingTaskStage.RELEASING,
        )

    private val interruptibleStages = activeStages + VibeCodingTaskStage.STALLED

    private val buildModeEligibleStages =
        setOf(
            VibeCodingTaskStage.READY_TO_BUILD,
            VibeCodingTaskStage.IMPLEMENTING,
            VibeCodingTaskStage.VALIDATING,
            VibeCodingTaskStage.DOCUMENTING,
            VibeCodingTaskStage.REVIEWING,
            VibeCodingTaskStage.LOCAL_BUILD,
            VibeCodingTaskStage.CLOUD_BUILD,
            VibeCodingTaskStage.RELEASING,
            VibeCodingTaskStage.PAUSE_REQUESTED,
            VibeCodingTaskStage.PAUSED,
            VibeCodingTaskStage.STALLED,
            VibeCodingTaskStage.BLOCKED,
            VibeCodingTaskStage.FAILED,
            VibeCodingTaskStage.RECOVERING,
        )

    fun createTask(
        id: String,
        sessionId: String,
        workspaceId: String,
    ): VibeCodingDecision {
        if (id.isBlank() || sessionId.isBlank() || workspaceId.isBlank()) {
            val task = VibeCodingTask(id = id, sessionId = sessionId, workspaceId = workspaceId)
            return reject(task, VibeCodingRejectionCode.INVALID_IDENTITY, "任务、会话和工作区 ID 不能为空")
        }
        val task = VibeCodingTask(id = id, sessionId = sessionId, workspaceId = workspaceId)
        return accept(task, VibeCodingTaskEvent.FactRecorded("task_created"))
    }

    fun recordRequirement(
        task: VibeCodingTask,
        requirement: RequirementSpec,
    ): VibeCodingDecision {
        if (task.stage !in setOf(VibeCodingTaskStage.NEW, VibeCodingTaskStage.CLARIFYING)) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "仅能在新建或需求澄清阶段更新需求")
        }
        if (!requirement.isComplete()) {
            return reject(task, VibeCodingRejectionCode.INCOMPLETE_REQUIREMENTS, "目标、范围、非范围和验收标准必须完整")
        }
        return accept(task.copy(requirement = requirement), VibeCodingTaskEvent.FactRecorded("requirement"))
    }

    fun recordResearch(
        task: VibeCodingTask,
        record: ResearchRecord,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.RESEARCHING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "研究证据只能在联网校准阶段记录")
        }
        if (record.query.isBlank() || record.conclusion.isBlank()) {
            return reject(task, VibeCodingRejectionCode.MISSING_RESEARCH_EVIDENCE, "研究查询和结论不能为空")
        }
        return accept(
            task.copy(researchRecords = task.researchRecords + record),
            VibeCodingTaskEvent.FactRecorded("research"),
        )
    }

    fun approveLocalEvidenceFallback(
        task: VibeCodingTask,
        actorType: VibeCodingActorType,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.RESEARCHING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "仅能在联网校准阶段批准本地证据降级")
        }
        if (actorType != VibeCodingActorType.USER) {
            return reject(task, VibeCodingRejectionCode.USER_ONLY, "仅用户可批准联网失败后的本地证据降级")
        }
        if (task.researchRecords.none {
                it.status != ResearchEvidenceStatus.VERIFIED &&
                    it.query.isNotBlank() &&
                    it.conclusion.isNotBlank()
            }
        ) {
            return reject(
                task,
                VibeCodingRejectionCode.MISSING_RESEARCH_EVIDENCE,
                "必须先记录联网不可用或来源冲突的证据",
            )
        }
        return accept(
            task.copy(localEvidenceFallbackApproved = true),
            VibeCodingTaskEvent.FactRecorded("local_evidence_fallback_approved"),
        )
    }

    fun revisePlan(
        task: VibeCodingTask,
        plan: TaskPlanRevision,
    ): VibeCodingDecision {
        if (task.stage !in setOf(VibeCodingTaskStage.PLANNING, VibeCodingTaskStage.WAITING_PLAN_APPROVAL)) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "仅能在计划或等待审批阶段修订计划")
        }
        if (!plan.isComplete()) {
            return reject(task, VibeCodingRejectionCode.INCOMPLETE_PLAN, "计划版本、范围、步骤、验证和回滚必须完整")
        }
        val previousRevision = task.plan?.revision ?: 0L
        if (plan.revision <= previousRevision) {
            return reject(task, VibeCodingRejectionCode.INCOMPLETE_PLAN, "新计划版本必须严格递增")
        }
        return accept(
            task.copy(plan = plan, approval = null),
            VibeCodingTaskEvent.PlanRevised(plan.revision),
        )
    }

    fun approvePlan(
        task: VibeCodingTask,
        actorType: VibeCodingActorType,
        approvedAtEpochMillis: Long,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.WAITING_PLAN_APPROVAL) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "仅能在等待计划审批阶段批准")
        }
        if (actorType != VibeCodingActorType.USER) {
            return reject(task, VibeCodingRejectionCode.USER_ONLY, "仅用户可以批准计划")
        }
        val plan = task.plan
            ?: return reject(task, VibeCodingRejectionCode.INCOMPLETE_PLAN, "缺少可批准的计划")
        if (!plan.isComplete()) {
            return reject(task, VibeCodingRejectionCode.INCOMPLETE_PLAN, "计划内容不完整")
        }
        if (approvedAtEpochMillis <= 0L) {
            return reject(task, VibeCodingRejectionCode.APPROVAL_REQUIRED, "审批时间无效")
        }
        return accept(
            task.copy(
                approval =
                    ApprovalRecord(
                        planRevision = plan.revision,
                        actorType = actorType,
                        approvedAtEpochMillis = approvedAtEpochMillis,
                    ),
            ),
            VibeCodingTaskEvent.PlanApproved(plan.revision),
        )
    }

    fun setMode(
        task: VibeCodingTask,
        requestedMode: CodingSessionMode,
        actorType: VibeCodingActorType,
    ): VibeCodingDecision {
        if (actorType != VibeCodingActorType.USER) {
            return reject(task, VibeCodingRejectionCode.USER_ONLY, "PLAN/BUILD 只能由用户切换")
        }
        if (requestedMode == task.mode) {
            return accept(task, VibeCodingTaskEvent.ModeChanged(task.mode, requestedMode))
        }
        if (requestedMode == CodingSessionMode.BUILD) {
            if (task.stage !in buildModeEligibleStages) {
                return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "当前阶段不允许切换 BUILD")
            }
            currentApprovalRejection(task)?.let { return it }
        }
        return accept(
            task.copy(mode = requestedMode),
            VibeCodingTaskEvent.ModeChanged(task.mode, requestedMode),
        )
    }

    fun recordChangedPaths(
        task: VibeCodingTask,
        paths: Set<String>,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.IMPLEMENTING || task.mode != CodingSessionMode.BUILD) {
            return reject(task, VibeCodingRejectionCode.BUILD_MODE_REQUIRED, "只有 BUILD 实施阶段可以记录代码改动")
        }
        if (paths.none(String::isNotBlank)) {
            return reject(task, VibeCodingRejectionCode.CHANGE_EVIDENCE_REQUIRED, "至少需要一个有效改动路径")
        }
        return accept(
            task.copy(
                changedPaths = task.changedPaths + paths.filter(String::isNotBlank),
                validationRuns = emptyList(),
                documentationDecision = null,
                review = null,
                buildStrategy = null,
                buildRuns = emptyList(),
                releaseEvidence = null,
            ),
            VibeCodingTaskEvent.FactRecorded("changed_paths"),
        )
    }

    fun recordValidation(
        task: VibeCodingTask,
        run: ValidationRun,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.VALIDATING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "验证结果只能在验证阶段记录")
        }
        if (run.id.isBlank() || run.command.isBlank() || run.evidence.isBlank()) {
            return reject(task, VibeCodingRejectionCode.VALIDATION_REQUIRED, "验证 ID、命令和证据不能为空")
        }
        return accept(
            task.copy(validationRuns = task.validationRuns + run),
            VibeCodingTaskEvent.FactRecorded("validation"),
        )
    }

    fun recordDocumentationDecision(
        task: VibeCodingTask,
        decision: DocumentationDecision,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.DOCUMENTING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "文档决定只能在文档阶段记录")
        }
        if (!decision.isComplete()) {
            return reject(task, VibeCodingRejectionCode.DOCUMENTATION_REQUIRED, "文档更新路径或无需更新理由不完整")
        }
        return accept(
            task.copy(documentationDecision = decision),
            VibeCodingTaskEvent.FactRecorded("documentation"),
        )
    }

    fun recordReview(
        task: VibeCodingTask,
        review: ReviewRecord,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.REVIEWING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "审查结果只能在审查阶段记录")
        }
        if (review.evidence.isBlank()) {
            return reject(task, VibeCodingRejectionCode.REVIEW_REQUIRED, "审查必须包含证据")
        }
        return accept(task.copy(review = review), VibeCodingTaskEvent.FactRecorded("review"))
    }

    fun recordBuildStrategy(
        task: VibeCodingTask,
        decision: BuildStrategyDecision,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.REVIEWING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "构建策略只能在审查阶段记录")
        }
        if (decision.reason.isBlank()) {
            return reject(task, VibeCodingRejectionCode.BUILD_STRATEGY_REQUIRED, "构建策略必须包含可解释依据")
        }
        return accept(
            task.copy(buildStrategy = decision),
            VibeCodingTaskEvent.FactRecorded("build_strategy"),
        )
    }

    fun recordBuildRun(
        task: VibeCodingTask,
        run: BuildRunEvidence,
    ): VibeCodingDecision {
        if (task.stage !in setOf(VibeCodingTaskStage.LOCAL_BUILD, VibeCodingTaskStage.CLOUD_BUILD)) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "构建证据只能在本地或云端构建阶段记录")
        }
        val expectedBackend =
            if (task.stage == VibeCodingTaskStage.LOCAL_BUILD) BuildBackend.LOCAL else BuildBackend.CLOUD
        if (task.buildStrategy?.backend != expectedBackend) {
            return reject(task, VibeCodingRejectionCode.BUILD_BACKEND_MISMATCH, "构建阶段与已记录策略不一致")
        }
        if (run.runId.isBlank() || run.sourceSha.isBlank() || run.evidence.isBlank()) {
            return reject(task, VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED, "构建 runId、源码 SHA 和证据不能为空")
        }
        if (run.status == BuildRunStatus.FAILED && run.failureCategory == null) {
            return reject(task, VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED, "失败构建必须分类")
        }
        if (run.status == BuildRunStatus.SUCCEEDED && run.failureCategory != null) {
            return reject(task, VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED, "成功构建不能携带失败分类")
        }
        return accept(
            task.copy(buildRuns = task.buildRuns + run),
            VibeCodingTaskEvent.FactRecorded("build_run"),
        )
    }

    fun recordReleaseEvidence(
        task: VibeCodingTask,
        evidence: ReleaseEvidence,
    ): VibeCodingDecision {
        if (task.stage != VibeCodingTaskStage.RELEASING) {
            return reject(task, VibeCodingRejectionCode.INVALID_STAGE, "发布证据只能在发布阶段记录")
        }
        if (!evidence.isComplete()) {
            return reject(task, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED, "发布或无需发布的证据不完整")
        }
        val successfulBuild = task.buildRuns.lastOrNull()
        if (successfulBuild?.isSuccessful() != true) {
            return reject(task, VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED, "发布前缺少成功构建证据")
        }
        if (task.buildStrategy?.backend == BuildBackend.CLOUD && evidence !is ReleaseEvidence.Published) {
            return reject(task, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED, "云端构建必须提供已发布 Release 的证据")
        }
        if (evidence is ReleaseEvidence.Published && evidence.sourceSha != successfulBuild.sourceSha) {
            return reject(task, VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED, "Release 源码 SHA 与成功构建不一致")
        }
        return accept(
            task.copy(releaseEvidence = evidence),
            VibeCodingTaskEvent.FactRecorded("release"),
        )
    }

    fun transitionTo(
        task: VibeCodingTask,
        target: VibeCodingTaskStage,
        actorType: VibeCodingActorType,
    ): VibeCodingDecision {
        if (task.stage == target) {
            return accept(task, VibeCodingTaskEvent.StageChanged(task.stage, target))
        }

        val from = task.stage
        if (target.requiresBuildMode() && from != VibeCodingTaskStage.RECOVERING) {
            if (task.mode != CodingSessionMode.BUILD) {
                return reject(task, VibeCodingRejectionCode.BUILD_MODE_REQUIRED, "目标阶段要求 BUILD 模式")
            }
            currentApprovalRejection(task)?.let { return it }
        }
        if (target == VibeCodingTaskStage.CANCELLED) {
            if (actorType != VibeCodingActorType.USER) {
                return reject(task, VibeCodingRejectionCode.USER_ONLY, "仅用户可取消任务")
            }
            if (from in setOf(VibeCodingTaskStage.COMPLETED, VibeCodingTaskStage.CANCELLED)) {
                return reject(task, VibeCodingRejectionCode.INVALID_TRANSITION, "已结束任务不能取消")
            }
            return stage(task, target)
        }

        if (target == VibeCodingTaskStage.PAUSE_REQUESTED) {
            if (actorType != VibeCodingActorType.USER) {
                return reject(task, VibeCodingRejectionCode.USER_ONLY, "仅用户可请求暂停")
            }
            if (from !in interruptibleStages) {
                return reject(task, VibeCodingRejectionCode.INVALID_TRANSITION, "当前阶段不可暂停")
            }
            return accept(
                task.copy(stage = target, recoveryTarget = from),
                VibeCodingTaskEvent.StageChanged(from, target),
            )
        }

        if (target == VibeCodingTaskStage.STALLED || target == VibeCodingTaskStage.BLOCKED || target == VibeCodingTaskStage.FAILED) {
            if (from !in interruptibleStages) {
                return reject(task, VibeCodingRejectionCode.INVALID_TRANSITION, "当前阶段不能进入异常状态")
            }
            return accept(
                task.copy(stage = target, recoveryTarget = from),
                VibeCodingTaskEvent.StageChanged(from, target),
            )
        }

        return when (from to target) {
            VibeCodingTaskStage.NEW to VibeCodingTaskStage.CLARIFYING -> stage(task, target)
            VibeCodingTaskStage.CLARIFYING to VibeCodingTaskStage.EXPLORING ->
                requireCondition(
                    task,
                    task.requirement?.isComplete() == true,
                    VibeCodingRejectionCode.INCOMPLETE_REQUIREMENTS,
                    "需求未完成，不能开始探索",
                    target,
                )
            VibeCodingTaskStage.EXPLORING to VibeCodingTaskStage.RESEARCHING -> stage(task, target)
            VibeCodingTaskStage.RESEARCHING to VibeCodingTaskStage.PLANNING ->
                requireCondition(
                    task,
                    task.researchRecords.any(ResearchRecord::isVerified) || task.localEvidenceFallbackApproved,
                    VibeCodingRejectionCode.MISSING_RESEARCH_EVIDENCE,
                    "缺少已验证联网来源，且用户未批准本地证据降级",
                    target,
                )
            VibeCodingTaskStage.PLANNING to VibeCodingTaskStage.WAITING_PLAN_APPROVAL ->
                requireCondition(
                    task,
                    task.plan?.isComplete() == true,
                    VibeCodingRejectionCode.INCOMPLETE_PLAN,
                    "计划不完整，不能等待审批",
                    target,
                )
            VibeCodingTaskStage.WAITING_PLAN_APPROVAL to VibeCodingTaskStage.READY_TO_BUILD ->
                requireCurrentApproval(task, target)
            VibeCodingTaskStage.READY_TO_BUILD to VibeCodingTaskStage.IMPLEMENTING ->
                requireCondition(
                    task,
                    task.mode == CodingSessionMode.BUILD,
                    VibeCodingRejectionCode.BUILD_MODE_REQUIRED,
                    "必须由用户切换到 BUILD 才能实施",
                    target,
                )
            VibeCodingTaskStage.IMPLEMENTING to VibeCodingTaskStage.VALIDATING ->
                requireCondition(
                    task,
                    task.changedPaths.isNotEmpty(),
                    VibeCodingRejectionCode.CHANGE_EVIDENCE_REQUIRED,
                    "没有代码改动证据，不能进入验证",
                    target,
                )
            VibeCodingTaskStage.VALIDATING to VibeCodingTaskStage.IMPLEMENTING ->
                requireCondition(
                    task,
                    task.validationRuns.lastOrNull()?.status == ValidationStatus.FAILED,
                    VibeCodingRejectionCode.CODE_FAILURE_REQUIRED,
                    "只有真实验证失败才能返回实施阶段",
                    target,
                )
            VibeCodingTaskStage.VALIDATING to VibeCodingTaskStage.DOCUMENTING ->
                requireCondition(
                    task,
                    task.validationRuns.isNotEmpty() && task.validationRuns.all { it.status == ValidationStatus.PASSED },
                    VibeCodingRejectionCode.VALIDATION_FAILED,
                    "所有必要验证必须通过",
                    target,
                )
            VibeCodingTaskStage.DOCUMENTING to VibeCodingTaskStage.REVIEWING ->
                requireCondition(
                    task,
                    task.documentationDecision?.isComplete() == true,
                    VibeCodingRejectionCode.DOCUMENTATION_REQUIRED,
                    "缺少文档更新或无需更新的明确结论",
                    target,
                )
            VibeCodingTaskStage.REVIEWING to VibeCodingTaskStage.LOCAL_BUILD ->
                requireReviewAndBuildStrategy(task, BuildBackend.LOCAL, target)
            VibeCodingTaskStage.REVIEWING to VibeCodingTaskStage.CLOUD_BUILD ->
                requireReviewAndBuildStrategy(task, BuildBackend.CLOUD, target)
            VibeCodingTaskStage.LOCAL_BUILD to VibeCodingTaskStage.RELEASING,
            VibeCodingTaskStage.CLOUD_BUILD to VibeCodingTaskStage.RELEASING ->
                requireCondition(
                    task,
                    task.buildRuns.lastOrNull()?.isSuccessful() == true,
                    VibeCodingRejectionCode.BUILD_EVIDENCE_REQUIRED,
                    "缺少成功构建证据",
                    target,
                )
            VibeCodingTaskStage.LOCAL_BUILD to VibeCodingTaskStage.IMPLEMENTING,
            VibeCodingTaskStage.CLOUD_BUILD to VibeCodingTaskStage.IMPLEMENTING ->
                requireCondition(
                    task,
                    task.buildRuns.lastOrNull()?.let {
                        it.status == BuildRunStatus.FAILED && it.failureCategory == BuildFailureCategory.CODE
                    } == true,
                    VibeCodingRejectionCode.CODE_FAILURE_REQUIRED,
                    "只有已分类的代码构建失败才能返回实施阶段",
                    target,
                )
            VibeCodingTaskStage.RELEASING to VibeCodingTaskStage.COMPLETED ->
                requireCondition(
                    task,
                    task.releaseEvidence?.isComplete() == true,
                    VibeCodingRejectionCode.RELEASE_EVIDENCE_REQUIRED,
                    "发布证据不完整，不能完成任务",
                    target,
                )
            VibeCodingTaskStage.PAUSE_REQUESTED to VibeCodingTaskStage.PAUSED -> stage(task, target)
            VibeCodingTaskStage.PAUSED to VibeCodingTaskStage.RECOVERING,
            VibeCodingTaskStage.STALLED to VibeCodingTaskStage.RECOVERING,
            VibeCodingTaskStage.BLOCKED to VibeCodingTaskStage.RECOVERING,
            VibeCodingTaskStage.FAILED to VibeCodingTaskStage.RECOVERING ->
                requireCondition(
                    task,
                    task.recoveryTarget != null,
                    VibeCodingRejectionCode.RECOVERY_TARGET_REQUIRED,
                    "缺少恢复目标",
                    target,
                )
            VibeCodingTaskStage.RECOVERING to task.recoveryTarget ->
                if (task.recoveryTarget == null) {
                    reject(task, VibeCodingRejectionCode.RECOVERY_TARGET_REQUIRED, "缺少恢复目标")
                } else if (task.recoveryTarget.requiresBuildMode() && task.mode != CodingSessionMode.BUILD) {
                    reject(task, VibeCodingRejectionCode.BUILD_MODE_REQUIRED, "恢复写阶段前必须仍处于 BUILD")
                } else if (task.recoveryTarget.requiresBuildMode()) {
                    currentApprovalRejection(task)
                        ?: accept(
                            task.copy(stage = target, recoveryTarget = null),
                            VibeCodingTaskEvent.StageChanged(from, target),
                        )
                } else {
                    accept(
                        task.copy(stage = target, recoveryTarget = null),
                        VibeCodingTaskEvent.StageChanged(from, target),
                    )
                }
            else -> reject(task, VibeCodingRejectionCode.INVALID_TRANSITION, "不允许从 $from 跳转到 $target")
        }
    }

    private fun requireCurrentApproval(
        task: VibeCodingTask,
        target: VibeCodingTaskStage,
    ): VibeCodingDecision {
        currentApprovalRejection(task)?.let { return it }
        return stage(task, target)
    }

    private fun currentApprovalRejection(task: VibeCodingTask): VibeCodingDecision.Rejected? {
        val plan = task.plan
            ?: return reject(task, VibeCodingRejectionCode.INCOMPLETE_PLAN, "缺少当前计划")
        val approval = task.approval
            ?: return reject(task, VibeCodingRejectionCode.APPROVAL_REQUIRED, "缺少用户审批")
        if (approval.actorType != VibeCodingActorType.USER) {
            return reject(task, VibeCodingRejectionCode.USER_ONLY, "审批人必须是用户")
        }
        if (approval.planRevision != plan.revision) {
            return reject(task, VibeCodingRejectionCode.APPROVAL_STALE, "计划已变化，旧审批失效")
        }
        return null
    }

    private fun requireReviewAndBuildStrategy(
        task: VibeCodingTask,
        backend: BuildBackend,
        target: VibeCodingTaskStage,
    ): VibeCodingDecision {
        if (task.review?.evidence.isNullOrBlank()) {
            return reject(task, VibeCodingRejectionCode.REVIEW_REQUIRED, "缺少改动审查证据")
        }
        val strategy = task.buildStrategy
            ?: return reject(task, VibeCodingRejectionCode.BUILD_STRATEGY_REQUIRED, "缺少构建策略")
        if (strategy.reason.isBlank()) {
            return reject(task, VibeCodingRejectionCode.BUILD_STRATEGY_REQUIRED, "构建策略缺少依据")
        }
        if (strategy.backend != backend) {
            return reject(task, VibeCodingRejectionCode.BUILD_BACKEND_MISMATCH, "构建目标与已记录策略不一致")
        }
        return stage(task, target)
    }

    private fun VibeCodingTaskStage.requiresBuildMode(): Boolean =
        this in
            setOf(
                VibeCodingTaskStage.IMPLEMENTING,
                VibeCodingTaskStage.VALIDATING,
                VibeCodingTaskStage.DOCUMENTING,
                VibeCodingTaskStage.REVIEWING,
                VibeCodingTaskStage.LOCAL_BUILD,
                VibeCodingTaskStage.CLOUD_BUILD,
                VibeCodingTaskStage.RELEASING,
            )

    private fun requireCondition(
        task: VibeCodingTask,
        condition: Boolean,
        code: VibeCodingRejectionCode,
        reason: String,
        target: VibeCodingTaskStage,
    ): VibeCodingDecision = if (condition) stage(task, target) else reject(task, code, reason)

    private fun stage(
        task: VibeCodingTask,
        target: VibeCodingTaskStage,
    ): VibeCodingDecision =
        accept(
            task.copy(stage = target),
            VibeCodingTaskEvent.StageChanged(task.stage, target),
        )

    private fun accept(
        task: VibeCodingTask,
        event: VibeCodingTaskEvent,
    ): VibeCodingDecision.Accepted = VibeCodingDecision.Accepted(task, event)

    private fun reject(
        task: VibeCodingTask,
        code: VibeCodingRejectionCode,
        reason: String,
    ): VibeCodingDecision.Rejected = VibeCodingDecision.Rejected(task, code, reason)
}