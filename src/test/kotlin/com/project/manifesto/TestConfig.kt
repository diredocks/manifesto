package com.project.manifesto

import com.project.manifesto.infra.rabbitmq.EventPublisher
import com.project.manifesto.infra.redis.LockService
import com.project.manifesto.modules.notification.event.NotificationEvent
import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.vote.event.PostVotedEvent
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary

@TestConfiguration
class TestConfig {

    @Bean
    @Primary
    fun lockService(): LockService = object : LockService {
        override fun tryLock(key: String, ttlSeconds: Long): LockService.LockToken? {
            return LockService.LockToken(key, "test-token")
        }

        override fun unlock(lockToken: LockService.LockToken): Boolean {
            return true
        }
    }

    @Bean
    @Primary
    fun eventPublisher(): EventPublisher = object : EventPublisher {
        override fun publishPostVoted(event: PostVotedEvent) {}
        override fun publishNotification(event: NotificationEvent) {}
        override fun publishPostCreated(event: PostCreatedEvent) {}
    }
}
