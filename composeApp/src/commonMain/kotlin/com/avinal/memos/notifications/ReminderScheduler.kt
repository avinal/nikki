package com.avinal.memos.notifications

import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.domain.Task
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

data class ScheduledAlarm(
    val taskId: String,
    val taskText: String,
    val label: String,
    val triggerAtMillis: Long,
)

object ReminderScheduler {

    fun computeAlarms(
        tasks: List<Task>,
        nowMillis: Long,
        timeZone: TimeZone,
        alreadyScheduledIds: Set<String>,
    ): List<ScheduledAlarm> {
        val alarms = mutableListOf<ScheduledAlarm>()

        val today = kotlin.time.Clock.System.todayIn(timeZone)

        tasks.forEach { task ->
            if (task.isCompleted) return@forEach
            val effectiveDate = task.dueDate ?: if (task.dueTime != null) today else return@forEach

            // Explicit reminder: fire at dueDateTime - duration
            if (task.reminder != null) {
                val dueInstant = effectiveDate.atTime(task.dueTime ?: LocalTime(8, 0)).toInstant(timeZone)
                val offsetMs = when (task.reminder.unit) {
                    ReminderUnit.MIN -> task.reminder.value * 60_000L
                    ReminderUnit.HR -> task.reminder.value * 3_600_000L
                    ReminderUnit.DAY -> task.reminder.value * 86_400_000L
                    ReminderUnit.WEEK -> task.reminder.value * 604_800_000L
                }
                val reminderMs = dueInstant.toEpochMilliseconds() - offsetMs
                if (reminderMs > nowMillis) {
                    alarms.add(ScheduledAlarm("${task.id}_remind", task.text, "reminder: ${task.reminder}", reminderMs))
                }
            }

            // Due time alarm
            if (task.dueTime != null) {
                val alarmMs = effectiveDate.atTime(task.dueTime).toInstant(timeZone).toEpochMilliseconds()
                if (alarmMs > nowMillis) {
                    alarms.add(ScheduledAlarm(task.id, task.text, "due at ${task.dueTime}", alarmMs))
                }
            } else {
                // Default: 8am and 8pm on due date
                val morning = effectiveDate.atTime(LocalTime(8, 0)).toInstant(timeZone).toEpochMilliseconds()
                val evening = effectiveDate.atTime(LocalTime(20, 0)).toInstant(timeZone).toEpochMilliseconds()
                if (morning > nowMillis) {
                    alarms.add(ScheduledAlarm("${task.id}_am", task.text, "due today", morning))
                }
                if (evening > nowMillis) {
                    alarms.add(ScheduledAlarm("${task.id}_pm", task.text, "reminder: still due today", evening))
                }
            }
        }

        return alarms
    }
}
