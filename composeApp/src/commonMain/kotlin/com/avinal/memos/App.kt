package com.avinal.memos

import androidx.compose.runtime.Composable
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.avinal.memos.ui.navigation.AppNavHost
import com.avinal.memos.ui.theme.NikkiTheme
import com.avinal.memos.util.LocalAppDependencies

@OptIn(ExperimentalCoilApi::class)
@Composable
fun App(sharedText: String? = null) {
    val deps = LocalAppDependencies.current

    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory(httpClient = deps.httpClient))
            }
            .build()
    }

    NikkiTheme {
        AppNavHost(deps, sharedText = sharedText)
    }
}
