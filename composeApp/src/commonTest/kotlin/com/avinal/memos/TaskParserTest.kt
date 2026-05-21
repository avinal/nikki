package com.avinal.memos

import com.avinal.memos.parser.TaskParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TaskParserTest {

    @Test
    fun extractsBasicUncheckedTask() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Buy milk")
        assertEquals(1, tasks.size)
        assertEquals("Buy milk", tasks[0].text)
        assertFalse(tasks[0].isCompleted)
    }

    @Test
    fun extractsCheckedTask() {
        val tasks = TaskParser.extractTasks("m1", "- [x] Done task")
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].isCompleted)
    }

    @Test
    fun extractsCheckedTaskUppercaseX() {
        val tasks = TaskParser.extractTasks("m1", "- [X] Done task")
        assertEquals(1, tasks.size)
        assertTrue(tasks[0].isCompleted)
    }

    @Test
    fun extractsPriority() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Fix bug p1")
        assertEquals(1, tasks[0].priority)
        assertEquals("Fix bug", tasks[0].text)
    }

    @Test
    fun extractsAllPriorities() {
        val content = """
            - [ ] Task A p1
            - [ ] Task B p2
            - [ ] Task C p3
        """.trimIndent()
        val tasks = TaskParser.extractTasks("m1", content)
        assertEquals(3, tasks.size)
        assertEquals(1, tasks[0].priority)
        assertEquals(2, tasks[1].priority)
        assertEquals(3, tasks[2].priority)
    }

    @Test
    fun extractsExplicitDate() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Review PR 2026-05-25")
        assertNotNull(tasks[0].dueDate)
        assertEquals(2026, tasks[0].dueDate!!.year)
        assertEquals(25, tasks[0].dueDate!!.dayOfMonth)
    }

    @Test
    fun extractsTodayKeyword() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Do thing @today")
        assertNotNull(tasks[0].dueDate)
    }

    @Test
    fun extractsTomorrowKeyword() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Do thing @tomorrow")
        assertNotNull(tasks[0].dueDate)
    }

    @Test
    fun extractsLabels() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Fix @backend @urgent")
        assertEquals(listOf("backend", "urgent"), tasks[0].labels)
    }

    @Test
    fun labelsExcludeDateKeywords() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Fix @today @backend")
        assertEquals(listOf("backend"), tasks[0].labels)
    }

    @Test
    fun extractsLists() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Review #work #devops")
        assertEquals(listOf("work", "devops"), tasks[0].lists)
    }

    @Test
    fun listsIgnoreNumericStart() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Task #123")
        assertTrue(tasks[0].lists.isEmpty())
    }

    @Test
    fun cleansMetadataFromText() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Buy groceries @today p2 @shopping #personal")
        assertEquals("Buy groceries", tasks[0].text)
    }

    @Test
    fun multipleTasksFromOneMemo() {
        val content = """
            Some text
            - [ ] Task 1
            - [x] Task 2
            - [ ] Task 3
            More text
        """.trimIndent()
        val tasks = TaskParser.extractTasks("m1", content)
        assertEquals(3, tasks.size)
        assertFalse(tasks[0].isCompleted)
        assertTrue(tasks[1].isCompleted)
        assertFalse(tasks[2].isCompleted)
    }

    @Test
    fun noTasksInPlainText() {
        val tasks = TaskParser.extractTasks("m1", "Just some text\nNo tasks here")
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun indentedTasksWork() {
        val tasks = TaskParser.extractTasks("m1", "  - [ ] Indented task")
        assertEquals(1, tasks.size)
        assertEquals("Indented task", tasks[0].text)
    }

    @Test
    fun stableIdsAcrossParses() {
        val content = "- [ ] Task A\n- [ ] Task B"
        val first = TaskParser.extractTasks("m1", content)
        val second = TaskParser.extractTasks("m1", content)
        assertEquals(first[0].id, second[0].id)
        assertEquals(first[1].id, second[1].id)
    }

    @Test
    fun differentMemosProduceDifferentIds() {
        val content = "- [ ] Same task"
        val fromMemo1 = TaskParser.extractTasks("m1", content)
        val fromMemo2 = TaskParser.extractTasks("m2", content)
        assertTrue(fromMemo1[0].id != fromMemo2[0].id)
    }

    @Test
    fun reconstructLinePreservesMetadata() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Buy milk @today p2 #personal")
        val reconstructed = TaskParser.reconstructLine(tasks[0])
        assertTrue(reconstructed.startsWith("- [ ]"))
        assertTrue(reconstructed.contains("Buy milk"))
        assertTrue(reconstructed.contains("p2"))
        assertTrue(reconstructed.contains("#personal"))
    }

    @Test
    fun reconstructLineForCompletedTask() {
        val tasks = TaskParser.extractTasks("m1", "- [x] Done task p1")
        val reconstructed = TaskParser.reconstructLine(tasks[0])
        assertTrue(reconstructed.startsWith("- [x]"))
    }

    @Test
    fun toggleTaskInContentChecks() {
        val content = "some text\n- [ ] Task A\n- [ ] Task B\nmore"
        val tasks = TaskParser.extractTasks("m1", content)
        val toggled = TaskParser.toggleTaskInContent(content, tasks[0])
        assertTrue(toggled.contains("- [x] Task A"))
        assertTrue(toggled.contains("- [ ] Task B"))
    }

    @Test
    fun toggleTaskInContentUnchecks() {
        val content = "- [x] Done\n- [ ] Not done"
        val tasks = TaskParser.extractTasks("m1", content)
        val toggled = TaskParser.toggleTaskInContent(content, tasks[0])
        assertTrue(toggled.contains("- [ ] Done"))
    }

    @Test
    fun replaceTaskLineInContent() {
        val content = "line1\n- [ ] Old task\nline3"
        val tasks = TaskParser.extractTasks("m1", content)
        val replaced = TaskParser.replaceTaskLineInContent(content, tasks[0], "- [ ] New task p1")
        assertTrue(replaced.contains("- [ ] New task p1"))
        assertFalse(replaced.contains("Old task"))
    }

    @Test
    fun noPriorityReturnsNull() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Simple task")
        assertNull(tasks[0].priority)
    }

    @Test
    fun noDateReturnsNull() {
        val tasks = TaskParser.extractTasks("m1", "- [ ] Simple task")
        assertNull(tasks[0].dueDate)
    }

    @Test
    fun originalLinePreserved() {
        val line = "- [ ] Buy groceries @today p2 #personal"
        val tasks = TaskParser.extractTasks("m1", line)
        assertEquals(line, tasks[0].originalLine)
    }

    @Test
    fun bulletListNotParsedAsTask() {
        val tasks = TaskParser.extractTasks("m1", "- Regular list item\n* Another item")
        assertTrue(tasks.isEmpty())
    }

    @Test
    fun memoIdStoredCorrectly() {
        val tasks = TaskParser.extractTasks("memo123", "- [ ] Task")
        assertEquals("memo123", tasks[0].memoId)
    }

    @Test
    fun lineIndexCorrect() {
        val content = "header\n\n- [ ] First\ntext\n- [ ] Second"
        val tasks = TaskParser.extractTasks("m1", content)
        assertEquals(2, tasks[0].lineIndex)
        assertEquals(4, tasks[1].lineIndex)
    }
}
