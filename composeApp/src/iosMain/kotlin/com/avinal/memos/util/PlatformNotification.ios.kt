package com.avinal.memos.util

actual fun triggerReminderCheck() {}
actual fun setLiveMemosProvider(provider: () -> List<com.avinal.memos.domain.Memo>) {}
