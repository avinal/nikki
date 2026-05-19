package com.avinal.memos.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFilePicker(onFilePicked: (PickedFile) -> Unit): () -> Unit
