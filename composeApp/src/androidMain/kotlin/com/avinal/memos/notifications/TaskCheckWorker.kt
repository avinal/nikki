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
        val nowMillis = Clock.System.now().toEpochMilliseconds()
        val defaultTime = readDefaultNotifyTime(appContext)

        val memos = com.avinal.memos.util.liveMemosProvider?.invoke()
            ?: readMemosFromDb()

        val allTasks = memos.flatMap { memo -> TaskParser.extractTasks(memo.id, memo.content, memo.tags) }
        val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tz = TimeZone.currentSystemDefault()

        val alarms = ReminderScheduler.computeAlarms(allTasks, nowMillis, tz, defaultTime)

        alarms.forEach { alarm ->
            scheduleAlarm(alarmManager, alarm.taskId, alarm.taskText, alarm.label, alarm.triggerAtMillis, alarm.priority)
        }

        return Result.success()
    }

    private suspend fun readMemosFromDb(): List<com.avinal.memos.domain.Memo> {
        val db = Room.databaseBuilder<MemosDatabase>(
            context = appContext,
            name = appContext.getDatabasePath("memos.db").absolutePath,
        )
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        return try {
            db.memoDao().getAll().map { it.toDomain() }
        } finally {
            db.close()
        }
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
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }
}
