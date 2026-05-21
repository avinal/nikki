package com.avinal.memos.util

import androidx.compose.runtime.Composable

@Composable
expect fun rememberFileSaver(onSaved: (Boolean) -> Unit): (String, String) -> Unit

@Composable
expect fun rememberFileLoader(onLoaded: (String) -> Unit): () -> Unit
