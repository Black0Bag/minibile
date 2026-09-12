package com.ai.assistance.operit.data.vibecoding

import com.ai.assistance.operit.core.vibecoding.domain.ApprovalRecord
import com.ai.assistance.operit.core.vibecoding.domain.BuildFailureCategory
import com.ai.assistance.operit.core.vibecoding.domain.BuildRunEvidence
import com.ai.assistance.operit.core.vibecoding.domain.BuildRunStatus
import com.ai.assistance.operit.core.vibecoding.domain.BuildStrategyDecision
import com.ai.assistance.operit.core.vibecoding.domain.CodingSessionMode
import com.ai.assistance.operit.core.vibecoding.domain.DocumentationDecision
import com.ai.assistance.operit.core.vibecoding.domain.RequirementSpec
import com.ai.assistance.operit.core.vibecoding.domain.ResearchEvidenceStatus
import com.ai.assistance.operit.core.vibecoding.domain.ResearchRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReviewRecord
import com.ai.assistance.operit.core.vibecoding.domain.ReleaseEvidence
import com.ai.assistance.operit.core.vibecoding.domain.TaskPlanRevision
import com.ai.assistance.operit.core.vibecoding.domain.ValidationRun
import com.ai.assistance.operit.core.vibecoding.domain.ValidationStatus
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTask
import com.ai.assistance.operit.core.vibecoding.domain.VibeCodingTaskStage
import com.ai.assistance.operit.data.model.VibeCodingBuildRunEntity
import com.ai.assistance.operit.data.model.VibeCodingResearchRecordEntity
import com.ai.assistance.operit.data.model.VibeCodingTaskEntity
import com.ai.assistance.operit.data.model.VibeCodingValidationRunEntity
import kotlinx.serialization.json.Json

/** Maps between pure domain models and Room entities using JSON for nested objects. */
object VibeCodingTaskMapper {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    fun toEntity(task: VibeCodingTask, now: Long = System.currentTimeMillis()): VibeCodingTaskEntity =
        VibeCodingTaskEntity(
            id = task.id,
            chatId = task.sessionId,
            workspaceId = task.workspaceId,
            stage = task.stage.name,
            mode = task.mode.name,
            requirementJson = task.requirement?.let { json.encodeToString(RequirementSpec.serializer(), it) },
            localEvidenceFallbackApproved = task.localEvidenceFallbackApproved,
            planJson = task.plan?.let { json.encodeToString(TaskPlanRevision.serializer(), it) },
            approvalJson = task.approval?.let { json.encodeToString(ApprovalRecord.serializer(), it) },
            changedPathsJson = task.changedPaths.takeIf { it.isNotEmpty() }?.let { json.encodeToString(it.toList()) },
            documentationDecisionJson = task.documentationDecision?.let { json.encodeToString(DocumentationDecision.serializer(), it) },
            reviewJson = task.review?.let { json.encodeToString(ReviewRecord.serializer(), it) },
            buildStrategyJson = task.buildStrategy?.let { json.encodeToString(BuildStrategyDecision.serializer(), it) },
            releaseEvidenceJson = task.releaseEvidence?.let { json.encodeToString(ReleaseEvidence.serializer(), it) },
            recoveryTarget = task.recoveryTarget?.name,
            createdAt = now,
            updatedAt = now,
        )

    fun toDomain(
        entity: VibeCodingTaskEntity,
        researchRecords: List<VibeCodingResearchRecordEntity>,
        validationRuns: List<VibeCodingValidationRunEntity>,
        buildRuns: List<VibeCodingBuildRunEntity>,
    ): VibeCodingTask = VibeCodingTask(
        id = entity.id,
        sessionId = entity.chatId,
        workspaceId = entity.workspaceId,
        stage = VibeCodingTaskStage.valueOf(entity.stage),
        mode = CodingSessionMode.valueOf(entity.mode),
        requirement = entity.requirementJson?.let { json.decodeFromString(RequirementSpec.serializer(), it) },
        localEvidenceFallbackApproved = entity.localEvidenceFallbackApproved,
        plan = entity.planJson?.let { json.decodeFromString(TaskPlanRevision.serializer(), it) },
        approval = entity.approvalJson?.let { json.decodeFromString(ApprovalRecord.serializer(), it) },
        changedPaths = entity.changedPathsJson?.let { json.decodeFromString<List<String>>(it).toSet() } ?: emptySet(),
        researchRecords = researchRecords.map { it.toDomain() },
        validationRuns = validationRuns.map { it.toDomain() },
        documentationDecision = entity.documentationDecisionJson?.let { json.decodeFromString(DocumentationDecision.serializer(), it) },
        review = entity.reviewJson?.let { json.decodeFromString(ReviewRecord.serializer(), it) },
        buildStrategy = entity.buildStrategyJson?.let { json.decodeFromString(BuildStrategyDecision.serializer(), it) },
        buildRuns = buildRuns.map { it.toDomain() },
        releaseEvidence = entity.releaseEvidenceJson?.let { json.decodeFromString(ReleaseEvidence.serializer(), it) },
        recoveryTarget = entity.recoveryTarget?.let { VibeCodingTaskStage.valueOf(it) },
    )

    fun toEntity(record: ResearchRecord, taskId: String): VibeCodingResearchRecordEntity =
        VibeCodingResearchRecordEntity(
            taskId = taskId,
            query = record.query,
            source = record.source,
            conclusion = record.conclusion,
            status = record.status.name,
        )

    fun VibeCodingResearchRecordEntity.toDomain(): ResearchRecord =
        ResearchRecord(
            query = query,
            source = source,
            conclusion = conclusion,
            status = ResearchEvidenceStatus.valueOf(status),
        )

    fun toEntity(run: ValidationRun, taskId: String): VibeCodingValidationRunEntity =
        VibeCodingValidationRunEntity(
            taskId = taskId,
            runId = run.id,
            status = run.status.name,
            command = run.command,
            evidence = run.evidence,
        )

    fun VibeCodingValidationRunEntity.toDomain(): ValidationRun =
        ValidationRun(
            id = runId,
            status = ValidationStatus.valueOf(status),
            command = command,
            evidence = evidence,
        )

    fun toEntity(run: BuildRunEvidence, taskId: String): VibeCodingBuildRunEntity =
        VibeCodingBuildRunEntity(
            taskId = taskId,
            runId = run.runId,
            sourceSha = run.sourceSha,
            status = run.status.name,
            evidence = run.evidence,
            failureCategory = run.failureCategory?.name,
        )

    fun VibeCodingBuildRunEntity.toDomain(): BuildRunEvidence =
        BuildRunEvidence(
            runId = runId,
            sourceSha = sourceSha,
            status = BuildRunStatus.valueOf(status),
            evidence = evidence,
            failureCategory = failureCategory?.let { BuildFailureCategory.valueOf(it) },
        )
}