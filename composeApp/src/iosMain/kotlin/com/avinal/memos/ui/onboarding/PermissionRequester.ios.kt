package com.avinal.memos.ui.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberPermissionRequester(
    onNotificationResult: (Boolean) -> Unit,
    onBatteryResult: (Boolean) -> Unit,
    onCalendarResult: (Boolean) -> Unit,
): PermissionRequester = remember {
    object : PermissionRequester {
        override fun requestNotification() { onNotificationResult(false) }
        override fun requestBatteryExemption() { onBatteryResult(false) }
        override fun requestCalendar() { onCalendarResult(false) }
    }
}
