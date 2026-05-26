package com.project.manifesto.common.config

import org.springframework.amqp.core.Binding
import org.springframework.amqp.core.BindingBuilder
import org.springframework.amqp.core.DirectExchange
import org.springframework.amqp.core.Queue
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["app.tagging.enabled"], havingValue = "true")
class RabbitMQConfig {
    @Bean
    fun postTaggingQueue(): Queue = Queue("post.tagging", true)

    @Bean
    fun postEventsExchange(): DirectExchange = DirectExchange("post.events")

    @Bean
    fun postTaggingBinding(
        postTaggingQueue: Queue,
        postEventsExchange: DirectExchange,
    ): Binding = BindingBuilder.bind(postTaggingQueue).to(postEventsExchange).with("post.created.tagging")

    @Bean
    fun messageConverter(): Jackson2JsonMessageConverter = Jackson2JsonMessageConverter()
}
