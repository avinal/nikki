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

            val alarms = ReminderScheduler.computeAlarms(allTasks, nowMillis, tz, scheduledIds)
            android.util.Log.d("TaskCheckWorker", "Computed ${alarms.size} alarms")

            alarms.forEach { alarm ->
                android.util.Log.d("TaskCheckWorker", "Scheduling: ${alarm.taskText} at ${alarm.triggerAtMillis} (${alarm.label})")
                scheduleAlarm(alarmManager, alarm.taskId, alarm.taskText, alarm.label, alarm.triggerAtMillis)
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
    ) {
        val intent = Intent(appContext, TaskAlarmReceiver::class.java).apply {
            putExtra("task_text", taskText)
            putExtra("due_label", dueLabel)
            putExtra("notification_id", alarmId.hashCode())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            alarmId.hashCode(),
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
