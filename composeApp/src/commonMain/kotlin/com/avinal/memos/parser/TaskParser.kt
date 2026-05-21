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

    fun extractTasks(memoId: String, content: String): List<Task> {
        var taskOrdinal = 0
        return content.lines().mapIndexedNotNull { index, line ->
            val match = taskLineRegex.find(line) ?: return@mapIndexedNotNull null
            val completed = match.groupValues[1].lowercase() == "x"
            val rawText = match.groupValues[2]
            val cleanText = cleanTaskText(rawText)
            taskOrdinal++

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
                lists = parseLists(rawText),
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
