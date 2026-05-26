package com.project.manifesto.modules.auth.dto

data class UserInfoResponse(
    val id: Long,
    val username: String,
    val email: String,
    val karma: Int,
    val role: String,
)
