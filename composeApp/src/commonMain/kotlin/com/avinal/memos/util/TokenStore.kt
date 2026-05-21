package com.avinal.memos.util

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TokenStore(private val dataStore: DataStore<Preferences>) {

    val serverUrl: Flow<String?> = dataStore.data.map { it[KEY_SERVER_URL] }
    val accessToken: Flow<String?> = dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    val theme: Flow<String> = dataStore.data.map { it[KEY_THEME] ?: "DARK" }
    val accentColor: Flow<String> = dataStore.data.map { it[KEY_ACCENT] ?: "Cobalt" }
    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { (it[KEY_NOTIFICATIONS] ?: "true") == "true" }

    suspend fun saveCredentials(serverUrl: String, token: String) {
        dataStore.edit { prefs ->
            prefs[KEY_SERVER_URL] = serverUrl
            prefs[KEY_ACCESS_TOKEN] = token
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

    suspend fun clear() {
        dataStore.edit {
            val theme = it[KEY_THEME]
            val accent = it[KEY_ACCENT]
            it.clear()
            if (theme != null) it[KEY_THEME] = theme
            if (accent != null) it[KEY_ACCENT] = accent
        }
    }

    companion object {
        private val KEY_SERVER_URL = stringPreferencesKey("server_url")
        private val KEY_ACCESS_TOKEN = stringPreferencesKey("access_token")
        private val KEY_THEME = stringPreferencesKey("app_theme")
        private val KEY_ACCENT = stringPreferencesKey("accent_color")
        private val KEY_NOTIFICATIONS = stringPreferencesKey("notifications_enabled")
    }
}
