package com.ai.assistance.operit.core.vibecoding.domain

/** User-owned mode for a VibeCoding session. Agents can observe but cannot change it. */
enum class CodingSessionMode {
    PLAN,
    BUILD,
}

enum class VibeCodingActorType {
    USER,
    MAIN_AGENT,
    SUBAGENT,
    TOOL,
    SYSTEM,
}

enum class VibeCodingRiskLevel {
    L1,
    L2,
    L3,
}

enum class VibeCodingTaskStage {
    NEW,
    CLARIFYING,
    EXPLORING,
    RESEARCHING,
    PLANNING,
    WAITING_PLAN_APPROVAL,
    READY_TO_BUILD,
    IMPLEMENTING,
    VALIDATING,
    DOCUMENTING,
    REVIEWING,
    LOCAL_BUILD,
    CLOUD_BUILD,
    RELEASING,
    COMPLETED,
    PAUSE_REQUESTED,
    PAUSED,
    STALLED,
    BLOCKED,
    FAILED,
    RECOVERING,
    CANCELLED,
}

enum class ResearchEvidenceStatus {
    VERIFIED,
    UNAVAILABLE,
    CONFLICTING,
}

enum class ValidationStatus {
    PASSED,
    FAILED,
}

enum class BuildBackend {
    LOCAL,
    CLOUD,
}

enum class BuildRunStatus {
    SUCCEEDED,
    FAILED,
}

enum class BuildFailureCategory {
    INFRASTRUCTURE,
    CODE,
    CONFIGURATION,
    SECRET,
    QUOTA,
    UNKNOWN,
}

data class RequirementSpec(
    val goal: String,
    val inScope: List<String>,
    val outOfScope: List<String>,
    val acceptanceCriteria: List<String>,
) {
    fun isComplete(): Boolean =
        goal.isNotBlank() &&
            inScope.any(String::isNotBlank) &&
            outOfScope.any(String::isNotBlank) &&
            acceptanceCriteria.any(String::isNotBlank)
}

data class ResearchRecord(
    val query: String,
    val source: String?,
    val conclusion: String,
    val status: ResearchEvidenceStatus,
) {
    fun isVerified(): Boolean =
        status == ResearchEvidenceStatus.VERIFIED &&
            query.isNotBlank() &&
            !source.isNullOrBlank() &&
            conclusion.isNotBlank()
}

data class PlanStep(
    val id: String,
    val description: String,
    val validation: String,
)

data class TaskPlanRevision(
    val revision: Long,
    val summary: String,
    val riskLevel: VibeCodingRiskLevel,
    val affectedPaths: List<String>,
    val steps: List<PlanStep>,
    val rollbackPlan: String,
) {
    fun isComplete(): Boolean =
        revision > 0L &&
            summary.isNotBlank() &&
            affectedPaths.any(String::isNotBlank) &&
            steps.isNotEmpty() &&
            steps.all { it.id.isNotBlank() && it.description.isNotBlank() && it.validation.isNotBlank() } &&
            rollbackPlan.isNotBlank()
}

data class ApprovalRecord(
    val planRevision: Long,
    val actorType: VibeCodingActorType,
    val approvedAtEpochMillis: Long,
)

data class ValidationRun(
    val id: String,
    val status: ValidationStatus,
    val command: String,
    val evidence: String,
)

data class DocumentationDecision(
    val required: Boolean,
    val updatedPaths: List<String> = emptyList(),
    val rationale: String,
) {
    fun isComplete(): Boolean =
        rationale.isNotBlank() && (!required || updatedPaths.any(String::isNotBlank))
}

data class ReviewRecord(
    val evidence: String,
)

data class BuildStrategyDecision(
    val backend: BuildBackend,
    val reason: String,
)

data class BuildRunEvidence(
    val runId: String,
    val sourceSha: String,
    val status: BuildRunStatus,
    val evidence: String,
    val failureCategory: BuildFailureCategory? = null,
) {
    fun isSuccessful(): Boolean =
        status == BuildRunStatus.SUCCEEDED &&
            runId.isNotBlank() &&
            sourceSha.isNotBlank() &&
            evidence.isNotBlank() &&
            failureCategory == null
}

