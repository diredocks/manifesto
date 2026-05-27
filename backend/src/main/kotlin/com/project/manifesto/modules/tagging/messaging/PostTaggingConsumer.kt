package com.project.manifesto.modules.tagging.messaging

import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.tagging.service.TagGenerationService
import com.project.manifesto.modules.tagging.service.TagService
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@ConditionalOnProperty(name = ["app.tagging.enabled"], havingValue = "true")
class PostTaggingConsumer(
    private val postRepository: PostRepository,
    private val tagGenerationService: TagGenerationService,
    private val tagService: TagService,
) {
    private val logger = LoggerFactory.getLogger(PostTaggingConsumer::class.java)

    @RabbitListener(queues = ["post.tagging"])
    @Transactional
    fun handleTagging(message: PostTaggingMessage) {
        logger.info("Received tagging request for postId={}", message.postId)

        val post =
            postRepository
                .findById(message.postId)
                .orElse(null)
                ?: run {
                    logger.warn("Post not found for tagging: postId={}", message.postId)
                    return
                }

        if (post.deleted) {
            logger.info("Skipping tagging for deleted post: postId={}", message.postId)
            return
        }

        val tags = tagGenerationService.generateTags(post)
        if (tags.isEmpty()) {
            logger.info("No tags generated for postId={}", message.postId)
            return
        }

        tagService.assignTags(message.postId, tags)
        logger.info("Tags updated for postId={}: {}", message.postId, tags)
    }
}
