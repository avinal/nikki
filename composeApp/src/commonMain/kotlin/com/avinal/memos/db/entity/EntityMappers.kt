package com.avinal.memos.db.entity

import com.avinal.memos.domain.Attachment
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.domain.Reaction
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class AttachmentJson(
    val name: String,
    val filename: String,
    val mimeType: String,
    val size: Long,
    val externalLink: String,
)

@Serializable
private data class ReactionJson(
    val id: String,
    val creator: String,
    val reactionType: String,
)

fun MemoEntity.toDomain(): Memo = Memo(
    id = id,
    uid = uid,
    content = content,
    visibility = MemoVisibility.fromApiString(visibility),
    pinned = pinned,
    state = state,
    createTime = Instant.fromEpochMilliseconds(createTime),
    updateTime = Instant.fromEpochMilliseconds(updateTime),
    displayTime = Instant.fromEpochMilliseconds(displayTime),
    creator = creator,
    hasTaskList = hasTaskList,
    hasIncompleteTasks = hasIncompleteTasks,
    title = title,
    tags = deserializeTags(tags),
    snippet = snippet,
    attachments = deserializeAttachments(attachmentsJson),
    reactions = deserializeReactions(reactionsJson),
    commentCount = commentCount,
)

fun Memo.toEntity(cachedAt: Long): MemoEntity = MemoEntity(
    id = id,
    uid = uid,
    content = content,
    visibility = visibility.toApiString(),
    pinned = pinned,
    state = state,
    createTime = createTime.toEpochMilliseconds(),
    updateTime = updateTime.toEpochMilliseconds(),
    displayTime = displayTime.toEpochMilliseconds(),
    creator = creator,
    hasTaskList = hasTaskList,
    hasIncompleteTasks = hasIncompleteTasks,
    title = title,
    tags = serializeTags(tags),
    snippet = snippet,
    attachmentsJson = serializeAttachments(attachments),
    reactionsJson = serializeReactions(reactions),
    commentCount = commentCount,
    cachedAt = cachedAt,
)

private fun serializeTags(tags: List<String>): String =
    JsonArray(tags.map { JsonPrimitive(it) }).toString()

private fun deserializeTags(value: String): List<String> =
    if (value.isEmpty() || value == "[]") emptyList()
    else try {
        json.parseToJsonElement(value).jsonArray.map { it.jsonPrimitive.content }
    } catch (_: Exception) {
        emptyList()
    }

private fun serializeAttachments(attachments: List<Attachment>): String =
    json.encodeToString(kotlinx.serialization.builtins.ListSerializer(AttachmentJson.serializer()),
        attachments.map { AttachmentJson(it.name, it.filename, it.mimeType, it.size, it.externalLink) }
    )

private fun deserializeAttachments(value: String): List<Attachment> =
    if (value.isEmpty() || value == "[]") emptyList()
    else try {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(AttachmentJson.serializer()), value)
            .map { Attachment(it.name, it.filename, it.mimeType, it.size, it.externalLink) }
    } catch (_: Exception) {
        emptyList()
    }

private fun serializeReactions(reactions: List<Reaction>): String =
    json.encodeToString(kotlinx.serialization.builtins.ListSerializer(ReactionJson.serializer()),
        reactions.map { ReactionJson(it.id, it.creator, it.reactionType) }
    )

private fun deserializeReactions(value: String): List<Reaction> =
    if (value.isEmpty() || value == "[]") emptyList()
    else try {
        json.decodeFromString(kotlinx.serialization.builtins.ListSerializer(ReactionJson.serializer()), value)
            .map { Reaction(it.id, it.creator, it.reactionType) }
    } catch (_: Exception) {
        emptyList()
    }
