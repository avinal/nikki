package com.avinal.memos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.avinal.memos.domain.Memo
import com.avinal.memos.parser.TaskParser
import kotlin.time.Clock
import kotlinx.datetime.TimeZone

object DirectAlarmScheduler {

    fun scheduleFromMemos(context: Context, memos: List<Memo>) {
        val allTasks = memos.flatMap { memo -> TaskParser.extractTasks(memo.id, memo.content, memo.tags) }
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val tz = TimeZone.currentSystemDefault()

        val defaultTime = readDefaultNotifyTime(context)

        val alarms = ReminderScheduler.computeAlarms(allTasks, nowMillis, tz, defaultTime)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        alarms.forEach { alarm ->
            val receiverClass = try {
                Class.forName("com.avinal.memos.TaskReminderReceiver")
            } catch (_: Exception) {
                TaskAlarmReceiver::class.java
            }
            val uniqueId = (alarm.taskId + alarm.triggerAtMillis.toString()).hashCode()
            val intent = Intent(context, receiverClass).apply {
                putExtra("task_text", alarm.taskText)
                putExtra("due_label", alarm.label)
                putExtra("notification_id", uniqueId)
                putExtra("priority", alarm.priority)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, uniqueId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            try {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
            } catch (_: SecurityException) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, alarm.triggerAtMillis, pendingIntent)
            }
        }
    }
}
