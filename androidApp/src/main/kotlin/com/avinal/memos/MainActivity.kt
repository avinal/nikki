package com.avinal.memos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
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
        enableEdgeToEdge()
        setContent {
            CompositionLocalProvider(LocalAppDependencies provides deps) {
                App()
            }
        }
    }
}
