package com.avinal.memos.ui.onboarding

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.avinal.memos.util.appContext

@Composable
actual fun rememberPermissionRequester(
    onNotificationResult: (Boolean) -> Unit,
    onBatteryResult: (Boolean) -> Unit,
    onCalendarResult: (Boolean) -> Unit,
): PermissionRequester {
    val notificationLauncher = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            onNotificationResult(granted)
        }
    } else null

    val calendarLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        onCalendarResult(results[Manifest.permission.READ_CALENDAR] == true)
    }

    return remember(notificationLauncher, calendarLauncher) {
        object : PermissionRequester {
            override fun requestNotification() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher?.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    onNotificationResult(true)
                }
            }

            override fun requestBatteryExemption() {
                val ctx = appContext ?: return
                val pm = ctx.getSystemService(android.os.PowerManager::class.java)
                if (pm.isIgnoringBatteryOptimizations(ctx.packageName)) {
                    onBatteryResult(true)
                    return
                }
                try {
                    val intent = Intent(
                        Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:${ctx.packageName}"),
                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    ctx.startActivity(intent)
                    onBatteryResult(true)
                } catch (_: Exception) {
                    onBatteryResult(false)
                }
            }

            override fun requestCalendar() {
                calendarLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                )
            }
        }
    }
}
