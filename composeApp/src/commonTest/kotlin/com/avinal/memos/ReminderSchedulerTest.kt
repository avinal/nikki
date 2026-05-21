package com.avinal.memos

import com.avinal.memos.domain.ReminderDuration
import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.domain.Task
import com.avinal.memos.notifications.ReminderScheduler
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ReminderSchedulerTest {

    private val tz = TimeZone.UTC
    private val dueDate = LocalDate(2026, 6, 15)
    // nowMillis = 2026-06-15 00:00 UTC
    private val nowMillis = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds()

    private fun task(
        id: String = "t1",
        completed: Boolean = false,
        date: LocalDate? = dueDate,
        time: LocalTime? = null,
        reminder: ReminderDuration? = null,
        priority: Int? = null,
    ) = Task(
        id = id, memoId = "m1", lineIndex = 0, text = "Test",
        isCompleted = completed, dueDate = date, dueTime = time,
        reminder = reminder, priority = priority,
    )

    @Test
    fun completedTaskProducesNoAlarms() {
        val alarms = ReminderScheduler.computeAlarms(listOf(task(completed = true)), nowMillis, tz, emptySet())
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun taskWithNoDateProducesNoAlarms() {
        val alarms = ReminderScheduler.computeAlarms(listOf(task(date = null)), nowMillis, tz, emptySet())
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun alreadyScheduledTaskStillRecomputed() {
        // Alarms are always recomputed — AlarmManager deduplicates via PendingIntent
        val alarms = ReminderScheduler.computeAlarms(listOf(task()), nowMillis, tz, setOf("t1"))
        assertTrue(alarms.isNotEmpty())
    }

    @Test
    fun taskWithDueTimeProducesOneAlarm() {
        val t = task(time = LocalTime(15, 0))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        assertEquals(1, alarms.size)
        assertEquals("t1", alarms[0].taskId)
        assertTrue(alarms[0].label.contains("due at"))
    }

    @Test
    fun taskWithDueTimeAlarmAtCorrectTime() {
        val t = task(time = LocalTime(15, 0))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        val expected = dueDate.atTime(LocalTime(15, 0)).toInstant(tz).toEpochMilliseconds()
        assertEquals(expected, alarms[0].triggerAtMillis)
    }

    @Test
    fun taskWithoutTimeProducesTwoAlarms() {
        val alarms = ReminderScheduler.computeAlarms(listOf(task()), nowMillis, tz, emptySet())
        assertEquals(2, alarms.size)
        assertTrue(alarms.any { it.taskId == "t1_am" })
        assertTrue(alarms.any { it.taskId == "t1_pm" })
    }

    @Test
    fun defaultAlarmsAt8amAnd8pm() {
        val alarms = ReminderScheduler.computeAlarms(listOf(task()), nowMillis, tz, emptySet())
        val am = alarms.first { it.taskId == "t1_am" }
        val pm = alarms.first { it.taskId == "t1_pm" }
        val expected8am = dueDate.atTime(LocalTime(8, 0)).toInstant(tz).toEpochMilliseconds()
        val expected8pm = dueDate.atTime(LocalTime(20, 0)).toInstant(tz).toEpochMilliseconds()
        assertEquals(expected8am, am.triggerAtMillis)
        assertEquals(expected8pm, pm.triggerAtMillis)
    }

    @Test
    fun reminderDurationOffset30min() {
        val t = task(time = LocalTime(15, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        val expected = dueDate.atTime(LocalTime(15, 0)).toInstant(tz).toEpochMilliseconds() - 30 * 60_000L
        assertEquals(expected, reminder.triggerAtMillis)
    }

    @Test
    fun reminderDurationOffset1hr() {
        val t = task(time = LocalTime(10, 0), reminder = ReminderDuration(1, ReminderUnit.HR))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        val expected = dueDate.atTime(LocalTime(10, 0)).toInstant(tz).toEpochMilliseconds() - 3_600_000L
        assertEquals(expected, reminder.triggerAtMillis)
    }

    @Test
    fun reminderDurationOffset1day() {
        val t = task(reminder = ReminderDuration(1, ReminderUnit.DAY))
        // Set now to 2 days before due date so the 1-day reminder is in the future
        val earlyNow = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds() - 2 * 86_400_000L
        val alarms = ReminderScheduler.computeAlarms(listOf(t), earlyNow, tz, emptySet())
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        val expected = dueDate.atTime(LocalTime(8, 0)).toInstant(tz).toEpochMilliseconds() - 86_400_000L
        assertEquals(expected, reminder.triggerAtMillis)
    }

    @Test
    fun reminderDurationOffset1week() {
        val t = task(reminder = ReminderDuration(1, ReminderUnit.WEEK))
        val earlyNow = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds() - 8 * 86_400_000L
        val alarms = ReminderScheduler.computeAlarms(listOf(t), earlyNow, tz, emptySet())
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        val expected = dueDate.atTime(LocalTime(8, 0)).toInstant(tz).toEpochMilliseconds() - 604_800_000L
        assertEquals(expected, reminder.triggerAtMillis)
    }

    @Test
    fun reminderWithDueTimeProducesBothAlarms() {
        val t = task(time = LocalTime(14, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        // Should have: reminder alarm + due time alarm
        assertEquals(2, alarms.size)
        assertTrue(alarms.any { it.taskId.endsWith("_remind") })
        assertTrue(alarms.any { it.taskId == "t1" })
    }

    @Test
    fun pastAlarmsNotScheduled() {
        // now is after the due time
        val lateNow = dueDate.atTime(LocalTime(23, 0)).toInstant(tz).toEpochMilliseconds()
        val t = task(time = LocalTime(10, 0))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), lateNow, tz, emptySet())
        assertTrue(alarms.isEmpty())
    }

    @Test
    fun pastReminderNotScheduledButDueTimeStillIs() {
        // now is after the reminder time but before due time
        val t = task(time = LocalTime(15, 0), reminder = ReminderDuration(2, ReminderUnit.HR))
        val midNow = dueDate.atTime(LocalTime(14, 0)).toInstant(tz).toEpochMilliseconds()
        val alarms = ReminderScheduler.computeAlarms(listOf(t), midNow, tz, emptySet())
        // Reminder at 13:00 is past, due at 15:00 is future
        assertEquals(1, alarms.size)
        assertEquals("t1", alarms[0].taskId)
    }

    @Test
    fun multipleTasksProduceCorrectAlarms() {
        val tasks = listOf(
            task(id = "a", time = LocalTime(9, 0)),
            task(id = "b", time = LocalTime(17, 0)),
            task(id = "c", completed = true),
            task(id = "d", date = null),
        )
        val alarms = ReminderScheduler.computeAlarms(tasks, nowMillis, tz, emptySet())
        assertEquals(2, alarms.size)
        assertTrue(alarms.any { it.taskId == "a" })
        assertTrue(alarms.any { it.taskId == "b" })
    }

    @Test
    fun alarmLabelsCorrect() {
        val t = task(time = LocalTime(14, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        assertTrue(reminder.label.contains("reminder"))
        val due = alarms.first { it.taskId == "t1" }
        assertTrue(due.label.contains("due at"))
    }

    @Test
    fun defaultAlarmLabelsCorrect() {
        val alarms = ReminderScheduler.computeAlarms(listOf(task()), nowMillis, tz, emptySet())
        val am = alarms.first { it.taskId == "t1_am" }
        val pm = alarms.first { it.taskId == "t1_pm" }
        assertEquals("due today", am.label)
        assertTrue(pm.label.contains("still due"))
    }

    @Test
    fun taskTextPreservedInAlarm() {
        val t = task().copy(text = "Buy groceries")
        val alarms = ReminderScheduler.computeAlarms(listOf(t), nowMillis, tz, emptySet())
        assertTrue(alarms.all { it.taskText == "Buy groceries" })
    }
}
