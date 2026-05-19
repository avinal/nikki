package com.avinal.memos.ui.tasks

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoRepository
import com.avinal.memos.domain.Task
import com.avinal.memos.parser.TaskParser
import kotlin.time.Clock
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

enum class GroupBy(val label: String) {
    DUE("Due Date"),
    LIST("List / Topic"),
    PRIORITY("Priority"),
    MEMO("Source Memo"),
    STATUS("Status"),
}

enum class SortBy(val label: String) {
    DUE("Due Date"),
    PRIORITY("Priority"),
}

data class TaskFilterState(
    val groupBy: GroupBy = GroupBy.DUE,
    val sortBy: SortBy = SortBy.DUE,
    val quickAddText: String = "",
)

data class TaskGroup(
    val title: String,
    val tasks: List<Task>,
    val collapsed: Boolean = false,
)

data class GroupedTasksResult(
    val groups: List<TaskGroup> = emptyList(),
    val availableLists: List<String> = emptyList(),
)

class TaskListViewModel(private val memoRepository: MemoRepository) : ViewModel() {

    private val _filterState = MutableStateFlow(TaskFilterState())
    val filterState: StateFlow<TaskFilterState> = _filterState.asStateFlow()

    private val _collapsedGroups = MutableStateFlow<Set<String>>(emptySet())

    val groupedTasks: StateFlow<GroupedTasksResult> = combine(
        memoRepository.observeMemos(),
        _filterState,
        _collapsedGroups,
    ) { memos, filters, collapsed ->
        buildGroups(memos, filters, collapsed)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GroupedTasksResult())

