package com.avinal.memos

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MemosApiUrlTest {

    private fun buildUrl(base: String, path: String): String =
        "${base.trimEnd('/')}/api$path"

    @Test
    fun urlUsesApiWithoutV1() {
        val url = buildUrl("https://memos.example.com", "/memos")
        assertTrue(url.contains("/api/memos"))
        assertFalse(url.contains("/api/v1"))
    }

    @Test
    fun urlTrimsTrailingSlash() {
        val url = buildUrl("https://memos.example.com/", "/memos")
        assertFalse(url.contains("//api"))
    }

    @Test
    fun urlPreservesSubpath() {
        val url = buildUrl("https://example.com/memos-instance", "/auth/me")
        assertTrue(url.endsWith("/api/auth/me"))
    }
}
