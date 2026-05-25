package com.project.manifesto.modules.notification.event

import com.project.manifesto.modules.notification.entity.NotificationType
import java.io.Serializable

data class NotificationEvent(
    val receiverId: Long,
    val type: NotificationType,
    val content: String,
    val relatedPostId: Long? = null,
    val relatedCommentId: Long? = null
) : Serializable
