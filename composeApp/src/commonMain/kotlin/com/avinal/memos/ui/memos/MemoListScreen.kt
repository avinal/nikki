package com.avinal.memos.ui.memos

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.avinal.memos.ui.components.EmptyArchivedIllustration
import com.avinal.memos.ui.components.EmptyMemoIllustration
import com.avinal.memos.ui.components.MemoCard
import com.avinal.memos.ui.components.MemoCardPlaceholder
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.util.rememberFilePicker
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
    sharedText: String? = null,
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
    val savedIndex = rememberSaveable { mutableStateOf(0) }
    val savedOffset = rememberSaveable { mutableStateOf(0) }
    val listState = rememberLazyListState(savedIndex.value, savedOffset.value)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            savedIndex.value = listState.firstVisibleItemIndex
            savedOffset.value = listState.firstVisibleItemScrollOffset
        }
    }
    val serverUrl by produceState("") { value = deps.tokenStore.serverUrl.first() ?: "" }
    val memoPreviewLines by produceState(8) { deps.tokenStore.memoPreviewLines.collect { value = it } }
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    var showSearch by remember { mutableStateOf(false) }

    var composeField by remember { mutableStateOf(TextFieldValue(sharedText ?: "")) }
    val defaultVis by produceState(MemoVisibility.PRIVATE) {
        deps.tokenStore.defaultVisibility.first().let { value = MemoVisibility.fromApiString(it) }
    }
    var composeVisibility by remember(defaultVis) { mutableStateOf(defaultVis) }
    var showVisibilityPicker by remember { mutableStateOf(false) }
    var uploadedAttachmentNames by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    val uploadScope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    val snackbarHostState = remember { SnackbarHostState() }

    val autoArchive by produceState(false) { deps.tokenStore.autoArchiveCompletedTasks.collect { value = it } }

    LaunchedEffect(Unit) {
        viewModel.allTasksDone.collect { memoId ->
            if (autoArchive) {
                viewModel.archiveMemo(memoId)
                val result = snackbarHostState.showSnackbar("archived — all tasks done", actionLabel = "undo", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.restoreMemo(memoId)
                }
            } else {
                val result = snackbarHostState.showSnackbar("all tasks done", actionLabel = "archive", withDismissAction = true)
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.archiveMemo(memoId)
                }
            }
        }
    }

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

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = textColor,
                    actionColor = accent,
                )
            }
        },
        containerColor = Color.Transparent,
    ) { scaffoldPadding ->
    Column(modifier = Modifier.fillMaxSize().padding(scaffoldPadding)) {
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

        if (!hasFilter && !showArchived) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 6.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                Text(
                    if (showSearch) "close" else "search",
                    fontSize = 13.sp,
                    color = subtleColor,
                    modifier = Modifier.clickable {
                        showSearch = !showSearch
                        if (!showSearch) viewModel.clearSearch()
                    },
                )
            }

            AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, bottom = 6.dp),
                    placeholder = { Text("search memos...", fontSize = 14.sp, color = subtleColor.copy(alpha = 0.4f)) },
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            Icon(
                                Icons.Default.Close, "clear",
                                modifier = Modifier.size(16.dp).clickable { viewModel.clearSearch() },
                                tint = subtleColor,
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accent,
                        unfocusedBorderColor = subtleColor.copy(alpha = 0.3f),
                        cursorColor = accent,
                    ),
                )
            }
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
                            val oldLines = oldText.lines()
                            val lastLine = oldLines.lastOrNull() ?: ""

                            // Backspace on empty auto-inserted task line: remove it
                            if (newText.length < oldText.length && lastLine.trim() == "- [ ]" && oldLines.size > 1) {
                                val withoutLast = oldLines.dropLast(1).joinToString("\n")
                                if (newText.trimEnd() == withoutLast.trimEnd()) {
                                    composeField = TextFieldValue(withoutLast, TextRange(withoutLast.length))
                                    return@TextField
                                }
                            }
                            // Enter on empty auto-inserted task line: remove it and exit task mode
                            if (newText.length > oldText.length && newText.endsWith("\n") && lastLine.trim() == "- [ ]" && oldLines.size > 1) {
                                val withoutLast = oldLines.dropLast(1).joinToString("\n") + "\n"
                                composeField = TextFieldValue(withoutLast, TextRange(withoutLast.length))
                                return@TextField
                            }
                            // Auto-checklist: continue task list on enter
                            if (newText.length > oldText.length && newText.endsWith("\n") && lastLine.trimStart().startsWith("- [") && lastLine.trim() != "- [ ]") {
                                val result = newText + "- [ ] "
                                composeField = TextFieldValue(result, TextRange(result.length))
                                return@TextField
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
                            initialSelectedDateMillis = today.toEpochDays() * 86400000L,
                        )
                        DatePickerDialog(
                            onDismissRequest = { showDatePicker = false },
                            confirmButton = {
                                TextButton(onClick = {
                                    dateState.selectedDateMillis?.let { ms ->
                                        val d = kotlin.time.Instant.fromEpochMilliseconds(ms)
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
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.createMemo(composeField.text, composeVisibility, uploadedAttachmentNames)
                                            composeField = TextFieldValue("")
                                            uploadedAttachmentNames = emptyList()
                                            uploadScope.launch { listState.animateScrollToItem(0) }
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

            if ((showArchived && isLoadingArchived) || (uiState.isInitialLoading && memos.isEmpty() && !showArchived)) {
                items(5) { MemoCardPlaceholder() }
            } else if (memos.isEmpty() && !uiState.isRefreshing) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (showArchived) EmptyArchivedIllustration() else EmptyMemoIllustration()
                            Spacer(Modifier.height(12.dp))
                            Text(
                                if (showArchived) "no archived memos" else "nothing here yet",
                                fontSize = 17.sp, fontWeight = FontWeight.Light, color = MaterialTheme.colorScheme.onBackground,
                            )
                            if (!showArchived) {
                                Spacer(Modifier.height(4.dp))
                                Text("tap above to write your first memo", fontSize = 13.sp, color = subtleColor)
                            }
                        }
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
                        maxPreviewLines = memoPreviewLines,
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
                        maxPreviewLines = memoPreviewLines,
                        onPin = { viewModel.togglePin(memo) },
                        onArchive = { viewModel.archiveMemo(memo.id) },
                        onDelete = { viewModel.deleteMemo(memo.id) },
                        onSave = { content, visibility ->
                            viewModel.updateMemo(memo.id, content, visibility)
                        },
                        onReact = { emoji -> viewModel.reactToMemo(memo.id, emoji) },
                        onTaskToggle = { lineIndex, checked -> viewModel.toggleTask(memo.id, lineIndex, checked) },
                        linkPreviewFetcher = deps.linkPreviewFetcher,
                    )
                }
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
