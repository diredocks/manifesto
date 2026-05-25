package com.project.manifesto.modules.ranking.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.submit.service.PostService
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.Instant

@Service
class RankingService(
    private val postRepository: PostRepository,
    private val postService: PostService,
    private val voteRepository: VoteRepository,
    @Autowired(required = false) private val redisTemplate: StringRedisTemplate?,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private const val CACHE_KEY = "feed:hot:top100"
        private val CACHE_TTL = Duration.ofMinutes(5)
    }

    fun getNewPosts(pageable: Pageable): List<PostResponse> {
        return postService.listNewPosts(pageable).content
    }

    fun getTopPosts(pageable: Pageable): List<PostResponse> {
        return postService.listTopPosts(pageable).content
    }

    fun getHotPosts(pageable: Pageable): List<PostResponse> {
        if (pageable.pageNumber == 0 && pageable.pageSize <= 100) {
            val cached = getCachedHotPosts()
            if (cached != null) {
                val from = pageable.offset.toInt()
                val to = (from + pageable.pageSize).coerceAtMost(cached.size)
                if (from < cached.size) {
                    return cached.subList(from, to)
                }
            }
        }
        val posts = postService.listHotPosts(pageable).content
        if (pageable.pageNumber == 0) {
            cacheHotPosts(posts)
        }
        return posts
    }

    fun recalculateScores() {
        val posts = postRepository.findByDeletedFalseOrderByHotScoreDesc(PageRequest.of(0, 200))
        for (post in posts) {
            val score = voteRepository.countByPostId(post.id)
            val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
            val hotScore = score / Math.pow(hours + 2.0, 1.5)
            postRepository.updateScoreAndHotScore(post.id, 0, hotScore)
        }
        cacheHotPosts(posts.content.map { postService.toPostResponse(it) })
    }

    fun recalculatePostScore(postId: Long) {
        val post = postRepository.findById(postId).orElse(null) ?: return
        val score = voteRepository.countByPostId(postId)
        val delta = score - post.score
        val hours = Duration.between(post.createdAt, Instant.now()).toHours().toDouble()
        val hotScore = score / Math.pow(hours + 2.0, 1.5)
        postRepository.updateScoreAndHotScore(postId, delta, hotScore)
    }

    private fun getCachedHotPosts(): List<PostResponse>? {
        val rt = redisTemplate ?: return null
        val json = rt.opsForValue().get(CACHE_KEY) ?: return null
        return try {
            val node = objectMapper.readTree(json)
            node.map { objectMapper.treeToValue(it, PostResponse::class.java) }
        } catch (_: Exception) {
            null
        }
    }

    private fun cacheHotPosts(posts: List<PostResponse>) {
        val rt = redisTemplate ?: return
        val json = objectMapper.writeValueAsString(posts.take(100))
        rt.opsForValue().set(CACHE_KEY, json, CACHE_TTL)
    }
}
