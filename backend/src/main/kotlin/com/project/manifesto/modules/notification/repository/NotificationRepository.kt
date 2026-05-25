package com.project.manifesto.modules.notification.repository

import com.project.manifesto.modules.notification.entity.Notification
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface NotificationRepository : JpaRepository<Notification, Long> {

    fun findByReceiverIdOrderByCreatedAtDesc(receiverId: Long, pageable: Pageable): Page<Notification>

    fun countByReceiverIdAndIsReadFalse(receiverId: Long): Long

    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.id = :id")
    fun markAsRead(@Param("id") id: Long)
}
