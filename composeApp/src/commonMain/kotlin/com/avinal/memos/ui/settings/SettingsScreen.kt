package com.avinal.memos.ui.settings

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.avinal.memos.AppDependencies
import com.avinal.memos.ui.theme.LocalAccentColor
import com.avinal.memos.ui.theme.MetroTheme
import com.avinal.memos.ui.theme.WpAccentColors

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
    var showLogoutDialog by remember { mutableStateOf(false) }

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 24.dp, end = 24.dp, top = 6.dp, bottom = 24.dp),
    ) {
        SectionHeader("account")
        SettingsItem("server", serverUrl ?: "not connected")
        currentUser?.let { user ->
            SettingsItem("username", user.username)
            if (user.nickname.isNotEmpty()) SettingsItem("name", user.nickname)
        }

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
                            if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onBackground, CircleShape)
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
                    color = if (isSelected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clickable { viewModel.setTheme(theme) },
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        SectionHeader("memos")
        Spacer(Modifier.height(6.dp))

        val defaultVis by viewModel.defaultVisibility.collectAsState()
        SettingToggle("default visibility", defaultVis.lowercase(), accent, MaterialTheme.colorScheme.onSurfaceVariant) {
            val next = when (defaultVis) { "PRIVATE" -> "PROTECTED"; "PROTECTED" -> "PUBLIC"; else -> "PRIVATE" }
            viewModel.setDefaultVisibility(next)
        }

        val defaultReminder by viewModel.defaultReminder.collectAsState()
        SettingToggle("default reminder", defaultReminder.ifEmpty { "none" }, accent, MaterialTheme.colorScheme.onSurfaceVariant) {
            val options = listOf("", "15min", "30min", "1hr", "1day")
            val idx = options.indexOf(defaultReminder)
            viewModel.setDefaultReminder(options[(idx + 1) % options.size])
        }

        val weekStart by viewModel.weekStartDay.collectAsState()
        val dayNames = listOf("sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday")
        SettingToggle("week starts on", dayNames[weekStart], accent, MaterialTheme.colorScheme.onSurfaceVariant) {
            viewModel.setWeekStartDay((weekStart + 1) % 7)
        }

        val syncInterval by viewModel.syncInterval.collectAsState()
        SettingToggle("auto sync", "${syncInterval} min", accent, MaterialTheme.colorScheme.onSurfaceVariant) {
            val options = listOf(1, 2, 5, 10, 15, 30, 60)
            val idx = options.indexOf(syncInterval)
            viewModel.setSyncInterval(options[(idx + 1) % options.size])
        }
        Text("how often to fetch from server", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(24.dp))
        SectionHeader("notifications")
        Spacer(Modifier.height(6.dp))

        val notificationsOn by viewModel.notificationsEnabled.collectAsState()
        Row(
            modifier = Modifier.fillMaxWidth().clickable { viewModel.setNotificationsEnabled(!notificationsOn) }.padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("task reminders", fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
            Text(
                if (notificationsOn) "on" else "off",
                fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                color = if (notificationsOn) accent else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("get notified when tasks are due or overdue", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        val defaultNotifyTime by viewModel.defaultNotifyTime.collectAsState()
        SettingToggle("default notify time", defaultNotifyTime, accent, MaterialTheme.colorScheme.onSurfaceVariant) {
            val options = listOf("08:00", "09:00", "12:00", "17:00", "18:00", "20:00", "21:00")
            val idx = options.indexOf(defaultNotifyTime)
            viewModel.setDefaultNotifyTime(options[(idx + 1) % options.size])
        }
        Text("when a task has a date but no time", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Text(
            "check reminders now",
            fontSize = 13.sp, color = accent,
            modifier = Modifier.clickable {
                com.avinal.memos.util.triggerReminderCheck()
            }.padding(vertical = 4.dp),
        )

        Spacer(Modifier.height(24.dp))
        SectionHeader("backup")
        Spacer(Modifier.height(6.dp))

        var backupStatus by remember { mutableStateOf("") }

        val saveFile = com.avinal.memos.util.rememberFileSaver { success ->
            backupStatus = if (success) "backup exported" else "export failed"
        }
        val loadFile = com.avinal.memos.util.rememberFileLoader { json ->
            viewModel.importFromJson(json) { count ->
                backupStatus = if (count >= 0) "$count memos imported" else "invalid backup file"
            }
        }

        Text(
            "export backup",
            fontSize = 15.sp, color = accent,
            modifier = Modifier.clickable {
                viewModel.getExportJson { json ->
                    saveFile("memos-backup.json", json)
                }
            }.padding(vertical = 6.dp),
        )
        Text(
            "import backup",
            fontSize = 15.sp, color = accent,
            modifier = Modifier.clickable { loadFile() }.padding(vertical = 6.dp),
        )
        if (backupStatus.isNotEmpty()) {
            Text(backupStatus, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
        }

        Spacer(Modifier.height(36.dp))
        SectionHeader("about")
        SettingsItem("version", "1.0.0")

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
private fun SettingsItem(label: String, value: String) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}

@Composable
private fun SettingToggle(label: String, value: String, accent: androidx.compose.ui.graphics.Color, subtleColor: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 15.sp, color = MaterialTheme.colorScheme.onBackground)
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = accent)
    }
}
