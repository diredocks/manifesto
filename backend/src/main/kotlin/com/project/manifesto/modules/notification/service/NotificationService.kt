package com.project.manifesto.modules.notification.service

import com.project.manifesto.modules.notification.dto.NotificationResponse
import com.project.manifesto.modules.notification.entity.Notification
import com.project.manifesto.modules.notification.entity.NotificationType
import com.project.manifesto.modules.notification.repository.NotificationRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional
    fun createNotification(
        receiverId: Long,
        type: NotificationType,
        content: String,
        relatedPostId: Long? = null,
        relatedCommentId: Long? = null,
    ): NotificationResponse {
        val notification =
            Notification(
                receiverId = receiverId,
                type = type,
                content = content,
                relatedPostId = relatedPostId,
                relatedCommentId = relatedCommentId,
            )
        val saved = notificationRepository.save(notification)
        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getUserNotifications(
        userId: Long,
        pageable: Pageable,
    ): Page<NotificationResponse> =
        notificationRepository
            .findByReceiverIdOrderByCreatedAtDesc(userId, pageable)
            .map { toResponse(it) }

    @Transactional(readOnly = true)
    fun getUnreadCount(userId: Long): Long = notificationRepository.countByReceiverIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(
        notificationId: Long,
        userId: Long,
    ): Boolean {
        val notification =
            notificationRepository
                .findById(notificationId)
                .orElseThrow { EntityNotFoundException("Notification not found: $notificationId") }
        require(notification.receiverId == userId) { "Not authorized" }
        notification.isRead = true
        notificationRepository.save(notification)
        return true
    }

    private fun toResponse(notification: Notification): NotificationResponse =
        NotificationResponse(
            id = notification.id,
            type = notification.type,
            content = notification.content,
            isRead = notification.isRead,
            relatedPostId = notification.relatedPostId,
            relatedCommentId = notification.relatedCommentId,
            createdAt = notification.createdAt,
        )
}
