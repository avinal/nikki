package com.avinal.memos.ui.onboarding

import androidx.compose.runtime.Composable

interface PermissionRequester {
    fun requestNotification()
    fun requestBatteryExemption()
}

@Composable
expect fun rememberPermissionRequester(
    onNotificationResult: (Boolean) -> Unit,
    onBatteryResult: (Boolean) -> Unit,
): PermissionRequester
