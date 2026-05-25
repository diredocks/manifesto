package com.project.manifesto.infra.rabbitmq

interface EventPublisher {
    fun publishPostVoted(event: com.project.manifesto.modules.vote.event.PostVotedEvent)
    fun publishNotification(event: com.project.manifesto.modules.notification.event.NotificationEvent)
}
