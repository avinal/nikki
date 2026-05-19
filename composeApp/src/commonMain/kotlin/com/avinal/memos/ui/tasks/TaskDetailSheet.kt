package com.avinal.memos.ui.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.avinal.memos.domain.Task
import com.avinal.memos.parser.TaskParser
import com.avinal.memos.ui.theme.DuePurple
import com.avinal.memos.ui.theme.PriorityP1
import com.avinal.memos.ui.theme.PriorityP2
import com.avinal.memos.ui.theme.PriorityP3
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    onDismiss: () -> Unit,
    onUpdate: (Task, String) -> Unit,
    onOpenMemo: (String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.titleMedium,
            )

            Spacer(Modifier.height(16.dp))

            Text("Due Date", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                val dateOptions = listOf(
                    "Today" to today,
                    "Tomorrow" to today.plus(1, DateTimeUnit.DAY),
                    "Next week" to today.plus(7, DateTimeUnit.DAY),
                    "No date" to null,
                )
                dateOptions.forEach { (label, date) ->
                    FilterChip(
                        selected = task.dueDate == date,
                        onClick = {
                            val updated = task.copy(dueDate = date)
                            onUpdate(task, TaskParser.reconstructLine(updated))
                        },
                        label = { Text(label) },
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Text("Priority", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val priorityOptions = listOf(null to "None", 1 to "P1", 2 to "P2", 3 to "P3")
                priorityOptions.forEach { (p, label) ->
                    FilterChip(
                        selected = task.priority == p,
                        onClick = {
                            val updated = task.copy(priority = p)
                            onUpdate(task, TaskParser.reconstructLine(updated))
                        },
                        label = { Text(label) },
                    )
                }
            }

            if (task.labels.isNotEmpty() || task.lists.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    task.labels.forEach { label ->
                        Text("@$label", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    task.lists.forEach { list ->
                        Text("#$list", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            TextButton(onClick = { onOpenMemo(task.memoId) }) {
                Text("Open in memo")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
