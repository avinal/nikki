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

fun Instant.toRelativeString(): String {
    val now = Clock.System.now()
    val diffMs = now.toEpochMilliseconds() - this.toEpochMilliseconds()
    val seconds = diffMs / 1000
    val minutes = diffMs / 60_000
    val hours = diffMs / 3_600_000
    val days = diffMs / 86_400_000

    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days == 1L -> "yesterday"
        days < 7 -> "${days}d ago"
        else -> {
            val local = this.toLocalDateTime(TimeZone.currentSystemDefault())
            "${monthNames[local.month.ordinal]} ${local.day}"
        }
    }
}
