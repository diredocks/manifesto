package com.project.manifesto.modules.notification.entity

import com.project.manifesto.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "notifications",
    indexes = [
        Index(name = "idx_notifications_receiver_id_is_read", columnList = "receiver_id, is_read")
    ]
)
class Notification(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "receiver_id", nullable = false)
    val receiverId: Long,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    val type: NotificationType,

    @Column(nullable = false, columnDefinition = "TEXT")
    val content: String,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "related_post_id")
    val relatedPostId: Long? = null,

    @Column(name = "related_comment_id")
    val relatedCommentId: Long? = null
) : BaseEntity()
