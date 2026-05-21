package com.avinal.memos.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat

object TaskNotificationManager {

    private const val CHANNEL_P1 = "task_p1"
    private const val CHANNEL_P2 = "task_p2"
    private const val CHANNEL_P3 = "task_p3"
    private const val CHANNEL_DEFAULT = "task_default"

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val audioAttr = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        manager.createNotificationChannel(NotificationChannel(CHANNEL_P1, "P1 — Urgent", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "High priority — alarm sound, strong vibration, wakes screen"
            enableVibration(true); vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 500)
            setSound(alarmUri, audioAttr); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC; setShowBadge(true); setBypassDnd(true)
        })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_P2, "P2 — Medium", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Medium priority — notification sound, vibration"
            enableVibration(true); vibrationPattern = longArrayOf(0, 300, 200, 300)
            setSound(soundUri, audioAttr); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC; setShowBadge(true)
        })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_P3, "P3 — Low", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Low priority — notification sound, short vibration"
            enableVibration(true); vibrationPattern = longArrayOf(0, 200)
            setSound(soundUri, audioAttr); lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        })
        manager.createNotificationChannel(NotificationChannel(CHANNEL_DEFAULT, "No Priority", NotificationManager.IMPORTANCE_LOW).apply {
            description = "No priority — silent notification"
            enableVibration(false); setSound(null, null)
        })

        manager.deleteNotificationChannel("task_reminders")
    }

    fun showTaskNotification(
        context: Context,
        notificationId: Int,
        taskText: String,
        dueLabel: String,
        priority: Int = 0,
    ) {
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        } ?: Intent()
        val pendingOpen = PendingIntent.getActivity(
            context, notificationId, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val channelId = when (priority) { 1 -> CHANNEL_P1; 2 -> CHANNEL_P2; 3 -> CHANNEL_P3; else -> CHANNEL_DEFAULT }
        val notifPriority = when (priority) { 1 -> NotificationCompat.PRIORITY_MAX; 2 -> NotificationCompat.PRIORITY_HIGH; 3 -> NotificationCompat.PRIORITY_DEFAULT; else -> NotificationCompat.PRIORITY_LOW }

        val priorityEmoji = when (priority) { 1 -> "🔴"; 2 -> "🟠"; 3 -> "🔵"; else -> "" }
        val priorityTag = when (priority) { 1 -> "URGENT"; 2 -> "MEDIUM"; 3 -> "LOW"; else -> "" }

        val title = buildString {
            if (priorityEmoji.isNotEmpty()) append("$priorityEmoji ")
            append(taskText)
        }

        val bigText = buildString {
            append(dueLabel)
            if (priorityTag.isNotEmpty()) append("\nPriority: $priorityTag")
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(dueLabel)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setSubText(when (priority) { 1 -> "p1 urgent"; 2 -> "p2 medium"; 3 -> "p3 low"; else -> "memos" })
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setPriority(notifPriority)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingOpen)
            .setDefaults(NotificationCompat.DEFAULT_LIGHTS)
            .setGroup("task_reminders_${notificationId}")
            .setColor(when (priority) { 1 -> 0xFFE51400.toInt(); 2 -> 0xFFF0A30A.toInt(); 3 -> 0xFF1BA1E2.toInt(); else -> 0xFF666666.toInt() })

        when (priority) {
            1 -> {
                builder.setFullScreenIntent(pendingOpen, true)
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM))
                builder.setVibrate(longArrayOf(0, 500, 200, 500, 200, 500))
            }
            2 -> {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                builder.setVibrate(longArrayOf(0, 300, 200, 300))
            }
            3 -> {
                builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                builder.setVibrate(longArrayOf(0, 200))
            }
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notificationId, builder.build())
    }
}
