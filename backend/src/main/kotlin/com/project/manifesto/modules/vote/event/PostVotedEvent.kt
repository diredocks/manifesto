package com.project.manifesto.modules.vote.event

import java.io.Serializable

data class PostVotedEvent(
    val postId: Long,
    val userId: Long,
    val voted: Boolean
) : Serializable
