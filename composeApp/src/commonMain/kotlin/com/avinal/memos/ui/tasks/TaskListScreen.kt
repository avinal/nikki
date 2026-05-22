package com.avinal.memos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.domain.Task
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.ui.theme.OverdueRed
import com.avinal.memos.ui.theme.PriorityP1
import com.avinal.memos.ui.theme.PriorityP2
import com.avinal.memos.ui.theme.PriorityP3
import kotlin.time.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.daysUntil

@Composable
fun TaskListScreen(
    deps: AppDependencies,
    onMemoClick: (String) -> Unit,
) {
    val viewModel = viewModel { TaskListViewModel(deps.memoRepository) }
    val grouped by viewModel.groupedTasks.collectAsState()
    val filters by viewModel.filterState.collectAsState()
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    selectedTask?.let { task ->
        TaskDetailSheet(
            task = task,
            onDismiss = { selectedTask = null },
            onUpdate = { original, newLine -> viewModel.updateTaskInMemo(original, newLine); selectedTask = null },
            onOpenMemo = { memoId -> selectedTask = null; onMemoClick(memoId) },
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MetroDropdown("group: ${filters.groupBy.label}", GroupBy.entries.toList(), filters.groupBy, { it.label }, accent, subtleColor) {
                viewModel.setGroupBy(it)
            }
            MetroDropdown("sort: ${filters.sortBy.label}", SortBy.entries.toList(), filters.sortBy, { it.label }, accent, subtleColor) {
                viewModel.setSortBy(it)
            }
        }

        Spacer(
            Modifier.fillMaxWidth().height(1.dp).padding(start = 24.dp)
                .background(subtleColor.copy(alpha = 0.15f))
        )

        // Parser doctor banner
        val allIssues = grouped.warnings
        val errorColor = Color(0xFFE51400)
        val warnColor = Color(0xFFF0A30A)
        val errors = allIssues.filter { it.severity == com.avinal.memos.parser.IssueSeverity.ERROR }
        val warns = allIssues.filter { it.severity == com.avinal.memos.parser.IssueSeverity.WARNING }
        var showWarningDetails by remember { mutableStateOf(false) }
        // Composite keys (memoId:lineIndex) for dot indicators
        val errorKeys = remember(allIssues) { allIssues.filter { it.severity == com.avinal.memos.parser.IssueSeverity.ERROR && it.taskText.isNotEmpty() }.map { "${it.memoId}:${it.lineIndex}" }.toSet() }
        val warnKeys = remember(allIssues) { allIssues.filter { it.severity == com.avinal.memos.parser.IssueSeverity.WARNING && it.taskText.isNotEmpty() }.map { "${it.memoId}:${it.lineIndex}" }.toSet() }

        if (allIssues.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showWarningDetails = !showWarningDetails },
            ) {
                val bannerBg = if (errors.isNotEmpty()) errorColor.copy(alpha = 0.08f) else warnColor.copy(alpha = 0.08f)
                val summaryText = buildString {
                    append("${allIssues.size} issue${if (allIssues.size > 1) "s" else ""} found")
                    val parts = mutableListOf<String>()
                    if (errors.isNotEmpty()) parts.add("${errors.size} error${if (errors.size > 1) "s" else ""}")
                    if (warns.isNotEmpty()) parts.add("${warns.size} warning${if (warns.size > 1) "s" else ""}")
                    append(": ${parts.joinToString(", ")}")
                }

                Column(modifier = Modifier.fillMaxWidth().background(bannerBg)) {
                    Text(summaryText, fontSize = 12.sp,
                        color = if (errors.isNotEmpty()) errorColor else warnColor,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp))

                    if (showWarningDetails) {
                        Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 6.dp)) {
                            MixedIssuesList(allIssues, errorColor, warnColor, textColor)
                        }
                    }
                }
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            grouped.groups.forEachIndexed { groupIndex, group ->
                item(key = "header_${group.title}") {
                    if (groupIndex > 0) {
                        Spacer(Modifier.height(6.dp))
                        Spacer(
                            Modifier.fillMaxWidth().height(1.dp).padding(start = 24.dp)
                                .background(subtleColor.copy(alpha = 0.15f))
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleGroupCollapse(group.title) }
                            .padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            group.title.lowercase(),
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Light,
                            color = textColor,
                            modifier = Modifier.weight(1f),
                        )
                        Text(
                            "${group.tasks.size}",
                            fontSize = 13.sp,
                            color = subtleColor,
                        )
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            if (group.collapsed) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                            contentDescription = if (group.collapsed) "Expand" else "Collapse",
                            modifier = Modifier.size(18.dp),
                            tint = subtleColor,
                        )
                    }
                }

                if (!group.collapsed) {
                    items(group.tasks, key = { it.id }) { task ->
                        val taskKey = "${task.memoId}:${task.lineIndex}"
                        val dotColor = when {
                            task.isCompleted -> null
                            taskKey in errorKeys -> errorColor
                            taskKey in warnKeys -> warnColor
                            else -> null
                        }
                        MetroTaskRow(
                            task = task,
                            accent = accent,
                            dotColor = dotColor,
                            textColor = textColor,
                            subtleColor = subtleColor,
                            onToggle = { viewModel.toggleTask(task) },
                            onClick = { selectedTask = task },
                        )
                    }
                }
            }

            if (grouped.groups.isEmpty() || grouped.groups.all { it.tasks.isEmpty() }) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("no tasks", fontSize = 15.sp, color = subtleColor)
                    }
                }
            }
        }
    }
}

