package com.avinal.memos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.AppVersion
import com.avinal.memos.ui.components.NikkiLogo
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun WelcomeScreen(onNext: () -> Unit) {
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 28.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                NikkiLogo(size = 120f)
                Column {
                    Text(
                        "nikki",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Thin,
                        color = textColor,
                        letterSpacing = (-2).sp,
                        lineHeight = 60.sp,
                    )
                    Text(
                        "日記",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Light,
                        color = subtleColor,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "a native client for memos\nwith task management built in",
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                color = subtleColor,
                lineHeight = 22.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        // version + server compat, bottom-left
        Text(
            "v${AppVersion.NAME}  ·  memos ≥26.10",
            fontSize = 11.sp,
            color = subtleColor.copy(alpha = 0.5f),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 28.dp, bottom = 20.dp),
        )

        // next button, bottom-right
        Text(
            "next",
            fontSize = 18.sp,
            fontWeight = FontWeight.Light,
            color = accent,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 28.dp, bottom = 20.dp)
                .clickable { onNext() },
        )
    }
}
