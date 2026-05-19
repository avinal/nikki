package com.avinal.memos.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

val PriorityP1 = Color(0xFFE51400)
val PriorityP2 = Color(0xFFF0A30A)
val PriorityP3 = Color(0xFF1BA1E2)
val LabelGreen = Color(0xFF60A917)
val DuePurple = Color(0xFF6A00FF)
val OverdueRed = Color(0xFFE51400)

// --- WP8 Accent Colors ---
data class AccentColor(val name: String, val color: Color)

val WpAccentColors = listOf(
    AccentColor("Lime", Color(0xFFA4C400)),
    AccentColor("Green", Color(0xFF60A917)),
    AccentColor("Emerald", Color(0xFF008A00)),
    AccentColor("Teal", Color(0xFF00ABA9)),
    AccentColor("Cyan", Color(0xFF1BA1E2)),
    AccentColor("Cobalt", Color(0xFF0050EF)),
    AccentColor("Indigo", Color(0xFF6A00FF)),
    AccentColor("Violet", Color(0xFFAA00FF)),
    AccentColor("Pink", Color(0xFFF472D0)),
    AccentColor("Magenta", Color(0xFFD80073)),
    AccentColor("Crimson", Color(0xFFA20025)),
    AccentColor("Red", Color(0xFFE51400)),
    AccentColor("Orange", Color(0xFFFA6800)),
    AccentColor("Amber", Color(0xFFF0A30A)),
    AccentColor("Yellow", Color(0xFFE3C800)),
    AccentColor("Brown", Color(0xFF825A2C)),
    AccentColor("Olive", Color(0xFF6D8764)),
    AccentColor("Steel", Color(0xFF647687)),
    AccentColor("Mauve", Color(0xFF76608A)),
    AccentColor("Taupe", Color(0xFF87794E)),
)

val LocalAccentColor = compositionLocalOf { Color(0xFF0050EF) }

// --- Metro Theme Variants ---
enum class MetroTheme(val label: String) {
    DARK("dark"),
    LIGHT("light"),
    AMOLED("amoled"),
}

fun metroColorScheme(theme: MetroTheme, accent: Color): ColorScheme = when (theme) {
    MetroTheme.DARK -> darkColorScheme(
        background = Color(0xFF1F1F1F),
        onBackground = Color.White,
        surface = Color(0xFF1F1F1F),
        onSurface = Color.White,
        surfaceVariant = Color(0xFF2A2A2A),
        onSurfaceVariant = Color(0xFF999999),
        primary = accent,
        onPrimary = Color.White,
        secondary = Color(0xFF333333),
        onSecondary = Color(0xFFCCCCCC),
        error = Color(0xFFE51400),
        onError = Color.White,
        outline = Color(0xFF666666),
        outlineVariant = Color(0xFF444444),
    )
    MetroTheme.LIGHT -> lightColorScheme(
        background = Color.White,
        onBackground = Color.Black,
        surface = Color.White,
        onSurface = Color.Black,
        surfaceVariant = Color(0xFFF0F0F0),
        onSurfaceVariant = Color(0xFF666666),
        primary = accent,
        onPrimary = Color.White,
        secondary = Color(0xFFE8E8E8),
        onSecondary = Color(0xFF333333),
        error = Color(0xFFE51400),
        onError = Color.White,
        outline = Color(0xFFCCCCCC),
        outlineVariant = Color(0xFFE0E0E0),
    )
    MetroTheme.AMOLED -> darkColorScheme(
        background = Color.Black,
        onBackground = Color.White,
        surface = Color.Black,
        onSurface = Color.White,
        surfaceVariant = Color(0xFF111111),
        onSurfaceVariant = Color(0xFF999999),
        primary = accent,
        onPrimary = Color.White,
        secondary = Color(0xFF1A1A1A),
        onSecondary = Color(0xFFCCCCCC),
        error = Color(0xFFE51400),
        onError = Color.White,
        outline = Color(0xFF555555),
        outlineVariant = Color(0xFF333333),
    )
}
