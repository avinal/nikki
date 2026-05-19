package com.avinal.memos.util

import android.content.Intent

actual fun sharePlainText(text: String, title: String) {
    val context = appContext ?: return
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        if (title.isNotEmpty()) putExtra(Intent.EXTRA_SUBJECT, title)
    }
    val chooser = Intent.createChooser(intent, "Share memo")
    chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(chooser)
}

var appContext: android.content.Context? = null
