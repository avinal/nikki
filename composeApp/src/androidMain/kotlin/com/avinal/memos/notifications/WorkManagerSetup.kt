package com.avinal.memos.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun runTaskCheckNow(context: Context) {
    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequestBuilder<TaskCheckWorker>().build()
    )
}

fun scheduleTaskChecker(context: Context) {
    // Run once immediately on app launch
    WorkManager.getInstance(context).enqueue(
        OneTimeWorkRequestBuilder<TaskCheckWorker>().build()
    )

    // Then every 15 minutes
    val request = PeriodicWorkRequestBuilder<TaskCheckWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "task_check",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
