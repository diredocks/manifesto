package com.project.manifesto.modules.vote.service

import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.ranking.service.RankingService
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.entity.Vote
import com.project.manifesto.modules.vote.repository.VoteRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class VoteService(
    private val voteRepository: VoteRepository,
    private val rankingService: RankingService,
    private val commentRepository: CommentRepository,
    private val postRepository: PostRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun upvote(userId: Long, postId: Long): Boolean {
        if (voteRepository.existsByUserIdAndPostId(userId, postId)) {
            return true
        }
        voteRepository.save(Vote(userId = userId, postId = postId))
        rankingService.recalculatePostScore(postId)
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        val author = userRepository.findById(post.authorId)
            .orElseThrow { EntityNotFoundException("User not found: ${post.authorId}") }
        author.karma += 1
        userRepository.save(author)
        return true
    }

    @Transactional
    fun removeVote(userId: Long, postId: Long): Boolean {
        voteRepository.deleteByUserIdAndPostId(userId, postId)
        rankingService.recalculatePostScore(postId)
        val post = postRepository.findById(postId)
            .orElseThrow { EntityNotFoundException("Post not found: $postId") }
        val author = userRepository.findById(post.authorId)
            .orElseThrow { EntityNotFoundException("User not found: ${post.authorId}") }
        if (author.karma > 0) {
            author.karma -= 1
            userRepository.save(author)
        }
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
        val author = userRepository.findById(comment.authorId)
            .orElseThrow { EntityNotFoundException("User not found: ${comment.authorId}") }
        author.karma += 1
        userRepository.save(author)
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
        val author = userRepository.findById(comment.authorId)
            .orElseThrow { EntityNotFoundException("User not found: ${comment.authorId}") }
        if (author.karma > 0) {
            author.karma -= 1
            userRepository.save(author)
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
