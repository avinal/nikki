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
    private val nowMillis = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds()
    private val defaultTime = LocalTime(20, 0)

    private fun task(
        id: String = "t1", completed: Boolean = false,
        date: LocalDate? = dueDate, time: LocalTime? = null,
        reminder: ReminderDuration? = null, priority: Int? = null,
    ) = Task(id = id, memoId = "m1", lineIndex = 0, text = "Test",
        isCompleted = completed, dueDate = date, dueTime = time,
        reminder = reminder, priority = priority)

    private fun compute(tasks: List<Task>, now: Long = nowMillis) =
        ReminderScheduler.computeAlarms(tasks, now, tz, emptySet(), defaultTime)

    @Test fun completedNoAlarms() { assertTrue(compute(listOf(task(completed = true))).isEmpty()) }
    @Test fun noDateNoTimeNoAlarms() { assertTrue(compute(listOf(task(date = null, time = null))).isEmpty()) }

    @Test fun dateOnlyUsesDefaultTime() {
        val alarms = compute(listOf(task()))
        assertEquals(1, alarms.size)
        val expected = dueDate.atTime(defaultTime).toInstant(tz).toEpochMilliseconds()
        assertEquals(expected, alarms[0].triggerAtMillis)
    }

    @Test fun dateAndTimeUsesExactTime() {
        val alarms = compute(listOf(task(time = LocalTime(15, 0))))
        assertEquals(1, alarms.size)
        assertEquals(dueDate.atTime(LocalTime(15, 0)).toInstant(tz).toEpochMilliseconds(), alarms[0].triggerAtMillis)
    }

    @Test fun reminderOffset30min() {
        val t = task(time = LocalTime(15, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarms = compute(listOf(t))
        assertEquals(2, alarms.size)
        val reminder = alarms.first { it.taskId.endsWith("_remind") }
        val expected = dueDate.atTime(LocalTime(15, 0)).toInstant(tz).toEpochMilliseconds() - 30 * 60_000L
        assertEquals(expected, reminder.triggerAtMillis)
    }

    @Test fun reminderOffset1hr() {
        val t = task(time = LocalTime(10, 0), reminder = ReminderDuration(1, ReminderUnit.HR))
        val reminder = compute(listOf(t)).first { it.taskId.endsWith("_remind") }
        assertEquals(dueDate.atTime(LocalTime(10, 0)).toInstant(tz).toEpochMilliseconds() - 3_600_000L, reminder.triggerAtMillis)
    }

    @Test fun reminderOffset1day() {
        val t = task(reminder = ReminderDuration(1, ReminderUnit.DAY))
        val earlyNow = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds() - 2 * 86_400_000L
        val reminder = compute(listOf(t), earlyNow).first { it.taskId.endsWith("_remind") }
        assertEquals(dueDate.atTime(defaultTime).toInstant(tz).toEpochMilliseconds() - 86_400_000L, reminder.triggerAtMillis)
    }

    @Test fun reminderOffset1week() {
        val t = task(reminder = ReminderDuration(1, ReminderUnit.WEEK))
        val earlyNow = dueDate.atTime(LocalTime(0, 0)).toInstant(tz).toEpochMilliseconds() - 8 * 86_400_000L
        val reminder = compute(listOf(t), earlyNow).first { it.taskId.endsWith("_remind") }
        assertEquals(dueDate.atTime(defaultTime).toInstant(tz).toEpochMilliseconds() - 604_800_000L, reminder.triggerAtMillis)
    }

    @Test fun reminderAndDueTimeBothFire() {
        val t = task(time = LocalTime(14, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarms = compute(listOf(t))
        assertEquals(2, alarms.size)
        assertTrue(alarms.any { it.taskId.endsWith("_remind") })
        assertTrue(alarms.any { it.taskId == "t1" })
    }

    @Test fun pastAlarmsNotScheduled() {
        val lateNow = dueDate.atTime(LocalTime(23, 0)).toInstant(tz).toEpochMilliseconds()
        assertTrue(compute(listOf(task(time = LocalTime(10, 0))), lateNow).isEmpty())
    }

    @Test fun pastReminderButFutureDue() {
        val t = task(time = LocalTime(15, 0), reminder = ReminderDuration(2, ReminderUnit.HR))
        val midNow = dueDate.atTime(LocalTime(14, 0)).toInstant(tz).toEpochMilliseconds()
        val alarms = compute(listOf(t), midNow)
        assertEquals(1, alarms.size)
        assertEquals("t1", alarms[0].taskId)
    }

    @Test fun multipleTasks() {
        val tasks = listOf(
            task(id = "a", time = LocalTime(9, 0)),
            task(id = "b", time = LocalTime(17, 0)),
            task(id = "c", completed = true),
            task(id = "d", date = null),
        )
        assertEquals(2, compute(tasks).size)
    }

    @Test fun dueLabelIncludesPriorityAndTags() {
        val t = task(time = LocalTime(14, 0), priority = 1).copy(lists = listOf("work"))
        val alarm = compute(listOf(t)).first { it.taskId == "t1" }
        assertTrue(alarm.label.contains("p1"))
        assertTrue(alarm.label.contains("#work"))
    }

    @Test fun reminderLabelIncludesDateAndTime() {
        val t = task(time = LocalTime(14, 0), reminder = ReminderDuration(30, ReminderUnit.MIN))
        val alarm = compute(listOf(t)).first { it.taskId.endsWith("_remind") }
        assertTrue(alarm.label.contains("Due"))
        assertTrue(alarm.label.contains("14:00"))
    }

    @Test fun taskTextPreserved() {
        val t = task().copy(text = "Buy groceries")
        assertTrue(compute(listOf(t)).all { it.taskText == "Buy groceries" })
    }

    @Test fun customDefaultTime() {
        val alarms = ReminderScheduler.computeAlarms(
            listOf(task()), nowMillis, tz, emptySet(), LocalTime(9, 0)
        )
        val expected = dueDate.atTime(LocalTime(9, 0)).toInstant(tz).toEpochMilliseconds()
        assertEquals(expected, alarms[0].triggerAtMillis)
    }
}
