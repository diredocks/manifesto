package com.project.manifesto.modules.comment.dto

import java.time.Instant

data class UserCommentResponse(
    val id: Long,
    val postId: Long,
    val postTitle: String,
    val content: String,
    val score: Int,
    val createdAt: Instant
)
