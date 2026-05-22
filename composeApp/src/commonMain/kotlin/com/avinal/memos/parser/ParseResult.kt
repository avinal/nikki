package com.avinal.memos.parser

enum class IssueSeverity { ERROR, WARNING }

data class ParseWarning(
    val memoId: String = "",
    val lineIndex: Int,
    val taskText: String,
    val issue: String,
    val highlight: String = "",
    val severity: IssueSeverity = IssueSeverity.WARNING,
)
