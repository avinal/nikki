package com.avinal.memos.parser

import com.avinal.memos.domain.Task
import kotlin.time.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

object TaskParser {

    private val taskLineRegex = Regex("""^\s*- \[([ xX])]\s+(.*)$""")
    private val dateRegex = Regex("""\d{4}-\d{2}-\d{2}""")
    private val priorityRegex = Regex("""\bp([1-3])\b""")
    private val labelRegex = Regex("""(?<!\w)@(\w+)""")
    private val listRegex = Regex("""(?<!\w)#(\w+)""")

    private val dateKeywords = setOf("today", "tomorrow", "yesterday")

    fun extractTasks(memoId: String, content: String): List<Task> {
        var taskOrdinal = 0
        return content.lines().mapIndexedNotNull { index, line ->
            val match = taskLineRegex.find(line) ?: return@mapIndexedNotNull null
            val completed = match.groupValues[1].lowercase() == "x"
            val rawText = match.groupValues[2]
            val cleanText = cleanTaskText(rawText)
            taskOrdinal++

            val stableId = "${memoId}:${hashTaskContent(cleanText, taskOrdinal)}"

            Task(
                id = stableId,
                memoId = memoId,
                lineIndex = index,
                text = cleanText,
                rawText = rawText,
                originalLine = line,
                isCompleted = completed,
                dueDate = parseDueDate(rawText),
                priority = parsePriority(rawText),
                labels = parseLabels(rawText),
                lists = parseLists(rawText),
            )
        }
    }

    fun toggleTaskInContent(content: String, task: Task): String {
        val lines = content.lines().toMutableList()

        val targetIndex = findTaskLine(lines, task)
        if (targetIndex < 0) return content

        val line = lines[targetIndex]
        lines[targetIndex] = if (task.isCompleted) {
            line.replaceFirst("- [x]", "- [ ]").replaceFirst("- [X]", "- [ ]")
        } else {
            line.replaceFirst("- [ ]", "- [x]")
        }
        return lines.joinToString("\n")
    }

    fun replaceTaskLineInContent(content: String, task: Task, newLine: String): String {
        val lines = content.lines().toMutableList()
        val targetIndex = findTaskLine(lines, task)
        if (targetIndex < 0) return content
        lines[targetIndex] = newLine
        return lines.joinToString("\n")
    }

    private fun findTaskLine(lines: List<String>, task: Task): Int {
        if (task.lineIndex in lines.indices && lines[task.lineIndex].trim() == task.originalLine.trim()) {
            return task.lineIndex
        }
        return lines.indexOfFirst { it.trim() == task.originalLine.trim() }
    }

    fun reconstructLine(task: Task): String {
        val checkbox = if (task.isCompleted) "- [x]" else "- [ ]"
        val parts = mutableListOf(task.text)
        task.dueDate?.let { parts.add(it.toString()) }
        task.priority?.let { parts.add("p$it") }
        task.labels.forEach { parts.add("@$it") }
        task.lists.forEach { parts.add("#$it") }
        return "$checkbox ${parts.joinToString(" ")}"
    }

    private fun hashTaskContent(cleanText: String, ordinal: Int): String {
        val input = "$cleanText#$ordinal"
        var hash = 0L
        for (c in input) {
            hash = hash * 31 + c.code
        }
        return hash.toULong().toString(36)
    }

    private fun parseDueDate(text: String): LocalDate? {
        val dateMatch = dateRegex.find(text)
        if (dateMatch != null) {
            return try { LocalDate.parse(dateMatch.value) } catch (_: Exception) { null }
        }

        val labels = labelRegex.findAll(text)
        for (label in labels) {
            val word = label.groupValues[1].lowercase()
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            when (word) {
                "today" -> return today
                "tomorrow" -> return today.plus(1, DateTimeUnit.DAY)
                "yesterday" -> return today.plus(-1, DateTimeUnit.DAY)
            }
        }
        return null
    }

    private fun parsePriority(text: String): Int? {
        val match = priorityRegex.find(text) ?: return null
        return match.groupValues[1].toIntOrNull()
    }

    private fun parseLabels(text: String): List<String> =
        labelRegex.findAll(text)
            .map { it.groupValues[1] }
            .filter { it.lowercase() !in dateKeywords }
            .toList()

    private fun parseLists(text: String): List<String> =
        listRegex.findAll(text)
            .map { it.groupValues[1] }
            .filter { it.first().isLetter() }
            .toList()

    private fun cleanTaskText(text: String): String {
        var clean = text
        clean = priorityRegex.replace(clean, "")
        clean = dateRegex.replace(clean, "")
        clean = labelRegex.replace(clean, "")
        clean = listRegex.replace(clean, "")
        return clean.trim().replace(Regex("""\s{2,}"""), " ")
    }
}
