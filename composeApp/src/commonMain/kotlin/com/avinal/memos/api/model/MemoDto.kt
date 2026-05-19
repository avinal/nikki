package com.avinal.memos.api.model

import kotlinx.serialization.Serializable

@Serializable
data class MemoDto(
    val name: String = "",
    val uid: String = "",
    val creator: String = "",
    val createTime: String = "",
    val updateTime: String = "",
    val displayTime: String = "",
    val content: String = "",
    val state: String = "NORMAL",
    val visibility: String = "PRIVATE",
    val pinned: Boolean = false,
    val parent: String = "",
    val property: MemoPropertyDto? = null,
    val tags: List<String> = emptyList(),
    val snippet: String = "",
    val attachments: List<AttachmentDto> = emptyList(),
    val reactions: List<ReactionDto> = emptyList(),
    val relations: List<RelationDto> = emptyList(),
)

@Serializable
data class MemoPropertyDto(
    val hasLink: Boolean = false,
    val hasTaskList: Boolean = false,
    val hasCode: Boolean = false,
    val hasIncompleteTasks: Boolean = false,
    val title: String = "",
)

@Serializable
data class AttachmentDto(
    val name: String = "",
    val filename: String = "",
    val type: String = "",
    val size: String = "0",
    val externalLink: String = "",
    val createTime: String = "",
    val memo: String = "",
)

@Serializable
data class ReactionDto(
    val name: String = "",
    val creator: String = "",
    val contentId: String = "",
    val reactionType: String = "",
    val createTime: String = "",
)

@Serializable
data class RelationDto(
    val memo: String = "",
    val relatedMemo: String = "",
    val type: String = "",
)

@Serializable
data class ListMemosResponse(
    val memos: List<MemoDto> = emptyList(),
    val nextPageToken: String = "",
)

@Serializable
data class CreateMemoRequest(
    val content: String,
    val visibility: String = "PRIVATE",
)

@Serializable
data class UpdateMemoBody(
    val content: String? = null,
    val visibility: String? = null,
    val pinned: Boolean? = null,
    val state: String? = null,
)

@Serializable
data class UpdateMemoRequest(
    val memo: UpdateMemoBody,
    val updateMask: FieldMask,
)

@Serializable
data class FieldMask(
    val paths: List<String>,
)

@Serializable
data class UpsertReactionRequest(
    val reaction: ReactionDto,
)
