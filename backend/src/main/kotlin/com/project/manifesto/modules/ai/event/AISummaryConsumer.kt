package com.project.manifesto.modules.ai.event

import com.project.manifesto.infra.rabbitmq.RabbitConfig
import com.project.manifesto.modules.ai.service.AIService
import com.project.manifesto.modules.ai.service.TagService
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.submit.service.PostService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!test & !e2e")
class AISummaryConsumer(
    private val aiService: AIService,
    private val tagService: TagService,
    private val postService: PostService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitConfig.QUEUE_AI_SUMMARY])
    @Transactional
    fun handlePostCreated(event: PostCreatedEvent) {
        logger.info("AI processing post {}: {}", event.postId, event.title)

        val content = if (event.type == PostType.LINK.name) {
            event.url
        } else {
            event.content
        }

        val tags = aiService.generateTags(event.title, content)
        if (tags.isNotEmpty()) {
            tagService.assignTags(event.postId, tags)
            logger.info("Tags assigned for post {}: {}", event.postId, tags)
        }

        if (event.type == PostType.LINK.name && !event.url.isNullOrBlank()) {
            val summary = aiService.summarizeUrl(event.url)
            if (summary != null) {
                postService.updateSummary(event.postId, summary)
                logger.info("AI summary saved for post {}", event.postId)
            }
        }
    }
}
