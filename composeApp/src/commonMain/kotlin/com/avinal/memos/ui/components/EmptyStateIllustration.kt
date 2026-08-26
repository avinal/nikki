package com.avinal.memos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun EmptyMemoIllustration(modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(80.dp)) {
        val w = size.width
        val h = size.height
        drawRect(subtle.copy(alpha = 0.08f), topLeft = Offset(w * 0.15f, h * 0.45f), size = Size(w * 0.6f, h * 0.3f))
        drawRect(subtle.copy(alpha = 0.12f), topLeft = Offset(w * 0.1f, h * 0.35f), size = Size(w * 0.6f, h * 0.3f))
        drawRect(accent.copy(alpha = 0.2f), topLeft = Offset(w * 0.05f, h * 0.25f), size = Size(w * 0.6f, h * 0.3f))
        drawLine(subtle.copy(alpha = 0.3f), Offset(w * 0.12f, h * 0.35f), Offset(w * 0.5f, h * 0.35f), strokeWidth = 2f)
        drawLine(subtle.copy(alpha = 0.2f), Offset(w * 0.12f, h * 0.43f), Offset(w * 0.4f, h * 0.43f), strokeWidth = 2f)
        val cx = w * 0.78f; val cy = h * 0.15f; val len = w * 0.08f
        drawLine(accent.copy(alpha = 0.4f), Offset(cx - len, cy), Offset(cx + len, cy), strokeWidth = 2.5f)
        drawLine(accent.copy(alpha = 0.4f), Offset(cx, cy - len), Offset(cx, cy + len), strokeWidth = 2.5f)
    }
}

@Composable
fun EmptyArchivedIllustration(modifier: Modifier = Modifier) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(80.dp)) {
        val w = size.width
        val h = size.height
        drawRect(subtle.copy(alpha = 0.15f), topLeft = Offset(w * 0.2f, h * 0.3f), size = Size(w * 0.6f, h * 0.4f))
        drawRect(subtle.copy(alpha = 0.1f), topLeft = Offset(w * 0.15f, h * 0.25f), size = Size(w * 0.7f, h * 0.1f))
        val cx = w * 0.5f
        drawLine(subtle.copy(alpha = 0.3f), Offset(cx, h * 0.45f), Offset(cx, h * 0.6f), strokeWidth = 2f)
        drawLine(subtle.copy(alpha = 0.3f), Offset(cx - w * 0.06f, h * 0.55f), Offset(cx, h * 0.6f), strokeWidth = 2f)
        drawLine(subtle.copy(alpha = 0.3f), Offset(cx + w * 0.06f, h * 0.55f), Offset(cx, h * 0.6f), strokeWidth = 2f)
    }
}

@Composable
fun EmptyTaskIllustration(modifier: Modifier = Modifier) {
    val accent = LocalAccentColor.current
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(80.dp)) {
        val w = size.width
        val h = size.height
        val boxSize = w * 0.08f
        val left = w * 0.2f
        val lineStart = left + boxSize + w * 0.06f
        val lineEnd = w * 0.75f

        listOf(0.3f, 0.45f, 0.6f).forEachIndexed { i, yFrac ->
            val y = h * yFrac
            val alpha = if (i == 1) 0.25f else 0.12f
            drawRect(subtle.copy(alpha = alpha), topLeft = Offset(left, y), size = Size(boxSize, boxSize))
            drawLine(subtle.copy(alpha = alpha + 0.05f), Offset(lineStart, y + boxSize / 2), Offset(lineEnd, y + boxSize / 2), strokeWidth = 2f)
        }

        val checkY = h * 0.45f
        val checkLeft = left + boxSize * 0.2f
        val checkMid = left + boxSize * 0.45f
        val checkRight = left + boxSize * 0.85f
        drawLine(accent.copy(alpha = 0.5f), Offset(checkLeft, checkY + boxSize * 0.5f), Offset(checkMid, checkY + boxSize * 0.75f), strokeWidth = 2.5f)
        drawLine(accent.copy(alpha = 0.5f), Offset(checkMid, checkY + boxSize * 0.75f), Offset(checkRight, checkY + boxSize * 0.2f), strokeWidth = 2.5f)
    }
}

@Composable
fun EmptyCommentsIllustration(modifier: Modifier = Modifier) {
    val subtle = MaterialTheme.colorScheme.onSurfaceVariant
    Canvas(modifier = modifier.size(48.dp)) {
        val w = size.width
        val h = size.height
        drawRect(subtle.copy(alpha = 0.12f), topLeft = Offset(w * 0.1f, h * 0.15f), size = Size(w * 0.8f, h * 0.5f))
        val tipX = w * 0.3f
        val tipY = h * 0.65f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(tipX - w * 0.06f, tipY)
            lineTo(tipX, tipY + h * 0.12f)
            lineTo(tipX + w * 0.06f, tipY)
            close()
        }
        drawPath(path, subtle.copy(alpha = 0.12f))
        drawLine(subtle.copy(alpha = 0.2f), Offset(w * 0.2f, h * 0.3f), Offset(w * 0.7f, h * 0.3f), strokeWidth = 1.5f)
        drawLine(subtle.copy(alpha = 0.15f), Offset(w * 0.2f, h * 0.42f), Offset(w * 0.55f, h * 0.42f), strokeWidth = 1.5f)
    }
}
