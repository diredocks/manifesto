package com.project.manifesto.modules.vote.service

import com.project.manifesto.infra.redis.LockService
import com.project.manifesto.modules.vote.entity.Vote
import com.project.manifesto.modules.vote.event.PostVotedEvent
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val lockService: LockService,
    private val publisher: ApplicationEventPublisher
) {

    @Transactional
    fun upvote(userId: Long, postId: Long): Boolean {
        val lockKey = "vote:post:$postId:user:$userId"
        val lockToken = lockService.tryLock(lockKey, ttlSeconds = 5)
            ?: throw IllegalStateException("Could not acquire vote lock, please try again")

        return try {
            if (voteRepository.existsByUserIdAndPostId(userId, postId)) {
                return true
            }
            voteRepository.save(Vote(userId = userId, postId = postId))
            publisher.publishEvent(PostVotedEvent(postId, userId, voted = true))
            true
        } finally {
            lockService.unlock(lockToken)
        }
    }

    @Transactional
    fun removeVote(userId: Long, postId: Long): Boolean {
        val lockKey = "vote:post:$postId:user:$userId"
        val lockToken = lockService.tryLock(lockKey, ttlSeconds = 5)
            ?: throw IllegalStateException("Could not acquire vote lock, please try again")

        return try {
            voteRepository.deleteByUserIdAndPostId(userId, postId)
            publisher.publishEvent(PostVotedEvent(postId, userId, voted = false))
            true
        } finally {
            lockService.unlock(lockToken)
        }
    }

    @Transactional(readOnly = true)
    fun hasVoted(userId: Long, postId: Long): Boolean {
        return voteRepository.existsByUserIdAndPostId(userId, postId)
    }

    @Transactional(readOnly = true)
    fun getVoteCount(postId: Long): Int {
        return voteRepository.countByPostId(postId)
    }
}
