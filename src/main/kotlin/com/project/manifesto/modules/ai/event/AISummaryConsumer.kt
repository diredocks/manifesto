package com.project.manifesto.modules.ai.event

import com.project.manifesto.infra.rabbitmq.RabbitConfig
import com.project.manifesto.modules.ai.service.AIService
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.event.PostCreatedEvent
import com.project.manifesto.modules.submit.repository.PostRepository
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Profile("!test")
class AISummaryConsumer(
    private val aiService: AIService,
    private val postRepository: PostRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @RabbitListener(queues = [RabbitConfig.QUEUE_AI_SUMMARY])
    @Transactional
    fun handlePostCreated(event: PostCreatedEvent) {
        logger.info("AI processing post {}: {}", event.postId, event.title)

        if (event.type == PostType.LINK.name && !event.url.isNullOrBlank()) {
            val summary = aiService.summarizeUrl(event.url)
            if (summary != null) {
                val post = postRepository.findById(event.postId).orElse(null)
                post?.summary = summary
                postRepository.save(post)
                logger.info("AI summary saved for post {}", event.postId)
            }
        }
    }
}
