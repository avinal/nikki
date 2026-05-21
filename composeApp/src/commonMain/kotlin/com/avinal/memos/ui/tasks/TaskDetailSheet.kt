package com.avinal.memos.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.domain.ReminderDuration
import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.domain.Task
import com.avinal.memos.parser.TaskParser
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
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
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showReminderPicker by remember { mutableStateOf(false) }
    var showPriorityPicker by remember { mutableStateOf(false) }
    var showTagEditor by remember { mutableStateOf(false) }

    // Track current task state for live updates without closing
    var currentTask by remember { mutableStateOf(task) }
    val doUpdate = { updated: Task ->
        currentTask = updated
        onUpdate(task, TaskParser.reconstructLine(updated))
    }

    if (showDatePicker) {
        val initial = currentTask.dueDate?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds()
        val state = rememberDatePickerState(initialSelectedDateMillis = initial)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = { TextButton(onClick = { showDatePicker = false; state.selectedDateMillis?.let { ms ->
                doUpdate(currentTask.copy(dueDate = Instant.fromEpochMilliseconds(ms).toLocalDateTime(TimeZone.currentSystemDefault()).date))
            } }) { Text("ok", color = accent) } },
            dismissButton = { Row { if (currentTask.dueDate != null) { TextButton(onClick = { showDatePicker = false; doUpdate(currentTask.copy(dueDate = null, dueTime = null)) }) { Text("clear", color = subtleColor) } }; TextButton(onClick = { showDatePicker = false }) { Text("cancel", color = subtleColor) } } },
        ) { DatePicker(state = state) }
    }

    if (showTimePicker) {
        val state = rememberTimePickerState(initialHour = currentTask.dueTime?.hour ?: 12, initialMinute = currentTask.dueTime?.minute ?: 0, is24Hour = true)
        AlertDialog(onDismissRequest = { showTimePicker = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer, title = null,
            text = { TimePicker(state = state) },
            confirmButton = { TextButton(onClick = { showTimePicker = false; doUpdate(currentTask.copy(dueTime = LocalTime(state.hour, state.minute))) }) { Text("ok", color = accent) } },
            dismissButton = { Row { if (currentTask.dueTime != null) { TextButton(onClick = { showTimePicker = false; doUpdate(currentTask.copy(dueTime = null)) }) { Text("clear", color = subtleColor) } }; TextButton(onClick = { showTimePicker = false }) { Text("cancel", color = subtleColor) } } })
    }

    if (showReminderPicker) {
        val options = listOf(ReminderDuration(15, ReminderUnit.MIN), ReminderDuration(30, ReminderUnit.MIN), ReminderDuration(1, ReminderUnit.HR), ReminderDuration(2, ReminderUnit.HR), ReminderDuration(1, ReminderUnit.DAY), ReminderDuration(2, ReminderUnit.DAY), ReminderDuration(1, ReminderUnit.WEEK))
        AlertDialog(onDismissRequest = { showReminderPicker = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer, title = null,
            text = { Column { options.forEach { d -> Text(d.toString(), fontSize = 15.sp, color = if (currentTask.reminder == d) accent else textColor, fontWeight = if (currentTask.reminder == d) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth().clickable { doUpdate(currentTask.copy(reminder = d)); showReminderPicker = false }.padding(vertical = 8.dp))
            }; if (currentTask.reminder != null) { Text("no reminder", fontSize = 15.sp, color = subtleColor, modifier = Modifier.fillMaxWidth().clickable { doUpdate(currentTask.copy(reminder = null)); showReminderPicker = false }.padding(vertical = 8.dp)) } } }, confirmButton = {})
    }

    if (showPriorityPicker) {
        AlertDialog(onDismissRequest = { showPriorityPicker = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer, title = null,
            text = { Column { listOf(null to "none", 1 to "p1", 2 to "p2", 3 to "p3").forEach { (p, label) -> Text(label, fontSize = 15.sp, color = if (currentTask.priority == p) accent else textColor, fontWeight = if (currentTask.priority == p) FontWeight.SemiBold else FontWeight.Normal,
                modifier = Modifier.fillMaxWidth().clickable { doUpdate(currentTask.copy(priority = p)); showPriorityPicker = false }.padding(vertical = 8.dp)) } } }, confirmButton = {})
    }

    if (showTagEditor) {
        var tagInput by remember { mutableStateOf(currentTask.lists.joinToString(" ") { "#$it" }) }
        AlertDialog(onDismissRequest = { showTagEditor = false }, containerColor = MaterialTheme.colorScheme.surfaceContainer, title = null,
            text = { Column { Text("space-separated #tags", fontSize = 12.sp, color = subtleColor); Spacer(Modifier.height(8.dp))
                TextField(value = tagInput, onValueChange = { tagInput = it }, modifier = Modifier.fillMaxWidth(), placeholder = { Text("#work #personal") }, singleLine = true, textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    colors = TextFieldDefaults.colors(focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent, focusedIndicatorColor = accent, unfocusedIndicatorColor = subtleColor.copy(alpha = 0.3f), cursorColor = accent)) } },
            confirmButton = { TextButton(onClick = { showTagEditor = false; doUpdate(currentTask.copy(lists = Regex("""#?([a-zA-Z]\w*)""").findAll(tagInput).map { it.groupValues[1] }.toList())) }) { Text("save", color = accent) } },
            dismissButton = { TextButton(onClick = { showTagEditor = false }) { Text("cancel", color = subtleColor) } })
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        title = null,
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(currentTask.text, fontSize = 17.sp, fontWeight = FontWeight.Medium, color = textColor)
                Spacer(Modifier.height(16.dp))

                val labelWidth = 72.dp
                // due: date + time as tappable boxes
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("due", fontSize = 13.sp, color = subtleColor, modifier = Modifier.width(labelWidth))
                    ValueBox(currentTask.dueDate?.toString() ?: "no date", currentTask.dueDate != null, accent, subtleColor) { showDatePicker = true }
                    Spacer(Modifier.width(6.dp))
                    ValueBox(currentTask.dueTime?.let { fmt(it) } ?: "no time", currentTask.dueTime != null, accent, subtleColor) { showTimePicker = true }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("reminder", fontSize = 13.sp, color = subtleColor, modifier = Modifier.width(labelWidth))
                    ValueBox(currentTask.reminder?.toString() ?: "no reminder", currentTask.reminder != null, accent, subtleColor) { showReminderPicker = true }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("priority", fontSize = 13.sp, color = subtleColor, modifier = Modifier.width(labelWidth))
                    ValueBox(currentTask.priority?.let { "p$it" } ?: "none", currentTask.priority != null, accent, subtleColor) { showPriorityPicker = true }
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("tags", fontSize = 13.sp, color = subtleColor, modifier = Modifier.width(labelWidth))
                    ValueBox(currentTask.lists.joinToString(" ") { "#$it" }.ifEmpty { "no tags" }, currentTask.lists.isNotEmpty(), accent, subtleColor) { showTagEditor = true }
                }

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("open in memo", fontSize = 13.sp, color = accent, modifier = Modifier.clickable { onOpenMemo(currentTask.memoId) }.padding(vertical = 4.dp))
                    Text("close", fontSize = 13.sp, color = subtleColor, modifier = Modifier.clickable(onClick = onDismiss).padding(vertical = 4.dp))
                }
            }
        },
        confirmButton = {},
    )
}

@Composable
private fun ValueBox(text: String, isSet: Boolean, accent: Color, subtleColor: Color, onClick: () -> Unit) {
    Text(
        text, fontSize = 13.sp,
        color = if (isSet) accent else subtleColor.copy(alpha = 0.5f),
        fontWeight = if (isSet) FontWeight.SemiBold else FontWeight.Normal,
        modifier = Modifier
            .border(1.dp, if (isSet) accent.copy(alpha = 0.3f) else subtleColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

private fun fmt(time: LocalTime): String = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
