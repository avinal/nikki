package com.avinal.memos.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.avinal.memos.domain.AuthRepository
import com.avinal.memos.domain.MemoRepository
import com.avinal.memos.domain.User
import com.avinal.memos.ui.theme.MetroTheme
import com.avinal.memos.util.TokenStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val tokenStore: TokenStore,
    private val memoRepository: MemoRepository,
) : ViewModel() {

    val serverUrl: StateFlow<String?> = tokenStore.serverUrl
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentUser: StateFlow<User?> = authRepository.currentUser

    val currentTheme: StateFlow<String> = tokenStore.theme
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "DARK")

    val currentAccent: StateFlow<String> = tokenStore.accentColor
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Cobalt")

    init {
        viewModelScope.launch { authRepository.validateToken() }
    }

    fun setTheme(theme: MetroTheme) {
        viewModelScope.launch { tokenStore.saveTheme(theme.name) }
    }

    fun setAccentColor(name: String) {
        viewModelScope.launch { tokenStore.saveAccentColor(name) }
    }

    fun logout() {
        viewModelScope.launch {
            memoRepository.clearCache()
            authRepository.logout()
        }
    }
}
