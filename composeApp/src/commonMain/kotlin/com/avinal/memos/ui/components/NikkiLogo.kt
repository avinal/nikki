package com.avinal.memos.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp

@Composable
fun NikkiLogo(size: Float, modifier: Modifier = Modifier) {
    val pink = Color(0xFFEE67A4)
    val black = Color(0xFF231F20)
    val teal = Color(0xFF35BEB8)

    Canvas(modifier = modifier.size(size.dp)) {
        val cx = this.size.width / 2f
        val cy = this.size.height / 2f
        val scale = this.size.width / 108f
        val rCircle = 28f * scale
        val rInner = 20f * scale
        val rOuter = 35f * scale
        val halfAngle = 23.5f

        drawCircle(pink, radius = rCircle, center = Offset(cx, cy))
        drawAnnularSector(cx, cy, rInner, rCircle, -halfAngle, halfAngle * 2f, black)
        drawAnnularSector(cx, cy, rCircle, rOuter, -halfAngle, halfAngle * 2f, teal)
    }
}

fun DrawScope.drawAnnularSector(
    cx: Float, cy: Float,
    innerR: Float, outerR: Float,
    startAngle: Float, sweepAngle: Float,
    color: Color,
) {
    val path = Path().apply {
        arcTo(Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR), startAngle, sweepAngle, true)
        arcTo(Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR), startAngle + sweepAngle, -sweepAngle, false)
        close()
    }
    drawPath(path, color)
}
