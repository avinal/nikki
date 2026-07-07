package com.avinal.memos.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.avinal.memos.db.entity.PendingSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PendingSyncDao {
    @Insert
    suspend fun insert(entity: PendingSyncEntity): Long

    @Query("SELECT * FROM pending_sync ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingSyncEntity>

    @Query("SELECT COUNT(*) FROM pending_sync")
    fun observeCount(): Flow<Int>

    @Query("DELETE FROM pending_sync WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM pending_sync")
    suspend fun deleteAll()

    @Query("SELECT * FROM pending_sync WHERE action = 'CREATE' AND payload = :payload LIMIT 1")
    suspend fun findCreateByPayload(payload: String): PendingSyncEntity?

    @Query("DELETE FROM pending_sync WHERE memoId = :memoId AND action = :action")
    suspend fun deleteByMemoIdAndAction(memoId: String, action: String)
}
