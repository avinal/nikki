package com.avinal.memos.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Route {
    @Serializable data object Login : Route
    @Serializable data object Main : Route
    @Serializable data class MemoDetail(val memoId: String) : Route
    @Serializable data class MemoEditor(val memoId: String = "") : Route
}
