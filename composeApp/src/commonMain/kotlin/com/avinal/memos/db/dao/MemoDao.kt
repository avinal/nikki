package com.avinal.memos.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avinal.memos.db.entity.MemoEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoDao {

    @Query("SELECT * FROM memos ORDER BY pinned DESC, updateTime DESC")
    fun observeAll(): Flow<List<MemoEntity>>

    @Query("SELECT * FROM memos ORDER BY pinned DESC, updateTime DESC")
    suspend fun getAll(): List<MemoEntity>

    @Query("SELECT * FROM memos WHERE id = :id")
    suspend fun getById(id: String): MemoEntity?

    @Query("SELECT * FROM memos WHERE id = :id")
    fun observeById(id: String): Flow<MemoEntity?>

    @Upsert
    suspend fun upsert(memo: MemoEntity)

    @Upsert
    suspend fun upsertAll(memos: List<MemoEntity>)

    @Query("DELETE FROM memos WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM memos")
    suspend fun deleteAll()
}
