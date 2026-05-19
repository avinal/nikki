package com.avinal.memos.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.ui.theme.LocalAccentColor

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    onTaskToggle: ((lineIndex: Int, checked: Boolean) -> Unit)? = null,
) {
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accent = LocalAccentColor.current

    Column(modifier = modifier) {
        val lines = markdown.lines()
        var lineIndex = 0
        var inCodeBlock = false
        val codeBlockLines = mutableListOf<String>()

        while (lineIndex < lines.size) {
            val line = lines[lineIndex]

            if (line.trimStart().startsWith("```")) {
                if (inCodeBlock) {
                    CodeBlock(codeBlockLines.joinToString("\n"), textColor)
                    codeBlockLines.clear()
                    inCodeBlock = false
                } else {
                    inCodeBlock = true
                }
                lineIndex++
                continue
            }

            if (inCodeBlock) {
                codeBlockLines.add(line)
                lineIndex++
                continue
            }

            when {
                line.startsWith("### ") -> HeadingBlock(line.removePrefix("### "), level = 3, textColor)
                line.startsWith("## ") -> HeadingBlock(line.removePrefix("## "), level = 2, textColor)
                line.startsWith("# ") && !line.startsWith("# ", 1) -> {
                    val text = line.removePrefix("# ")
                    if (text.all { it == '#' || it.isLetterOrDigit() || it == '_' || it == '-' } && !text.contains(' ')) {
                        TagChip(text, accent)
                    } else {
                        HeadingBlock(text, level = 1, textColor)
                    }
                }
                line.trimStart().startsWith("- [") && line.contains("]") -> {
                    val checked = line.contains("[x]", ignoreCase = true)
                    val text = line.substringAfter("] ").trim()
                    TaskItem(text, checked, lineIndex, onTaskToggle, textColor, subtleColor, accent)
                }
                line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ") -> {
                    val text = line.trimStart().removePrefix("- ").removePrefix("* ")
                    ListItem(text, textColor, accent)
                }
                line.startsWith("> ") -> BlockquoteBlock(line.removePrefix("> "), subtleColor, accent)
                line.trim() == "---" || line.trim() == "***" || line.trim() == "___" -> {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = subtleColor.copy(alpha = 0.3f))
                }
                line.trimStart().startsWith("|") && line.trimStart().indexOf("|", 1) > 0 -> {
                    val tableLines = mutableListOf(line)
                    while (lineIndex + 1 < lines.size && lines[lineIndex + 1].trimStart().startsWith("|")) {
                        lineIndex++
                        tableLines.add(lines[lineIndex])
                    }
                    TableBlock(tableLines, textColor, accent)
                }
                line.isBlank() -> Spacer(Modifier.height(4.dp))
                else -> {
                    val tagRegex = Regex("""^#(\w+)(\s+.*)?$""")
                    val tagMatch = tagRegex.find(line.trim())
                    if (tagMatch != null && !line.trim().startsWith("##")) {
                        FlowRow(
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp),
                        ) {
                            Regex("""#(\w+)""").findAll(line).forEach { match ->
                                TagChip(match.groupValues[1], accent)
                            }
                            val nonTagText = line.replace(Regex("""#\w+"""), "").trim()
                            if (nonTagText.isNotEmpty()) {
                                Text(parseInlineFormatting(nonTagText, textColor, accent), style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        ParagraphBlock(line, textColor, accent)
                    }
                }
            }
            lineIndex++
        }

        if (inCodeBlock && codeBlockLines.isNotEmpty()) {
            CodeBlock(codeBlockLines.joinToString("\n"), textColor)
        }
    }
}

