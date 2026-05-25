package com.project.manifesto.modules.auth.dto

data class UserListItem(
    val id: Long,
    val username: String,
    val email: String,
    val karma: Int,
    val role: String
)
