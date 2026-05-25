package com.project.manifesto.modules.vote.repository

import com.project.manifesto.modules.vote.entity.Vote
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface VoteRepository : JpaRepository<Vote, Long> {
    fun findByUserIdAndPostId(userId: Long, postId: Long): Vote?
    fun existsByUserIdAndPostId(userId: Long, postId: Long): Boolean
    fun countByPostId(postId: Long): Int
    fun deleteByUserIdAndPostId(userId: Long, postId: Long)
}
