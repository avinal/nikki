package com.avinal.memos.domain

import kotlinx.datetime.LocalDate

data class Task(
    val id: String,
    val memoId: String,
    val lineIndex: Int,
    val text: String,
    val rawText: String = text,
    val originalLine: String = "",
    val isCompleted: Boolean,
    val dueDate: LocalDate? = null,
    val priority: Int? = null,
    val labels: List<String> = emptyList(),
    val lists: List<String> = emptyList(),
)
