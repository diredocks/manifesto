package com.project.manifesto.modules.auth.dto

data class AuthResponse(
    val token: String,
    val tokenType: String = "Bearer",
    val username: String,
    val role: String
)
