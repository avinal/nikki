package com.avinal.memos.util

import com.avinal.memos.notifications.runTaskCheckNow

actual fun triggerReminderCheck() {
    val ctx = appContext ?: return
    runTaskCheckNow(ctx)
}
