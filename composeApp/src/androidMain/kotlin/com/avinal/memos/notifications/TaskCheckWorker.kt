package com.avinal.memos.notifications

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.avinal.memos.db.MemosDatabase
import com.avinal.memos.db.entity.toDomain
import com.avinal.memos.parser.TaskParser
import kotlinx.coroutines.Dispatchers
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn

class TaskCheckWorker(
    private val context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = context.getSharedPreferences("task_notifications", Context.MODE_PRIVATE)
        val notificationsEnabled = prefs.getBoolean("enabled", true)
        if (!notificationsEnabled) return Result.success()

        val notifiedIds = prefs.getStringSet("notified_ids", emptySet()) ?: emptySet()
        val today = kotlin.time.Clock.System.todayIn(TimeZone.currentSystemDefault())

        val db = Room.databaseBuilder<MemosDatabase>(
            context = context,
            name = context.getDatabasePath("memos.db").absolutePath,
        )
            .fallbackToDestructiveMigration(true)
            .setDriver(BundledSQLiteDriver())
            .setQueryCoroutineContext(Dispatchers.IO)
            .build()

        try {
            val memos = db.memoDao().getAll().map { it.toDomain() }
            val allTasks = memos.flatMap { memo -> TaskParser.extractTasks(memo.id, memo.content) }

            val dueTasks = allTasks.filter { task ->
                !task.isCompleted &&
                    task.dueDate != null &&
                    task.dueDate <= today &&
                    task.id !in notifiedIds
            }

            val newNotifiedIds = notifiedIds.toMutableSet()

            dueTasks.forEach { task ->
                val dueLabel = when {
                    task.dueDate!! < today -> "overdue"
                    else -> "due today"
                }
                val priority = task.priority?.let { " p$it" } ?: ""
                TaskNotificationManager.showTaskNotification(
                    context = context,
                    notificationId = task.id.hashCode(),
                    taskText = task.text,
                    dueLabel = "$dueLabel$priority",
                )
                newNotifiedIds.add(task.id)
            }

            if (newNotifiedIds.size > notifiedIds.size) {
                prefs.edit().putStringSet("notified_ids", newNotifiedIds).apply()
            }

            // Clean up old IDs (tasks that no longer exist)
            val allTaskIds = allTasks.map { it.id }.toSet()
            val cleaned = newNotifiedIds.filter { it in allTaskIds }.toSet()
            if (cleaned.size < newNotifiedIds.size) {
                prefs.edit().putStringSet("notified_ids", cleaned).apply()
            }
        } finally {
            db.close()
        }

        return Result.success()
    }
}
