package com.avinal.memos

import com.avinal.memos.api.LinkPreviewFetcher
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class LinkPreviewFetcherTest {

    @Test
    fun parsesStandardOgTags() {
        val html = """
            <html><head>
            <meta property="og:title" content="Example Page">
            <meta property="og:description" content="A description of the page">
            <meta property="og:image" content="https://example.com/img.jpg">
            <meta property="og:site_name" content="Example">
            </head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals("Example Page", preview.title)
        assertEquals("A description of the page", preview.description)
        assertEquals("https://example.com/img.jpg", preview.imageUrl)
        assertEquals("Example", preview.siteName)
    }

    @Test
    fun parsesReversedAttributeOrder() {
        val html = """
            <html><head>
            <meta content="Reversed Title" property="og:title">
            <meta content="Reversed Desc" property="og:description">
            </head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals("Reversed Title", preview.title)
        assertEquals("Reversed Desc", preview.description)
    }

    @Test
    fun fallsBackToHtmlTitle() {
        val html = """
            <html><head><title>Fallback Title</title></head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com/page", html)
        assertNotNull(preview)
        assertEquals("Fallback Title", preview.title)
        assertNull(preview.description)
        assertNull(preview.imageUrl)
    }

    @Test
    fun returnsNullWhenNoTitleFound() {
        val html = """<html><head></head><body>No metadata</body></html>"""
        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNull(preview)
    }

    @Test
    fun returnsNullForBlankTitle() {
        val html = """
            <html><head>
            <meta property="og:title" content="   ">
            <title>   </title>
            </head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNull(preview)
    }

    @Test
    fun extractsDomainAsSiteName() {
        val html = """
            <html><head>
            <meta property="og:title" content="Page Title">
            </head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://www.example.com/path", html)
        assertNotNull(preview)
        assertEquals("example.com", preview.siteName)
    }

    @Test
    fun ogSiteNameTakesPrecedenceOverDomain() {
        val html = """
            <html><head>
            <meta property="og:title" content="Title">
            <meta property="og:site_name" content="My Site">
            </head><body></body></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://www.example.com", html)
        assertNotNull(preview)
        assertEquals("My Site", preview.siteName)
    }

    @Test
    fun truncatesLongTitle() {
        val longTitle = "A".repeat(300)
        val html = """<html><head><meta property="og:title" content="$longTitle"></head></html>"""
        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals(200, preview.title!!.length)
    }

    @Test
    fun truncatesLongDescription() {
        val longDesc = "B".repeat(500)
        val html = """<html><head>
            <meta property="og:title" content="Title">
            <meta property="og:description" content="$longDesc">
        </head></html>"""
        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals(300, preview.description!!.length)
    }

    @Test
    fun handlesDoubleAndSingleQuotes() {
        val html = """
            <html><head>
            <meta property='og:title' content='Single Quoted'>
            <meta property="og:description" content="Double Quoted">
            </head></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals("Single Quoted", preview.title)
        assertEquals("Double Quoted", preview.description)
    }

    @Test
    fun preservesUrlInPreview() {
        val html = """<html><head><meta property="og:title" content="Title"></head></html>"""
        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com/specific-page", html)
        assertNotNull(preview)
        assertEquals("https://example.com/specific-page", preview.url)
    }

    @Test
    fun standardOrderTakesPrecedenceOverReversed() {
        val html = """
            <html><head>
            <meta property="og:title" content="Standard Order">
            <meta content="Reversed Order" property="og:title">
            </head></html>
        """.trimIndent()

        val preview = LinkPreviewFetcher.parseOpenGraph("https://example.com", html)
        assertNotNull(preview)
        assertEquals("Standard Order", preview.title)
    }
}
