package com.avinal.memos

import com.avinal.memos.api.HttpClientFactory
import com.avinal.memos.api.MemosApiClient
import com.avinal.memos.db.MemosDatabase
import com.avinal.memos.db.createPlatformDatabase
import com.avinal.memos.domain.AuthRepository
import com.avinal.memos.domain.MemoRepository
import com.avinal.memos.util.TokenStore
import com.avinal.memos.util.createDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import com.avinal.memos.db.entity.toDomain

class AppDependencies(
    dataStorePath: String,
    platformContext: Any,
) {
    @Volatile private var cachedToken: String? = null
    @Volatile private var cachedServerUrl: String? = null

    private val dataStore = createDataStore(dataStorePath)
    val tokenStore = TokenStore(dataStore)

    val database: MemosDatabase by lazy { createPlatformDatabase(platformContext) }

    val httpClient by lazy {
        HttpClientFactory.create { cachedToken }
    }

    val apiClient: MemosApiClient by lazy {
        MemosApiClient(
            httpClient = httpClient,
            baseUrlProvider = { cachedServerUrl ?: "" },
        )
    }

    val authRepository: AuthRepository by lazy { AuthRepository(apiClient, tokenStore) }
    val memoRepository: MemoRepository by lazy {
        MemoRepository(apiClient, database.memoDao()) {
            com.avinal.memos.util.triggerReminderCheck()
        }
    }

    private var initJob: kotlinx.coroutines.Job? = null

    fun initialize() {
        initJob?.cancel()
        initJob = CoroutineScope(Dispatchers.IO).launch {
            launch { tokenStore.accessToken.collect { cachedToken = it } }
            launch { tokenStore.serverUrl.collect { cachedServerUrl = it } }
            launch { tokenStore.syncInterval.collect { memoRepository.syncIntervalMinutes = it } }
            launch { initializeLiveMemosProvider() }
        }
    }

    private suspend fun initializeLiveMemosProvider() {
        try {
            val dao = database.memoDao()
            com.avinal.memos.util.setLiveMemosProvider {
                runBlocking { dao.getAll().map { it.toDomain() } }
            }
        } catch (_: Exception) {}
    }
}
