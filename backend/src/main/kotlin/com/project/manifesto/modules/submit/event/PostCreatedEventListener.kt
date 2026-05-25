package com.project.manifesto.modules.submit.event

import com.project.manifesto.infra.rabbitmq.EventPublisher
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@Profile("!test & !generate")
class PostCreatedEventListener(
    private val eventPublisher: EventPublisher,
    @Autowired(required = false) private val redisTemplate: StringRedisTemplate?
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PostCreatedEvent) {
        redisTemplate?.delete("feed:hot:top100")
        eventPublisher.publishPostCreated(event)
    }
}
