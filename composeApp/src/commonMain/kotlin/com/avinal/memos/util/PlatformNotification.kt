package com.avinal.memos.util

expect fun triggerReminderCheck()
expect fun setLiveMemosProvider(provider: () -> List<com.avinal.memos.domain.Memo>)
