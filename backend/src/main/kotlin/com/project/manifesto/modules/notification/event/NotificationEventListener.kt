package com.project.manifesto.modules.notification.event

import com.project.manifesto.infra.rabbitmq.EventPublisher
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

@Component
@Profile("!test & !generate")
class NotificationEventListener(
    private val eventPublisher: EventPublisher
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    fun handle(event: NotificationEvent) {
        eventPublisher.publishNotification(event)
    }
}
