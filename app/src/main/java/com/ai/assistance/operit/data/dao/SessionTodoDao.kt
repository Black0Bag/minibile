package com.ai.assistance.operit.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.ai.assistance.operit.data.model.SessionTodoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionTodoDao {
    @Query("SELECT * FROM vibecoding_session_todos WHERE sessionId = :sessionId ORDER BY `order` ASC")
    fun observeBySession(sessionId: String): Flow<List<SessionTodoEntity>>

    @Query("SELECT * FROM vibecoding_session_todos WHERE sessionId = :sessionId ORDER BY `order` ASC")
    suspend fun getBySession(sessionId: String): List<SessionTodoEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(todo: SessionTodoEntity): Long

    @Query("DELETE FROM vibecoding_session_todos WHERE sessionId = :sessionId")
    suspend fun deleteBySession(sessionId: String)

    @Transaction
    suspend fun replaceBySession(sessionId: String, todos: List<SessionTodoEntity>) {
        deleteBySession(sessionId)
        todos.forEach { upsert(it.copy(sessionId = sessionId)) }
    }
}