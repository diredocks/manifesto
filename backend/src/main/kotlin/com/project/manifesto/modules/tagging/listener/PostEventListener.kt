package com.project.manifesto.modules.tagging.listener

import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.tagging.messaging.PostTaggingMessage
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@ConditionalOnProperty(name = ["app.tagging.enabled"], havingValue = "true")
class PostEventListener(
    private val rabbitTemplate: RabbitTemplate,
) {
    private val logger = LoggerFactory.getLogger(PostEventListener::class.java)

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handlePostCreated(event: PostCreatedEvent) {
        logger.info("Post created, sending tagging message for postId={}", event.postId)
        rabbitTemplate.convertAndSend(
            "post.events",
            "post.created.tagging",
            PostTaggingMessage(postId = event.postId),
        )
    }
}
