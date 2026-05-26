package com.project.manifesto.modules.auth.dto

import java.time.Instant

data class UserListItem(
    val id: Long,
    val username: String,
    val email: String,
    val karma: Int,
    val role: String,
    val bannedUntil: Instant?,
)