@Composable
private fun TagChip(tag: String, accent: Color) {
    Text(
        text = "#$tag",
        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
        color = accent,
        modifier = Modifier
            .border(1.dp, accent.copy(alpha = 0.4f), RoundedCornerShape(50))
            .background(accent.copy(alpha = 0.1f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}

@Composable
private fun HeadingBlock(text: String, level: Int, textColor: Color) {
    val accent = LocalAccentColor.current
    val style = when (level) {
        1 -> MaterialTheme.typography.headlineMedium
        2 -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.titleMedium
    }
    Text(
        text = parseInlineFormatting(text, textColor, accent),
        style = style,
        color = textColor,
        modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun ParagraphBlock(text: String, textColor: Color, accent: Color) {
    Text(
        text = parseInlineFormatting(text, textColor, accent),
        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
        color = textColor,
    )
}

@Composable
private fun ListItem(text: String, textColor: Color, accent: Color) {
    Row(modifier = Modifier.padding(start = 4.dp, top = 1.dp, bottom = 1.dp)) {
        Text("•", style = MaterialTheme.typography.bodyMedium, color = textColor)
        Spacer(Modifier.width(8.dp))
        Text(
            text = parseInlineFormatting(text, textColor, accent),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun TaskItem(
    text: String,
    checked: Boolean,
    actualLineIndex: Int,
    onToggle: ((Int, Boolean) -> Unit)?,
    textColor: Color,
    subtleColor: Color,
    accent: Color,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 3.dp, bottom = 3.dp),
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = { newValue -> onToggle?.invoke(actualLineIndex, newValue) },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = subtleColor,
                checkmarkColor = Color.White,
            ),
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = parseInlineFormattingWithTags(text, textColor, accent),
            style = MaterialTheme.typography.bodyMedium.copy(
                lineHeight = 22.sp,
                textDecoration = if (checked) TextDecoration.LineThrough else TextDecoration.None,
            ),
            color = if (checked) subtleColor else textColor,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun BlockquoteBlock(text: String, subtleColor: Color, accent: Color) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Spacer(
            modifier = Modifier
                .width(3.dp)
                .height(20.dp)
                .background(accent.copy(alpha = 0.4f)),
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = parseInlineFormatting(text, subtleColor, accent),
            style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
            color = subtleColor,
        )
    }
}

@Composable
private fun TableBlock(tableLines: List<String>, textColor: Color, accent: Color) {
    val rows = tableLines
        .filter { !it.trim().matches(Regex("""^\|[-:|\\s]+\|$""")) }
        .map { line ->
            line.trim().removePrefix("|").removeSuffix("|").split("|").map { it.trim() }
        }
    if (rows.isEmpty()) return

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
    ) {
        rows.forEachIndexed { rowIndex, cells ->
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
                cells.forEach { cell ->
                    Text(
                        text = cell,
                        fontSize = 13.sp,
                        fontWeight = if (rowIndex == 0) FontWeight.SemiBold else FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                    )
                }
            }
            if (rowIndex == 0) {
                Spacer(Modifier.fillMaxWidth().height(1.dp).background(textColor.copy(alpha = 0.1f)))
            }
        }
    }
}

@Composable
private fun CodeBlock(code: String, textColor: Color) {
    val accent = LocalAccentColor.current
    val highlighted = remember(code) { highlightCode(code, textColor, accent) }

    Text(
        text = highlighted,
        style = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 20.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
    )
}

private val codeKeywords = setOf(
    "fun", "val", "var", "class", "object", "interface", "if", "else", "when", "for", "while",
    "return", "import", "package", "suspend", "override", "private", "public", "internal",
    "data", "sealed", "enum", "companion", "abstract", "open", "const", "lateinit",
    "def", "self", "lambda", "yield", "async", "await", "try", "catch", "finally", "throw",
    "function", "let", "const", "export", "default", "from", "this", "new", "delete", "typeof",
    "int", "float", "double", "string", "bool", "boolean", "void", "null", "true", "false",
    "struct", "impl", "trait", "pub", "fn", "mod", "use", "mut", "ref", "match",
)
private val codeStringRegex = Regex("""(".*?"|'.*?')""")
private val codeCommentRegex = Regex("""(//.*$|#.*$)""", RegexOption.MULTILINE)
private val codeNumberRegex = Regex("""\b(\d+\.?\d*)\b""")

private fun highlightCode(code: String, textColor: Color, accent: Color): AnnotatedString = buildAnnotatedString {
    val stringColor = Color(0xFF6A9955)
    val commentColor = Color(0xFF6A6A6A)
    val keywordColor = accent
    val numberColor = Color(0xFFB5CEA8)

    data class Span(val range: IntRange, val style: SpanStyle)
    val spans = mutableListOf<Span>()

    codeCommentRegex.findAll(code).forEach { spans.add(Span(it.range, SpanStyle(color = commentColor))) }
    codeStringRegex.findAll(code).forEach {
        if (spans.none { s -> s.range.first <= it.range.first && s.range.last >= it.range.last }) {
            spans.add(Span(it.range, SpanStyle(color = stringColor)))
        }
    }
    Regex("""\b(\w+)\b""").findAll(code).forEach { match ->
        if (match.groupValues[1] in codeKeywords) {
            if (spans.none { s -> s.range.first <= match.range.first && s.range.last >= match.range.last }) {
                spans.add(Span(match.range, SpanStyle(color = keywordColor, fontWeight = FontWeight.SemiBold)))
            }
        }
    }
    codeNumberRegex.findAll(code).forEach { match ->
        if (spans.none { s -> s.range.first <= match.range.first && s.range.last >= match.range.last }) {
            spans.add(Span(match.range, SpanStyle(color = numberColor)))
        }
    }

    spans.sortBy { it.range.first }

    var pos = 0
    for (span in spans) {
        if (span.range.first > pos) {
            withStyle(SpanStyle(color = textColor)) { append(code.substring(pos, span.range.first)) }
        }
        if (span.range.first >= pos) {
            withStyle(span.style) { append(code.substring(span.range)) }
            pos = span.range.last + 1
        }
    }
    if (pos < code.length) {
        withStyle(SpanStyle(color = textColor)) { append(code.substring(pos)) }
    }
}

private val boldItalicRegex = Regex("""\*\*\*(.+?)\*\*\*""")
private val boldItalicUnderRegex = Regex("""___(.+?)___""")
private val boldRegex = Regex("""\*\*(.+?)\*\*""")
private val boldUnderRegex = Regex("""__(.+?)__""")
private val italicRegex = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""")
private val italicUnderRegex = Regex("""(?<!_)_(?!_)(.+?)(?<!_)_(?!_)""")
private val strikethroughRegex = Regex("""~~(.+?)~~""")
private val codeRegex = Regex("""`(.+?)`""")
private val linkRegex = Regex("""\[(.+?)]\((.+?)\)""")
private val urlRegex = Regex("""https?://\S+""")
private val inlineTagRegex = Regex("""#(\w+)""")

private fun parseInlineFormatting(text: String, textColor: Color, accent: Color): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val matches = listOfNotNull(
            boldItalicRegex.find(remaining),
            boldItalicUnderRegex.find(remaining),
            boldRegex.find(remaining),
            boldUnderRegex.find(remaining),
            strikethroughRegex.find(remaining),
            italicRegex.find(remaining),
            italicUnderRegex.find(remaining),
            codeRegex.find(remaining),
            linkRegex.find(remaining),
            urlRegex.find(remaining),
        )

        val firstMatch = matches.minByOrNull { it.range.first }

        if (firstMatch == null) {
            withStyle(SpanStyle(color = textColor)) { append(remaining) }
            break
        }

        withStyle(SpanStyle(color = textColor)) { append(remaining.substring(0, firstMatch.range.first)) }

        val matchedRegex = matches.first { it.range == firstMatch.range }
        val inner = if (firstMatch.groupValues.size > 1) firstMatch.groupValues[1] else firstMatch.value

        when {
            matchedRegex.value.startsWith("http") ->
                withLink(LinkAnnotation.Url(firstMatch.value, TextLinkStyles(SpanStyle(color = accent)))) { append(firstMatch.value) }
            matchedRegex.value.startsWith("***") || matchedRegex.value.startsWith("___") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor)) { append(inner) }
            matchedRegex.value.startsWith("**") || matchedRegex.value.startsWith("__") ->
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) { append(inner) }
            matchedRegex.value.startsWith("~~") ->
                withStyle(SpanStyle(textDecoration = TextDecoration.LineThrough, color = textColor)) { append(inner) }
            matchedRegex.value.startsWith("*") || matchedRegex.value.startsWith("_") ->
                withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) { append(inner) }
            matchedRegex.value.startsWith("`") ->
                withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, background = accent.copy(alpha = 0.1f), color = textColor)) { append(inner) }
            matchedRegex.value.startsWith("[") -> {
                val url = if (firstMatch.groupValues.size > 2) firstMatch.groupValues[2] else ""
                if (url.startsWith("http")) {
                    withLink(LinkAnnotation.Url(url, TextLinkStyles(SpanStyle(color = accent)))) { append(inner) }
                } else {
                    withStyle(SpanStyle(color = accent)) { append(inner) }
                }
            }
        }

        remaining = remaining.substring(firstMatch.range.last + 1)
    }
}

