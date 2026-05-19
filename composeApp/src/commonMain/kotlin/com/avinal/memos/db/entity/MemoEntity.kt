package com.avinal.memos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memos")
data class MemoEntity(
    @PrimaryKey val id: String,
    val uid: String,
    val content: String,
    val visibility: String,
    val pinned: Boolean,
    val state: String,
    val createTime: Long,
    val updateTime: Long,
    val displayTime: Long,
    val creator: String,
    val hasTaskList: Boolean,
    val hasIncompleteTasks: Boolean,
    val title: String,
    val tags: String,
    val snippet: String,
    val attachmentsJson: String = "[]",
    val reactionsJson: String = "[]",
    val commentCount: Int = 0,
    val cachedAt: Long,
)
