package com.avinal.memos.parser

import com.avinal.memos.domain.ReminderDuration
import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.domain.Task
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

object TaskParser {

    private val taskLineRegex = Regex("""^\s*- \[([ xX])]\s+(.*)$""")

    private val isoDateRegex = Regex("""\b(\d{4}-\d{2}-\d{2})\b""")
    private val naturalDateRegex = Regex("""\b(today|tomorrow|yesterday)\b""", RegexOption.IGNORE_CASE)

    private val time12Regex = Regex("""\b(\d{1,2})(?::(\d{2}))?\s*(am|pm)\b""", RegexOption.IGNORE_CASE)
    private val time24Regex = Regex("""\b(\d{1,2}):(\d{2})\b""")

    // Reminder duration: !30min, !1hr, !2day, !1week
    private val reminderRegex = Regex("""!(\d+)\s*(min|hr|day|week)s?\b""", RegexOption.IGNORE_CASE)

    private val priorityRegex = Regex("""\bp([1-3])\b""")
    private val listRegex = Regex("""(?<!\w)#([a-zA-Z]\w*)""")

    fun extractTasks(memoId: String, content: String, memoTags: List<String> = emptyList()): List<Task> {
        var taskOrdinal = 0
        return content.lines().mapIndexedNotNull { index, line ->
            val match = taskLineRegex.find(line) ?: return@mapIndexedNotNull null
            val completed = match.groupValues[1].lowercase() == "x"
            val rawText = match.groupValues[2]
            val cleanText = cleanTaskText(rawText)
            taskOrdinal++

            val parsedLists = parseLists(rawText)
            Task(
                id = "${memoId}:${hashContent(cleanText, taskOrdinal)}",
                memoId = memoId,
                lineIndex = index,
                text = cleanText,
                rawText = rawText,
                originalLine = line,
                isCompleted = completed,
                dueDate = parseDueDate(rawText),
                dueTime = parseDueTime(rawText),
                reminder = parseReminder(rawText),
                priority = parsePriority(rawText),
                lists = parsedLists.ifEmpty { memoTags },
            )
        }
    }

    fun toggleTaskInContent(content: String, task: Task): String {
        val lines = content.lines().toMutableList()
        val idx = findTaskLine(lines, task)
        if (idx < 0) return content
        val line = lines[idx]
        lines[idx] = if (task.isCompleted) {
            line.replaceFirst("- [x]", "- [ ]").replaceFirst("- [X]", "- [ ]")
        } else {
            line.replaceFirst("- [ ]", "- [x]")
        }
        return lines.joinToString("\n")
    }

    fun replaceTaskLineInContent(content: String, task: Task, newLine: String): String {
        val lines = content.lines().toMutableList()
        val idx = findTaskLine(lines, task)
        if (idx < 0) return content
        lines[idx] = newLine
        return lines.joinToString("\n")
    }

    fun reconstructLine(task: Task): String {
        val checkbox = if (task.isCompleted) "- [x]" else "- [ ]"
        val parts = mutableListOf(task.text)
        task.dueDate?.let { parts.add(it.toString()) }
        task.dueTime?.let { parts.add(formatTime(it)) }
        task.reminder?.let { parts.add("!$it") }
        task.priority?.let { parts.add("p$it") }
        task.lists.forEach { parts.add("#$it") }
        return "$checkbox ${parts.joinToString(" ")}"
    }

    private fun findTaskLine(lines: List<String>, task: Task): Int {
        if (task.lineIndex in lines.indices && lines[task.lineIndex].trim() == task.originalLine.trim()) {
            return task.lineIndex
        }
        return lines.indexOfFirst { it.trim() == task.originalLine.trim() }
    }

    private fun formatTime(time: LocalTime): String {
        val h = time.hour; val m = time.minute
        return when {
            m == 0 && h == 0 -> "12am"
            m == 0 && h < 12 -> "${h}am"
            m == 0 && h == 12 -> "12pm"
            m == 0 -> "${h - 12}pm"
            else -> "${h}:${m.toString().padStart(2, '0')}"
        }
    }

    private fun hashContent(text: String, ordinal: Int): String {
        var hash = 0L
        for (c in "$text#$ordinal") hash = hash * 31 + c.code
        return hash.toULong().toString(36)
    }

