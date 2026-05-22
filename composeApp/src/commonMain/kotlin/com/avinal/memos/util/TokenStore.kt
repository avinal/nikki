package com.avinal.memos.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@OptIn(ExperimentalEncodingApi::class)
class TokenStore(private val dataStore: DataStore<Preferences>) {

    val serverUrl: Flow<String?> = dataStore.data.map { it[KEY_SERVER_URL]?.let(::readSecure) }
    val accessToken: Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN]?.let(::readSecure) }
    val theme: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: "DARK" }
    val accentColor: Flow<String> = dataStore.data.map { it[KEY_ACCENT] ?: "Cobalt" }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { (it[KEY_NOTIFICATIONS] ?: "true") == "true" }
    val defaultVisibility: Flow<String> = dataStore.data.map { it[KEY_DEFAULT_VIS] ?: "PRIVATE" }
    val defaultReminder: Flow<String> = dataStore.data.map { it[KEY_DEFAULT_REMINDER] ?: "" }
    val weekStartDay: Flow<Int> = dataStore.data.map { (it[KEY_WEEK_START] ?: "0").toIntOrNull() ?: 0 }
    val defaultNotifyTime: Flow<String> = dataStore.data.map { it[KEY_DEFAULT_NOTIFY_TIME] ?: "20:00" }
    val syncInterval: Flow<Int> = dataStore.data.map { (it[KEY_SYNC_INTERVAL] ?: "5").toIntOrNull() ?: 5 }

    suspend fun saveCredentials(serverUrl: String, token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = writeSecure(serverUrl)
            prefs[KEY_ACCESS_TOKEN] = writeSecure(token)
        }
    }

    suspend fun saveTheme(theme: String) {
        dataStore.edit { it[KEY_THEME] = theme }
    }

    suspend fun saveAccentColor(name: String) {
        dataStore.edit { it[KEY_ACCENT] = name }
    }

    suspend fun saveNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_NOTIFICATIONS] = enabled.toString() }
    }

    suspend fun saveDefaultVisibility(vis: String) {
        dataStore.edit { it[KEY_DEFAULT_VIS] = vis }
    }

    suspend fun saveDefaultReminder(reminder: String) {
        dataStore.edit { it[KEY_DEFAULT_REMINDER] = reminder }
    }

    suspend fun saveWeekStartDay(day: Int) {
        dataStore.edit { it[KEY_WEEK_START] = day.toString() }
    }

    suspend fun saveDefaultNotifyTime(time: String) {
        dataStore.edit { it[KEY_DEFAULT_NOTIFY_TIME] = time }
    }

    suspend fun saveSyncInterval(minutes: Int) {
        dataStore.edit { it[KEY_SYNC_INTERVAL] = minutes.toString() }
    }

    suspend fun clear() {
        dataStore.edit {
            val theme = it[KEY_THEME]
            val accent = it[KEY_ACCENT]
            it.clear()
            if (theme != null) it[KEY_THEME] = theme
            if (accent != null) it[KEY_ACCENT] = accent
        }
    }

    private fun writeSecure(value: String): String {
        val key = OBFUSCATION_KEY
        val xored = value.encodeToByteArray().mapIndexed { i, b ->
            (b.toInt() xor key[i % key.length].code).toByte()
        }.toByteArray()
        return SECURE_PREFIX + Base64.encode(xored)
    }

    private fun readSecure(stored: String): String {
        if (!stored.startsWith(SECURE_PREFIX)) return stored
        val key = OBFUSCATION_KEY
        val xored = Base64.decode(stored.removePrefix(SECURE_PREFIX))
        return String(xored.mapIndexed { i, b ->
            (b.toInt() xor key[i % key.length].code).toByte()
        }.toByteArray())
    }

    companion object {
        private const val SECURE_PREFIX = "OBF:"
        private const val OBFUSCATION_KEY = "nikki-credential-obfuscation"
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_ACCENT = stringPreferencesKey("accent_color")
        private val KEY_NOTIFICATIONS = stringPreferencesKey("notifications_enabled")
        private val KEY_DEFAULT_VIS = stringPreferencesKey("default_visibility")
        private val KEY_DEFAULT_REMINDER = stringPreferencesKey("default_reminder")
        private val KEY_WEEK_START = stringPreferencesKey("week_start_day")
        private val KEY_DEFAULT_NOTIFY_TIME = stringPreferencesKey("default_notify_time")
        private val KEY_SYNC_INTERVAL = stringPreferencesKey("sync_interval")
    }
}
