package com.avinal.memos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class InlineParsingTest {

    private val emailRegex = Regex("""[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""")
    private val phoneRegex = Regex("""(?<!\w)(\+?\d{1,3}[-.\s]?)?\(?\d{2,4}\)?[-.\s]?\d{3,4}[-.\s]?\d{3,4}(?!\w)""")
    private val urlRegex = Regex("""https?://\S+""")

    @Test
    fun emailSimple() {
        val match = emailRegex.find("contact user@example.com for info")
        assertNotNull(match)
        assertEquals("user@example.com", match.value)
    }

    @Test
    fun emailWithSubdomain() {
        val match = emailRegex.find("send to admin@mail.company.co.uk")
        assertNotNull(match)
        assertEquals("admin@mail.company.co.uk", match.value)
    }

    @Test
    fun emailWithPlusAndDots() {
        val match = emailRegex.find("email: first.last+tag@gmail.com")
        assertNotNull(match)
        assertEquals("first.last+tag@gmail.com", match.value)
    }

    @Test
    fun emailMultipleInLine() {
        val matches = emailRegex.findAll("from alice@a.com to bob@b.org").toList()
        assertEquals(2, matches.size)
        assertEquals("alice@a.com", matches[0].value)
        assertEquals("bob@b.org", matches[1].value)
    }

    @Test
    fun emailNotMatchedInUrl() {
        val line = "visit https://example.com for more"
        val emailMatch = emailRegex.find(line)
        assertNull(emailMatch)
    }

    @Test
    fun phoneInternational() {
        val match = phoneRegex.find("call +1-555-123-4567 now")
        assertNotNull(match)
        assertTrue(match.value.contains("555"))
        assertTrue(match.value.contains("4567"))
    }

    @Test
    fun phoneWithParens() {
        val match = phoneRegex.find("call (555) 123-4567")
        assertNotNull(match)
        assertTrue(match.value.contains("555"))
    }

    @Test
    fun phoneDotSeparated() {
        val match = phoneRegex.find("fax: 555.123.4567")
        assertNotNull(match)
        assertEquals("555.123.4567", match.value)
    }

    @Test
    fun phoneSpaceSeparated() {
        val match = phoneRegex.find("dial 555 123 4567")
        assertNotNull(match)
        assertTrue(match.value.contains("555"))
    }

    @Test
    fun phoneWithCountryCode() {
        val match = phoneRegex.find("+44 207 946 0958")
        assertNotNull(match)
        assertTrue(match.value.contains("44"))
        assertTrue(match.value.contains("0958"))
    }

    @Test
    fun urlDetection() {
        val match = urlRegex.find("see https://github.com/avinal/nikki for source")
        assertNotNull(match)
        assertEquals("https://github.com/avinal/nikki", match.value)
    }

    @Test
    fun urlInListItem() {
        val line = "- check https://example.com/page"
        val match = urlRegex.find(line)
        assertNotNull(match)
        assertEquals("https://example.com/page", match.value)
    }

    @Test
    fun urlInTaskLine() {
        val line = "- [ ] review https://docs.google.com/doc/123"
        val match = urlRegex.find(line)
        assertNotNull(match)
        assertEquals("https://docs.google.com/doc/123", match.value)
    }

    @Test
    fun emailAndUrlInSameLine() {
        val line = "contact user@test.com or visit https://test.com"
        val email = emailRegex.find(line)
        val url = urlRegex.find(line)
        assertNotNull(email)
        assertNotNull(url)
        assertEquals("user@test.com", email.value)
        assertEquals("https://test.com", url.value)
    }

    @Test
    fun phoneDigitsExtraction() {
        val match = phoneRegex.find("+1-555-123-4567")
        assertNotNull(match)
        val digits = match.value.filter { it.isDigit() || it == '+' }
        assertEquals("+15551234567", digits)
    }

    @Test
    fun noFalsePositiveOnShortNumbers() {
        val line = "version 2.3.1 released"
        val match = phoneRegex.find(line)
        assertNull(match)
    }
}
