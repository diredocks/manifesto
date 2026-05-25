package com.project.manifesto.modules.notification.event

import com.project.manifesto.infra.rabbitmq.RabbitConfig
import com.project.manifesto.modules.notification.service.NotificationService
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test & !generate")
class NotificationConsumer(
    private val notificationService: NotificationService
) {

    @RabbitListener(queues = [RabbitConfig.QUEUE_NOTIFICATION])
    fun handleNotificationEvent(event: NotificationEvent) {
        notificationService.createNotification(
            receiverId = event.receiverId,
            type = event.type,
            content = event.content,
            relatedPostId = event.relatedPostId,
            relatedCommentId = event.relatedCommentId
        )
    }
}
