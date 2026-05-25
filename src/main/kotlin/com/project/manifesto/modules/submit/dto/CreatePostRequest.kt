package com.project.manifesto.modules.submit.dto

import com.project.manifesto.modules.submit.entity.PostType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class CreatePostRequest(
    @field:NotBlank(message = "Title is required")
    @field:Size(max = 300, message = "Title must be at most 300 characters")
    val title: String,

    @field:NotNull(message = "Type is required")
    val type: PostType,

    val url: String? = null,

    val content: String? = null
)
