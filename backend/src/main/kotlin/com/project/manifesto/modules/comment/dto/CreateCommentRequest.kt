package com.project.manifesto.modules.comment.dto

import jakarta.validation.constraints.NotBlank

data class CreateCommentRequest(
    @field:NotBlank(message = "Content is required")
    val content: String,

    val parentId: Long? = null
)
