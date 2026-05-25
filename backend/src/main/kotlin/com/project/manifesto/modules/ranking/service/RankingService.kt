package com.project.manifesto.modules.ranking.service

import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.submit.service.PostService
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.transaction.annotation.Transactional
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class RankingService(
    private val postRepository: PostRepository,
    private val postService: PostService,
    private val voteRepository: VoteRepository
) {

    fun getNewPosts(pageable: Pageable): List<PostResponse> {
        return postService.listNewPosts(pageable).content
    }

    fun getTopPosts(pageable: Pageable): List<PostResponse> {
        return postService.listTopPosts(pageable).content
    }

    fun getHotPosts(pageable: Pageable): List<PostResponse> {
        return postService.listHotPosts(pageable).content
    }

    @Transactional
    fun recalculateScores() {
        val posts = postRepository.findByDeletedFalseOrderByHotScoreDesc(PageRequest.of(0, 200))
        for (post in posts) {
            val score = voteRepository.countByPostId(post.id)
            val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
            val hotScore = score / Math.pow(hours + 2.0, 1.5)
            postRepository.updateScoreAndHotScore(post.id, 0, hotScore)
        }
    }

    @Transactional
    fun recalculatePostScore(postId: Long) {
        val post = postRepository.findById(postId).orElse(null) ?: return
        val score = voteRepository.countByPostId(postId)
        val delta = score - post.score
        val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
        val hotScore = score / Math.pow(hours + 2.0, 1.5)
        postRepository.updateScoreAndHotScore(postId, delta, hotScore)
    }
}
