package com.project.manifesto.modules.submit.dto

import java.time.Instant

data class PostDetailResponse(
    val id: Long,
    val title: String,
    val url: String?,
    val content: String?,
    val summary: String?,
    val score: Int,
    val hotScore: Double,
    val commentCount: Int,
    val type: String,
    val authorId: Long,
    val authorUsername: String,
    val createdAt: Instant,
    val tags: List<String> = emptyList(),
)
