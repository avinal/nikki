package com.avinal.memos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pending_sync")
data class PendingSyncEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val memoId: String? = null,
    val action: String,
    val payload: String,
    val createdAt: Long,
)