sealed interface ReleaseEvidence {
    fun isComplete(): Boolean

    data class Published(
        val tag: String,
        val sourceSha: String,
        val assetNames: List<String>,
        val digestVerified: Boolean,
        val signatureRequired: Boolean,
        val signatureVerified: Boolean,
    ) : ReleaseEvidence {
        override fun isComplete(): Boolean =
            tag.isNotBlank() &&
                sourceSha.isNotBlank() &&
                assetNames.any(String::isNotBlank) &&
                digestVerified &&
                (!signatureRequired || signatureVerified)
    }

    data class NotRequired(val reason: String) : ReleaseEvidence {
        override fun isComplete(): Boolean = reason.isNotBlank()
    }
}

data class VibeCodingTask(
    val id: String,
    val sessionId: String,
    val workspaceId: String,
    val stage: VibeCodingTaskStage = VibeCodingTaskStage.NEW,
    val mode: CodingSessionMode = CodingSessionMode.PLAN,
    val requirement: RequirementSpec? = null,
    val researchRecords: List<ResearchRecord> = emptyList(),
    val localEvidenceFallbackApproved: Boolean = false,
    val plan: TaskPlanRevision? = null,
    val approval: ApprovalRecord? = null,
    val changedPaths: Set<String> = emptySet(),
    val validationRuns: List<ValidationRun> = emptyList(),
    val documentationDecision: DocumentationDecision? = null,
    val review: ReviewRecord? = null,
    val buildStrategy: BuildStrategyDecision? = null,
    val buildRuns: List<BuildRunEvidence> = emptyList(),
    val releaseEvidence: ReleaseEvidence? = null,
    val recoveryTarget: VibeCodingTaskStage? = null,
)

sealed interface VibeCodingTaskEvent {
    data class StageChanged(
        val from: VibeCodingTaskStage,
        val to: VibeCodingTaskStage,
    ) : VibeCodingTaskEvent

    data class ModeChanged(
        val from: CodingSessionMode,
        val to: CodingSessionMode,
    ) : VibeCodingTaskEvent

    data class PlanRevised(val revision: Long) : VibeCodingTaskEvent

    data class PlanApproved(val revision: Long) : VibeCodingTaskEvent

    data class FactRecorded(val kind: String) : VibeCodingTaskEvent

    data class Rejected(
        val code: VibeCodingRejectionCode,
        val reason: String,
    ) : VibeCodingTaskEvent
}

enum class VibeCodingRejectionCode {
    INVALID_IDENTITY,
    INVALID_STAGE,
    INVALID_TRANSITION,
    INCOMPLETE_REQUIREMENTS,
    MISSING_RESEARCH_EVIDENCE,
    INCOMPLETE_PLAN,
    APPROVAL_REQUIRED,
    APPROVAL_STALE,
    USER_ONLY,
    BUILD_MODE_REQUIRED,
    CHANGE_EVIDENCE_REQUIRED,
    VALIDATION_REQUIRED,
    VALIDATION_FAILED,
    DOCUMENTATION_REQUIRED,
    REVIEW_REQUIRED,
    BUILD_STRATEGY_REQUIRED,
    BUILD_BACKEND_MISMATCH,
    BUILD_EVIDENCE_REQUIRED,
    CODE_FAILURE_REQUIRED,
    RELEASE_EVIDENCE_REQUIRED,
    RECOVERY_TARGET_REQUIRED,
}

sealed interface VibeCodingDecision {
    data class Accepted(
        val task: VibeCodingTask,
        val event: VibeCodingTaskEvent,
    ) : VibeCodingDecision

    data class Rejected(
        val task: VibeCodingTask,
        val code: VibeCodingRejectionCode,
        val reason: String,
        val event: VibeCodingTaskEvent.Rejected = VibeCodingTaskEvent.Rejected(code, reason),
    ) : VibeCodingDecision
}
