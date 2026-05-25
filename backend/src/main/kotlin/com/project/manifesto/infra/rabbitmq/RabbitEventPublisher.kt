package com.project.manifesto.infra.rabbitmq

import com.project.manifesto.modules.notification.event.NotificationEvent
import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.vote.event.PostVotedEvent
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class RabbitEventPublisher(
    private val rabbitTemplate: RabbitTemplate
) : EventPublisher {

    override fun publishPostVoted(event: PostVotedEvent) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.ROUTING_KEY_POST_VOTED,
            event
        )
    }

    override fun publishNotification(event: NotificationEvent) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.ROUTING_KEY_NOTIFICATION,
            event
        )
    }

    override fun publishPostCreated(event: PostCreatedEvent) {
        rabbitTemplate.convertAndSend(
            RabbitConfig.EXCHANGE,
            RabbitConfig.ROUTING_KEY_AI_SUMMARY,
            event
        )
    }
}
