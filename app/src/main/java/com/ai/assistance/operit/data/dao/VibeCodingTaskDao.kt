package com.ai.assistance.operit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ai.assistance.operit.data.model.VibeCodingBuildAttemptEntity
import com.ai.assistance.operit.data.model.VibeCodingBuildRunEntity
import com.ai.assistance.operit.data.model.VibeCodingRecoveryCheckpointEntity
import com.ai.assistance.operit.data.model.VibeCodingResearchRecordEntity
import com.ai.assistance.operit.data.model.VibeCodingSubagentTaskEntity
import com.ai.assistance.operit.data.model.VibeCodingTaskEntity
import com.ai.assistance.operit.data.model.VibeCodingValidationRunEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VibeCodingTaskDao {
    @Query("SELECT * FROM vibecoding_tasks WHERE chatId = :chatId LIMIT 1")
    suspend fun getTaskByChatId(chatId: String): VibeCodingTaskEntity?

    @Query("SELECT * FROM vibecoding_tasks WHERE chatId = :chatId LIMIT 1")
    fun observeTaskByChatId(chatId: String): Flow<VibeCodingTaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTask(task: VibeCodingTaskEntity)

    @Query("DELETE FROM vibecoding_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)

    @Query("DELETE FROM vibecoding_tasks WHERE chatId = :chatId")
    suspend fun deleteTaskByChatId(chatId: String)

    @Query("SELECT * FROM vibecoding_research_records WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getResearchRecords(taskId: String): List<VibeCodingResearchRecordEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResearchRecord(record: VibeCodingResearchRecordEntity): Long

    @Query("DELETE FROM vibecoding_research_records WHERE taskId = :taskId")
    suspend fun deleteResearchRecords(taskId: String)

    @Query("SELECT * FROM vibecoding_validation_runs WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getValidationRuns(taskId: String): List<VibeCodingValidationRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertValidationRun(run: VibeCodingValidationRunEntity): Long

    @Query("DELETE FROM vibecoding_validation_runs WHERE taskId = :taskId")
    suspend fun deleteValidationRuns(taskId: String)

    @Query("SELECT * FROM vibecoding_build_runs WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getBuildRuns(taskId: String): List<VibeCodingBuildRunEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildRun(run: VibeCodingBuildRunEntity): Long

    @Query("DELETE FROM vibecoding_build_runs WHERE taskId = :taskId")
    suspend fun deleteBuildRuns(taskId: String)

    @Transaction
    suspend fun replaceResearchRecords(taskId: String, records: List<VibeCodingResearchRecordEntity>) {
        deleteResearchRecords(taskId)
        records.forEach { insertResearchRecord(it.copy(taskId = taskId)) }
    }

    @Transaction
    suspend fun replaceValidationRuns(taskId: String, runs: List<VibeCodingValidationRunEntity>) {
        deleteValidationRuns(taskId)
        runs.forEach { insertValidationRun(it.copy(taskId = taskId)) }
    }

    @Transaction
    suspend fun replaceBuildRuns(taskId: String, runs: List<VibeCodingBuildRunEntity>) {
        deleteBuildRuns(taskId)
        runs.forEach { insertBuildRun(it.copy(taskId = taskId)) }
    }

    // ─── Phase G: Build Attempts ───

    @Query("SELECT * FROM vibecoding_build_attempts WHERE taskId = :taskId ORDER BY createdAt ASC")
    suspend fun getBuildAttempts(taskId: String): List<VibeCodingBuildAttemptEntity>

    @Query("SELECT * FROM vibecoding_build_attempts WHERE failureFingerprint = :fingerprint LIMIT 1")
    suspend fun getAttemptByFingerprint(fingerprint: String): VibeCodingBuildAttemptEntity?

    @Query("SELECT * FROM vibecoding_build_attempts WHERE runId = :runId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestAttemptByRunId(runId: String): VibeCodingBuildAttemptEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBuildAttempt(attempt: VibeCodingBuildAttemptEntity)

    @Query("UPDATE vibecoding_build_attempts SET status = :status, fixCommitSha = :fixCommitSha WHERE attemptId = :attemptId")
    suspend fun updateAttemptStatus(attemptId: String, status: String, fixCommitSha: String?)

    @Query("DELETE FROM vibecoding_build_attempts WHERE taskId = :taskId")
    suspend fun deleteBuildAttempts(taskId: String)

    // ─── Phase G: Subagent Tasks ───

    @Query("SELECT * FROM vibecoding_subagent_tasks WHERE parentTaskId = :taskId ORDER BY createdAt ASC")
    suspend fun getSubagentTasks(taskId: String): List<VibeCodingSubagentTaskEntity>

    @Query("SELECT * FROM vibecoding_subagent_tasks WHERE subtaskId = :subtaskId LIMIT 1")
    suspend fun getSubagentTask(subtaskId: String): VibeCodingSubagentTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubagentTask(task: VibeCodingSubagentTaskEntity)

    @Query("UPDATE vibecoding_subagent_tasks SET status = :status, result = :result, errorMessage = :errorMessage, completedAt = :completedAt WHERE subtaskId = :subtaskId")
    suspend fun updateSubagentTaskResult(subtaskId: String, status: String, result: String?, errorMessage: String?, completedAt: Long?)

    @Query("DELETE FROM vibecoding_subagent_tasks WHERE parentTaskId = :taskId")
    suspend fun deleteSubagentTasks(taskId: String)

    // ─── Phase G: Recovery Checkpoints ───

    @Query("SELECT * FROM vibecoding_recovery_checkpoints WHERE taskId = :taskId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestCheckpoint(taskId: String): VibeCodingRecoveryCheckpointEntity?

    @Query("SELECT * FROM vibecoding_recovery_checkpoints WHERE buildRunId = :buildRunId ORDER BY createdAt DESC LIMIT 1")
    suspend fun getCheckpointByBuildRunId(buildRunId: String): VibeCodingRecoveryCheckpointEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCheckpoint(checkpoint: VibeCodingRecoveryCheckpointEntity)

    @Query("DELETE FROM vibecoding_recovery_checkpoints WHERE taskId = :taskId")
    suspend fun deleteCheckpoints(taskId: String)
}