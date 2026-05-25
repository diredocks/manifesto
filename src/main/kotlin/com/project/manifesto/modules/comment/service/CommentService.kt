package com.project.manifesto.modules.comment.service

import com.project.manifesto.infra.rabbitmq.EventPublisher
import com.project.manifesto.modules.comment.dto.CommentResponse
import com.project.manifesto.modules.comment.dto.CreateCommentRequest
import com.project.manifesto.modules.comment.entity.Comment
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.entity.NotificationType
import com.project.manifesto.modules.notification.event.NotificationEvent
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val eventPublisher: EventPublisher
) {

    @Transactional
    fun createComment(authorId: Long, postId: Long, request: CreateCommentRequest): CommentResponse {
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        if (post.deleted) throw EntityNotFoundException("Post not found: $postId")

        var parentComment: Comment? = null
        var depth = 0

        if (request.parentId != null) {
            parentComment = commentRepository.findById(request.parentId)
                .orElseThrow { EntityNotFoundException("Parent comment not found: ${request.parentId}") }
            if (parentComment.postId != postId) {
                throw IllegalArgumentException("Parent comment does not belong to this post")
            }
            depth = parentComment.depth + 1

            eventPublisher.publishNotification(
                NotificationEvent(
                    receiverId = parentComment.authorId,
                    type = NotificationType.COMMENT_REPLY,
                    content = "Someone replied to your comment",
                    relatedPostId = postId,
                    relatedCommentId = request.parentId
                )
            )
        }

        val comment = Comment(
            postId = postId,
            authorId = authorId,
            parentId = request.parentId,
            content = request.content,
            depth = depth
        )
        val saved = commentRepository.save(comment)

        postRepository.incrementCommentCount(postId)

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getCommentTree(postId: Long): List<CommentResponse> {
        val allComments = commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId)
        val commentMap = allComments.associate { it.id to toResponse(it) }.toMutableMap()

        for (comment in allComments) {
            val response = commentMap[comment.id] ?: continue
            val pid = comment.parentId
            if (pid != null) {
                val parent = commentMap[pid]
                if (parent != null) {
                    val updated = parent.copy(children = parent.children + response)
                    commentMap[comment.id] = response
                    commentMap[pid] = updated
                }
            }
        }

        return commentMap.values.filter { it.parentId == null }
            .sortedBy { it.createdAt }
    }

    @Transactional
    fun deleteComment(commentId: Long, userId: Long): Boolean {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found: $commentId") }
        require(comment.authorId == userId) { "Not authorized to delete this comment" }
        comment.deleted = true
        comment.content = "[deleted]"
        commentRepository.save(comment)
        return true
    }

    private fun toResponse(comment: Comment): CommentResponse {
        val author = userRepository.findById(comment.authorId).orElse(null)
        return CommentResponse(
            id = comment.id,
            postId = comment.postId,
            authorId = comment.authorId,
            authorUsername = author?.username ?: "deleted",
            parentId = comment.parentId,
            content = if (comment.deleted) "[deleted]" else comment.content,
            score = comment.score,
            depth = comment.depth,
            deleted = comment.deleted,
            createdAt = comment.createdAt
        )
    }
}
