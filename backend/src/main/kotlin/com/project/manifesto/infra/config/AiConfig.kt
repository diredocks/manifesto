package com.project.manifesto.infra.config

import org.springframework.ai.chat.client.ChatClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["app.tagging.enabled"], havingValue = "true")
class AiConfig {
    @Bean
    fun chatClient(builder: ChatClient.Builder): ChatClient = builder.build()
}
