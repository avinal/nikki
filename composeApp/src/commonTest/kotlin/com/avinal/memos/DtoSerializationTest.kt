package com.avinal.memos

import com.avinal.memos.api.model.ListMemosResponse
import com.avinal.memos.api.model.MemoDto
import com.avinal.memos.api.model.MemoPropertyDto
import com.avinal.memos.api.model.AttachmentDto
import com.avinal.memos.api.model.ReactionDto
import com.avinal.memos.api.model.RelationDto
import com.avinal.memos.api.model.RelationRefDto
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DtoSerializationTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun deserializeMinimalMemo() {
        val str = """{"name":"memos/abc","content":"hello"}"""
        val dto = json.decodeFromString(MemoDto.serializer(), str)
        assertEquals("memos/abc", dto.name)
        assertEquals("hello", dto.content)
        assertEquals("NORMAL", dto.state)
        assertEquals("PRIVATE", dto.visibility)
    }

    @Test
    fun deserializeFullMemo() {
        val str = """
        {
            "name":"memos/xyz",
            "uid":"uid1",
            "creator":"users/avinal",
            "createTime":"2026-05-13T09:10:31Z",
            "updateTime":"2026-05-13T09:10:31Z",
            "content":"# Hello",
            "state":"NORMAL",
            "visibility":"PRIVATE",
            "pinned":true,
            "tags":["work","devops"],
            "snippet":"Hello",
            "attachments":[{"name":"attachments/a1","filename":"img.jpg","type":"image/jpeg","size":"1024"}],
            "reactions":[{"name":"memos/xyz/reactions/1","creator":"users/avinal","reactionType":"❤️","createTime":"2026-05-14T13:29:20Z"}],
            "relations":[{"memo":{"name":"memos/c1","snippet":""},"relatedMemo":{"name":"memos/xyz","snippet":""},"type":"COMMENT"}],
            "property":{"hasLink":false,"hasTaskList":true,"hasCode":false,"hasIncompleteTasks":true,"title":"Hello"}
        }
        """.trimIndent()
        val dto = json.decodeFromString(MemoDto.serializer(), str)
        assertEquals("memos/xyz", dto.name)
        assertTrue(dto.pinned)
        assertEquals(listOf("work", "devops"), dto.tags)
        assertEquals(1, dto.attachments.size)
        assertEquals("image/jpeg", dto.attachments[0].type)
        assertEquals(1, dto.reactions.size)
        assertEquals("❤️", dto.reactions[0].reactionType)
        assertEquals(1, dto.relations.size)
        assertEquals("COMMENT", dto.relations[0].type)
        assertEquals("memos/c1", dto.relations[0].memo.name)
        assertTrue(dto.property!!.hasTaskList)
    }

    @Test
    fun deserializeListMemosResponse() {
        val str = """{"memos":[{"name":"memos/1","content":"a"},{"name":"memos/2","content":"b"}],"nextPageToken":"abc123"}"""
        val response = json.decodeFromString(ListMemosResponse.serializer(), str)
        assertEquals(2, response.memos.size)
        assertEquals("abc123", response.nextPageToken)
    }

    @Test
    fun deserializeEmptyListMemosResponse() {
        val str = """{"memos":[],"nextPageToken":""}"""
        val response = json.decodeFromString(ListMemosResponse.serializer(), str)
        assertTrue(response.memos.isEmpty())
        assertEquals("", response.nextPageToken)
    }

    @Test
    fun unknownFieldsIgnored() {
        val str = """{"name":"memos/1","content":"hello","someNewField":"value","anotherField":42}"""
        val dto = json.decodeFromString(MemoDto.serializer(), str)
        assertEquals("hello", dto.content)
    }

    @Test
    fun relationWithObjectRefs() {
        val str = """{"memo":{"name":"memos/c1","snippet":"comment"},"relatedMemo":{"name":"memos/m1","snippet":"parent"},"type":"COMMENT"}"""
        val rel = json.decodeFromString(RelationDto.serializer(), str)
        assertEquals("memos/c1", rel.memo.name)
        assertEquals("memos/m1", rel.relatedMemo.name)
        assertEquals("COMMENT", rel.type)
    }

    @Test
    fun attachmentSizeAsString() {
        val str = """{"name":"attachments/a1","filename":"doc.pdf","type":"application/pdf","size":"999999"}"""
        val att = json.decodeFromString(AttachmentDto.serializer(), str)
        assertEquals("999999", att.size)
    }
}
