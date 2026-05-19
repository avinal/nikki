package com.avinal.memos.api.model

import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    val name: String = "",
    val role: String = "USER",
    val username: String = "",
    val email: String = "",
    val nickname: String = "",
    val avatarUrl: String = "",
    val createTime: String = "",
    val updateTime: String = "",
)
