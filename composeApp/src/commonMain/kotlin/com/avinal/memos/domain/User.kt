package com.avinal.memos.domain

data class User(
    val id: String,
    val username: String,
    val nickname: String,
    val email: String,
    val avatarUrl: String,
    val role: String,
)