private fun parseInlineFormattingWithTags(text: String, textColor: Color, accent: Color): AnnotatedString = buildAnnotatedString {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val tagMatch = inlineTagRegex.find(remaining)
        val boldMatch = boldRegex.find(remaining)
        val italicMatch = italicRegex.find(remaining)
        val codeMatch = codeRegex.find(remaining)

        val firstMatch = listOfNotNull(tagMatch, boldMatch, italicMatch, codeMatch)
            .minByOrNull { it.range.first }

        if (firstMatch == null) {
            withStyle(SpanStyle(color = textColor)) { append(remaining) }
            break
        }

        withStyle(SpanStyle(color = textColor)) { append(remaining.substring(0, firstMatch.range.first)) }

        when (firstMatch) {
            tagMatch -> withStyle(SpanStyle(color = accent, fontSize = 13.sp)) {
                append("#${firstMatch.groupValues[1]}")
            }
            boldMatch -> withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                append(firstMatch.groupValues[1])
            }
            italicMatch -> withStyle(SpanStyle(fontStyle = FontStyle.Italic, color = textColor)) {
                append(firstMatch.groupValues[1])
            }
            codeMatch -> withStyle(SpanStyle(fontFamily = FontFamily.Monospace, fontSize = 13.sp, color = textColor)) {
                append(firstMatch.groupValues[1])
            }
        }

        remaining = remaining.substring(firstMatch.range.last + 1)
    }
}
