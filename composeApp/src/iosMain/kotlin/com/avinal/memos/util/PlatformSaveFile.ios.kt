package com.avinal.memos.util

import androidx.compose.runtime.Composable

@Composable
actual fun rememberFileSaver(onSaved: (Boolean) -> Unit): (String, String) -> Unit {
    return { _, _ -> /* TODO: iOS file saver */ }
}

@Composable
actual fun rememberFileLoader(onLoaded: (String) -> Unit): () -> Unit {
    return { /* TODO: iOS file loader */ }
}
