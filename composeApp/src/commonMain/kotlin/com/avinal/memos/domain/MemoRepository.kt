package com.avinal.memos.domain

import com.avinal.memos.api.ApiResult
import com.avinal.memos.api.MemosApiClient
import com.avinal.memos.api.model.toDomain
import com.avinal.memos.db.dao.MemoDao
import com.avinal.memos.db.entity.toEntity
import com.avinal.memos.db.entity.toDomain
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class MemoRepository(
    private val apiClient: MemosApiClient,
    private val memoDao: MemoDao,
) {
    private var nextPageToken: String = ""
    private var hasMorePages: Boolean = true
    private var lastFetchTime: Long = 0L

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun isCacheStale(): Boolean = (nowMillis() - lastFetchTime) > CACHE_TTL_MS

    fun observeMemos(): Flow<List<Memo>> {
        if (isCacheStale()) {
            CoroutineScope(Dispatchers.IO).launch { refreshMemos() }
        }
        return memoDao.observeAll().map { entities -> entities.map { it.toDomain() } }
    }

    fun observeMemo(id: String): Flow<Memo?> =
        memoDao.observeById(id).map { it?.toDomain() }

    suspend fun getMemo(id: String): Memo? {
        val cached = memoDao.getById(id)
        if (cached != null) return cached.toDomain()
        return when (val result = apiClient.getMemo(id)) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.upsert(memo.toEntity(nowMillis()))
                memo
            }
            else -> null
        }
    }

    suspend fun refreshMemos(): ApiResult<List<Memo>> {
        nextPageToken = ""
        hasMorePages = true
        return when (val result = apiClient.listMemos(pageSize = 50)) {
            is ApiResult.Success -> {
                val memos = result.data.memos.map { it.toDomain() }
                val now = nowMillis()
                lastFetchTime = now
                memoDao.upsertAll(memos.map { it.toEntity(now) })
                nextPageToken = result.data.nextPageToken
                hasMorePages = nextPageToken.isNotEmpty()
                ApiResult.Success(memos)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun loadNextPage(): ApiResult<List<Memo>> {
        if (!hasMorePages) return ApiResult.Success(emptyList())
        return when (val result = apiClient.listMemos(pageSize = 50, pageToken = nextPageToken)) {
            is ApiResult.Success -> {
                val memos = result.data.memos.map { it.toDomain() }
                val now = nowMillis()
                memoDao.upsertAll(memos.map { it.toEntity(now) })
                nextPageToken = result.data.nextPageToken
                hasMorePages = nextPageToken.isNotEmpty()
                ApiResult.Success(memos)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun createMemo(
        content: String,
        visibility: MemoVisibility = MemoVisibility.PRIVATE,
    ): ApiResult<Memo> {
        return when (val result = apiClient.createMemo(content, visibility.toApiString())) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.upsert(memo.toEntity(nowMillis()))
                ApiResult.Success(memo)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun updateMemo(
        id: String,
        content: String? = null,
        visibility: MemoVisibility? = null,
        pinned: Boolean? = null,
    ): ApiResult<Memo> {
        return when (val result = apiClient.updateMemo(
            id = id,
            content = content,
            visibility = visibility?.toApiString(),
            pinned = pinned,
        )) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.upsert(memo.toEntity(nowMillis()))
                ApiResult.Success(memo)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun reactToMemo(memoId: String, emoji: String): ApiResult<Unit> {
        return when (apiClient.upsertReaction(memoId, emoji)) {
            is ApiResult.Success -> {
                refreshMemos()
                ApiResult.Success(Unit)
            }
            is ApiResult.Error -> ApiResult.Error(0, "Failed to react")
            is ApiResult.NetworkError -> ApiResult.NetworkError(Exception("Network error"))
        }
    }

    suspend fun archiveMemo(id: String): ApiResult<Memo> {
        return when (val result = apiClient.updateMemo(id = id, state = "ARCHIVED")) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.deleteById(id)
                ApiResult.Success(memo)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun deleteMemo(id: String): ApiResult<Unit> {
        return when (val result = apiClient.deleteMemo(id)) {
            is ApiResult.Success -> {
                memoDao.deleteById(id)
                result
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun clearCache() {
        memoDao.deleteAll()
        lastFetchTime = 0L
        nextPageToken = ""
        hasMorePages = true
    }

    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L
    }
}
