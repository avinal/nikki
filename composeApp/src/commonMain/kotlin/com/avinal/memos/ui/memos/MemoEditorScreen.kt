package com.avinal.memos.ui.memos

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun MemoEditorScreen(
    memoId: String?,
    deps: AppDependencies,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    val viewModel = viewModel { MemoEditorViewModel(memoId, deps.memoRepository) }
    val uiState by viewModel.uiState.collectAsState()
    val accent = LocalAccentColor.current
    var showDiscardDialog by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.isSaved) { if (uiState.isSaved) onSaved() }

    val handleBack = { if (uiState.isDirty) showDiscardDialog = true else onBack() }
    BackHandler(enabled = uiState.isDirty) { showDiscardDialog = true }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("discard changes?") },
            confirmButton = {
                TextButton(onClick = { showDiscardDialog = false; onBack() }) { Text("discard") }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) { Text("keep editing") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(start = 24.dp, end = 24.dp, top = 12.dp),
    ) {
        Text(
            if (memoId == null) "new memo" else "edit memo",
            fontSize = 32.sp,
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )

        Spacer(Modifier.height(18.dp))

        TextField(
            value = uiState.content,
            onValueChange = viewModel::updateContent,
            modifier = Modifier.fillMaxWidth().weight(1f),
            placeholder = { Text("any thoughts...", fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) },
            textStyle = MaterialTheme.typography.bodyMedium,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = accent,
                unfocusedIndicatorColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            ),
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            var showVisMenu by remember { mutableStateOf(false) }
            androidx.compose.foundation.layout.Box {
                Text(
                    uiState.visibility.name.lowercase(),
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { showVisMenu = true },
                )
                DropdownMenu(expanded = showVisMenu, onDismissRequest = { showVisMenu = false }) {
                    MemoVisibility.entries.forEach { vis ->
                        Text(
                            vis.name.lowercase(),
                            fontSize = 14.sp,
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.setVisibility(vis); showVisMenu = false }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "cancel",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { handleBack() },
                )
                if (uiState.isSaving) {
                    CircularProgressIndicator(color = accent, strokeWidth = 2.dp)
                } else {
                    Text(
                        "save",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (uiState.content.isNotBlank()) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.then(
                            if (uiState.content.isNotBlank()) Modifier.clickable(onClick = viewModel::save) else Modifier
                        ),
                    )
                }
            }
        }

        if (uiState.error != null) {
            Text(uiState.error!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.error)
            Spacer(Modifier.height(12.dp))
        }
    }
}
