package com.project.manifesto.modules.vote.event

import com.project.manifesto.infra.rabbitmq.EventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@Profile("!test & !generate")
class PostVotedEventListener(
    private val eventPublisher: EventPublisher
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: PostVotedEvent) {
        eventPublisher.publishPostVoted(event)
    }
}
