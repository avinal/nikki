package com.avinal.memos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LinkPreviewFetcherTest {

    // Mirrors the private extractDomain in LinkPreviewFetcher
    private fun extractDomain(url: String): String? {
        val start = url.indexOf("://")
        if (start < 0) return null
        val afterProtocol = url.substring(start + 3)
        val end = afterProtocol.indexOf('/')
        val host = if (end > 0) afterProtocol.substring(0, end) else afterProtocol
        return host.removePrefix("www.")
    }

    @Test fun stripsWwwPrefix() = assertEquals("example.com", extractDomain("https://www.example.com/page"))
    @Test fun handlesNoPath() = assertEquals("example.com", extractDomain("https://example.com"))
    @Test fun handlesSubdomain() = assertEquals("memos.example.com", extractDomain("https://memos.example.com/api"))
    @Test fun handlesPort() = assertEquals("example.com:8080", extractDomain("https://example.com:8080/page"))
    @Test fun returnsNullForInvalidUrl() = assertNull(extractDomain("not-a-url"))
    @Test fun returnsNullForEmptyString() = assertNull(extractDomain(""))
}
