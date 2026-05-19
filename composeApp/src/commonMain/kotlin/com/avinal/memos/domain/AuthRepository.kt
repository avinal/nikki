package com.avinal.memos.domain

import com.avinal.memos.api.ApiResult
import com.avinal.memos.api.MemosApiClient
import com.avinal.memos.api.model.toDomain
import com.avinal.memos.util.TokenStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class AuthRepository(
    private val apiClient: MemosApiClient,
    private val tokenStore: TokenStore,
) {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    val isLoggedIn: Flow<Boolean> = tokenStore.accessToken.map { it != null }

    suspend fun login(serverUrl: String, token: String): ApiResult<User> {
        val tempClient = MemosApiClient(
            httpClient = com.avinal.memos.api.HttpClientFactory.create { token },
            baseUrlProvider = { serverUrl },
        )

        return when (val result = tempClient.getMe()) {
            is ApiResult.Success -> {
                val user = result.data.toDomain()
                tokenStore.saveCredentials(serverUrl, token)
                _currentUser.value = user
                ApiResult.Success(user)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun validateToken(): ApiResult<User> {
        return when (val result = apiClient.getMe()) {
            is ApiResult.Success -> {
                val user = result.data.toDomain()
                _currentUser.value = user
                ApiResult.Success(user)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun logout() {
        tokenStore.clear()
        _currentUser.value = null
    }
}
