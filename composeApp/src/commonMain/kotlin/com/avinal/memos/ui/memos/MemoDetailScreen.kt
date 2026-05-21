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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.api.ApiResult
import com.avinal.memos.api.model.toDomain
import com.avinal.memos.domain.Memo
import com.avinal.memos.ui.components.AttachmentGrid
import com.avinal.memos.ui.components.MarkdownText
import com.avinal.memos.ui.components.ReactionBar
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun MemoDetailScreen(
    memoId: String,
    deps: AppDependencies,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel = viewModel { MemoDetailViewModel(memoId, deps.memoRepository) }
    val memo by viewModel.memo.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val serverUrl by produceState("") { value = deps.tokenStore.serverUrl.first() ?: "" }
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("← back", fontSize = 14.sp, color = accent, modifier = Modifier.clickable(onClick = onBack))
            Text("edit", fontSize = 14.sp, color = accent, modifier = Modifier.clickable(onClick = onEdit))
        }

        when {
            isLoading && memo == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            memo == null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("could not load memo", fontSize = 15.sp, color = textColor)
                        Spacer(Modifier.height(8.dp))
                        Text("retry", fontSize = 14.sp, color = accent, modifier = Modifier.clickable { viewModel.retry() })
                    }
                }
            }
            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(start = 24.dp, end = 12.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(memo!!.visibility.name.lowercase(), fontSize = 12.sp, color = subtleColor)
                        Text(formatDateTime(memo!!.createTime), fontSize = 12.sp, color = subtleColor)
                        if (memo!!.updateTime != memo!!.createTime) {
                            Text("edited ${formatDateTime(memo!!.updateTime)}", fontSize = 12.sp, color = subtleColor)
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    MarkdownText(
                        markdown = memo!!.content,
                        onTaskToggle = { lineIndex, checked -> viewModel.toggleTask(lineIndex, checked) },
                    )

                    if (memo!!.attachments.any { it.isImage } && serverUrl.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        AttachmentGrid(attachments = memo!!.attachments, serverUrl = serverUrl)
                    }

                    if (memo!!.reactions.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        ReactionBar(reactions = memo!!.reactions)
                    }

                    Spacer(Modifier.height(20.dp))
                    CommentsSection(memoId = memoId, deps = deps, accent = accent, textColor = textColor, subtleColor = subtleColor)

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun formatDateTime(instant: kotlin.time.Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${monthNames[local.month.ordinal]} ${local.day}, ${local.year}"
}

@Composable
private fun CommentsSection(
    memoId: String,
    deps: AppDependencies,
    accent: Color,
    textColor: Color,
    subtleColor: Color,
) {
    var comments by remember { mutableStateOf<List<Memo>>(emptyList()) }
    var commentText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(memoId) {
        isLoading = true
        when (val result = deps.apiClient.listComments(memoId)) {
            is ApiResult.Success -> comments = result.data.memos.map { it.toDomain() }
            else -> {}
        }
        isLoading = false
    }

    fun submitComment() {
        if (commentText.isBlank()) return
        val text = commentText
        commentText = ""
        scope.launch {
            when (val result = deps.apiClient.createComment(memoId, text)) {
                is ApiResult.Success -> comments = comments + result.data.toDomain()
                else -> {}
            }
        }
    }

    Column {
        Text("comments", fontSize = 19.sp, fontWeight = FontWeight.Light, color = textColor)

        Spacer(Modifier.height(8.dp))

        if (isLoading) {
            Text("loading...", fontSize = 13.sp, color = subtleColor)
        } else if (comments.isEmpty()) {
            Text("no comments yet", fontSize = 13.sp, color = subtleColor)
        } else {
            comments.forEach { comment ->
                Column(modifier = Modifier.padding(bottom = 12.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(comment.creator, fontSize = 12.sp, color = subtleColor)
                            Text(formatDateTime(comment.createTime), fontSize = 12.sp, color = subtleColor)
                        }
                        Text("delete", fontSize = 12.sp, color = subtleColor.copy(alpha = 0.5f),
                            modifier = Modifier.clickable {
                                scope.launch {
                                    deps.apiClient.deleteMemo(comment.id)
                                    comments = comments.filter { it.id != comment.id }
                                }
                            })
                    }
                    Spacer(Modifier.height(2.dp))
                    MarkdownText(markdown = comment.content)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextField(
                value = commentText,
                onValueChange = { commentText = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("add a comment...", fontSize = 14.sp, color = subtleColor.copy(alpha = 0.4f)) },
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitComment() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = accent,
                    unfocusedIndicatorColor = subtleColor.copy(alpha = 0.2f),
                    cursorColor = accent,
                ),
            )
            Text(
                "send",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (commentText.isNotBlank()) accent else subtleColor.copy(alpha = 0.3f),
                modifier = Modifier
                    .then(if (commentText.isNotBlank()) Modifier.clickable { submitComment() } else Modifier)
                    .padding(start = 8.dp),
            )
        }
    }
}
