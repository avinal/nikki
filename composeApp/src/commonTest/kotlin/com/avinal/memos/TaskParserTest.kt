package com.avinal.memos

import com.avinal.memos.domain.ReminderUnit
import com.avinal.memos.parser.TaskParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskParserTest {

    // --- Basic extraction ---

    @Test fun extractsUncheckedTask() {
        val t = TaskParser.extractTasks("m1", "- [ ] Buy milk")
        assertEquals(1, t.size); assertEquals("Buy milk", t[0].text); assertFalse(t[0].isCompleted)
    }
    @Test fun extractsCheckedTask() { assertTrue(TaskParser.extractTasks("m1", "- [x] Done")[0].isCompleted) }
    @Test fun extractsCheckedUpperX() { assertTrue(TaskParser.extractTasks("m1", "- [X] Done")[0].isCompleted) }
    @Test fun multipleTasks() { assertEquals(3, TaskParser.extractTasks("m1", "t\n- [ ] A\n- [x] B\n- [ ] C\nm").size) }
    @Test fun noTasksInPlainText() { assertTrue(TaskParser.extractTasks("m1", "Just text").isEmpty()) }
    @Test fun bulletNotTask() { assertTrue(TaskParser.extractTasks("m1", "- Regular\n* Another").isEmpty()) }
    @Test fun indentedTask() { assertEquals("Indented", TaskParser.extractTasks("m1", "  - [ ] Indented")[0].text) }

    // --- ISO Date ---

    @Test fun parsesIsoDate() {
        val t = TaskParser.extractTasks("m1", "- [ ] Review 2026-05-25")[0]
        assertEquals(2026, t.dueDate!!.year); assertEquals(25, t.dueDate!!.dayOfMonth)
    }
    @Test fun invalidIsoDate() { assertNull(TaskParser.extractTasks("m1", "- [ ] Review 2026-13-45")[0].dueDate) }
    @Test fun isoDateCleaned() { assertFalse(TaskParser.extractTasks("m1", "- [ ] Fix 2026-05-25")[0].text.contains("2026")) }

    // --- Natural dates (no @ prefix) ---

    @Test fun parsesToday() { assertNotNull(TaskParser.extractTasks("m1", "- [ ] Do today")[0].dueDate) }
    @Test fun parsesTomorrow() { assertNotNull(TaskParser.extractTasks("m1", "- [ ] Do tomorrow")[0].dueDate) }
    @Test fun parsesYesterday() { assertNotNull(TaskParser.extractTasks("m1", "- [ ] Missed yesterday")[0].dueDate) }
    @Test fun todayCleaned() { assertEquals("Buy groceries", TaskParser.extractTasks("m1", "- [ ] Buy groceries today")[0].text) }
    @Test fun naturalDateCaseInsensitive() { assertNotNull(TaskParser.extractTasks("m1", "- [ ] Fix TODAY")[0].dueDate) }
    @Test fun todayInWord() { assertNull(TaskParser.extractTasks("m1", "- [ ] todaying stuff")[0].dueDate) }

    // --- Due time ---

    @Test fun parses12hPm() { assertEquals(17, TaskParser.extractTasks("m1", "- [ ] Meeting 5pm")[0].dueTime!!.hour) }
    @Test fun parses12hAm() { assertEquals(9, TaskParser.extractTasks("m1", "- [ ] Standup 9am")[0].dueTime!!.hour) }
    @Test fun parses12hMinutes() {
        val t = TaskParser.extractTasks("m1", "- [ ] Call 2:30pm")[0]
        assertEquals(14, t.dueTime!!.hour); assertEquals(30, t.dueTime!!.minute)
    }
    @Test fun parses24h() {
        val t = TaskParser.extractTasks("m1", "- [ ] Deploy 14:30")[0]
        assertEquals(14, t.dueTime!!.hour); assertEquals(30, t.dueTime!!.minute)
    }
    @Test fun parses12am() { assertEquals(0, TaskParser.extractTasks("m1", "- [ ] Reset 12am")[0].dueTime!!.hour) }
    @Test fun parses12pm() { assertEquals(12, TaskParser.extractTasks("m1", "- [ ] Lunch 12pm")[0].dueTime!!.hour) }
    @Test fun noTime() { assertNull(TaskParser.extractTasks("m1", "- [ ] Simple")[0].dueTime) }
    @Test fun timeCleaned() { assertFalse(TaskParser.extractTasks("m1", "- [ ] Meeting 5pm today")[0].text.contains("5pm")) }
    @Test fun isoDateColonNotTime() { assertNull(TaskParser.extractTasks("m1", "- [ ] Fix 2026-05-25")[0].dueTime) }

    // --- Reminder duration (!Nunit) ---

    @Test fun parsesReminderMin() {
        val r = TaskParser.extractTasks("m1", "- [ ] Call !30min today")[0].reminder
        assertNotNull(r); assertEquals(30, r.value); assertEquals(ReminderUnit.MIN, r.unit)
    }
    @Test fun parsesReminderHr() {
        val r = TaskParser.extractTasks("m1", "- [ ] Deploy !2hr tomorrow")[0].reminder
        assertEquals(2, r!!.value); assertEquals(ReminderUnit.HR, r.unit)
    }
    @Test fun parsesReminderDay() {
        val r = TaskParser.extractTasks("m1", "- [ ] Review !1day 2026-05-25")[0].reminder
        assertEquals(1, r!!.value); assertEquals(ReminderUnit.DAY, r.unit)
    }
    @Test fun parsesReminderWeek() {
        val r = TaskParser.extractTasks("m1", "- [ ] Plan !1week")[0].reminder
        assertEquals(1, r!!.value); assertEquals(ReminderUnit.WEEK, r.unit)
    }
    @Test fun reminderPlural() {
        val r = TaskParser.extractTasks("m1", "- [ ] Call !2days")[0].reminder
        assertEquals(2, r!!.value); assertEquals(ReminderUnit.DAY, r.unit)
    }
    @Test fun noReminder() { assertNull(TaskParser.extractTasks("m1", "- [ ] Simple")[0].reminder) }
    @Test fun reminderCleaned() { assertFalse(TaskParser.extractTasks("m1", "- [ ] Call !30min")[0].text.contains("!")) }
    @Test fun reminderNotDueTime() {
        val t = TaskParser.extractTasks("m1", "- [ ] Task !30min today")[0]
        assertNotNull(t.reminder); assertNull(t.dueTime)
    }
    @Test fun reminderToString() {
        val r = TaskParser.extractTasks("m1", "- [ ] Task !30min")[0].reminder
        assertEquals("30min", r.toString())
    }
    @Test fun reminderDayToString() {
        assertEquals("2day", TaskParser.extractTasks("m1", "- [ ] Task !2day")[0].reminder.toString())
    }

    // --- Priority ---

    @Test fun extractsP1() { assertEquals(1, TaskParser.extractTasks("m1", "- [ ] Fix p1")[0].priority) }
    @Test fun extractsP2() { assertEquals(2, TaskParser.extractTasks("m1", "- [ ] Fix p2")[0].priority) }
    @Test fun extractsP3() { assertEquals(3, TaskParser.extractTasks("m1", "- [ ] Fix p3")[0].priority) }
    @Test fun noPriority() { assertNull(TaskParser.extractTasks("m1", "- [ ] Fix")[0].priority) }
    @Test fun priorityCleaned() { assertEquals("Fix bug", TaskParser.extractTasks("m1", "- [ ] Fix bug p1")[0].text) }
    @Test fun p4NotParsed() { assertNull(TaskParser.extractTasks("m1", "- [ ] Fix p4")[0].priority) }
    @Test fun priorityInWord() { assertNull(TaskParser.extractTasks("m1", "- [ ] Download mp3")[0].priority) }

    // --- Lists (#tag) ---

    @Test fun extractsList() { assertEquals(listOf("work"), TaskParser.extractTasks("m1", "- [ ] Task #work")[0].lists) }
    @Test fun multipleList() { assertEquals(listOf("work", "devops"), TaskParser.extractTasks("m1", "- [ ] Task #work #devops")[0].lists) }
    @Test fun numericHash() { assertTrue(TaskParser.extractTasks("m1", "- [ ] Issue #123")[0].lists.isEmpty()) }
    @Test fun listCleaned() { assertFalse(TaskParser.extractTasks("m1", "- [ ] Task #work")[0].text.contains("#")) }

    // --- No @labels ---

    @Test fun atNotParsed() { assertTrue(TaskParser.extractTasks("m1", "- [ ] Email user@example.com")[0].text.contains("user@example.com")) }

    // --- Stable IDs ---

    @Test fun stableIds() {
        val a = TaskParser.extractTasks("m1", "- [ ] A\n- [ ] B")
        val b = TaskParser.extractTasks("m1", "- [ ] A\n- [ ] B")
        assertEquals(a[0].id, b[0].id); assertEquals(a[1].id, b[1].id)
    }
    @Test fun differentMemoIds() { assertTrue(TaskParser.extractTasks("m1", "- [ ] T")[0].id != TaskParser.extractTasks("m2", "- [ ] T")[0].id) }

    // --- Toggle / Replace ---

    @Test fun toggleChecks() {
        val c = "t\n- [ ] A\n- [ ] B"
        assertTrue(TaskParser.toggleTaskInContent(c, TaskParser.extractTasks("m1", c)[0]).contains("- [x] A"))
    }
    @Test fun toggleUnchecks() {
        val c = "- [x] Done\n- [ ] Not"
        assertTrue(TaskParser.toggleTaskInContent(c, TaskParser.extractTasks("m1", c)[0]).contains("- [ ] Done"))
    }
    @Test fun replaceLine() {
        val c = "a\n- [ ] Old\nb"
        val r = TaskParser.replaceTaskLineInContent(c, TaskParser.extractTasks("m1", c)[0], "- [ ] New p1")
        assertTrue(r.contains("New p1")); assertFalse(r.contains("Old"))
    }

    // --- Reconstruct ---

    @Test fun reconstructFull() {
        val t = TaskParser.extractTasks("m1", "- [ ] Buy milk 2026-06-01 5pm !30min p2 #personal")[0]
        val line = TaskParser.reconstructLine(t)
        assertTrue(line.startsWith("- [ ]")); assertTrue(line.contains("Buy milk"))
        assertTrue(line.contains("2026-06-01")); assertTrue(line.contains("p2"))
        assertTrue(line.contains("#personal")); assertTrue(line.contains("!30min"))
    }
    @Test fun reconstructCompleted() { assertTrue(TaskParser.reconstructLine(TaskParser.extractTasks("m1", "- [x] Done p1")[0]).startsWith("- [x]")) }

    // --- Line index / metadata ---

    @Test fun lineIndex() {
        val t = TaskParser.extractTasks("m1", "h\n\n- [ ] A\nt\n- [ ] B")
        assertEquals(2, t[0].lineIndex); assertEquals(4, t[1].lineIndex)
    }
    @Test fun originalLine() { assertEquals("- [ ] Buy today p2 #a", TaskParser.extractTasks("m1", "- [ ] Buy today p2 #a")[0].originalLine) }
    @Test fun memoIdStored() { assertEquals("m123", TaskParser.extractTasks("m123", "- [ ] T")[0].memoId) }

    // --- Combined / corner cases ---

    @Test fun fullCombined() {
        val t = TaskParser.extractTasks("m1", "- [ ] Review PR 2026-05-25 3pm !1hr p1 #work #devops")[0]
        assertEquals("Review PR", t.text)
        assertEquals(2026, t.dueDate!!.year); assertEquals(15, t.dueTime!!.hour)
        assertEquals(1, t.reminder!!.value); assertEquals(ReminderUnit.HR, t.reminder!!.unit)
        assertEquals(1, t.priority); assertEquals(listOf("work", "devops"), t.lists)
    }
    @Test fun emptyContent() { assertTrue(TaskParser.extractTasks("m1", "").isEmpty()) }
    @Test fun whitespaceOnly() { assertTrue(TaskParser.extractTasks("m1", "   \n  ").isEmpty()) }
    @Test fun noMetadata() {
        val t = TaskParser.extractTasks("m1", "- [ ] Just a plain task")[0]
        assertNull(t.dueDate); assertNull(t.dueTime); assertNull(t.reminder); assertNull(t.priority); assertTrue(t.lists.isEmpty())
    }
    @Test fun urlHash() { assertTrue(TaskParser.extractTasks("m1", "- [ ] Check https://ex.com/p#sec")[0].text.contains("https://ex.com")) }
    @Test fun multipleTimesFirstWins() { assertEquals(9, TaskParser.extractTasks("m1", "- [ ] Call 9am then 5pm")[0].dueTime!!.hour) }
    @Test fun dateAndTimeAndReminder() {
        val t = TaskParser.extractTasks("m1", "- [ ] Meet tomorrow 3pm !15min #work")[0]
        assertNotNull(t.dueDate); assertEquals(15, t.dueTime!!.hour)
        assertEquals(15, t.reminder!!.value); assertEquals(ReminderUnit.MIN, t.reminder!!.unit)
    }
}
