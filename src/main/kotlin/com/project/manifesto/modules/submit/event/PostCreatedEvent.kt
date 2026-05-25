package com.project.manifesto.modules.submit.event

import java.io.Serializable

data class PostCreatedEvent(
    val postId: Long,
    val title: String,
    val url: String?,
    val content: String?,
    val type: String
) : Serializable
