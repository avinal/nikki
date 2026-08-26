package com.avinal.memos.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "link_previews")
data class LinkPreviewEntity(
    @PrimaryKey val url: String,
    val title: String?,
    val description: String?,
    val imageUrl: String?,
    val siteName: String?,
    val cachedAt: Long,
)