    private fun buildGroups(memos: List<Memo>, filters: TaskFilterState, collapsed: Set<String>): GroupedTasksResult {
        val allTasks = memos.flatMap { memo -> TaskParser.extractTasks(memo.id, memo.content) }
        val availableLists = allTasks.flatMap { it.lists }.distinct().sorted()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

        val sorter = when (filters.sortBy) {
            SortBy.DUE -> compareBy<Task> { it.dueDate ?: LocalDate(9999, 12, 31) }.thenBy { it.priority ?: 9 }
            SortBy.PRIORITY -> compareBy<Task> { it.priority ?: 9 }.thenBy { it.dueDate ?: LocalDate(9999, 12, 31) }
        }

        val groups = when (filters.groupBy) {
            GroupBy.DUE -> {
                val overdue = mutableListOf<Task>()
                val todayTasks = mutableListOf<Task>()
                val upcoming = mutableListOf<Task>()
                val noDate = mutableListOf<Task>()
                val completed = mutableListOf<Task>()

                allTasks.forEach { task ->
                    if (task.isCompleted) { completed.add(task); return@forEach }
                    when {
                        task.dueDate == null -> noDate.add(task)
                        task.dueDate < today -> overdue.add(task)
                        task.dueDate == today -> todayTasks.add(task)
                        else -> upcoming.add(task)
                    }
                }

                buildList {
                    if (overdue.isNotEmpty()) add(TaskGroup("Overdue", overdue.sortedWith(sorter)))
                    if (todayTasks.isNotEmpty()) add(TaskGroup("Today", todayTasks.sortedWith(sorter)))
                    if (upcoming.isNotEmpty()) add(TaskGroup("Upcoming", upcoming.sortedWith(sorter)))
                    if (noDate.isNotEmpty()) add(TaskGroup("No Date", noDate.sortedWith(sorter)))
                    if (completed.isNotEmpty()) add(TaskGroup("Completed", completed.sortedWith(sorter), collapsed = "Completed" in collapsed))
                }
            }

            GroupBy.LIST -> {
                val byList = mutableMapOf<String, MutableList<Task>>()
                val completed = mutableListOf<Task>()

                allTasks.forEach { task ->
                    if (task.isCompleted) { completed.add(task); return@forEach }
                    val listName = task.lists.firstOrNull() ?: "Untagged"
                    byList.getOrPut(listName) { mutableListOf() }.add(task)
                }

                buildList {
                    byList.entries.sortedBy { it.key }.forEach { (name, tasks) ->
                        add(TaskGroup("#$name", tasks.sortedWith(sorter)))
                    }
                    if (completed.isNotEmpty()) add(TaskGroup("Completed", completed.sortedWith(sorter), collapsed = "Completed" in collapsed))
                }
            }

            GroupBy.PRIORITY -> {
                val p1 = mutableListOf<Task>()
                val p2 = mutableListOf<Task>()
                val p3 = mutableListOf<Task>()
                val noPriority = mutableListOf<Task>()
                val completed = mutableListOf<Task>()

                allTasks.forEach { task ->
                    if (task.isCompleted) { completed.add(task); return@forEach }
                    when (task.priority) {
                        1 -> p1.add(task)
                        2 -> p2.add(task)
                        3 -> p3.add(task)
                        else -> noPriority.add(task)
                    }
                }

                buildList {
                    if (p1.isNotEmpty()) add(TaskGroup("P1 — High", p1.sortedWith(sorter)))
                    if (p2.isNotEmpty()) add(TaskGroup("P2 — Medium", p2.sortedWith(sorter)))
                    if (p3.isNotEmpty()) add(TaskGroup("P3 — Low", p3.sortedWith(sorter)))
                    if (noPriority.isNotEmpty()) add(TaskGroup("No Priority", noPriority.sortedWith(sorter)))
                    if (completed.isNotEmpty()) add(TaskGroup("Completed", completed.sortedWith(sorter), collapsed = "Completed" in collapsed))
                }
            }

            GroupBy.MEMO -> {
                val byMemo = mutableMapOf<String, MutableList<Task>>()
                val completed = mutableListOf<Task>()

                allTasks.forEach { task ->
                    if (task.isCompleted) { completed.add(task); return@forEach }
                    byMemo.getOrPut(task.memoId) { mutableListOf() }.add(task)
                }

                val memoTitles = memos.associate { it.id to (it.title.ifEmpty { it.content.lines().first().take(40) }) }

                buildList {
                    byMemo.entries.forEach { (memoId, tasks) ->
                        val title = memoTitles[memoId] ?: memoId.take(8)
                        add(TaskGroup(title, tasks.sortedWith(sorter)))
                    }
                    if (completed.isNotEmpty()) add(TaskGroup("Completed", completed.sortedWith(sorter), collapsed = "Completed" in collapsed))
                }
            }

            GroupBy.STATUS -> {
                val incomplete = allTasks.filter { !it.isCompleted }.sortedWith(sorter)
                val completed = allTasks.filter { it.isCompleted }.sortedWith(sorter)

                buildList {
                    if (incomplete.isNotEmpty()) add(TaskGroup("Incomplete", incomplete))
                    if (completed.isNotEmpty()) add(TaskGroup("Completed", completed, collapsed = "Completed" in collapsed))
                }
            }
        }

        return GroupedTasksResult(
            groups = groups.map { it.copy(collapsed = it.title in collapsed) },
            availableLists = availableLists,
        )
    }

    fun setGroupBy(groupBy: GroupBy) {
        _filterState.update { it.copy(groupBy = groupBy) }
    }

    fun setSortBy(sortBy: SortBy) {
        _filterState.update { it.copy(sortBy = sortBy) }
    }

    fun toggleGroupCollapse(title: String) {
        _collapsedGroups.update { current ->
            if (title in current) current - title else current + title
        }
    }

    fun updateQuickAddText(text: String) {
        _filterState.update { it.copy(quickAddText = text) }
    }

    fun quickAddTask() {
        val text = _filterState.value.quickAddText.trim()
        if (text.isEmpty()) return

        viewModelScope.launch {
            memoRepository.createMemo("- [ ] $text")
            _filterState.update { it.copy(quickAddText = "") }
        }
    }

    fun toggleTask(task: Task) {
        viewModelScope.launch {
            val memo = memoRepository.getMemo(task.memoId) ?: return@launch
            val newContent = TaskParser.toggleTaskInContent(memo.content, task)
            if (newContent != memo.content) {
                memoRepository.updateMemo(task.memoId, content = newContent)
            }
        }
    }

    fun updateTaskInMemo(task: Task, newLine: String) {
        viewModelScope.launch {
            val memo = memoRepository.getMemo(task.memoId) ?: return@launch
            val newContent = TaskParser.replaceTaskLineInContent(memo.content, task, newLine)
            if (newContent != memo.content) {
                memoRepository.updateMemo(task.memoId, content = newContent)
            }
        }
    }
}
