package com.avinal.memos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.domain.AuthRepository
import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoRepository
import com.avinal.memos.domain.MemoVisibility
import com.avinal.memos.domain.User
import com.avinal.memos.ui.theme.MetroTheme
import com.avinal.memos.util.BackupManager
import com.avinal.memos.util.TokenStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    val tokenStore: TokenStore,
    private val memoRepository: MemoRepository,
) : ViewModel() {

    val serverUrl: StateFlow<String?> = tokenStore.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: StateFlow<User?> = authRepository.currentUser

    val currentTheme: StateFlow<String> = tokenStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    val currentAccent: StateFlow<String> = tokenStore.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cobalt")

    val notificationsEnabled: StateFlow<Boolean> = tokenStore.notificationsEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        viewModelScope.launch { authRepository.validateToken() }
    }

    fun setTheme(theme: MetroTheme) {
        viewModelScope.launch { tokenStore.saveTheme(theme.name) }
    }

    fun setAccentColor(name: String) {
        viewModelScope.launch { tokenStore.saveAccentColor(name) }
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { tokenStore.saveNotificationsEnabled(enabled) }
    }

    fun getExportJson(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val memos = memoRepository.observeMemos().first()
            onResult(BackupManager.exportToJson(memos))
        }
    }

    fun importFromJson(json: String, onResult: (Int) -> Unit) {
        viewModelScope.launch {
            val backup = BackupManager.parseFromJson(json)
            if (backup == null) { onResult(-1); return@launch }

            var imported = 0
            backup.memos.forEach { backupMemo ->
                val visibility = MemoVisibility.fromApiString(backupMemo.visibility)
                val result = memoRepository.createMemo(backupMemo.content, visibility)
                if (result is com.avinal.memos.api.ApiResult.Success) imported++
            }
            memoRepository.refreshMemos()
            onResult(imported)
        }
    }

    fun logout() {
        viewModelScope.launch {
            memoRepository.clearCache()
            authRepository.logout()
        }
    }
}
