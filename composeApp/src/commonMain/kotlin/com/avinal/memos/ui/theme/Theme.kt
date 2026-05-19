package com.avinal.memos.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.avinal.memos.util.LocalAppDependencies

@Composable
fun MemosAppTheme(content: @Composable () -> Unit) {
    val deps = LocalAppDependencies.current
    val themeName by deps.tokenStore.theme.collectAsState(initial = "DARK")
    val accentName by deps.tokenStore.accentColor.collectAsState(initial = "Cobalt")

    val metroTheme = try { MetroTheme.valueOf(themeName) } catch (_: Exception) { MetroTheme.DARK }
    val accent = WpAccentColors.find { it.name == accentName }?.color ?: Color(0xFF0050EF)

    CompositionLocalProvider(LocalAccentColor provides accent) {
        MaterialTheme(
            colorScheme = metroColorScheme(metroTheme, accent),
            typography = MetroTypography,
            content = content,
        )
    }
}
