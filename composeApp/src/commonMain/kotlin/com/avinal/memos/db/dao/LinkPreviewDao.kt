package com.avinal.memos.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.avinal.memos.db.entity.LinkPreviewEntity

@Dao
interface LinkPreviewDao {
    @Query("SELECT * FROM link_previews WHERE url = :url AND cachedAt > :minTimestamp LIMIT 1")
    suspend fun getIfFresh(url: String, minTimestamp: Long): LinkPreviewEntity?

    @Upsert
    suspend fun upsert(entity: LinkPreviewEntity)

    @Query("DELETE FROM link_previews WHERE cachedAt < :olderThan")
    suspend fun deleteOlderThan(olderThan: Long)
}
