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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.ui.components.MemoCard
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoListScreen(
    deps: AppDependencies,
    onMemoClick: (String) -> Unit,
    onCreateMemo: () -> Unit,
    dateFilter: String? = null,
    onClearDateFilter: (() -> Unit)? = null,
) {
    val viewModel = viewModel { MemoListViewModel(deps.memoRepository) }
    val allMemos by viewModel.memos.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    val memos = remember(allMemos, dateFilter) {
        if (dateFilter == null) allMemos
        else {
            val parts = dateFilter.split("-")
            if (parts.size == 3) {
                val (year, month, day) = parts.map { it.toIntOrNull() ?: 0 }
                allMemos.filter { memo ->
                    val local = memo.displayTime.toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
                    local.year == year && (local.month.ordinal + 1) == month && local.day == day
                }
            } else allMemos
        }
    }
    val listState = rememberLazyListState()
    val serverUrl by produceState("") { value = deps.tokenStore.serverUrl.first() ?: "" }
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    var composeText by remember { mutableStateOf("") }
    var composeVisibility by remember { mutableStateOf(MemoVisibility.PRIVATE) }
    var showVisibilityPicker by remember { mutableStateOf(false) }

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
            containerColor = MaterialTheme.colorScheme.surface,
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
        if (dateFilter != null) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("memos from $dateFilter", fontSize = 13.sp, color = accent)
                Text(
                    "clear",
                    fontSize = 13.sp,
                    color = subtleColor,
                    modifier = Modifier.clickable { onClearDateFilter?.invoke() }.padding(4.dp),
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
            item(key = "compose") {
                var showInsertMenu by remember { mutableStateOf(false) }

                if (showInsertMenu) {
                    AlertDialog(
                        onDismissRequest = { showInsertMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface,
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
                                                    "code block" -> composeText += "\n```\n\n```"
                                                    "link memo" -> composeText += "\n[memo]()"
                                                    else -> { /* TODO: file picker */ }
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
                        value = composeText,
                        onValueChange = { composeText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("any thoughts...", fontSize = 15.sp, color = subtleColor.copy(alpha = 0.4f))
                        },
                        singleLine = false,
                        minLines = 1,
                        maxLines = 10,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = accent,
                            unfocusedIndicatorColor = subtleColor.copy(alpha = 0.2f),
                            cursorColor = accent,
                        ),
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "+",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Light,
                                color = subtleColor,
                                modifier = Modifier.clickable { showInsertMenu = true },
                            )
                            Text(
                                composeVisibility.name.lowercase(),
                                fontSize = 12.sp,
                                color = subtleColor,
                                modifier = Modifier.clickable { showVisibilityPicker = true },
                            )
                        }

                        Text(
                            "post",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (composeText.isNotBlank()) accent else subtleColor.copy(alpha = 0.3f),
                            modifier = Modifier
                                .then(
                                    if (composeText.isNotBlank()) Modifier.clickable {
                                        viewModel.createMemo(composeText, composeVisibility)
                                        composeText = ""
                                    } else Modifier
                                )
                                .padding(horizontal = 4.dp, vertical = 4.dp),
                        )
                    }
                }

                Spacer(
                    Modifier.fillMaxWidth().height(1.dp)
                        .padding(start = 24.dp)
                        .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                )
            }

            if (memos.isEmpty() && !uiState.isRefreshing) {
                item {
                    Box(Modifier.fillMaxWidth().padding(top = 48.dp), contentAlignment = Alignment.Center) {
                        Text("no memos yet", fontSize = 15.sp, color = subtleColor)
                    }
                }
            }

            items(memos, key = { it.id }) { memo ->
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
                )
            }
        }
        }
    }
}
