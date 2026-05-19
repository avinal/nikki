package com.avinal.memos.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.util.sharePlainText
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private const val COMPACT_MAX_LINES = 12

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MemoCard(
    memo: Memo,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    serverUrl: String = "",
    onPin: (() -> Unit)? = null,
    onArchive: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onSave: ((String, MemoVisibility) -> Unit)? = null,
    onReact: ((String) -> Unit)? = null,
    onTaskToggle: ((Int, Boolean) -> Unit)? = null,
) {
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    val contentLines = remember(memo.content) { memo.content.lines() }
    val isLong = contentLines.size > COMPACT_MAX_LINES
    var expanded by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var editContent by remember { mutableStateOf("") }
    var editVisibility by remember { mutableStateOf(memo.visibility) }
    var showMenu by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("delete memo?", color = textColor) },
            text = { Text("this cannot be undone.", color = subtleColor) },
            containerColor = MaterialTheme.colorScheme.surface,
            confirmButton = {
                TextButton(onClick = { showDeleteDialog = false; onDelete?.invoke() }) {
                    Text("delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("cancel", color = subtleColor) }
            },
        )
    }

    if (showMenu) {
        AlertDialog(
            onDismissRequest = { showMenu = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = null,
            text = {
                Column {
                    MetroMenuItem(if (memo.pinned) "unpin" else "pin", textColor) { showMenu = false; onPin?.invoke() }
                    MetroMenuItem("edit", textColor) {
                        showMenu = false; editContent = memo.content; editVisibility = memo.visibility; isEditing = true
                    }
                    MetroMenuItem("copy content", textColor) { showMenu = false; clipboardManager.setText(AnnotatedString(memo.content)) }
                    MetroMenuItem("share", textColor) { showMenu = false; sharePlainText(memo.content) }
                    MetroMenuItem("archive", textColor) { showMenu = false; onArchive?.invoke() }
                    Spacer(Modifier.height(8.dp))
                    MetroMenuItem("delete", MaterialTheme.colorScheme.error) { showMenu = false; showDeleteDialog = true }
                }
            },
            confirmButton = {},
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = { if (!isEditing) onClick() },
                    onLongClick = { if (!isEditing) showMenu = true },
                )
                .padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                if (memo.pinned) {
                    Text("pinned", fontSize = 12.sp, color = accent, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.width(8.dp))
                }
                Text(formatAbsoluteDate(memo.displayTime), fontSize = 12.sp, color = subtleColor)
                if (memo.commentCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Text("${memo.commentCount} comment${if (memo.commentCount > 1) "s" else ""}", fontSize = 12.sp, color = subtleColor)
                }
            }

            Spacer(Modifier.height(8.dp))

            if (isEditing) {
                InlineEditor(
                    content = editContent, visibility = editVisibility, accent = accent,
                    textColor = textColor, subtleColor = subtleColor,
                    onContentChange = { editContent = it }, onVisibilityChange = { editVisibility = it },
                    onSave = { onSave?.invoke(editContent, editVisibility); isEditing = false },
                    onCancel = { isEditing = false },
                )
            } else {
                val displayContent = if (!expanded && isLong) {
                    contentLines.take(COMPACT_MAX_LINES).joinToString("\n")
                } else {
                    memo.content
                }

                Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                    MarkdownText(
                        markdown = displayContent,
                        modifier = Modifier.fillMaxWidth(),
                        onTaskToggle = onTaskToggle,
                    )
                }

                if (isLong) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        if (expanded) "show less" else "show more",
                        fontSize = 12.sp, color = accent,
                        modifier = Modifier.clickable { expanded = !expanded }.padding(vertical = 2.dp),
                    )
                }

                if (memo.attachments.any { it.isImage } && serverUrl.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    AttachmentGrid(attachments = memo.attachments, serverUrl = serverUrl)
                }

                if (memo.reactions.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    ReactionBar(reactions = memo.reactions)
                }
            }
        }

        Spacer(
            Modifier.fillMaxWidth().height(1.dp).padding(start = 24.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
        )
    }
}

@Composable
private fun MetroMenuItem(text: String, color: Color, onClick: () -> Unit) {
    Text(
        text = text, fontSize = 17.sp, color = color,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 10.dp),
    )
}

@Composable
private fun InlineEditor(
    content: String, visibility: MemoVisibility, accent: Color,
    textColor: Color, subtleColor: Color,
    onContentChange: (String) -> Unit, onVisibilityChange: (MemoVisibility) -> Unit,
    onSave: () -> Unit, onCancel: () -> Unit,
) {
    var showVisibilityMenu by remember { mutableStateOf(false) }

    TextField(
        value = content, onValueChange = onContentChange,
        modifier = Modifier.fillMaxWidth().height(180.dp),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
            focusedIndicatorColor = accent, unfocusedIndicatorColor = subtleColor.copy(alpha = 0.3f), cursorColor = accent,
        ),
    )
    Spacer(Modifier.height(8.dp))
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Box {
            Text(visibility.name.lowercase(), fontSize = 13.sp, color = subtleColor, modifier = Modifier.clickable { showVisibilityMenu = true })
            if (showVisibilityMenu) {
                AlertDialog(
                    onDismissRequest = { showVisibilityMenu = false }, containerColor = MaterialTheme.colorScheme.surface, title = null,
                    text = {
                        Column {
                            MemoVisibility.entries.forEach { vis ->
                                Text(vis.name.lowercase(), fontSize = 17.sp, color = if (vis == visibility) accent else textColor,
                                    modifier = Modifier.fillMaxWidth().clickable { onVisibilityChange(vis); showVisibilityMenu = false }.padding(vertical = 10.dp))
                            }
                        }
                    },
                    confirmButton = {},
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("cancel", fontSize = 14.sp, color = subtleColor, modifier = Modifier.clickable(onClick = onCancel))
            Text("save", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = if (content.isNotBlank()) accent else subtleColor,
                modifier = Modifier.then(if (content.isNotBlank()) Modifier.clickable(onClick = onSave) else Modifier))
        }
    }
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

private fun formatAbsoluteDate(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    val dow = dayNames[local.dayOfWeek.ordinal]
    val month = monthNames[local.month.ordinal]
    return "$dow, $month ${local.day}"
}
