package com.project.manifesto.infra.rabbitmq

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.Queue
import org.springframework.amqp.core.TopicExchange
import org.springframework.amqp.rabbit.connection.ConnectionFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile

@Configuration
@Profile("!test")
class RabbitConfig {

    companion object {
        const val EXCHANGE = "manifesto.exchange"
        const val QUEUE_POST_VOTED = "manifesto.queue.post-voted"
        const val QUEUE_NOTIFICATION = "manifesto.queue.notification"
        const val QUEUE_AI_SUMMARY = "manifesto.queue.ai-summary"
        const val ROUTING_KEY_POST_VOTED = "post.voted"
        const val ROUTING_KEY_NOTIFICATION = "notification"
        const val ROUTING_KEY_AI_SUMMARY = "post.created"
    }

    @Bean
    fun exchange(): TopicExchange = TopicExchange(EXCHANGE)

    @Bean
    fun postVotedQueue(): Queue = Queue(QUEUE_POST_VOTED)

    @Bean
    fun notificationQueue(): Queue = Queue(QUEUE_NOTIFICATION)

    @Bean
    fun aiSummaryQueue(): Queue = Queue(QUEUE_AI_SUMMARY)

    @Bean
    fun postVotedBinding(postVotedQueue: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(postVotedQueue).to(exchange).with(ROUTING_KEY_POST_VOTED)

    @Bean
    fun notificationBinding(notificationQueue: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(notificationQueue).to(exchange).with(ROUTING_KEY_NOTIFICATION)

    @Bean
    fun aiSummaryBinding(aiSummaryQueue: Queue, exchange: TopicExchange): Binding =
        BindingBuilder.bind(aiSummaryQueue).to(exchange).with(ROUTING_KEY_AI_SUMMARY)

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter {
        return Jackson2JsonMessageConverter()
    }

    @Bean
    fun rabbitTemplate(connectionFactory: ConnectionFactory): RabbitTemplate {
        val template = RabbitTemplate(connectionFactory)
        template.messageConverter = messageConverter()
        return template
    }
}
