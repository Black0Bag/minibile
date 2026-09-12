package com.ai.assistance.operit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ai.assistance.operit.data.model.VibeCodingBuildRunEntity
import com.ai.assistance.operit.data.model.VibeCodingResearchRecordEntity
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
}