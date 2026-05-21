package com.avinal.memos.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class TaskAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskText = intent.getStringExtra("task_text") ?: "Task reminder"
        val dueLabel = intent.getStringExtra("due_label") ?: "due"
        val notificationId = intent.getIntExtra("notification_id", 0)

        TaskNotificationManager.showTaskNotification(
            context = context,
            notificationId = notificationId,
            taskText = taskText,
            dueLabel = dueLabel,
        )
    }
}
