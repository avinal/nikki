package com.avinal.memos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.domain.Task
import com.avinal.memos.parser.TaskParser
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlin.time.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TaskDetailSheet(
    task: Task,
    onDismiss: () -> Unit,
    onUpdate: (Task, String) -> Unit,
    onOpenMemo: (String) -> Unit,
) {
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = null,
        text = {
            Column {
                Text(task.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textColor)

                Spacer(Modifier.height(16.dp))

                Text("due date", fontSize = 13.sp, color = subtleColor)
                Spacer(Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
                    listOf(
                        "today" to today,
                        "tomorrow" to today.plus(1, DateTimeUnit.DAY),
                        "next week" to today.plus(7, DateTimeUnit.DAY),
                        "no date" to null,
                    ).forEach { (label, date) ->
                        val isSelected = task.dueDate == date
                        Text(
                            label,
                            fontSize = 14.sp,
                            color = if (isSelected) accent else textColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable { onUpdate(task, TaskParser.reconstructLine(task.copy(dueDate = date))) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text("priority", fontSize = 13.sp, color = subtleColor)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(null to "none", 1 to "p1", 2 to "p2", 3 to "p3").forEach { (p, label) ->
                        val isSelected = task.priority == p
                        Text(
                            label,
                            fontSize = 14.sp,
                            color = if (isSelected) accent else textColor,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            modifier = Modifier
                                .background(
                                    if (isSelected) accent.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                    RoundedCornerShape(4.dp),
                                )
                                .clickable { onUpdate(task, TaskParser.reconstructLine(task.copy(priority = p))) }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        )
                    }
                }

                if (task.labels.isNotEmpty() || task.lists.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        task.labels.forEach { Text("@$it", fontSize = 13.sp, color = subtleColor) }
                        task.lists.forEach { Text("#$it", fontSize = 13.sp, color = accent) }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    "open in memo",
                    fontSize = 15.sp,
                    color = accent,
                    modifier = Modifier.clickable { onOpenMemo(task.memoId) }.padding(vertical = 6.dp),
                )
            }
        },
        confirmButton = {},
    )
}
