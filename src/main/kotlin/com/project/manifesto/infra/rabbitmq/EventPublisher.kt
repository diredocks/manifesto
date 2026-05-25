package com.project.manifesto.infra.rabbitmq

import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.vote.event.PostVotedEvent
import com.project.manifesto.modules.notification.event.NotificationEvent

interface EventPublisher {
    fun publishPostVoted(event: PostVotedEvent)
    fun publishNotification(event: NotificationEvent)
    fun publishPostCreated(event: PostCreatedEvent)
}
