package com.avinal.memos

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.core.content.ContextCompat
import com.avinal.memos.notifications.TaskNotificationManager
import com.avinal.memos.notifications.consumeCalendarPermissionFlag
import com.avinal.memos.notifications.scheduleTaskChecker
import com.avinal.memos.util.LocalAppDependencies

class MainActivity : ComponentActivity() {

    private val deps by lazy {
        AppDependencies(
            dataStorePath = filesDir.resolve("memos_prefs.preferences_pb").absolutePath,
            platformContext = applicationContext,
        )
    }

    private val calendarPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { /* result handled by CalendarReminderManager on next sync */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        deps.initialize()
        com.avinal.memos.util.appContext = applicationContext

        TaskNotificationManager.createChannels(this)
        scheduleTaskChecker(applicationContext)

        val sharedText = if (intent?.action == Intent.ACTION_SEND && intent.type == "text/plain") {
            intent.getStringExtra(Intent.EXTRA_TEXT)
        } else null

        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppDependencies provides deps) {
                App(sharedText = sharedText)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (consumeCalendarPermissionFlag(applicationContext)) {
            val alreadyGranted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_CALENDAR,
            ) == PackageManager.PERMISSION_GRANTED
            if (!alreadyGranted) {
                calendarPermissionLauncher.launch(
                    arrayOf(Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR),
                )
            }
        }
    }
}
