package com.project.manifesto.modules.submit.service

import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.dto.PostDetailResponse
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.entity.Post
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PostService(
    private val postRepository: PostRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun createPost(
        authorId: Long,
        request: CreatePostRequest,
    ): PostDetailResponse {
        val author =
            userRepository
                .findById(authorId)
                .orElseThrow { EntityNotFoundException("User not found: $authorId") }
        require(!author.isBanned()) { "You are banned and cannot post" }

        require(request.type == PostType.LINK || request.type == PostType.ASK) {
            "Invalid post type"
        }
        if (request.type == PostType.LINK) {
            require(!request.url.isNullOrBlank()) { "URL is required for LINK posts" }
        }
        if (request.type == PostType.ASK) {
            require(!request.content.isNullOrBlank()) { "Content is required for ASK posts" }
        }

        val post =
            Post(
                authorId = authorId,
                title = request.title,
                url = request.url,
                content = request.content,
                type = request.type,
                hotScore = calculateInitialHotScore(),
            )
        val saved = postRepository.save(post)
        return toDetailResponse(saved)
    }

    @Transactional(readOnly = true)
    fun getPostById(postId: Long): PostDetailResponse {
        val post =
            postRepository
                .findById(postId)
                .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        if (post.deleted) {
            throw EntityNotFoundException("Post not found: $postId")
        }
        return toDetailResponse(post)
    }

    @Transactional(readOnly = true)
    fun listNewPosts(pageable: Pageable): Page<PostResponse> =
        postRepository
            .findByDeletedFalseOrderByCreatedAtDesc(pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listNewPostsByType(
        type: PostType,
        pageable: Pageable,
    ): Page<PostResponse> =
        postRepository
            .findByTypeAndDeletedFalseOrderByCreatedAtDesc(type, pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listTopPosts(pageable: Pageable): Page<PostResponse> =
        postRepository
            .findByDeletedFalseOrderByScoreDesc(pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listTopPostsByType(
        type: PostType,
        pageable: Pageable,
    ): Page<PostResponse> =
        postRepository
            .findByTypeAndDeletedFalseOrderByScoreDesc(type, pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listHotPosts(pageable: Pageable): Page<PostResponse> =
        postRepository
            .findByDeletedFalseOrderByHotScoreDesc(pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listHotPostsByType(
        type: PostType,
        pageable: Pageable,
    ): Page<PostResponse> =
        postRepository
            .findByTypeAndDeletedFalseOrderByHotScoreDesc(type, pageable)
            .map { toPostResponse(it) }

    @Transactional(readOnly = true)
    fun listPostsByIds(ids: List<Long>): List<PostResponse> {
        if (ids.isEmpty()) return emptyList()
        return postRepository
            .findByIdInAndDeletedFalse(ids)
            .map { toPostResponse(it) }
    }

    @Transactional(readOnly = true)
    fun listPostsByUser(
        userId: Long,
        pageable: Pageable,
    ): Page<PostResponse> =
        postRepository
            .findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(userId, pageable)
            .map { toPostResponse(it) }

    @Transactional
    fun deletePost(
        postId: Long,
        userId: Long,
    ): Boolean {
        val post =
            postRepository
                .findById(postId)
                .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        require(post.authorId == userId) { "Not authorized to delete this post" }
        softDeletePost(post)
        return true
    }

    @Transactional
    fun deletePostAsModerator(postId: Long): Boolean {
        val post =
            postRepository
                .findById(postId)
                .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        softDeletePost(post)
        return true
    }

    private fun softDeletePost(post: Post) {
        post.deleted = true
        postRepository.save(post)
    }

    @Transactional
    fun updateSummary(
        postId: Long,
        summary: String,
    ) {
        val post = postRepository.findById(postId).orElse(null) ?: return
        post.summary = summary
        postRepository.save(post)
    }

    private fun calculateInitialHotScore(): Double = 1.0 / Math.pow(2.0, 1.5)

    fun toPostResponse(post: Post): PostResponse {
        val author =
            userRepository
                .findById(post.authorId)
                .orElse(null)
        return PostResponse(
            id = post.id,
            title = post.title,
            url = post.url,
            content = post.content,
            summary = post.summary,
            score = post.score,
            hotScore = post.hotScore,
            commentCount = post.commentCount,
            type = post.type.name,
            authorUsername = author?.username ?: "deleted",
            createdAt = post.createdAt,
        )
    }

    private fun toDetailResponse(post: Post): PostDetailResponse {
        val author =
            userRepository
                .findById(post.authorId)
                .orElse(null)
        return PostDetailResponse(
            id = post.id,
            title = post.title,
            url = post.url,
            content = post.content,
            summary = post.summary,
            score = post.score,
            hotScore = post.hotScore,
            commentCount = post.commentCount,
            type = post.type.name,
            authorId = post.authorId,
            authorUsername = author?.username ?: "deleted",
            createdAt = post.createdAt,
        )
    }
}
