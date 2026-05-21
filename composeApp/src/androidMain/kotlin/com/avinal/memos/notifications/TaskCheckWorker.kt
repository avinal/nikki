package com.avinal.memos.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avinal.memos.db.MemosDatabase
import com.avinal.memos.db.entity.toDomain
import com.avinal.memos.parser.TaskParser
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone

class TaskCheckWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("task_notifications", Context.MODE_PRIVATE)
        val scheduledIds = prefs.getStringSet("scheduled_ids", emptySet()) ?: emptySet()
        val nowMillis = Clock.System.now().toEpochMilliseconds()

        // Read default notify time from DataStore file (shared prefs fallback)
        val notifyPrefs = appContext.getSharedPreferences("memos_prefs", Context.MODE_PRIVATE)
        val defaultTimeStr = notifyPrefs.getString("default_notify_time", "20:00") ?: "20:00"
        val defaultTimeParts = defaultTimeStr.split(":")
        val defaultTime = try {
            kotlinx.datetime.LocalTime(defaultTimeParts[0].toInt(), defaultTimeParts.getOrElse(1) { "0" }.toInt())
        } catch (_: Exception) {
            kotlinx.datetime.LocalTime(20, 0)
        }

        val db = Room.databaseBuilder<MemosDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("memos.db").absolutePath,
        )
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val memos = db.memoDao().getAll().map { it.toDomain() }
            val allTasks = memos.flatMap { memo -> TaskParser.extractTasks(memo.id, memo.content) }
            val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val tz = TimeZone.currentSystemDefault()

            android.util.Log.d("TaskCheckWorker", "Found ${memos.size} memos, ${allTasks.size} tasks, scheduled=${scheduledIds.size}")
            allTasks.forEach { t ->
                android.util.Log.d("TaskCheckWorker", "Task: ${t.text}, date=${t.dueDate}, time=${t.dueTime}, reminder=${t.reminder}, completed=${t.isCompleted}, id=${t.id}")
            }

            android.util.Log.d("TaskCheckWorker", "nowMillis=$nowMillis, tz=$tz, defaultTime=$defaultTime")
            val alarms = ReminderScheduler.computeAlarms(allTasks, nowMillis, tz, scheduledIds, defaultTime)
            android.util.Log.d("TaskCheckWorker", "Computed ${alarms.size} alarms")

            alarms.forEach { alarm ->
                android.util.Log.d("TaskCheckWorker", "Scheduling: ${alarm.taskText} at ${alarm.triggerAtMillis} (${alarm.label}) p=${alarm.priority}")
                scheduleAlarm(alarmManager, alarm.taskId, alarm.taskText, alarm.label, alarm.triggerAtMillis, alarm.priority)
            }

            val newScheduledIds = scheduledIds.toMutableSet()
            alarms.forEach { newScheduledIds.add(it.taskId) }

            val activeTaskIds = allTasks.filter { !it.isCompleted }.map { it.id }.toSet()
            val cleaned = newScheduledIds.filter { id ->
                val baseId = id.removeSuffix("_am").removeSuffix("_pm").removeSuffix("_remind")
                baseId in activeTaskIds
            }.toSet()

            prefs.edit().putStringSet("scheduled_ids", cleaned).apply()
        } finally {
            db.close()
        }

        return Result.success()
    }

    private fun scheduleAlarm(
        alarmManager: AlarmManager,
        alarmId: String,
        taskText: String,
        dueLabel: String,
        triggerAtMillis: Long,
        priority: Int = 0,
    ) {
        val uniqueId = (alarmId + triggerAtMillis.toString()).hashCode()
        val receiverClass = try {
            Class.forName("com.avinal.memos.TaskReminderReceiver")
        } catch (_: Exception) {
            TaskAlarmReceiver::class.java
        }
        val intent = Intent(appContext, receiverClass).apply {
            putExtra("task_text", taskText)
            putExtra("due_label", dueLabel)
            putExtra("notification_id", uniqueId)
            putExtra("priority", priority)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            uniqueId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        } catch (_: SecurityException) {
            // Fallback if exact alarm permission not granted
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
