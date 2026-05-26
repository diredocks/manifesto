package com.project.manifesto.modules.ranking.service

import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.submit.service.PostService
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant

@Service
class RankingService(
    private val postRepository: PostRepository,
    private val postService: PostService,
    private val voteRepository: VoteRepository,
) {
    companion object {
        private const val HOT_SCORE_GRAVITY = 1.5
        private const val RECALC_BATCH_SIZE = 200
    }

    fun getNewPosts(
        pageable: Pageable,
        type: PostType? = null,
    ): List<PostResponse> {
        val page =
            if (type != null) {
                postService.listNewPostsByType(type, pageable)
            } else {
                postService.listNewPosts(pageable)
            }
        return page.content
    }

    fun getTopPosts(
        pageable: Pageable,
        type: PostType? = null,
    ): List<PostResponse> {
        val page =
            if (type != null) {
                postService.listTopPostsByType(type, pageable)
            } else {
                postService.listTopPosts(pageable)
            }
        return page.content
    }

    fun getHotPosts(
        pageable: Pageable,
        type: PostType? = null,
    ): List<PostResponse> {
        val page =
            if (type != null) {
                postService.listHotPostsByType(type, pageable)
            } else {
                postService.listHotPosts(pageable)
            }
        return page.content
    }

    @Transactional
    fun recalculateScores() {
        val posts = postRepository.findByDeletedFalseOrderByHotScoreDesc(PageRequest.of(0, RECALC_BATCH_SIZE))
        for (post in posts) {
            val score = voteRepository.countByPostId(post.id)
            val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
            val hotScore = score / Math.pow(hours + 2.0, HOT_SCORE_GRAVITY)
            postRepository.updateScoreAndHotScore(post.id, 0, hotScore)
        }
    }

    @Transactional
    fun recalculatePostScore(postId: Long) {
        val post = postRepository.findById(postId).orElse(null) ?: return
        val score = voteRepository.countByPostId(postId)
        val delta = score - post.score
        val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
        val hotScore = score / Math.pow(hours + 2.0, HOT_SCORE_GRAVITY)
        postRepository.updateScoreAndHotScore(postId, delta, hotScore)
    }
}
