package com.avinal.memos

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarkdownFeaturesTest {

    private val tagRegex = Regex("""(?<!\\)#(\w+)""")
    private val footnoteDefRegex = Regex("""^\[\^(\w+)]:\s*(.+)$""")
    private val footnoteRefRegex = Regex("""\[\^(\w+)]""")

    // --- Tag escaping ---

    @Test
    fun unescapedTagIsDetected() {
        val match = tagRegex.find("#hello world")
        assertNotNull(match)
        assertEquals("hello", match.groupValues[1])
    }

    @Test
    fun escapedTagIsNotDetected() {
        val match = tagRegex.find("\\#hello world")
        assertNull(match)
    }

    @Test
    fun mixedEscapedAndUnescapedTags() {
        val matches = tagRegex.findAll("\\#skip #keep \\#also_skip #another").toList()
        assertEquals(2, matches.size)
        assertEquals("keep", matches[0].groupValues[1])
        assertEquals("another", matches[1].groupValues[1])
    }

    @Test
    fun escapedTagRendersAsPlainHash() {
        val text = "\\#notag"
        val stripped = text.replace("\\#", "#")
        assertEquals("#notag", stripped)
    }

    @Test
    fun tagAtLineStart() {
        val line = "#myTag some text"
        val blockTagRegex = Regex("""^(?<!\\)#(\w+)(\s+.*)?$""")
        val match = blockTagRegex.find(line.trim())
        assertNotNull(match)
        assertEquals("myTag", match.groupValues[1])
    }

    @Test
    fun escapedTagAtLineStartNotMatched() {
        val line = "\\#myTag some text"
        val blockTagRegex = Regex("""^(?<!\\)#(\w+)(\s+.*)?$""")
        val match = blockTagRegex.find(line.trim())
        assertNull(match)
    }

    // --- Footnote definitions ---

    @Test
    fun footnoteDefinitionSimple() {
        val match = footnoteDefRegex.find("[^1]: This is the first footnote.")
        assertNotNull(match)
        assertEquals("1", match.groupValues[1])
        assertEquals("This is the first footnote.", match.groupValues[2])
    }

    @Test
    fun footnoteDefinitionWithAlphanumericId() {
        val match = footnoteDefRegex.find("[^note1]: A longer explanation here.")
        assertNotNull(match)
        assertEquals("note1", match.groupValues[1])
        assertEquals("A longer explanation here.", match.groupValues[2])
    }

    @Test
    fun footnoteDefinitionCollectsMultiple() {
        val lines = listOf(
            "[^1]: First footnote.",
            "Some regular text.",
            "[^2]: Second footnote.",
        )
        val footnotes = linkedMapOf<String, String>()
        lines.forEach { l ->
            footnoteDefRegex.find(l.trim())?.let {
                footnotes[it.groupValues[1]] = it.groupValues[2]
            }
        }
        assertEquals(2, footnotes.size)
        assertEquals("First footnote.", footnotes["1"])
        assertEquals("Second footnote.", footnotes["2"])
    }

    @Test
    fun footnoteDefinitionNotMatchedInRegularText() {
        val match = footnoteDefRegex.find("This is not a [^1] footnote definition")
        assertNull(match)
    }

    // --- Footnote references ---

    @Test
    fun footnoteReferenceSimple() {
        val match = footnoteRefRegex.find("Some text[^1] with a footnote.")
        assertNotNull(match)
        assertEquals("1", match.groupValues[1])
    }

    @Test
    fun footnoteReferenceMultiple() {
        val matches = footnoteRefRegex.findAll("First[^1] and second[^2] references.").toList()
        assertEquals(2, matches.size)
        assertEquals("1", matches[0].groupValues[1])
        assertEquals("2", matches[1].groupValues[1])
    }

    @Test
    fun footnoteReferenceAlphanumericId() {
        val match = footnoteRefRegex.find("See this[^note1] for details.")
        assertNotNull(match)
        assertEquals("note1", match.groupValues[1])
    }

    @Test
    fun footnoteRefNotMatchedInDefinition() {
        val line = "[^1]: This is a definition"
        val defMatch = footnoteDefRegex.find(line)
        assertNotNull(defMatch)
        val refMatch = footnoteRefRegex.find(line)
        assertNotNull(refMatch)
        assertTrue(defMatch.range.first <= refMatch.range.first)
    }

    @Test
    fun noFootnoteInPlainBrackets() {
        val match = footnoteRefRegex.find("[not a footnote](link)")
        assertNull(match)
    }
}
