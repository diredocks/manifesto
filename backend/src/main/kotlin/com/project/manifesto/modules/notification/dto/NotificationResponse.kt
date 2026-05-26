package com.project.manifesto.modules.notification.dto

import com.project.manifesto.modules.notification.entity.NotificationType
import java.time.Instant

data class NotificationResponse(
    val id: Long,
    val type: NotificationType,
    val content: String,
    val isRead: Boolean,
    val relatedPostId: Long?,
    val relatedCommentId: Long?,
    val createdAt: Instant,
)
