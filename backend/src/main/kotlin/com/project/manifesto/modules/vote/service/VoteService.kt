package com.project.manifesto.modules.vote.service

import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.ranking.service.RankingService
import com.project.manifesto.modules.vote.entity.Vote
import com.project.manifesto.modules.vote.repository.VoteRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val rankingService: RankingService,
    private val commentRepository: CommentRepository
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

    @Transactional
    fun upvoteComment(userId: Long, commentId: Long): Boolean {
        if (voteRepository.existsByUserIdAndCommentId(userId, commentId)) {
            return true
        }
        voteRepository.save(Vote(userId = userId, commentId = commentId))
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found: $commentId") }
        comment.score += 1
        commentRepository.save(comment)
        return true
    }

    @Transactional
    fun removeVoteComment(userId: Long, commentId: Long): Boolean {
        voteRepository.deleteByUserIdAndCommentId(userId, commentId)
        val comment = commentRepository.findById(commentId)
            .orElseThrow { EntityNotFoundException("Comment not found: $commentId") }
        if (comment.score > 0) {
            comment.score -= 1
            commentRepository.save(comment)
        }
        return true
    }

    @Transactional(readOnly = true)
    fun hasVotedComment(userId: Long, commentId: Long): Boolean {
        return voteRepository.existsByUserIdAndCommentId(userId, commentId)
    }

    @Transactional(readOnly = true)
    fun getCommentVoteCount(commentId: Long): Int {
        return voteRepository.countByCommentId(commentId)
    }
}
