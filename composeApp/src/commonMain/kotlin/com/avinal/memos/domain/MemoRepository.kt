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
    private val onContentChanged: (() -> Unit)? = null,
) {
    private var nextPageToken: String = ""
    private var hasMorePages: Boolean = true
    private var lastFetchTime: Long = 0L
    var syncIntervalMinutes: Int = 5

    private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private fun isCacheStale(): Boolean = (nowMillis() - lastFetchTime) > (syncIntervalMinutes * 60 * 1000L)

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
        val allFetched = mutableListOf<Memo>()

        var token = ""
        do {
            val result = apiClient.listMemos(pageSize = 50, pageToken = token)
            when (result) {
                is ApiResult.Success -> {
                    allFetched.addAll(result.data.memos.map { it.toDomain() })
                    token = result.data.nextPageToken
                }
                is ApiResult.Error -> return result
                is ApiResult.NetworkError -> return result
            }
        } while (token.isNotEmpty())

        val now = nowMillis()
        lastFetchTime = now
        memoDao.deleteAll()
        memoDao.upsertAll(allFetched.map { it.toEntity(now) })
        nextPageToken = ""
        hasMorePages = false
        return ApiResult.Success(allFetched)
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
        attachmentNames: List<String> = emptyList(),
    ): ApiResult<Memo> {
        return when (val result = apiClient.createMemo(content, visibility.toApiString(), attachmentNames)) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.upsert(memo.toEntity(nowMillis()))
                onContentChanged?.invoke()
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
                onContentChanged?.invoke()
                ApiResult.Success(memo)
            }
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun uploadAttachment(filename: String, mimeType: String, contentBase64: String): ApiResult<String> {
        return when (val result = apiClient.uploadAttachment(filename, mimeType, contentBase64)) {
            is ApiResult.Success -> ApiResult.Success(result.data.name)
            is ApiResult.Error -> result
            is ApiResult.NetworkError -> result
        }
    }

    suspend fun reactToMemo(memoId: String, emoji: String): ApiResult<Unit> {
        return when (apiClient.upsertReaction(memoId, emoji)) {
            is ApiResult.Success -> {
                when (val memoResult = apiClient.getMemo(memoId)) {
                    is ApiResult.Success -> {
                        val memo = memoResult.data.toDomain()
                        memoDao.upsert(memo.toEntity(nowMillis()))
                    }
                    else -> {}
                }
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

    suspend fun restoreMemo(id: String): ApiResult<Memo> {
        return when (val result = apiClient.updateMemo(id = id, state = "NORMAL")) {
            is ApiResult.Success -> {
                val memo = result.data.toDomain()
                memoDao.upsert(memo.toEntity(nowMillis()))
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
}
