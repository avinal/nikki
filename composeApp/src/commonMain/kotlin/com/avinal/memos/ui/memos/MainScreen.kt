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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.settings.SettingsScreen
import com.avinal.memos.ui.tasks.TaskListScreen
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val pivotTitles = listOf("explore", "memos", "tasks", "settings")
private const val START_PAGE = 1
private const val PARALLAX_FACTOR = 0.5f

@Composable
fun MainScreen(
    deps: AppDependencies,
    onMemoClick: (String) -> Unit,
    onCreateMemo: () -> Unit,
    onLogout: () -> Unit,
) {
    val pagerState = rememberPagerState(initialPage = START_PAGE, pageCount = { pivotTitles.size })
    val scope = rememberCoroutineScope()
    val accent = LocalAccentColor.current
    val density = LocalDensity.current

    var dateFilter by remember { mutableStateOf<String?>(null) }
    var tagFilter by remember { mutableStateOf<String?>(null) }
    var searchFilter by remember { mutableStateOf<String?>(null) }

    val navigateToMemosWithFilter: () -> Unit = { scope.launch { pagerState.animateScrollToPage(1) } }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 6.dp),
        ) {
            val scrollFraction = pagerState.currentPage + pagerState.currentPageOffsetFraction
            val parallaxOffset = with(density) { (-scrollFraction * PARALLAX_FACTOR * 100.dp.toPx()).toInt() }

            Row(
                modifier = Modifier
                    .offset { IntOffset(parallaxOffset, 0) }
                    .padding(start = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                pivotTitles.forEachIndexed { index, title ->
                    val distance = kotlin.math.abs(scrollFraction - index)
                    val alpha = (1f - distance * 0.5f).coerceIn(0.2f, 1f)
                    val isSelected = pagerState.currentPage == index

                    Text(
                        text = title,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Light,
                        color = if (isSelected) accent.copy(alpha = alpha)
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha * 0.5f),
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        softWrap = false,
                        modifier = Modifier
                            .clickable { scope.launch { pagerState.animateScrollToPage(index) } }
                            .padding(vertical = 4.dp),
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1,
        ) { page ->
            Box(modifier = Modifier.fillMaxSize()) {
                when (page) {
                    0 -> ExplorerPage(
                        deps = deps,
                        onMemoClick = onMemoClick,
                        onDateSelected = { date ->
                            dateFilter = date; tagFilter = null; searchFilter = null
                            navigateToMemosWithFilter()
                        },
                        onTagSelected = { tag ->
                            tagFilter = tag; dateFilter = null; searchFilter = null
                            navigateToMemosWithFilter()
                        },
                        onSearchSubmit = { query ->
                            searchFilter = query; dateFilter = null; tagFilter = null
                            navigateToMemosWithFilter()
                        },
                    )
                    1 -> MemoListScreen(
                        deps = deps,
                        onMemoClick = onMemoClick,
                        onCreateMemo = onCreateMemo,
                        dateFilter = dateFilter,
                        tagFilter = tagFilter,
                        searchFilter = searchFilter,
                        onClearFilter = { dateFilter = null; tagFilter = null; searchFilter = null },
                    )
                    2 -> TaskListScreen(deps = deps, onMemoClick = onMemoClick)
                    3 -> SettingsScreen(deps = deps, onLogout = onLogout)
                }
            }
        }
    }
}

@Composable
private fun ExplorerPage(
    deps: AppDependencies,
    onMemoClick: (String) -> Unit,
    onDateSelected: (String) -> Unit,
    onTagSelected: (String) -> Unit,
    onSearchSubmit: (String) -> Unit,
) {
    val memos by deps.memoRepository.observeMemos().collectAsState(initial = emptyList())
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    var searchQuery by remember { mutableStateOf("") }
    var showSearch by remember { mutableStateOf(false) }

    val now = remember { kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    var calYear by remember { mutableStateOf(now.year) }
    var calMonthIdx by remember { mutableStateOf(now.month.ordinal) }

    val memosByDate = remember(memos) {
        memos.groupBy { memo ->
            val local = memo.displayTime.toLocalDateTime(TimeZone.currentSystemDefault())
            "${local.year}-${local.month.ordinal + 1}-${local.day}"
        }
    }

    val daysInMonth = remember(calYear, calMonthIdx) {
        val lengths = listOf(31, if (calYear % 4 == 0 && (calYear % 100 != 0 || calYear % 400 == 0)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        lengths[calMonthIdx]
    }

    val monthNames = listOf("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec")
    val allTags = remember(memos) { memos.flatMap { it.tags }.distinct().sorted() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 12.dp, top = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("${memos.size} memos", fontSize = 14.sp, color = subtleColor)
            Icon(
                Icons.Default.Search, contentDescription = "Search",
                modifier = Modifier.size(20.dp).clickable { showSearch = !showSearch }, tint = subtleColor,
            )
        }

        AnimatedVisibility(visible = showSearch, enter = expandVertically(), exit = shrinkVertically()) {
            OutlinedTextField(
                value = searchQuery, onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                placeholder = { Text("search memos...", fontSize = 14.sp) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        Icon(Icons.Default.Close, contentDescription = "Clear",
                            modifier = Modifier.size(16.dp).clickable { searchQuery = "" }, tint = subtleColor)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accent, unfocusedBorderColor = subtleColor.copy(alpha = 0.3f), cursorColor = accent,
                ),
            )
        }

        if (showSearch && searchQuery.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "search for \"$searchQuery\"",
                fontSize = 14.sp, color = accent,
                modifier = Modifier.clickable {
                    onSearchSubmit(searchQuery)
                    showSearch = false
                }.padding(vertical = 6.dp),
            )
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month",
                modifier = Modifier.size(22.dp).clickable {
                    if (calMonthIdx == 0) { calMonthIdx = 11; calYear-- } else calMonthIdx--
                },
                tint = subtleColor,
            )
            Text(
                "${monthNames[calMonthIdx]} $calYear",
                fontSize = 19.sp, fontWeight = FontWeight.Light, color = textColor,
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month",
                modifier = Modifier.size(22.dp).clickable {
                    if (calMonthIdx == 11) { calMonthIdx = 0; calYear++ } else calMonthIdx++
                },
                tint = subtleColor,
            )
        }

        Spacer(Modifier.height(8.dp))

        val dayLabels = listOf("m", "t", "w", "t", "f", "s", "s")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            dayLabels.forEach { day ->
                Text(day, fontSize = 11.sp, color = subtleColor.copy(alpha = 0.5f), modifier = Modifier.width(28.dp), textAlign = TextAlign.Center)
            }
        }
        Spacer(Modifier.height(4.dp))

        val firstDayOfWeek = remember(calYear, calMonthIdx) {
            val month = calMonthIdx + 1
            val y = if (month <= 2) calYear - 1 else calYear
            val m = if (month <= 2) month + 12 else month
            val h = (1 + (13 * (m + 1)) / 5 + y + y / 4 - y / 100 + y / 400) % 7
            val mondayBased = ((h + 5) % 7)
            if (mondayBased < 0) mondayBased + 7 else mondayBased
        }

        val cells = buildList {
            repeat(firstDayOfWeek) { add(0) }
            for (d in 1..daysInMonth) add(d)
        }

        val isCurrentMonth = calYear == now.year && calMonthIdx == now.month.ordinal

        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (i in 0 until 7) {
                    val day = week.getOrNull(i) ?: 0
                    if (day == 0) {
                        Box(Modifier.size(28.dp))
                    } else {
                        val dateKey = "$calYear-${calMonthIdx + 1}-$day"
                        val count = memosByDate[dateKey]?.size ?: 0
                        val isToday = isCurrentMonth && day == now.day
                        val intensity = if (count > 0) (count.coerceAtMost(4).toFloat() / 4f) else 0f

                        Box(
                            modifier = Modifier.size(28.dp).clip(RoundedCornerShape(4.dp))
                                .background(
                                    when {
                                        isToday -> accent
                                        count > 0 -> accent.copy(alpha = 0.15f + intensity * 0.45f)
                                        else -> Color.Transparent
                                    }
                                )
                                .clickable(enabled = count > 0) {
                                    onDateSelected(dateKey)
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "$day", fontSize = 11.sp,
                                color = when {
                                    isToday -> Color.White
                                    count > 0 -> textColor
                                    else -> subtleColor.copy(alpha = 0.4f)
                                },
                            )
                        }
                    }
                }
            }
        }

        if (allTags.isNotEmpty()) {
            val tasksByTag = remember(memos) {
                val parser = com.avinal.memos.parser.TaskParser
                val allTasks = memos.flatMap { memo -> parser.extractTasks(memo.id, memo.content) }
                allTasks.filter { !it.isCompleted }.groupBy { it.lists.firstOrNull() ?: "" }
            }

            Spacer(Modifier.height(20.dp))
            Text("tags", fontSize = 19.sp, fontWeight = FontWeight.Light, color = textColor)
            Spacer(Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                Text("tag", fontSize = 11.sp, color = subtleColor.copy(alpha = 0.5f), modifier = Modifier.weight(1f))
                Text("memos", fontSize = 11.sp, color = subtleColor.copy(alpha = 0.5f), modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                Text("tasks", fontSize = 11.sp, color = subtleColor.copy(alpha = 0.5f), modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
            }

            allTags.forEach { tag ->
                val memoCount = memos.count { it.tags.contains(tag) }
                val taskCount = tasksByTag[tag]?.size ?: 0
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { onTagSelected(tag) }.padding(vertical = 5.dp),
                ) {
                    Text("#$tag", fontSize = 14.sp, color = accent, modifier = Modifier.weight(1f))
                    Text("$memoCount", fontSize = 13.sp, color = subtleColor, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                    Text("$taskCount", fontSize = 13.sp, color = if (taskCount > 0) accent else subtleColor, modifier = Modifier.width(50.dp), textAlign = TextAlign.End)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}
