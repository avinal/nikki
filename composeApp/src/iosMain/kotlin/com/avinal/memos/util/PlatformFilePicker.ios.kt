package com.avinal.memos.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFilePicker(onFilePicked: (PickedFile) -> Unit): () -> Unit {
    return { /* TODO: iOS file picker */ }
}
