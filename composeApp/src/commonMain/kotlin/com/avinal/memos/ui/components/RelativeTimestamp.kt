package com.avinal.memos.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun RelativeTimestamp(instant: Instant, modifier: Modifier = Modifier) {
    Text(
        text = instant.toRelativeString(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

private val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val dayNames = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

fun Instant.toRelativeString(): String {
    val now = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val diffMs = now.toEpochMilliseconds() - this.toEpochMilliseconds()
    val seconds = diffMs / 1000
    val minutes = diffMs / 60_000
    val hours = diffMs / 3_600_000

    val nowLocal = now.toLocalDateTime(tz)
    val thisLocal = this.toLocalDateTime(tz)
    val dayDiff = nowLocal.date.toEpochDays() - thisLocal.date.toEpochDays()

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 && dayDiff <= 1L -> "${hours}h ago"
        dayDiff == 1L -> "yesterday"
        dayDiff < 7L -> dayNames[thisLocal.dayOfWeek.ordinal]
        thisLocal.year == nowLocal.year -> "${monthNames[thisLocal.month.ordinal]} ${thisLocal.day}"
        else -> "${monthNames[thisLocal.month.ordinal]} ${thisLocal.day}, ${thisLocal.year}"
    }
}
