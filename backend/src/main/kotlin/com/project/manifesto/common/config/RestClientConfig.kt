package com.project.manifesto.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {
    @Bean
    fun restClientBuilder(): RestClient.Builder {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()

        val factory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(Duration.ofSeconds(30))
            }

        return RestClient.builder().requestFactory(factory)
    }
}
