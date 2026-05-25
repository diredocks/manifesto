package com.project.manifesto.modules.submit.dto

import java.time.Instant

data class PostResponse(
    val id: Long,
    val title: String,
    val url: String?,
    val content: String?,
    val summary: String?,
    val score: Int,
    val hotScore: Double,
    val commentCount: Int,
    val type: String,
    val authorUsername: String,
    val createdAt: Instant,
    val tags: List<String> = emptyList()
)
