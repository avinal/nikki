package com.avinal.memos.ui.memos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.api.ApiResult
import com.avinal.memos.api.model.toDomain
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.ui.components.MemoCard
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.util.rememberFilePicker
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn

@OptIn(ExperimentalMaterial3Api::class, ExperimentalEncodingApi::class)
@Composable
fun MemoListScreen(
    deps: AppDependencies,
    onMemoClick: (String) -> Unit,
    onCreateMemo: () -> Unit,
    dateFilter: String? = null,
    tagFilter: String? = null,
    searchFilter: String? = null,
    showArchived: Boolean = false,
    onClearFilter: (() -> Unit)? = null,
) {
    val viewModel = viewModel { MemoListViewModel(deps.memoRepository) }
    val allMemos by viewModel.memos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var archivedMemos by remember { mutableStateOf<List<Memo>>(emptyList()) }
    var isLoadingArchived by remember { mutableStateOf(false) }

    LaunchedEffect(showArchived) {
        if (showArchived) {
            isLoadingArchived = true
            when (val result = deps.apiClient.listArchivedMemos()) {
                is ApiResult.Success -> {
                    archivedMemos = result.data.memos.map { it.toDomain() }
                }
                else -> {}
            }
            isLoadingArchived = false
        }
    }

    val hasFilter = dateFilter != null || tagFilter != null || searchFilter != null || showArchived
    val filterLabel = when {
        showArchived -> "archived memos"
        dateFilter != null -> "date: $dateFilter"
        tagFilter != null -> "tag: #$tagFilter"
        searchFilter != null -> "search: $searchFilter"
        else -> ""
    }

    val memos = remember(allMemos, dateFilter, tagFilter, searchFilter, showArchived, archivedMemos) {
        when {
            showArchived -> archivedMemos
            dateFilter != null -> {
                val parts = dateFilter.split("-")
                if (parts.size == 3) {
                    val (year, month, day) = parts.map { it.toIntOrNull() ?: 0 }
                    allMemos.filter { memo ->
                        val local = memo.displayTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                        local.year == year && (local.month.ordinal + 1) == month && local.day == day
                    }
                } else allMemos
            }
            tagFilter != null -> allMemos.filter { it.tags.contains(tagFilter) }
            searchFilter != null -> {
                val q = searchFilter.lowercase()
                allMemos.filter { it.content.lowercase().contains(q) || it.tags.any { t -> t.lowercase().contains(q) } }
            }
            else -> allMemos
        }
    }
    val listState = rememberLazyListState()
    val serverUrl by produceState("") { value = deps.tokenStore.serverUrl.first() ?: "" }
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    var composeField by remember { mutableStateOf(TextFieldValue("")) }
    val defaultVis by produceState(MemoVisibility.PRIVATE) {
        deps.tokenStore.defaultVisibility.first().let { value = MemoVisibility.fromApiString(it) }
    }
    var composeVisibility by remember(defaultVis) { mutableStateOf(defaultVis) }
    var showVisibilityPicker by remember { mutableStateOf(false) }
    var uploadedAttachmentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    val uploadScope = rememberCoroutineScope()

    val launchFilePicker = rememberFilePicker { pickedFile ->
        isUploading = true
        uploadScope.launch {
            val base64 = Base64.encode(pickedFile.bytes)
            when (val result = deps.memoRepository.uploadAttachment(pickedFile.name, pickedFile.mimeType, base64)) {
                is ApiResult.Success -> {
                    uploadedAttachmentNames = uploadedAttachmentNames + result.data
                }
                else -> {}
            }
            isUploading = false
        }
    }

    val reachedEnd by remember {
        derivedStateOf {
            val lastVisibleItem = listState.layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem != null && lastVisibleItem.index >= listState.layoutInfo.totalItemsCount - 3
        }
    }

    LaunchedEffect(reachedEnd) {
        if (reachedEnd && !uiState.isLoadingMore && uiState.searchQuery.isEmpty()) viewModel.loadMore()
    }

    if (showVisibilityPicker) {
        AlertDialog(
            onDismissRequest = { showVisibilityPicker = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            title = null,
            text = {
                Column {
                    MemoVisibility.entries.forEach { vis ->
                        Text(
                            vis.name.lowercase(),
                            fontSize = 17.sp,
                            color = if (vis == composeVisibility) accent else textColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { composeVisibility = vis; showVisibilityPicker = false }
                                .padding(vertical = 10.dp),
                        )
                    }
                }
            },
            confirmButton = {},
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (hasFilter) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(filterLabel, fontSize = 13.sp, color = accent)
                Text(
                    "clear",
                    fontSize = 13.sp,
                    color = subtleColor,
                    modifier = Modifier.clickable { onClearFilter?.invoke() }.padding(4.dp),
                )
            }
            Spacer(Modifier.fillMaxWidth().height(1.dp).padding(start = 24.dp).background(subtleColor.copy(alpha = 0.15f)))
        }

        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier.weight(1f),
        ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (!showArchived) {
            item(key = "compose") {
                var showInsertMenu by remember { mutableStateOf(false) }

                if (showInsertMenu) {
                    AlertDialog(
                        onDismissRequest = { showInsertMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        title = null,
                        text = {
                            Column {
                                listOf("media", "file", "link memo", "code block").forEach { item ->
                                    Text(
                                        item, fontSize = 17.sp, color = textColor,
                                        modifier = Modifier.fillMaxWidth()
                                            .clickable {
                                                showInsertMenu = false
                                                when (item) {
                                                    "code block" -> { val t = composeField.text + "\n```\n\n```"; composeField = TextFieldValue(t, TextRange(t.length)) }
                                                    "link memo" -> { val t = composeField.text + "\n[memo]()"; composeField = TextFieldValue(t, TextRange(t.length)) }
                                                    "media", "file" -> launchFilePicker()
                                                }
                                            }
                                            .padding(vertical = 10.dp),
                                    )
                                }
                            }
                        },
                        confirmButton = {},
                    )
                }

                Column(modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)) {
                    TextField(
                        value = composeField,
                        onValueChange = { newField ->
                            val newText = newField.text
                            val oldText = composeField.text
                            // Backspace on empty auto-inserted line: remove it
                            if (newText.length < oldText.length && oldText.endsWith("- [ ] ") && newText == oldText.dropLast(6).trimEnd() + "\n") {
                                val cleaned = newText.trimEnd('\n')
                                composeField = TextFieldValue(cleaned, TextRange(cleaned.length))
                                return@TextField
                            }
                            // Enter on empty auto-inserted line: remove it
                            if (newText.length > oldText.length && newText.endsWith("\n") && oldText.endsWith("- [ ] ")) {
                                val lastLine = oldText.lines().last()
                                if (lastLine.trim() == "- [ ]") {
                                    val cleaned = oldText.dropLast(lastLine.length + 1).trimEnd('\n') + "\n"
                                    composeField = TextFieldValue(cleaned, TextRange(cleaned.length))
                                    return@TextField
                                }
                            }
                            // Auto-checklist: if user pressed enter after a task line, auto-insert "- [ ] "
                            if (newText.length > oldText.length && newText.endsWith("\n")) {
                                val beforeNewline = newText.dropLast(1)
                                val lastLine = beforeNewline.lines().lastOrNull() ?: ""
                                if (lastLine.trimStart().startsWith("- [")) {
                                    val result = newText + "- [ ] "
                                    composeField = TextFieldValue(result, TextRange(result.length))
                                    return@TextField
                                }
                            }
                            composeField = newField
                        },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("any thoughts...", fontSize = 15.sp, color = subtleColor.copy(alpha = 0.4f))
                        },
                        singleLine = false,
                        minLines = 1,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = accent,
                            unfocusedIndicatorColor = subtleColor.copy(alpha = 0.2f),
                            cursorColor = accent,
                        ),
                    )

                    // Per-task live preview
                    val previewTasks = remember(composeField.text) {
                        com.avinal.memos.parser.TaskParser.extractTasks("preview", composeField.text)
                    }

                    if (previewTasks.isNotEmpty()) {
                        Column(modifier = Modifier.padding(top = 6.dp)) {
                            previewTasks.forEach { task ->
                                Row(
                                    modifier = Modifier.padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        if (task.isCompleted) "☑" else "☐",
                                        fontSize = 12.sp,
                                        color = if (task.isCompleted) accent else subtleColor,
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        task.text,
                                        fontSize = 12.sp,
                                        color = if (task.isCompleted) subtleColor else textColor,
                                        modifier = Modifier.weight(1f),
                                    )
                                    task.dueDate?.let { MetadataChip("$it", accent) }
                                    task.dueTime?.let { MetadataChip("$it", accent) }
                                    task.reminder?.let { MetadataChip("!$it", subtleColor) }
                                    task.priority?.let { p ->
                                        val c = when (p) { 1 -> com.avinal.memos.ui.theme.PriorityP1; 2 -> com.avinal.memos.ui.theme.PriorityP2; else -> com.avinal.memos.ui.theme.PriorityP3 }
                                        MetadataChip("p$p", c)
                                    }
                                    task.lists.forEach { MetadataChip("#$it", accent) }
                                }
                            }
                        }
                    }

                    val parseWarnings = remember(composeField.text) { com.avinal.memos.parser.TaskParser.validateContent(composeField.text) }
                    if (parseWarnings.isNotEmpty()) {
                        parseWarnings.forEach { warning ->
                            Text(
                                "${warning.taskText}: ${warning.issue}",
                                fontSize = 11.sp, color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }

                    if (uploadedAttachmentNames.isNotEmpty()) {
                        Text(
                            "${uploadedAttachmentNames.size} attachment(s) ready",
                            fontSize = 12.sp, color = accent,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    if (isUploading) {
                        Text("uploading...", fontSize = 12.sp, color = subtleColor, modifier = Modifier.padding(top = 4.dp))
                    }

                    Spacer(Modifier.height(6.dp))

                    // Compose toolbar
                    var showDatePicker by remember { mutableStateOf(false) }
                    var showTimePicker by remember { mutableStateOf(false) }

                    if (showDatePicker) {
                        val today = kotlin.time.Clock.System.todayIn(kotlinx.datetime.TimeZone.currentSystemDefault())
                        val dateState = rememberDatePickerState(
                            initialSelectedDateMillis = today.toEpochDays().toLong() * 86400000L,
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    dateState.selectedDateMillis?.let { ms ->
                                        val d = kotlinx.datetime.Instant.fromEpochMilliseconds(ms)
                                            .toLocalDateTime(kotlinx.datetime.TimeZone.UTC).date
                                        val r = composeField.text.trimEnd() + " $d"; composeField = TextFieldValue(r, TextRange(r.length))
                                    }
                                    showDatePicker = false
                                }) { Text("ok", color = accent) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showDatePicker = false }) { Text("cancel") }
                            },
                        ) { DatePicker(state = dateState) }
                    }

                    if (showTimePicker) {
                        val timeState = rememberTimePickerState()
                        AlertDialog(
                            onDismissRequest = { showTimePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    val h = timeState.hour
                                    val m = timeState.minute
                                    val timeStr = if (m == 0) {
                                        if (h == 0) "12am" else if (h < 12) "${h}am" else if (h == 12) "12pm" else "${h - 12}pm"
                                    } else {
                                        "${h}:${m.toString().padStart(2, '0')}"
                                    }
                                    val r = composeField.text.trimEnd() + " $timeStr"; composeField = TextFieldValue(r, TextRange(r.length))
                                    showTimePicker = false
                                }) { Text("ok", color = accent) }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTimePicker = false }) { Text("cancel") }
                            },
                            text = { TimePicker(state = timeState) },
                        )
                    }

                    val isEditingTask = remember(composeField.text) {
                        val lastLine = composeField.text.lines().lastOrNull { it.isNotBlank() } ?: ""
                        lastLine.trimStart().startsWith("- [")
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            ToolbarButton("add task", subtleColor) { val t = composeField.text.let { if (it.isEmpty() || it.endsWith("\n")) it else "$it\n" } + "- [ ] "; composeField = TextFieldValue(t, TextRange(t.length)) }
                            if (isEditingTask) {
                                ToolbarButton("due", subtleColor) { showDatePicker = true }
                                ToolbarButton("at", subtleColor) { showTimePicker = true }
                            }
                            ToolbarButton("+", subtleColor) { showInsertMenu = true }
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                composeVisibility.name.lowercase(),
                                fontSize = 11.sp,
                                color = subtleColor,
                                modifier = Modifier.clickable { showVisibilityPicker = true },
                            )
                            Text(
                                "post",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (composeField.text.isNotBlank()) accent else subtleColor.copy(alpha = 0.3f),
                                modifier = Modifier
                                    .then(
                                        if (composeField.text.isNotBlank()) Modifier.clickable {
                                            viewModel.createMemo(composeField.text, composeVisibility, uploadedAttachmentNames)
                                            composeField = TextFieldValue("")
                                            uploadedAttachmentNames = emptyList()
                                        } else Modifier
                                    )
                                    .padding(horizontal = 4.dp, vertical = 4.dp),
                            )
                        }
                    }
                }

                Spacer(
                    Modifier.fillMaxWidth().height(1.dp)
                        .padding(start = 24.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }
            }

            if (uiState.statusMessage != null) {
                item {
                    Text(
                        uiState.statusMessage!!,
                        fontSize = 12.sp,
                        color = accent,
                        modifier = Modifier.padding(start = 24.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
                    )
                }
            }

            if (showArchived && isLoadingArchived) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
                    }
                }
            } else if (uiState.isInitialLoading && memos.isEmpty() && !showArchived) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.material3.CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
                    }
                }
            } else if (memos.isEmpty() && !uiState.isRefreshing) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text(if (showArchived) "no archived memos" else "no memos yet", fontSize = 15.sp, color = subtleColor)
                    }
                }
            }

            // Errors shown via bottom banner in MainScreen, not inline

            items(memos, key = { it.id }) { memo ->
                if (showArchived) {
                    MemoCard(
                        memo = memo,
                        onClick = { onMemoClick(memo.id) },
                        serverUrl = serverUrl,
                        onPin = null,
                        onArchive = null,
                        onDelete = { viewModel.deleteMemo(memo.id) },
                        onSave = null,
                        onReact = null,
                        onTaskToggle = null,
                        onRestore = {
                            viewModel.restoreMemo(memo.id)
                            archivedMemos = archivedMemos.filter { it.id != memo.id }
                        },
                    )
                } else {
                    MemoCard(
                        memo = memo,
                        onClick = { onMemoClick(memo.id) },
                        serverUrl = serverUrl,
                        onPin = { viewModel.togglePin(memo) },
                        onArchive = { viewModel.archiveMemo(memo.id) },
                        onDelete = { viewModel.deleteMemo(memo.id) },
                        onSave = { content, visibility ->
                            viewModel.updateMemo(memo.id, content, visibility)
                        },
                        onReact = { emoji -> viewModel.reactToMemo(memo.id, emoji) },
                        onTaskToggle = { lineIndex, checked -> viewModel.toggleTask(memo.id, lineIndex, checked) },
                    )
                }
            }
        }
        }
    }
}

@Composable
private fun ToolbarButton(label: String, color: Color, onClick: () -> Unit) {
    Text(
        label,
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = color,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
    )
}

@Composable
private fun MetadataChip(label: String, color: Color) {
    Text(
        label,
        fontSize = 10.sp,
        color = color,
        modifier = Modifier
            .padding(start = 4.dp)
            .background(color.copy(alpha = 0.1f), androidx.compose.foundation.shape.RoundedCornerShape(3.dp))
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}
