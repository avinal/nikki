package com.avinal.memos.util

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberFileSaver(onSaved: (Boolean) -> Unit): (String, String) -> Unit {
    val context = LocalContext.current
    var pendingContent: String? = null

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json"),
    ) { uri: Uri? ->
        if (uri != null && pendingContent != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    stream.write(pendingContent!!.toByteArray())
                }
                onSaved(true)
            } catch (_: Exception) {
                onSaved(false)
            }
        }
        pendingContent = null
    }

    return { filename: String, content: String ->
        pendingContent = content
        launcher.launch(filename)
    }
}

@Composable
actual fun rememberFileLoader(onLoaded: (String) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() }
            if (text != null) onLoaded(text)
        } catch (_: Exception) {}
    }
    return { launcher.launch("application/json") }
}
