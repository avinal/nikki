package com.avinal.memos.notifications

import android.content.Context
import kotlinx.datetime.LocalTime

fun readDefaultNotifyTime(context: Context): LocalTime {
    val prefs = context.getSharedPreferences("nikki_notify", Context.MODE_PRIVATE)
    val timeStr = prefs.getString("default_notify_time", "20:00") ?: "20:00"
    val parts = timeStr.split(":")
    return try {
        LocalTime(parts[0].toInt(), parts.getOrElse(1) { "0" }.toInt())
    } catch (_: Exception) {
        LocalTime(20, 0)
    }
}

fun writeDefaultNotifyTime(context: Context, time: String) {
    context.getSharedPreferences("nikki_notify", Context.MODE_PRIVATE)
        .edit().putString("default_notify_time", time).apply()
}
