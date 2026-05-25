package com.project.manifesto.e2e

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("e2e")
@Tag("e2e")
class E2ESmokeTest {

    @Autowired
    private lateinit var context: ApplicationContext

    @Autowired(required = false)
    private var redisTemplate: StringRedisTemplate? = null

    @Test
    fun `context loads with real infra`() {
        val activeProfiles = context.environment.activeProfiles
        println("Active profiles: ${activeProfiles.joinToString()}")

        val hasRedis = context.containsBean("stringRedisTemplate")
        println("Redis available: $hasRedis")

        val hasRabbit = context.containsBean("rabbitTemplate")
        println("RabbitMQ available: $hasRabbit")

        val hasLockService = context.containsBean("redisLockService")
        println("RedisLockService available: $hasLockService")

        val hasEventPublisher = context.containsBean("rabbitEventPublisher")
        println("RabbitEventPublisher available: $hasEventPublisher")

        val hasPostVotedConsumer = context.containsBean("postVotedConsumer")
        println("PostVotedConsumer available: $hasPostVotedConsumer")

        val hasNotificationConsumer = context.containsBean("notificationConsumer")
        println("NotificationConsumer available: $hasNotificationConsumer")

        val hasAISummaryConsumer = context.containsBean("AISummaryConsumer")
        println("AISummaryConsumer available: $hasAISummaryConsumer")

        val hasTestConfig = context.containsBean("com.project.manifesto.TestConfig")
        println("TestConfig loaded: $hasTestConfig")
    }
}
