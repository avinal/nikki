package com.avinal.memos.api

import com.avinal.memos.api.model.CreateMemoRequest
import com.avinal.memos.api.model.FieldMask
import com.avinal.memos.api.model.ListMemosResponse
import com.avinal.memos.api.model.MemoDto
import com.avinal.memos.api.model.ReactionDto
import com.avinal.memos.api.model.UpsertReactionRequest
import com.avinal.memos.api.model.UpdateMemoBody
import com.avinal.memos.api.model.UpdateMemoRequest
import com.avinal.memos.api.model.UserDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess

class MemosApiClient(
    private val httpClient: HttpClient,
    val baseUrlProvider: () -> String,
) {
    private fun url(path: String): String = "${baseUrlProvider().trimEnd('/')}/api/v1$path"

    suspend fun getMe(): ApiResult<UserDto> = apiCall {
        httpClient.get(url("/auth/me")).body()
    }

    suspend fun listMemos(
        pageSize: Int = 20,
        pageToken: String = "",
        filter: String = "",
    ): ApiResult<ListMemosResponse> = apiCall {
        httpClient.get(url("/memos")) {
            parameter("pageSize", pageSize)
            if (pageToken.isNotEmpty()) parameter("pageToken", pageToken)
            if (filter.isNotEmpty()) parameter("filter", filter)
        }.body()
    }

    suspend fun getMemo(id: String): ApiResult<MemoDto> = apiCall {
        httpClient.get(url("/memos/$id")).body()
    }

    suspend fun createMemo(
        content: String,
        visibility: String = "PRIVATE",
    ): ApiResult<MemoDto> = apiCall {
        httpClient.post(url("/memos")) {
            contentType(ContentType.Application.Json)
            setBody(CreateMemoRequest(content = content, visibility = visibility))
        }.body()
    }

    suspend fun updateMemo(
        id: String,
        content: String? = null,
        visibility: String? = null,
        pinned: Boolean? = null,
        state: String? = null,
    ): ApiResult<MemoDto> = apiCall {
        val paths = buildList {
            if (content != null) add("content")
            if (visibility != null) add("visibility")
            if (pinned != null) add("pinned")
            if (state != null) add("state")
        }
        httpClient.patch(url("/memos/$id")) {
            parameter("updateMask", paths.joinToString(","))
            contentType(ContentType.Application.Json)
            setBody(UpdateMemoBody(
                content = content,
                visibility = visibility,
                pinned = pinned,
                state = state,
            ))
        }.body()
    }

    suspend fun searchMemos(query: String): ApiResult<ListMemosResponse> = apiCall {
        httpClient.get(url("/memos")) {
            parameter("pageSize", 50)
            parameter("filter", "content.contains(\"$query\")")
        }.body()
    }

    suspend fun upsertReaction(memoId: String, reactionType: String): ApiResult<ReactionDto> = apiCall {
        httpClient.post(url("/memos/$memoId/reactions")) {
            contentType(ContentType.Application.Json)
            setBody(UpsertReactionRequest(
                reaction = ReactionDto(reactionType = reactionType, contentId = "memos/$memoId")
            ))
        }.body()
    }

    suspend fun deleteReaction(memoId: String, reactionId: String): ApiResult<Unit> = apiCall {
        val response = httpClient.delete(url("/memos/$memoId/reactions/$reactionId"))
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    suspend fun deleteMemo(id: String): ApiResult<Unit> = apiCall {
        val response = httpClient.delete(url("/memos/$id"))
        if (!response.status.isSuccess()) {
            throw ApiException(response.status.value, response.bodyAsText())
        }
    }

    private suspend inline fun <T> apiCall(block: () -> T): ApiResult<T> =
        try {
            ApiResult.Success(block())
        } catch (e: ApiException) {
            ApiResult.Error(e.code, e.message ?: "Unknown error")
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            ApiResult.Error(e.response.status.value, e.message)
        } catch (e: io.ktor.client.plugins.ServerResponseException) {
            ApiResult.Error(e.response.status.value, e.message)
        } catch (e: Exception) {
            ApiResult.NetworkError(e)
        }
}

class ApiException(val code: Int, message: String) : Exception(message)
