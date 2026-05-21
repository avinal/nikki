package com.avinal.memos.util

import com.avinal.memos.domain.Memo
import com.avinal.memos.domain.MemoVisibility
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val backupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
}

@Serializable
data class BackupData(
    val version: Int = 1,
    val exportedAt: String = "",
    val memoCount: Int = 0,
    val memos: List<BackupMemo> = emptyList(),
)

@Serializable
data class BackupMemo(
    val content: String,
    val visibility: String = "PRIVATE",
    val pinned: Boolean = false,
    val tags: List<String> = emptyList(),
)

object BackupManager {

    fun exportToJson(memos: List<Memo>): String {
        val now = kotlin.time.Clock.System.now().toString()
        val data = BackupData(
            version = 1,
            exportedAt = now,
            memoCount = memos.size,
            memos = memos.map { memo ->
                BackupMemo(
                    content = memo.content,
                    visibility = memo.visibility.toApiString(),
                    pinned = memo.pinned,
                    tags = memo.tags,
                )
            },
        )
        return backupJson.encodeToString(BackupData.serializer(), data)
    }

    fun parseFromJson(json: String): BackupData? {
        return try {
            backupJson.decodeFromString(BackupData.serializer(), json)
        } catch (_: Exception) {
            null
        }
    }
}
