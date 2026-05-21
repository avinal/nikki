package com.avinal.memos

import com.avinal.memos.domain.MemoVisibility
import kotlin.test.Test
import kotlin.test.assertEquals

class MemoVisibilityTest {

    @Test
    fun fromApiStringPrivate() {
        assertEquals(MemoVisibility.PRIVATE, MemoVisibility.fromApiString("PRIVATE"))
    }

    @Test
    fun fromApiStringPublic() {
        assertEquals(MemoVisibility.PUBLIC, MemoVisibility.fromApiString("PUBLIC"))
    }

    @Test
    fun fromApiStringProtected() {
        assertEquals(MemoVisibility.PROTECTED, MemoVisibility.fromApiString("PROTECTED"))
    }

    @Test
    fun fromApiStringUnknownDefaultsToPrivate() {
        assertEquals(MemoVisibility.PRIVATE, MemoVisibility.fromApiString("UNKNOWN"))
    }

    @Test
    fun toApiStringRoundTrips() {
        MemoVisibility.entries.forEach { vis ->
            assertEquals(vis, MemoVisibility.fromApiString(vis.toApiString()))
        }
    }
}
