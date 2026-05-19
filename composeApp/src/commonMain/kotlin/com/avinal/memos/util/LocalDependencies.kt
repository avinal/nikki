package com.avinal.memos.util

import androidx.compose.runtime.staticCompositionLocalOf
import com.avinal.memos.AppDependencies

val LocalAppDependencies = staticCompositionLocalOf<AppDependencies> {
    error("AppDependencies not provided")
}
