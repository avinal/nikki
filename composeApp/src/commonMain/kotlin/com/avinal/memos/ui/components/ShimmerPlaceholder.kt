package com.avinal.memos.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerEffect(modifier: Modifier = Modifier) {
    val shimmerColors = listOf(
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    )
    val transition = rememberInfiniteTransition()
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 300f, 0f),
        end = Offset(translateAnim, 0f),
    )
    Box(modifier = modifier.background(brush))
}

@Composable
fun MemoCardPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 14.dp, bottom = 14.dp)) {
        ShimmerEffect(Modifier.fillMaxWidth(0.3f).height(12.dp))
        Spacer(Modifier.height(10.dp))
        ShimmerEffect(Modifier.fillMaxWidth().height(14.dp))
        Spacer(Modifier.height(6.dp))
        ShimmerEffect(Modifier.fillMaxWidth(0.8f).height(14.dp))
        Spacer(Modifier.height(6.dp))
        ShimmerEffect(Modifier.fillMaxWidth(0.5f).height(14.dp))
    }
    Spacer(
        Modifier.fillMaxWidth().height(1.dp).padding(start = 24.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    )
}

@Composable
fun MemoDetailPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ShimmerEffect(Modifier.width(60.dp).height(12.dp))
            ShimmerEffect(Modifier.width(80.dp).height(12.dp))
        }
        Spacer(Modifier.height(14.dp))
        repeat(5) { i ->
            ShimmerEffect(Modifier.fillMaxWidth(if (i == 4) 0.4f else 1f).height(16.dp))
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun TaskRowPlaceholder() {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ShimmerEffect(Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            ShimmerEffect(Modifier.fillMaxWidth(0.7f).height(15.dp))
            Spacer(Modifier.height(4.dp))
            ShimmerEffect(Modifier.fillMaxWidth(0.3f).height(12.dp))
        }
    }
}

@Composable
fun TaskGroupPlaceholder() {
    Column {
        ShimmerEffect(Modifier.fillMaxWidth(0.4f).height(19.dp).padding(start = 24.dp, top = 14.dp))
        Spacer(Modifier.height(8.dp))
        repeat(3) {
            TaskRowPlaceholder()
            Spacer(Modifier.height(4.dp))
        }
    }
}

@Composable
fun CommentPlaceholder() {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ShimmerEffect(Modifier.width(60.dp).height(12.dp))
            ShimmerEffect(Modifier.width(80.dp).height(12.dp))
        }
        Spacer(Modifier.height(4.dp))
        ShimmerEffect(Modifier.fillMaxWidth(0.6f).height(14.dp))
    }
}
