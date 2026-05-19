package com.avinal.memos

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.avinal.memos.ui.navigation.AppNavHost
import com.avinal.memos.ui.theme.MemosAppTheme
import com.avinal.memos.util.LocalAppDependencies

@Composable
fun App() {
    val deps = LocalAppDependencies.current

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = deps.httpClient))
            }
            .build()
    }

    MemosAppTheme {
        AppNavHost(deps)
    }
}
