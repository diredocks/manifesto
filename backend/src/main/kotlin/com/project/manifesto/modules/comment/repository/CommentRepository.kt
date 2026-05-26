package com.project.manifesto.modules.comment.repository

import com.project.manifesto.modules.comment.entity.Comment
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface CommentRepository : JpaRepository<Comment, Long> {
    fun findByPostIdAndParentIdIsNullAndDeletedFalseOrderByCreatedAtDesc(
        postId: Long,
        pageable: Pageable,
    ): Page<Comment>

    fun findByPostIdAndDeletedFalseOrderByCreatedAtAsc(postId: Long): List<Comment>

    fun findByParentIdAndDeletedFalseOrderByCreatedAtAsc(parentId: Long): List<Comment>

    fun countByPostIdAndDeletedFalse(postId: Long): Int

    fun findByPostIdAndDeletedFalse(
        postId: Long,
        pageable: Pageable,
    ): Page<Comment>

    fun findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(
        authorId: Long,
        pageable: Pageable,
    ): Page<Comment>
}
