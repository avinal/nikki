package com.avinal.memos.ui.memos

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.components.AttachmentGrid
import com.avinal.memos.ui.components.MarkdownText
import com.avinal.memos.ui.components.ReactionBar
import com.avinal.memos.ui.theme.LocalAccentColor
import kotlinx.coroutines.flow.first

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding(),
    ) {
        Text(
            "← back",
            fontSize = 14.sp,
            color = accent,
            modifier = Modifier
                .clickable(onClick = onBack)
                .padding(start = 24.dp, top = 12.dp, bottom = 12.dp),
        )

        when {
            isLoading && memo == null -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = accent)
                }
            }
            memo == null -> {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("could not load memo", fontSize = 15.sp)
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
                    Text(
                        memo!!.visibility.name.lowercase(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    Spacer(Modifier.height(8.dp))

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

                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
