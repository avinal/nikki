package com.avinal.memos.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

fun scheduleTaskChecker(context: Context) {
    val request = PeriodicWorkRequestBuilder<TaskCheckWorker>(15, TimeUnit.MINUTES).build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        "task_check",
        ExistingPeriodicWorkPolicy.KEEP,
        request,
    )
}
