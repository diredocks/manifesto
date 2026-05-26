package com.project.manifesto.modules.vote.repository

import com.project.manifesto.modules.vote.entity.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository : JpaRepository<Vote, Long> {
    fun existsByUserIdAndPostId(
        userId: Long,
        postId: Long,
    ): Boolean

    fun countByPostId(postId: Long): Int

    fun deleteByUserIdAndPostId(
        userId: Long,
        postId: Long,
    )

    fun existsByUserIdAndCommentId(
        userId: Long,
        commentId: Long,
    ): Boolean

    fun countByCommentId(commentId: Long): Int

    fun deleteByUserIdAndCommentId(
        userId: Long,
        commentId: Long,
    )
}
