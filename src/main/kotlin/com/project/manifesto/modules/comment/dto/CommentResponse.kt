package com.project.manifesto.modules.comment.dto

import java.time.Instant

data class CommentResponse(
    val id: Long,
    val postId: Long,
    val authorId: Long,
    val authorUsername: String,
    val parentId: Long?,
    val content: String,
    val score: Int,
    val depth: Int,
    val deleted: Boolean,
    val createdAt: Instant,
    val children: List<CommentResponse> = emptyList()
)
