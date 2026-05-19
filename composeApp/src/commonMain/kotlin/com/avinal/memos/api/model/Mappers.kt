package com.avinal.memos.api.model

import com.avinal.memos.domain.Attachment
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.domain.Reaction
import com.avinal.memos.domain.User
import kotlin.time.Instant

private fun String.extractId(): String = substringAfterLast("/")

fun MemoDto.toDomain(): Memo = Memo(
    id = name.extractId(),
    uid = uid,
    content = content,
    visibility = MemoVisibility.fromApiString(visibility),
    pinned = pinned,
    state = state,
    createTime = parseTimestamp(createTime),
    updateTime = parseTimestamp(updateTime),
    displayTime = parseTimestamp(displayTime.ifEmpty { updateTime }),
    creator = creator.extractId(),
    hasTaskList = property?.hasTaskList ?: false,
    hasIncompleteTasks = property?.hasIncompleteTasks ?: false,
    title = property?.title ?: "",
    tags = tags,
    snippet = snippet,
    attachments = attachments.map { it.toDomain() },
    reactions = reactions.map { it.toDomain() },
)

fun AttachmentDto.toDomain(): Attachment = Attachment(
    name = name,
    filename = filename,
    mimeType = type,
    size = size.toLongOrNull() ?: 0L,
    externalLink = externalLink,
)

fun ReactionDto.toDomain(): Reaction = Reaction(
    id = name.extractId(),
    creator = creator.extractId(),
    reactionType = reactionType,
)

fun UserDto.toDomain(): User = User(
    id = name.extractId(),
    username = username,
    nickname = nickname,
    email = email,
    avatarUrl = avatarUrl,
    role = role,
)

private fun parseTimestamp(value: String): Instant =
    if (value.isNotEmpty()) {
        try {
            Instant.parse(value)
        } catch (_: Exception) {
            Instant.DISTANT_PAST
        }
    } else {
        Instant.DISTANT_PAST
    }
