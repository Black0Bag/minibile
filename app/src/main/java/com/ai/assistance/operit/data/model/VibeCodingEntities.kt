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