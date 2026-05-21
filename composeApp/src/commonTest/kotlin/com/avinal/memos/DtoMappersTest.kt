package com.avinal.memos

import com.avinal.memos.api.model.AttachmentDto
import com.avinal.memos.api.model.MemoDto
import com.avinal.memos.api.model.MemoPropertyDto
import com.avinal.memos.api.model.ReactionDto
import com.avinal.memos.api.model.RelationDto
import com.avinal.memos.api.model.RelationRefDto
import com.avinal.memos.api.model.UserDto
import com.avinal.memos.api.model.toDomain
import com.avinal.memos.domain.MemoVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DtoMappersTest {

    @Test
    fun memoIdExtractedFromName() {
        val dto = MemoDto(name = "memos/abc123def")
        val memo = dto.toDomain()
        assertEquals("abc123def", memo.id)
    }

    @Test
    fun creatorIdExtractedFromName() {
        val dto = MemoDto(creator = "users/avinal")
        val memo = dto.toDomain()
        assertEquals("avinal", memo.creator)
    }

    @Test
    fun visibilityMapped() {
        assertEquals(MemoVisibility.PUBLIC, MemoDto(visibility = "PUBLIC").toDomain().visibility)
        assertEquals(MemoVisibility.PRIVATE, MemoDto(visibility = "PRIVATE").toDomain().visibility)
        assertEquals(MemoVisibility.PROTECTED, MemoDto(visibility = "PROTECTED").toDomain().visibility)
    }

    @Test
    fun timestampsParsed() {
        val dto = MemoDto(
            createTime = "2026-05-13T09:10:31Z",
            updateTime = "2026-05-13T09:10:31Z",
        )
        val memo = dto.toDomain()
        assertTrue(memo.createTime.toEpochMilliseconds() > 0)
    }

    @Test
    fun emptyTimestampHandled() {
        val dto = MemoDto(createTime = "", updateTime = "")
        val memo = dto.toDomain()
        assertEquals(kotlin.time.Instant.DISTANT_PAST, memo.createTime)
    }

    @Test
    fun invalidTimestampHandled() {
        val dto = MemoDto(createTime = "not-a-date")
        val memo = dto.toDomain()
        assertEquals(kotlin.time.Instant.DISTANT_PAST, memo.createTime)
    }

    @Test
    fun propertyFieldsMapped() {
        val dto = MemoDto(property = MemoPropertyDto(hasTaskList = true, hasIncompleteTasks = true, title = "My Title"))
        val memo = dto.toDomain()
        assertTrue(memo.hasTaskList)
        assertTrue(memo.hasIncompleteTasks)
        assertEquals("My Title", memo.title)
    }

    @Test
    fun nullPropertyHandled() {
        val dto = MemoDto(property = null)
        val memo = dto.toDomain()
        assertFalse(memo.hasTaskList)
        assertEquals("", memo.title)
    }

    @Test
    fun tagsMapped() {
        val dto = MemoDto(tags = listOf("work", "urgent"))
        val memo = dto.toDomain()
        assertEquals(listOf("work", "urgent"), memo.tags)
    }

    @Test
    fun attachmentsMapped() {
        val dto = MemoDto(attachments = listOf(
            AttachmentDto(name = "attachments/abc", filename = "img.jpg", type = "image/jpeg", size = "1024")
        ))
        val memo = dto.toDomain()
        assertEquals(1, memo.attachments.size)
        assertEquals("img.jpg", memo.attachments[0].filename)
        assertEquals("image/jpeg", memo.attachments[0].mimeType)
        assertTrue(memo.attachments[0].isImage)
        assertEquals(1024L, memo.attachments[0].size)
    }

    @Test
    fun nonImageAttachment() {
        val dto = MemoDto(attachments = listOf(
            AttachmentDto(type = "application/pdf")
        ))
        assertFalse(dto.toDomain().attachments[0].isImage)
    }

    @Test
    fun reactionsMapped() {
        val dto = MemoDto(reactions = listOf(
            ReactionDto(name = "memos/m1/reactions/1", creator = "users/avinal", reactionType = "❤️")
        ))
        val memo = dto.toDomain()
        assertEquals(1, memo.reactions.size)
        assertEquals("❤️", memo.reactions[0].reactionType)
        assertEquals("avinal", memo.reactions[0].creator)
    }

    @Test
    fun commentCountFromRelations() {
        val dto = MemoDto(
            name = "memos/m1",
            relations = listOf(
                RelationDto(
                    memo = RelationRefDto(name = "memos/comment1"),
                    relatedMemo = RelationRefDto(name = "memos/m1"),
                    type = "COMMENT",
                ),
                RelationDto(
                    memo = RelationRefDto(name = "memos/comment2"),
                    relatedMemo = RelationRefDto(name = "memos/m1"),
                    type = "COMMENT",
                ),
                RelationDto(
                    memo = RelationRefDto(name = "memos/m1"),
                    relatedMemo = RelationRefDto(name = "memos/m2"),
                    type = "REFERENCE",
                ),
            ),
        )
        assertEquals(2, dto.toDomain().commentCount)
    }

    @Test
    fun userDtoMapped() {
        val dto = UserDto(name = "users/avinal", username = "avinal", nickname = "Avinal", email = "a@b.com", role = "USER")
        val user = dto.toDomain()
        assertEquals("avinal", user.id)
        assertEquals("avinal", user.username)
        assertEquals("Avinal", user.nickname)
    }

    @Test
    fun attachmentSizeStringParsed() {
        val dto = AttachmentDto(size = "197065")
        assertEquals(197065L, dto.toDomain().size)
    }

    @Test
    fun attachmentSizeInvalidStringDefaultsToZero() {
        val dto = AttachmentDto(size = "not-a-number")
        assertEquals(0L, dto.toDomain().size)
    }
}
