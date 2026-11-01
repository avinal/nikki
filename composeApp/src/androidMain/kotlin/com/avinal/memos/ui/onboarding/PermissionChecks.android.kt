package com.avinal.memos.ui.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.avinal.memos.util.appContext

actual fun checkNotificationPermission(): Boolean {
    val ctx = appContext ?: return false
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

actual fun checkBatteryExemption(): Boolean {
    val ctx = appContext ?: return false
    val pm = ctx.getSystemService(android.os.PowerManager::class.java)
    return pm.isIgnoringBatteryOptimizations(ctx.packageName)
}
