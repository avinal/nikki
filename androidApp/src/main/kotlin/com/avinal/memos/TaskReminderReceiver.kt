package com.avinal.memos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.avinal.memos.notifications.TaskNotificationManager

class TaskReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val taskText = intent.getStringExtra("task_text") ?: "Task reminder"
        val dueLabel = intent.getStringExtra("due_label") ?: "due"
        val notificationId = intent.getIntExtra("notification_id", 0)
        val priority = intent.getIntExtra("priority", 0)
        TaskNotificationManager.createChannels(context)
        TaskNotificationManager.showTaskNotification(
            context = context,
            notificationId = notificationId,
            taskText = taskText,
            dueLabel = dueLabel,
            priority = priority,
        )
    }
}
