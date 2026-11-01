package com.avinal.memos.ui.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.avinal.memos.ui.theme.LocalAccentColor

@Composable
fun PermissionsScreen(onBack: () -> Unit, onNext: () -> Unit) {
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant

    var notificationGranted by remember { mutableStateOf(checkNotificationPermission()) }
    var batteryGranted by remember { mutableStateOf(checkBatteryExemption()) }

    val permissionRequester = rememberPermissionRequester(
        onNotificationResult = { granted -> notificationGranted = granted },
        onBatteryResult = { granted -> batteryGranted = granted },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 28.dp),
    ) {
        Spacer(Modifier.height(48.dp))

        Text(
            "permissions",
            fontSize = 42.sp,
            fontWeight = FontWeight.Thin,
            color = textColor,
            letterSpacing = (-1).sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "all optional — you can change these later in settings.",
            fontSize = 14.sp,
            fontWeight = FontWeight.Light,
            color = subtleColor,
            lineHeight = 20.sp,
        )

        Spacer(Modifier.height(44.dp))

        PermissionRow(
            name = "notifications",
            reason = "get reminded when tasks are due",
            tag = "recommended",
            granted = notificationGranted,
            accent = accent,
            subtleColor = subtleColor,
            textColor = textColor,
            onGrant = { permissionRequester.requestNotification() },
        )

        Spacer(Modifier.height(28.dp))

        PermissionRow(
            name = "battery",
            reason = "ensure reminders fire even when the app is in background",
            tag = "recommended",
            granted = batteryGranted,
            accent = accent,
            subtleColor = subtleColor,
            textColor = textColor,
            onGrant = { permissionRequester.requestBatteryExemption() },
        )

        Spacer(Modifier.height(28.dp))

        PermissionRow(
            name = "calendar",
            reason = "schedule reminders as calendar events for reliability",
            tag = "optional",
            granted = false,
            accent = subtleColor,
            subtleColor = subtleColor,
            textColor = textColor,
            onGrant = null,
            grantLabel = "asked when first used",
        )

        Spacer(Modifier.weight(1f))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "back",
                fontSize = 15.sp,
                fontWeight = FontWeight.Light,
                color = subtleColor,
                modifier = Modifier.clickable { onBack() },
            )
            Spacer(Modifier.weight(1f))
            Text(
                "next",
                fontSize = 18.sp,
                fontWeight = FontWeight.Light,
                color = accent,
                modifier = Modifier.clickable { onNext() },
            )
        }
    }
}

@Composable
private fun PermissionRow(
    name: String,
    reason: String,
    tag: String,
    granted: Boolean,
    accent: Color,
    textColor: Color,
    subtleColor: Color,
    onGrant: (() -> Unit)?,
    grantLabel: String = "grant",
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, fontSize = 17.sp, fontWeight = FontWeight.Normal, color = textColor)
                Spacer(Modifier.width(10.dp))
                Text(
                    tag,
                    fontSize = 11.sp,
                    color = subtleColor,
                    fontWeight = FontWeight.Light,
                )
            }
            Spacer(Modifier.height(3.dp))
            Text(reason, fontSize = 13.sp, color = subtleColor, fontWeight = FontWeight.Light, lineHeight = 18.sp)
        }

        Spacer(Modifier.width(16.dp))

        // fixed-width slot so layout doesn't shift when granted text appears
        if (granted) {
            Text(
                "granted",
                fontSize = 14.sp,
                color = accent,
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 2.dp),
            )
        } else if (onGrant != null) {
            Text(
                grantLabel,
                fontSize = 14.sp,
                color = accent,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .clickable { onGrant() }
                    .padding(top = 2.dp),
            )
        } else {
            Text(
                grantLabel,
                fontSize = 14.sp,
                color = subtleColor.copy(alpha = 0.5f),
                fontWeight = FontWeight.Light,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
    }
}
