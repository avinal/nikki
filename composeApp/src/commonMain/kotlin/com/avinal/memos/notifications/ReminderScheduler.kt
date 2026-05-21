package com.avinal.memos.notifications

import com.avinal.memos.domain.ReminderDuration
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
    val priority: Int = 0,
)

object ReminderScheduler {

    /**
     * Compute alarms for tasks. Logic:
     * - time only, no date → date = today
     * - date only, no time → time = [defaultTime] (default 20:00)
     * - both set → use as-is
     * - neither → skip (no alarm possible)
     *
     * Reminder:
     * - explicit !duration → alarm at dueDateTime - duration
     * - no explicit reminder → alarm at dueDateTime itself
     */
    fun computeAlarms(
        tasks: List<Task>,
        nowMillis: Long,
        timeZone: TimeZone,
        alreadyScheduledIds: Set<String> = emptySet(),
        defaultTime: LocalTime = LocalTime(20, 0),
    ): List<ScheduledAlarm> {
        val alarms = mutableListOf<ScheduledAlarm>()
        val today = kotlin.time.Clock.System.todayIn(timeZone)

        tasks.forEach { task ->
            if (task.isCompleted) return@forEach

            val effectiveDate = task.dueDate ?: if (task.dueTime != null) today else return@forEach
            val effectiveTime = task.dueTime ?: defaultTime

            val dueMs = effectiveDate.atTime(effectiveTime).toInstant(timeZone).toEpochMilliseconds()

            println("ReminderScheduler: task=${task.text} effectiveDate=$effectiveDate effectiveTime=$effectiveTime dueMs=$dueMs nowMillis=$nowMillis future=${dueMs > nowMillis}")

            // Explicit reminder: fire at dueDateTime - duration
            if (task.reminder != null) {
                val offsetMs = durationToMillis(task.reminder)
                val reminderMs = dueMs - offsetMs
                if (reminderMs > nowMillis) {
                    val label = buildReminderLabel(task, effectiveDate, effectiveTime)
                    alarms.add(ScheduledAlarm("${task.id}_remind", task.text, label, reminderMs, task.priority ?: 0))
                }
            }

            // Due time alarm
            if (dueMs > nowMillis) {
                val label = buildDueLabel(task, effectiveDate, effectiveTime)
                alarms.add(ScheduledAlarm(task.id, task.text, label, dueMs, task.priority ?: 0))
            }
        }

        return alarms
    }

    private fun durationToMillis(d: ReminderDuration): Long = when (d.unit) {
        ReminderUnit.MIN -> d.value * 60_000L
        ReminderUnit.HR -> d.value * 3_600_000L
        ReminderUnit.DAY -> d.value * 86_400_000L
        ReminderUnit.WEEK -> d.value * 604_800_000L
    }

    private fun buildReminderLabel(task: Task, date: kotlinx.datetime.LocalDate, time: LocalTime): String {
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        val priority = task.priority?.let { " · p$it" } ?: ""
        val tags = task.lists.joinToString("") { " #$it" }
        return "Due $date $timeStr$priority$tags"
    }

    private fun buildDueLabel(task: Task, date: kotlinx.datetime.LocalDate, time: LocalTime): String {
        val timeStr = "${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
        val priority = task.priority?.let { " · p$it" } ?: ""
        val tags = task.lists.joinToString("") { " #$it" }
        return "Now · $date $timeStr$priority$tags"
    }
}
