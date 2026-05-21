package com.avinal.memos

import com.avinal.memos.domain.Attachment
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.domain.Reaction
import com.avinal.memos.util.BackupManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class BackupManagerTest {

    private fun makeMemo(
        id: String = "m1",
        content: String = "hello",
        visibility: MemoVisibility = MemoVisibility.PRIVATE,
        pinned: Boolean = false,
        tags: List<String> = emptyList(),
    ) = Memo(
        id = id, uid = "", content = content, visibility = visibility,
        pinned = pinned, state = "NORMAL",
        createTime = Instant.DISTANT_PAST, updateTime = Instant.DISTANT_PAST,
        displayTime = Instant.DISTANT_PAST, creator = "test",
        hasTaskList = false, hasIncompleteTasks = false, title = "",
        tags = tags, snippet = "",
    )

    @Test
    fun exportProducesValidJson() {
        val memos = listOf(makeMemo())
        val json = BackupManager.exportToJson(memos)
        assertNotNull(json)
        assertTrue(json.isNotEmpty(), "json should not be empty")
        val parsed = BackupManager.parseFromJson(json)
        assertNotNull(parsed, "should be parseable back")
        assertEquals(1, parsed.memos.size)
        assertEquals("hello", parsed.memos[0].content)
    }

    @Test
    fun exportImportRoundTrip() {
        val original = listOf(
            makeMemo(id = "1", content = "First memo", tags = listOf("work")),
            makeMemo(id = "2", content = "Second memo", visibility = MemoVisibility.PUBLIC, pinned = true),
        )
        val json = BackupManager.exportToJson(original)
        val parsed = BackupManager.parseFromJson(json)
        assertNotNull(parsed)
        assertEquals(2, parsed.memos.size)
        assertEquals("First memo", parsed.memos[0].content)
        assertEquals("Second memo", parsed.memos[1].content)
        assertEquals("PRIVATE", parsed.memos[0].visibility)
        assertEquals("PUBLIC", parsed.memos[1].visibility)
        assertTrue(parsed.memos[1].pinned)
        assertEquals(listOf("work"), parsed.memos[0].tags)
    }

    @Test
    fun exportSetsVersion() {
        val json = BackupManager.exportToJson(emptyList())
        val parsed = BackupManager.parseFromJson(json)
        assertNotNull(parsed)
        assertEquals(1, parsed.version)
    }

    @Test
    fun exportSetsMemoCount() {
        val json = BackupManager.exportToJson(listOf(makeMemo(), makeMemo(id = "2")))
        val parsed = BackupManager.parseFromJson(json)
        assertEquals(2, parsed!!.memoCount)
    }

    @Test
    fun parseInvalidJsonReturnsNull() {
        assertNull(BackupManager.parseFromJson("not valid json"))
    }

    @Test
    fun parseEmptyObjectReturnsDefaults() {
        val parsed = BackupManager.parseFromJson("{}")
        assertNotNull(parsed)
        assertEquals(0, parsed.memos.size)
    }

    @Test
    fun exportedAtIsNonEmpty() {
        val json = BackupManager.exportToJson(emptyList())
        val parsed = BackupManager.parseFromJson(json)
        assertTrue(parsed!!.exportedAt.isNotEmpty())
    }
}
