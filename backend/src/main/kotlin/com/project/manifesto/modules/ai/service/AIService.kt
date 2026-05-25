package com.project.manifesto.modules.ai.service

import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Service
import java.net.InetSocketAddress
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

@Service
@Profile("!test & !e2e")
class AIService(
    private val chatModel: ChatModel
) {

    private val logger = LoggerFactory.getLogger(javaClass)
    private val httpClient: HttpClient by lazy {
        val builder = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)

        val proxyUrl = System.getProperty("http.proxyHost")?.let { host ->
            val port = System.getProperty("http.proxyPort", "80").toIntOrNull() ?: 80
            "http://$host:$port"
        } ?: System.getenv("HTTP_PROXY")

        if (proxyUrl != null) {
            try {
                val uri = URI(proxyUrl)
                builder.proxy(java.net.ProxySelector.of(
                    java.net.InetSocketAddress(uri.host, uri.port)
                ))
            } catch (_: Exception) {}
        }

        builder.build()
    }

    fun summarizeUrl(url: String): String? {
        return try {
            val content = fetchAndExtract(url)
            if (content.isBlank()) return null
            val response = chatModel.call(
                Prompt(UserMessage("Summarize the following article in 100 words or less:\n\n$content"))
            )
            response.result.output.content?.trim()
        } catch (e: Exception) {
            logger.warn("AI summary failed for URL: $url", e)
            null
        }
    }

    fun generateTags(title: String, content: String?): List<String> {
        return try {
            val text = buildString {
                append("Title: $title\n")
                if (!content.isNullOrBlank()) append("Content: $content\n")
            }
            val response = chatModel.call(
                Prompt(UserMessage("Generate exactly 3 concise, single-word tags for this content, comma-separated:\n\n$text"))
            )
            response.result.output.content
                ?.split(",")
                ?.map { it.trim().lowercase().replace(Regex("[^a-z0-9-]"), "") }
                ?.filter { it.isNotBlank() }
                ?.take(3)
                ?: emptyList()
        } catch (e: Exception) {
            logger.warn("AI tag generation failed for title: $title", e)
            emptyList()
        }
    }

    private fun fetchAndExtract(url: String): String {
        try {
            val uri = URI(url)
            val request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "ManifestoBot/1.0")
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() !in 200..299) return ""

            val doc = Jsoup.parse(response.body())
            return doc.body().text()
        } catch (e: Exception) {
            logger.warn("Failed to fetch URL: $url", e)
            return ""
        }
    }
}
