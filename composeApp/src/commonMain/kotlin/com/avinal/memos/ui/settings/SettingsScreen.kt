package com.avinal.memos.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.ui.theme.MetroTheme
import com.avinal.memos.ui.theme.WpAccentColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsScreen(
    deps: AppDependencies,
    onLogout: () -> Unit,
) {
    val viewModel = viewModel { SettingsViewModel(deps.authRepository, deps.tokenStore, deps.memoRepository) }
    val serverUrl by viewModel.serverUrl.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val currentTheme by viewModel.currentTheme.collectAsState()
    val currentAccent by viewModel.currentAccent.collectAsState()
    val accent = LocalAccentColor.current
    val textColor = MaterialTheme.colorScheme.onBackground
    val subtleColor = MaterialTheme.colorScheme.onSurfaceVariant
    var showLogoutDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("sign out?") },
            confirmButton = {
                TextButton(onClick = { showLogoutDialog = false; viewModel.logout(); onLogout() }) {
                    Text("sign out", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) { Text("cancel") }
            },
        )
    }

    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("clear local cache?") },
            text = { Text("all memos will be re-fetched from the server.", color = subtleColor) },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            confirmButton = {
                TextButton(onClick = {
                    showClearCacheDialog = false
                    scope.launch {
                        deps.memoRepository.clearCache()
                        deps.memoRepository.refreshMemos()
                    }
                }) { Text("clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) { Text("cancel") }
            },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            AppLogo(size = 96f)
            Column {
                Text("nikki", fontSize = 24.sp, fontWeight = FontWeight.Light, color = textColor)
                Text("v${com.avinal.memos.AppVersion.NAME}", fontSize = 12.sp, color = subtleColor)
                Spacer(Modifier.height(4.dp))
                Text("a memos client with todoist-style tasks", fontSize = 13.sp, color = subtleColor)
                Spacer(Modifier.height(2.dp))
                Text("by avinal kumar", fontSize = 12.sp, color = subtleColor)
            }
        }
        Text(
            "report issues",
            fontSize = 13.sp,
            color = accent,
            modifier = Modifier
                .clickable { uriHandler.openUri("https://github.com/avinal/nikki/issues") }
                .padding(vertical = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("account")

        currentUser?.let { user ->
            Text(user.username, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = textColor)
            if (user.nickname.isNotEmpty()) {
                Text(user.nickname, fontSize = 14.sp, color = subtleColor)
            }
            Spacer(Modifier.height(4.dp))
        }
        Text(serverUrl ?: "not connected", fontSize = 13.sp, color = subtleColor)

        Spacer(Modifier.height(12.dp))
        Text(
            "switch server",
            fontSize = 14.sp, color = accent,
            modifier = Modifier.clickable { showLogoutDialog = true }.padding(vertical = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("accent color")
        Spacer(Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            WpAccentColors.forEach { ac ->
                val isSelected = ac.name == currentAccent
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ac.color)
                        .then(
                            if (isSelected) Modifier.border(3.dp, textColor, CircleShape)
                            else Modifier
                        )
                        .clickable { viewModel.setAccentColor(ac.name) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("theme")
        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetroTheme.entries.forEach { theme ->
                val isSelected = currentTheme == theme.label
                Text(
                    text = theme.label,
                    fontSize = 15.sp,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) accent else subtleColor,
                    modifier = Modifier.clickable { viewModel.setTheme(theme) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("memos")
        Spacer(Modifier.height(6.dp))

        val defaultVis by viewModel.defaultVisibility.collectAsState()
        SettingToggle("default visibility", defaultVis.lowercase(), accent, subtleColor) {
            val next = when (defaultVis) { "PRIVATE" -> "PROTECTED"; "PROTECTED" -> "PUBLIC"; else -> "PRIVATE" }
            viewModel.setDefaultVisibility(next)
        }

        val defaultReminder by viewModel.defaultReminder.collectAsState()
        SettingToggle("default reminder", defaultReminder.ifEmpty { "none" }, accent, subtleColor) {
            val options = listOf("", "15min", "30min", "1hr", "1day")
            val idx = options.indexOf(defaultReminder)
            viewModel.setDefaultReminder(options[(idx + 1) % options.size])
        }

        val weekStart by viewModel.weekStartDay.collectAsState()
        val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
        SettingToggle("week starts on", dayNames[weekStart], accent, subtleColor) {
            viewModel.setWeekStartDay((weekStart + 1) % 7)
        }

        val syncInterval by viewModel.syncInterval.collectAsState()
        SettingToggle("auto sync", "${syncInterval} min", accent, subtleColor) {
            val options = listOf(1, 2, 5, 10, 15, 30, 60)
            val idx = options.indexOf(syncInterval)
            viewModel.setSyncInterval(options[(idx + 1) % options.size])
        }
        Text("how often to fetch from server", fontSize = 12.sp, color = subtleColor)

        val autoArchive by viewModel.autoArchiveCompletedTasks.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setAutoArchiveCompletedTasks(!autoArchive) }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("auto-archive completed", fontSize = 15.sp, color = textColor)
            Text(
                if (autoArchive) "on" else "off",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (autoArchive) accent else subtleColor,
            )
        }
        Text("archive memos when all tasks are done", fontSize = 12.sp, color = subtleColor)

        Spacer(Modifier.height(24.dp))
        SectionHeader("notifications")
        Spacer(Modifier.height(6.dp))

        val notificationsOn by viewModel.notificationsEnabled.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setNotificationsEnabled(!notificationsOn) }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("task reminders", fontSize = 15.sp, color = textColor)
            Text(
                if (notificationsOn) "on" else "off",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (notificationsOn) accent else subtleColor,
            )
        }
        Text("get notified when tasks are due or overdue", fontSize = 12.sp, color = subtleColor)

        val defaultNotifyTime by viewModel.defaultNotifyTime.collectAsState()
        SettingToggle("default notify time", defaultNotifyTime, accent, subtleColor) {
            val options = listOf("08:00", "09:00", "12:00", "17:00", "18:00", "20:00", "21:00")
            val idx = options.indexOf(defaultNotifyTime)
            viewModel.setDefaultNotifyTime(options[(idx + 1) % options.size])
        }
        Text("when a task has a date but no time", fontSize = 12.sp, color = subtleColor)

        Text(
            "check reminders now",
            fontSize = 13.sp, color = accent,
            modifier = Modifier.clickable {
                com.avinal.memos.util.triggerReminderCheck()
            }.padding(vertical = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("data")
        Spacer(Modifier.height(6.dp))

        Text(
            "clear local cache",
            fontSize = 15.sp, color = accent,
            modifier = Modifier.clickable { showClearCacheDialog = true }.padding(vertical = 6.dp),
        )
        Text("re-fetch all memos from server", fontSize = 12.sp, color = subtleColor)

        Spacer(Modifier.height(36.dp))

        Text(
            "sign out",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.clickable { showLogoutDialog = true },
        )
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontSize = 24.sp,
        fontWeight = FontWeight.Light,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun AppLogo(size: Float) {
    val pink = Color(0xFFEE67A4)
    val black = Color(0xFF231F20)
    val teal = Color(0xFF35BEB8)

    Canvas(modifier = Modifier.size(size.dp)) {
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

private fun DrawScope.drawAnnularSector(
    cx: Float, cy: Float,
    innerR: Float, outerR: Float,
    startAngle: Float, sweepAngle: Float,
    color: Color,
) {
    val path = Path().apply {
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - outerR, cy - outerR, cx + outerR, cy + outerR),
            startAngleDegrees = startAngle,
            sweepAngleDegrees = sweepAngle,
            forceMoveTo = true,
        )
        arcTo(
            rect = androidx.compose.ui.geometry.Rect(cx - innerR, cy - innerR, cx + innerR, cy + innerR),
            startAngleDegrees = startAngle + sweepAngle,
            sweepAngleDegrees = -sweepAngle,
            forceMoveTo = false,
        )
        close()
    }
    drawPath(path, color)
}

@Composable
private fun SettingToggle(label: String, value: String, accent: Color, subtleColor: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}
