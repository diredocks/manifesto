package com.project.manifesto.modules.vote.service

import com.project.manifesto.modules.ranking.service.RankingService
import com.project.manifesto.modules.vote.entity.Vote
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val rankingService: RankingService
) {

    @Transactional
    fun upvote(userId: Long, postId: Long): Boolean {
        if (voteRepository.existsByUserIdAndPostId(userId, postId)) {
            return true
        }
        voteRepository.save(Vote(userId = userId, postId = postId))
        rankingService.recalculatePostScore(postId)
        return true
    }

    @Transactional
    fun removeVote(userId: Long, postId: Long): Boolean {
        voteRepository.deleteByUserIdAndPostId(userId, postId)
        rankingService.recalculatePostScore(postId)
        return true
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
