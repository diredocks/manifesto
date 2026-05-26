package com.project.manifesto.infra.ai

import com.project.manifesto.modules.submit.entity.Post
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(name = ["app.tagging.enabled"], havingValue = "true")
class TagGenerationService(
    private val chatClient: ChatClient,
) {
    private val logger = LoggerFactory.getLogger(TagGenerationService::class.java)

    fun generateTags(post: Post): List<String> {
        val promptText =
            buildString {
                appendLine("You are a tagging assistant for a community forum.")
                appendLine("Analyze the post and generate 3-5 relevant tags.")
                appendLine()
                appendLine("Rules:")
                appendLine("- Tags must be lowercase English keywords or short phrases")
                appendLine("- Focus on technology, programming languages, frameworks, concepts")
                appendLine("- Be specific but concise")
                appendLine("- Reply with ONLY a valid JSON object. The JSON must have a single key \"tags\"")
                appendLine("  whose value is an array of up to 5 strings.")
                appendLine("- Do not include markdown fences, explanations, or any other text.")
                appendLine()
                appendLine("Post title: ${sanitize(post.title)}")
                if (!post.content.isNullOrBlank()) {
                    appendLine("Post content: ${sanitize(post.content!!.take(CONTENT_MAX_LENGTH))}")
                }
                if (!post.url.isNullOrBlank()) {
                    appendLine("Post URL: ${sanitize(post.url!!)}")
                }
            }

        return try {
            val prompt = Prompt(UserMessage(promptText))
            val response =
                chatClient
                    .prompt(prompt)
                    .call()
                    .entity(TaggingResponse::class.java)

            response
                ?.tags
                ?.take(MAX_TAGS)
                ?.filter { it.isNotBlank() }
                ?: emptyList()
        } catch (e: Exception) {
            logger.error("Failed to generate tags for post ${post.id}", e)
            emptyList()
        }
    }

    private fun sanitize(text: String): String =
        text
            .replace("{", "[")
            .replace("}", "]")

    companion object {
        private const val MAX_TAGS = 5
        private const val CONTENT_MAX_LENGTH = 2000
    }
}
