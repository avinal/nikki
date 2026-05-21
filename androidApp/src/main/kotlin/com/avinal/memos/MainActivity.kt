package com.avinal.memos

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.avinal.memos.notifications.TaskNotificationManager
import com.avinal.memos.notifications.scheduleTaskChecker
import com.avinal.memos.util.LocalAppDependencies

class MainActivity : ComponentActivity() {

    private val deps by lazy {
        AppDependencies(
            dataStorePath = filesDir.resolve("memos_prefs.preferences_pb").absolutePath,
            platformContext = applicationContext,
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deps.initialize()
        com.avinal.memos.util.appContext = applicationContext

        TaskNotificationManager.createChannels(this)
        requestNotificationPermission()
        requestBatteryOptimizationExemption()
        scheduleTaskChecker(applicationContext)

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppDependencies provides deps) {
                App()
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }
    }

    private fun requestBatteryOptimizationExemption() {
        val pm = getSystemService(android.os.PowerManager::class.java)
        if (!pm.isIgnoringBatteryOptimizations(packageName)) {
            try {
                startActivity(android.content.Intent(
                    android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                    android.net.Uri.parse("package:$packageName")
                ))
            } catch (_: Exception) {}
        }
    }
}
