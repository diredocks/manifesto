package com.project.manifesto.modules.comment.service

import com.project.manifesto.modules.comment.dto.CommentResponse
import com.project.manifesto.modules.comment.dto.CreateCommentRequest
import com.project.manifesto.modules.comment.dto.UserCommentResponse
import com.project.manifesto.modules.comment.entity.Comment
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.entity.NotificationType
import com.project.manifesto.modules.notification.service.NotificationService
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CommentService(
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
    private val notificationService: NotificationService,
) {
    @Transactional
    fun createComment(
        authorId: Long,
        postId: Long,
        request: CreateCommentRequest,
    ): CommentResponse {
        val author =
            userRepository
                .findById(authorId)
                .orElseThrow { EntityNotFoundException("User not found: $authorId") }
        require(!author.isBanned()) { "You are banned and cannot comment" }

        val post =
            postRepository
                .findById(postId)
                .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        if (post.deleted) throw EntityNotFoundException("Post not found: $postId")

        var depth = 0

        if (request.parentId != null) {
            val parentComment =
                commentRepository
                    .findById(request.parentId)
                    .orElseThrow { EntityNotFoundException("Parent comment not found: ${request.parentId}") }
            if (parentComment.postId != postId) {
                throw IllegalArgumentException("Parent comment does not belong to this post")
            }
            depth = parentComment.depth + 1

            notificationService.createNotification(
                receiverId = parentComment.authorId,
                type = NotificationType.COMMENT_REPLY,
                content = "Someone replied to your comment",
                relatedPostId = postId,
                relatedCommentId = request.parentId,
            )
        } else if (post.authorId != authorId) {
            notificationService.createNotification(
                receiverId = post.authorId,
                type = NotificationType.POST_COMMENT,
                content = "Someone commented on your post",
                relatedPostId = postId,
                relatedCommentId = null,
            )
        }

        val comment =
            Comment(
                postId = postId,
                authorId = authorId,
                parentId = request.parentId,
                content = request.content,
                depth = depth,
            )
        val saved = commentRepository.save(comment)

        postRepository.incrementCommentCount(postId)

        return toResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getCommentTree(postId: Long): List<CommentResponse> {
        val allComments = commentRepository.findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId)
        val responses = allComments.map { toResponse(it) }
        val childrenByParent = responses.groupBy { it.parentId }

        fun buildTree(parentId: Long?): List<CommentResponse> =
            (childrenByParent[parentId] ?: emptyList()).map { child ->
                child.copy(children = buildTree(child.id))
            }

        return buildTree(null)
    }

    @Transactional
    fun deleteComment(
        commentId: Long,
        userId: Long,
    ): Boolean {
        val comment =
            commentRepository
                .findById(commentId)
                .orElseThrow { EntityNotFoundException("Comment not found: $commentId") }
        require(comment.authorId == userId) { "Not authorized to delete this comment" }
        softDeleteComment(comment)
        return true
    }

    @Transactional
    fun deleteCommentAsModerator(commentId: Long): Boolean {
        val comment =
            commentRepository
                .findById(commentId)
                .orElseThrow { EntityNotFoundException("Comment not found: $commentId") }
        softDeleteComment(comment)
        return true
    }

    @Transactional(readOnly = true)
    fun listCommentsByUser(
        userId: Long,
        pageable: Pageable,
    ): Page<UserCommentResponse> =
        commentRepository
            .findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
            .map { comment ->
                val post = postRepository.findById(comment.postId).orElse(null)
                UserCommentResponse(
                    id = comment.id,
                    postId = comment.postId,
                    postTitle = post?.title ?: "[deleted]",
                    content = comment.content,
                    score = comment.score,
                    createdAt = comment.createdAt,
                )
            }

    private fun softDeleteComment(comment: Comment) {
        comment.deleted = true
        comment.content = "[deleted]"
        commentRepository.save(comment)
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
            createdAt = comment.createdAt,
        )
    }
}