    private fun parseDueDate(text: String): LocalDate? {
        isoDateRegex.find(text)?.let {
            return try { LocalDate.parse(it.groupValues[1]) } catch (_: Exception) { null }
        }
        naturalDateRegex.find(text)?.let {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            return when (it.groupValues[1].lowercase()) {
                "today" -> today
                "tomorrow" -> today.plus(1, DateTimeUnit.DAY)
                "yesterday" -> today.plus(-1, DateTimeUnit.DAY)
                else -> null
            }
        }
        return null
    }

    private fun parseDueTime(text: String): LocalTime? {
        val cleaned = text.replace(reminderRegex, "")
        time12Regex.find(cleaned)?.let { m ->
            var h = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].toIntOrNull() ?: 0
            val ampm = m.groupValues[3].lowercase()
            if (h !in 1..12 || min !in 0..59) return null
            if (ampm == "pm" && h != 12) h += 12
            if (ampm == "am" && h == 12) h = 0
            return LocalTime(h, min)
        }
        time24Regex.find(cleaned)?.let { m ->
            val h = m.groupValues[1].toIntOrNull() ?: return null
            val min = m.groupValues[2].toIntOrNull() ?: return null
            if (h !in 0..23 || min !in 0..59) return null
            val start = m.range.first
            if (start >= 4 && cleaned.substring(start - 4, start).matches(Regex("""\d{4}"""))) return null
            return LocalTime(h, min)
        }
        return null
    }

    private fun parseReminder(text: String): ReminderDuration? {
        reminderRegex.find(text)?.let { m ->
            val value = m.groupValues[1].toIntOrNull() ?: return null
            if (value <= 0) return null
            val unit = when (m.groupValues[2].lowercase()) {
                "min" -> ReminderUnit.MIN
                "hr" -> ReminderUnit.HR
                "day" -> ReminderUnit.DAY
                "week" -> ReminderUnit.WEEK
                else -> return null
            }
            return ReminderDuration(value, unit)
        }
        return null
    }

    private fun parsePriority(text: String): Int? =
        priorityRegex.find(text)?.groupValues?.get(1)?.toIntOrNull()

    private fun parseLists(text: String): List<String> =
        listRegex.findAll(text).map { it.groupValues[1] }.toList()

    private val typoMap = mapOf(
        "tday" to "today", "todya" to "today", "toaday" to "today", "toady" to "today",
        "tmrw" to "tomorrow", "tomorow" to "tomorrow", "tommorow" to "tomorrow", "tomorraww" to "tomorrow", "tomorrw" to "tomorrow",
        "yestrday" to "yesterday", "ysterday" to "yesterday", "yesterady" to "yesterday",
        "munday" to "monday", "monady" to "monday", "mnday" to "monday",
        "tusday" to "tuesday", "tueday" to "tuesday",
        "wendsday" to "wednesday", "wensday" to "wednesday", "wednsday" to "wednesday",
        "thurday" to "thursday", "thrusday" to "thursday",
        "firday" to "friday", "frday" to "friday",
        "saterday" to "saturday", "sturday" to "saturday",
        "sundie" to "sunday", "sundya" to "sunday", "sunady" to "sunday",
    )

    fun validateContent(content: String): List<ParseWarning> {
        val warnings = mutableListOf<ParseWarning>()
        var noDateCount = 0

        content.lines().forEachIndexed { index, line ->
            val match = taskLineRegex.find(line) ?: return@forEachIndexed
            if (match.groupValues[1].lowercase() == "x") return@forEachIndexed

            val rawText = match.groupValues[2]

            fun err(issue: String, highlight: String = "") { warnings.add(ParseWarning(lineIndex = index, taskText = rawText.trim(), issue = issue, highlight = highlight, severity = IssueSeverity.ERROR)) }
            fun warn(issue: String, highlight: String = "") { warnings.add(ParseWarning(lineIndex = index, taskText = rawText.trim(), issue = issue, highlight = highlight, severity = IssueSeverity.WARNING)) }

            // --- ERRORS ---

            // E1: Invalid date
            isoDateRegex.find(rawText)?.let { m ->
                try { kotlinx.datetime.LocalDate.parse(m.groupValues[1]) }
                catch (_: Exception) { err("invalid date, use YYYY-MM-DD format", m.groupValues[1]) }
            }

            // E2: Invalid priority
            Regex("""\bp([4-9])\b""").find(rawText)?.let {
                err("invalid priority, only p1, p2, p3 are supported", "p${it.groupValues[1]}")
            }

            // E3: Reminder without any due date or time
            val cleaned = rawText.replace(reminderRegex, "")
            val hasTime = time12Regex.containsMatchIn(cleaned) || time24Regex.containsMatchIn(cleaned)
            val hasDate = isoDateRegex.containsMatchIn(rawText) || naturalDateRegex.containsMatchIn(rawText)
            val reminderMatch = reminderRegex.find(rawText)
            if (reminderMatch != null && !hasDate && !hasTime) {
                err("reminder has no due date or time to count back from", reminderMatch.value)
            }

            // E4: Invalid reminder format
            Regex("""!(\d+)\s*([a-zA-Z]+)""").find(rawText)?.let { m ->
                val unit = m.groupValues[2].lowercase().removeSuffix("s")
                if (unit !in listOf("min", "hr", "day", "week")) {
                    err("invalid reminder unit \"${m.groupValues[2]}\", use min, hr, day, or week", m.value)
                }
                val value = m.groupValues[1].toIntOrNull()
                if (value == null || value <= 0) {
                    err("invalid reminder value", m.value)
                }
            }

            // --- WARNINGS ---

            // W1: Time but no date
            val timeMatch = time12Regex.find(cleaned) ?: time24Regex.find(cleaned)
            if (timeMatch != null && !hasDate) {
                warn("time without date, using today", timeMatch.value)
            }

            // W2: Date/time in past
            isoDateRegex.find(rawText)?.let { m ->
                try {
                    val date = kotlinx.datetime.LocalDate.parse(m.groupValues[1])
                    val today = kotlin.time.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                    if (date < today) warn("date is in the past", m.groupValues[1])
                } catch (_: Exception) {}
            }

            // W3: Multiple priorities
            val pMatches = Regex("""\bp[1-3]\b""").findAll(rawText).toList()
            if (pMatches.size > 1) {
                warn("multiple priorities, using first (${pMatches[0].value})", pMatches.drop(1).joinToString(" ") { it.value })
            }

            // W3: Multiple dates
            val dateMatches = isoDateRegex.findAll(rawText).toList()
            val naturalMatches = naturalDateRegex.findAll(rawText).toList()
            if (dateMatches.size + naturalMatches.size > 1) {
                warn("multiple dates found, using first", (dateMatches + naturalMatches).drop(1).joinToString(" ") { it.value })
            }

            // W3: Multiple reminders
            val reminderMatches = reminderRegex.findAll(rawText).toList()
            if (reminderMatches.size > 1) {
                warn("multiple reminders, using first", reminderMatches.drop(1).joinToString(" ") { it.value })
            }

            // W5: Typo detection
            Regex("""\b\w+\b""").findAll(rawText).forEach { wordMatch ->
                val word = wordMatch.value.lowercase()
                typoMap[word]?.let { correction ->
                    warn("did you mean \"$correction\"?", wordMatch.value)
                }
            }

            // Track tasks without date for W4
            if (!hasDate && !hasTime) noDateCount++
        }

        // W4: Combined warning for tasks without dates
        if (noDateCount > 1) {
            warnings.add(ParseWarning(lineIndex = -1, taskText = "", issue = "$noDateCount tasks have no due date or time set"))
        }

        return warnings
    }

    private fun cleanTaskText(text: String): String {
        var clean = text
        clean = priorityRegex.replace(clean, "")
        clean = isoDateRegex.replace(clean, "")
        clean = naturalDateRegex.replace(clean, "")
        clean = reminderRegex.replace(clean, "")
        clean = time12Regex.replace(clean, "")
        clean = time24Regex.replace(clean, "")
        clean = listRegex.replace(clean, "")
        return clean.trim().replace(Regex("""\s{2,}"""), " ")
    }
}
