package com.avinal.memos.util

import com.avinal.memos.notifications.DirectAlarmScheduler
import com.avinal.memos.notifications.runTaskCheckNow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

var liveMemosProvider: (() -> List<com.avinal.memos.domain.Memo>)? = null
    private set

actual fun setLiveMemosProvider(provider: () -> List<com.avinal.memos.domain.Memo>) {
    liveMemosProvider = provider
}

actual fun syncNotifyTime(time: String) {
    val ctx = appContext ?: return
    com.avinal.memos.notifications.writeDefaultNotifyTime(ctx, time)
}

actual fun triggerReminderCheck() {
    val ctx = appContext ?: return
    val memos = liveMemosProvider?.invoke()
    if (memos != null && memos.isNotEmpty()) {
        DirectAlarmScheduler.scheduleFromMemos(ctx, memos)
    } else {
        runTaskCheckNow(ctx)
    }
}
