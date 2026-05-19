package com.avinal.memos.domain

enum class MemoVisibility {
    PRIVATE, PROTECTED, PUBLIC;

    fun toApiString(): String = name

    companion object {
        fun fromApiString(value: String): MemoVisibility =
            entries.firstOrNull { it.name == value } ?: PRIVATE
    }
}
