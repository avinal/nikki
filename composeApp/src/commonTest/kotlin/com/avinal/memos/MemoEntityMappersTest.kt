package com.avinal.memos

import com.avinal.memos.db.entity.MemoEntity
import com.avinal.memos.db.entity.toDomain
import com.avinal.memos.db.entity.toEntity
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class MemoEntityMappersTest {

    private fun makeMemo(
        id: String = "abc",
        content: String = "hello",
        tags: List<String> = listOf("work"),
        pinned: Boolean = false,
        commentCount: Int = 0,
    ) = Memo(
        id = id, uid = "uid1", content = content, visibility = MemoVisibility.PRIVATE,
        pinned = pinned, state = "NORMAL",
        createTime = Instant.fromEpochMilliseconds(1000000),
        updateTime = Instant.fromEpochMilliseconds(2000000),
        displayTime = Instant.fromEpochMilliseconds(2000000),
        creator = "test",
        hasTaskList = false, hasIncompleteTasks = false, title = "Title",
        tags = tags, snippet = "hello...",
        commentCount = commentCount,
    )

    @Test
    fun roundTripPreservesAllFields() {
        val original = makeMemo(tags = listOf("work", "urgent"), pinned = true, commentCount = 3)
        val entity = original.toEntity(999L)
        val restored = entity.toDomain()
        assertEquals(original.id, restored.id)
        assertEquals(original.content, restored.content)
        assertEquals(original.visibility, restored.visibility)
        assertEquals(original.pinned, restored.pinned)
        assertEquals(original.tags, restored.tags)
        assertEquals(original.createTime, restored.createTime)
        assertEquals(original.commentCount, restored.commentCount)
    }

    @Test
    fun roundTripPreservesEmptyTags() {
        val original = makeMemo(tags = emptyList())
        val restored = original.toEntity(0).toDomain()
        assertTrue(restored.tags.isEmpty())
    }

    @Test
    fun cachedAtIsStored() {
        val entity = makeMemo().toEntity(12345L)
        assertEquals(12345L, entity.cachedAt)
    }

    @Test
    fun visibilityRoundTrips() {
        MemoVisibility.entries.forEach { vis ->
            val memo = makeMemo().copy(visibility = vis)
            val restored = memo.toEntity(0).toDomain()
            assertEquals(vis, restored.visibility)
        }
    }

    @Test
    fun timestampsPreserved() {
        val memo = makeMemo()
        val entity = memo.toEntity(0)
        assertEquals(1000000L, entity.createTime)
        assertEquals(2000000L, entity.updateTime)
        val restored = entity.toDomain()
        assertEquals(memo.createTime, restored.createTime)
    }
}
