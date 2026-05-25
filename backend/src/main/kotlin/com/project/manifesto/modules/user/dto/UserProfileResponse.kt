package com.project.manifesto.modules.user.dto

import java.time.Instant

data class UserProfileResponse(
    val username: String,
    val karma: Int,
    val createdAt: Instant
)
