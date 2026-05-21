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
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.todayIn

class TaskCheckWorker(
    private val appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val prefs = appContext.getSharedPreferences("task_notifications", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("enabled", true)
        if (!notificationsEnabled) return Result.success()

        val scheduledIds = prefs.getStringSet("scheduled_ids", emptySet()) ?: emptySet()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
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
            val newScheduledIds = scheduledIds.toMutableSet()

            allTasks.forEach { task ->
                if (task.isCompleted || task.dueDate == null || task.id in scheduledIds) return@forEach

                if (task.dueTime != null) {
                    // Specific time: schedule one alarm at that exact time
                    val alarmInstant = task.dueDate.atTime(task.dueTime).toInstant(tz)
                    val alarmMs = alarmInstant.toEpochMilliseconds()
                    if (alarmMs > nowMillis) {
                        scheduleAlarm(alarmManager, task.id, task.text, "due at ${task.dueTime}", alarmMs)
                        newScheduledIds.add(task.id)
                    }
                } else {
                    // No specific time: schedule at 8am and 8pm on the due date
                    val morning = task.dueDate.atTime(LocalTime(8, 0)).toInstant(tz).toEpochMilliseconds()
                    val evening = task.dueDate.atTime(LocalTime(20, 0)).toInstant(tz).toEpochMilliseconds()

                    if (morning > nowMillis) {
                        scheduleAlarm(alarmManager, "${task.id}_am", task.text, "due today", morning)
                    }
                    if (evening > nowMillis) {
                        scheduleAlarm(alarmManager, "${task.id}_pm", task.text, "reminder: still due today", evening)
                    }
                    newScheduledIds.add(task.id)
                }
            }

            // Clean up IDs for completed/removed tasks
            val activeTaskIds = allTasks.filter { !it.isCompleted }.map { it.id }.toSet()
            val cleaned = newScheduledIds.filter { id ->
                val baseId = id.removeSuffix("_am").removeSuffix("_pm")
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
