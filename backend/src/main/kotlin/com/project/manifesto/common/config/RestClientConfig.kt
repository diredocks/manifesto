package com.project.manifesto.common.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.net.http.HttpClient
import java.time.Duration

@Configuration
class RestClientConfig {
    companion object {
        private val READ_TIMEOUT = Duration.ofSeconds(30)
    }

    @Bean
    fun restClientBuilder(): RestClient.Builder {
        val httpClient =
            HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build()

        val factory =
            JdkClientHttpRequestFactory(httpClient).apply {
                setReadTimeout(READ_TIMEOUT)
            }

        return RestClient.builder().requestFactory(factory)
    }
}
