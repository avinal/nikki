package com.avinal.memos

import com.avinal.memos.parser.TaskParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ParserDoctorTest {

    // --- Errors ---

    @Test fun errorInvalidDate() {
        val w = TaskParser.validateContent("- [ ] Fix 2026-13-45")
        assertTrue(w.any { it.issue.contains("invalid date") })
        assertEquals("2026-13-45", w.first { it.issue.contains("invalid date") }.highlight)
    }

    @Test fun errorInvalidPriority() {
        val w = TaskParser.validateContent("- [ ] Fix p5")
        assertTrue(w.any { it.issue.contains("invalid priority") })
        assertEquals("p5", w.first { it.issue.contains("invalid priority") }.highlight)
    }

    @Test fun errorReminderWithoutDue() {
        val w = TaskParser.validateContent("- [ ] Task !30min")
        assertTrue(w.any { it.issue.contains("no due date") })
    }

    @Test fun errorInvalidReminderUnit() {
        val w = TaskParser.validateContent("- [ ] Task !5blah today")
        assertTrue(w.any { it.issue.contains("invalid reminder unit") })
    }

    // --- Warnings ---

    @Test fun warnTimeWithoutDate() {
        val w = TaskParser.validateContent("- [ ] Call 5pm")
        assertTrue(w.any { it.issue.contains("time without date") })
        assertEquals("5pm", w.first { it.issue.contains("time without date") }.highlight)
    }

    @Test fun warnDateInPast() {
        val w = TaskParser.validateContent("- [ ] Old 2020-01-01")
        assertTrue(w.any { it.issue.contains("in the past") })
    }

    @Test fun warnMultiplePriorities() {
        val w = TaskParser.validateContent("- [ ] Fix p1 p2")
        assertTrue(w.any { it.issue.contains("multiple priorities") })
    }

    @Test fun warnMultipleDates() {
        val w = TaskParser.validateContent("- [ ] Fix 2026-06-01 2026-07-01")
        assertTrue(w.any { it.issue.contains("multiple dates") })
    }

    @Test fun warnMultipleReminders() {
        val w = TaskParser.validateContent("- [ ] Fix !30min !1hr today")
        assertTrue(w.any { it.issue.contains("multiple reminders") })
    }

    @Test fun warnTypoToday() {
        val w = TaskParser.validateContent("- [ ] Call tday")
        assertTrue(w.any { it.issue.contains("today") })
        assertEquals("tday", w.first { it.issue.contains("today") }.highlight)
    }

    @Test fun warnTypoTomorrow() {
        val w = TaskParser.validateContent("- [ ] Fix tomorow")
        assertTrue(w.any { it.issue.contains("tomorrow") })
    }

    @Test fun warnTypoSunday() {
        val w = TaskParser.validateContent("- [ ] Meet sundie")
        assertTrue(w.any { it.issue.contains("sunday") })
    }

    @Test fun warnCombinedNoDates() {
        val content = "- [ ] Task A\n- [ ] Task B\n- [ ] Task C"
        val w = TaskParser.validateContent(content)
        assertTrue(w.any { it.issue.contains("tasks have no due date") })
    }

    // --- No false positives ---

    @Test fun noWarningsForValid() {
        assertTrue(TaskParser.validateContent("- [ ] Buy milk today 5pm !30min p1 #work").isEmpty())
    }

    @Test fun skipsCompletedTasks() {
        assertTrue(TaskParser.validateContent("- [x] Done p5 2099-99-99").isEmpty())
    }

    @Test fun noWarningPlainText() {
        assertTrue(TaskParser.validateContent("Just text").isEmpty())
    }

    @Test fun noWarningTodayTime() {
        assertTrue(TaskParser.validateContent("- [ ] Call today 5pm").isEmpty())
    }

    @Test fun noWarningTomorrowReminder() {
        assertTrue(TaskParser.validateContent("- [ ] Call tomorrow !1hr").isEmpty())
    }

    // --- Metadata ---

    @Test fun taskTextIncluded() {
        val w = TaskParser.validateContent("- [ ] Buy groceries 2020-01-01")
        assertTrue(w.first().taskText.contains("Buy groceries"))
    }

    @Test fun correctLineIndex() {
        val w = TaskParser.validateContent("header\n\n- [ ] Task 2026-99-99")
        assertEquals(2, w[0].lineIndex)
    }

    @Test fun singleTaskNoDateNotWarned() {
        // Only 1 task without date should not trigger the combined warning
        val w = TaskParser.validateContent("- [ ] Single task")
        assertTrue(w.none { it.issue.contains("tasks have no due date") })
    }
}
