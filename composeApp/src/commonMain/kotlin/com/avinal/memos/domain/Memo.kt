package com.avinal.memos.domain

import kotlin.time.Instant

data class Memo(
    val id: String,
    val uid: String,
    val content: String,
    val visibility: MemoVisibility,
    val pinned: Boolean,
    val state: String,
    val createTime: Instant,
    val updateTime: Instant,
    val displayTime: Instant,
    val creator: String,
    val hasTaskList: Boolean,
    val hasIncompleteTasks: Boolean,
    val title: String,
    val tags: List<String>,
    val snippet: String,
    val attachments: List<Attachment> = emptyList(),
    val reactions: List<Reaction> = emptyList(),
    val commentCount: Int = 0,
)

data class Attachment(
    val name: String,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val externalLink: String,
) {
    val isImage: Boolean get() = mimeType.startsWith("image/")
}

data class Reaction(
    val id: String,
    val creator: String,
    val reactionType: String,
)
