package com.ai.assistance.operit.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "vibecoding_tasks",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["chatId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("chatId")],
)
data class VibeCodingTaskEntity(
    @PrimaryKey val id: String,
    val chatId: String,
    val workspaceId: String,
    val stage: String,
    val mode: String,
    val requirementJson: String? = null,
    val localEvidenceFallbackApproved: Boolean = false,
    val planJson: String? = null,
    val approvalJson: String? = null,
    val changedPathsJson: String? = null,
    val documentationDecisionJson: String? = null,
    val reviewJson: String? = null,
    val buildStrategyJson: String? = null,
    val releaseEvidenceJson: String? = null,
    val recoveryTarget: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vibecoding_research_records",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class VibeCodingResearchRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val query: String,
    val source: String? = null,
    val conclusion: String,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vibecoding_validation_runs",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class VibeCodingValidationRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val runId: String,
    val status: String,
    val command: String,
    val evidence: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vibecoding_build_runs",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId")],
)
data class VibeCodingBuildRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val taskId: String,
    val runId: String,
    val sourceSha: String,
    val status: String,
    val evidence: String,
    val failureCategory: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vibecoding_session_todos",
    foreignKeys = [
        ForeignKey(
            entity = ChatEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class SessionTodoEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val status: String,
    val priority: String,
    val order: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val parentTaskId: String? = null,
    val blockedReason: String? = null,
)

@Entity(
    tableName = "vibecoding_build_attempts",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("runId"), Index("failureFingerprint")],
)
data class VibeCodingBuildAttemptEntity(
    @PrimaryKey val attemptId: String,
    val taskId: String,
    val runId: String,
    val sourceSha: String,
    val failureFingerprint: String,
    val failureCategory: String,
    val fixDescription: String,
    val fixCommitSha: String? = null,
    val status: String,
    val createdAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "vibecoding_subagent_tasks",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["parentTaskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("parentTaskId"), Index("parentSessionId")],
)
data class VibeCodingSubagentTaskEntity(
    @PrimaryKey val subtaskId: String,
    val parentSessionId: String,
    val parentTaskId: String,
    val agentName: String,
    val instruction: String,
    val mode: String,
    val maxSteps: Int = 10,
    val status: String,
    val result: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
)

@Entity(
    tableName = "vibecoding_recovery_checkpoints",
    foreignKeys = [
        ForeignKey(
            entity = VibeCodingTaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("taskId"), Index("buildRunId")],
)
data class VibeCodingRecoveryCheckpointEntity(
    @PrimaryKey val checkpointId: String,
    val taskId: String,
    val buildRunId: String,
    val currentAttemptId: String? = null,
    val sourceSha: String,
    val headSha: String,
    val recoveryTarget: String,
    val snapshotJson: String,
    val createdAt: Long = System.currentTimeMillis(),
)