@Composable
private fun <T> MetroDropdown(
    label: String,
    options: List<T>,
    selected: T,
    display: (T) -> String,
    accent: Color,
    subtleColor: Color,
    onSelect: (T) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Text(
            label.lowercase(),
            fontSize = 12.sp,
            color = subtleColor,
            modifier = Modifier.clickable { expanded = true },
        )
        androidx.compose.material3.DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                Text(
                    display(option).lowercase(),
                    fontSize = 14.sp,
                    color = if (option == selected) accent else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option); expanded = false }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }
        }
    }
}

@Composable
private fun MetroTaskRow(
    task: Task,
    accent: Color,
    textColor: Color,
    subtleColor: Color,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    dotColor: Color? = null,
) {
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(
            checked = task.isCompleted,
            onCheckedChange = { onToggle() },
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = accent,
                uncheckedColor = subtleColor,
                checkmarkColor = Color.White,
            ),
        )

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                task.text,
                fontSize = 15.sp,
                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                color = if (task.isCompleted) subtleColor else textColor,
            )

            if (!task.isCompleted) {
                val metadata = buildList {
                    task.dueDate?.let { date ->
                        val isOverdue = date < today
                        add(formatRelativeDate(date, today) to if (isOverdue) OverdueRed else accent)
                    }
                    task.dueTime?.let { add("${it.hour.toString().padStart(2,'0')}:${it.minute.toString().padStart(2,'0')}" to accent) }
                    task.reminder?.let { add("!$it" to subtleColor) }
                    task.priority?.let { p ->
                        val color = when (p) { 1 -> PriorityP1; 2 -> PriorityP2; else -> PriorityP3 }
                        add("p$p" to color)
                    }
                    task.lists.forEach { add("#$it" to accent) }
                }

                if (metadata.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        metadata.forEach { (text, color) ->
                            Text(text, fontSize = 12.sp, color = color)
                        }
                    }
                }
            }
        }

        if (dotColor != null) {
            Spacer(Modifier.width(6.dp))
            Box(Modifier.size(5.dp).background(dotColor, androidx.compose.foundation.shape.CircleShape))
        }
    }
}

@Composable
private fun MixedIssuesList(
    issues: List<com.avinal.memos.parser.ParseWarning>,
    errorColor: Color, warnColor: Color, textColor: Color,
) {
    val combined = issues.filter { it.taskText.isEmpty() }
    val taskIssues = issues.filter { it.taskText.isNotEmpty() }

    // Group by memo+line preserving document order
    val grouped = mutableListOf<List<com.avinal.memos.parser.ParseWarning>>()
    val seen = mutableSetOf<String>()
    taskIssues.forEach { w ->
        val key = "${w.memoId}:${w.lineIndex}"
        if (key !in seen) {
            seen.add(key)
            grouped.add(taskIssues.filter { it.memoId == w.memoId && it.lineIndex == w.lineIndex })
        }
    }

    // Task issues first, then combined at the end
    var num = 0
    grouped.forEach { group ->
        num++
        val first = group.first()
            val allHighlights = group.map { w ->
                w.highlight to (if (w.severity == com.avinal.memos.parser.IssueSeverity.ERROR) errorColor else warnColor)
            }.filter { it.first.isNotEmpty() }

            Column(modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)) {
                Row {
                    Text("$num. ", fontSize = 11.sp, color = textColor.copy(alpha = 0.5f))
                    HighlightedTextMultiColor(first.taskText, allHighlights, textColor)
                }
                group.forEach { w ->
                    val color = if (w.severity == com.avinal.memos.parser.IssueSeverity.ERROR) errorColor else warnColor
                    Text(w.issue, fontSize = 11.sp, color = color.copy(alpha = 0.8f),
                        modifier = Modifier.padding(start = 16.dp, top = 1.dp))
                }
            }
    }

    combined.forEach { w ->
        num++
        val color = if (w.severity == com.avinal.memos.parser.IssueSeverity.ERROR) errorColor else warnColor
        Text("$num. ${w.issue}", fontSize = 11.sp, color = color.copy(alpha = 0.8f), modifier = Modifier.padding(top = 3.dp))
    }
}

@Composable
private fun HighlightedTextMultiColor(text: String, highlights: List<Pair<String, Color>>, normalColor: Color) {
    if (highlights.isEmpty()) {
        Text(text, fontSize = 11.sp, color = normalColor)
        return
    }

    data class Span(val start: Int, val end: Int, val color: Color)
    val spans = highlights.flatMap { (h, color) ->
        val results = mutableListOf<Span>()
        var searchFrom = 0
        while (true) {
            val idx = text.indexOf(h, searchFrom, ignoreCase = true)
            if (idx < 0) break
            results.add(Span(idx, idx + h.length, color))
            searchFrom = idx + h.length
        }
        results
    }.sortedBy { it.start }

    var pos = 0
    Row {
        spans.forEach { span ->
            if (span.start > pos) {
                Text(text.substring(pos, span.start), fontSize = 11.sp, color = normalColor)
            }
            if (span.start >= pos) {
                Text(text.substring(span.start, span.end), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = span.color,
                    modifier = Modifier.background(span.color.copy(alpha = 0.12f), androidx.compose.foundation.shape.RoundedCornerShape(2.dp)).padding(horizontal = 2.dp))
                pos = span.end
            }
        }
        if (pos < text.length) {
            Text(text.substring(pos), fontSize = 11.sp, color = normalColor)
        }
    }
}

private fun formatRelativeDate(date: kotlinx.datetime.LocalDate, today: kotlinx.datetime.LocalDate): String {
    val days = today.daysUntil(date)
    return when {
        days < -1 -> "${-days}d overdue"
        days == -1 -> "yesterday"
        days == 0 -> "today"
        days == 1 -> "tomorrow"
        days < 7 -> "in ${days}d"
        else -> date.toString()
    }
}
