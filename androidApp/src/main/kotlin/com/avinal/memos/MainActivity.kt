package com.avinal.memos

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
}
