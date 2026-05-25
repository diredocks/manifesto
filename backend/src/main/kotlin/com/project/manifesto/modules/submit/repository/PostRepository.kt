package com.project.manifesto.modules.submit.repository

import com.project.manifesto.modules.submit.entity.Post
import com.project.manifesto.modules.submit.entity.PostType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

@Repository
interface PostRepository : JpaRepository<Post, Long> {

    fun findByDeletedFalseOrderByCreatedAtDesc(pageable: Pageable): Page<Post>

    fun findByDeletedFalseOrderByHotScoreDesc(pageable: Pageable): Page<Post>

    fun findByDeletedFalseOrderByScoreDesc(pageable: Pageable): Page<Post>

    fun findByTypeAndDeletedFalseOrderByCreatedAtDesc(type: PostType, pageable: Pageable): Page<Post>

    fun findByTypeAndDeletedFalseOrderByHotScoreDesc(type: PostType, pageable: Pageable): Page<Post>

    fun findByTypeAndDeletedFalseOrderByScoreDesc(type: PostType, pageable: Pageable): Page<Post>

    @Modifying
    @Query("UPDATE Post p SET p.commentCount = p.commentCount + 1 WHERE p.id = :postId")
    fun incrementCommentCount(@Param("postId") postId: Long)

    @Modifying
    @Query("UPDATE Post p SET p.score = p.score + :delta, p.hotScore = :hotScore WHERE p.id = :postId")
    fun updateScoreAndHotScore(
        @Param("postId") postId: Long,
        @Param("delta") delta: Int,
        @Param("hotScore") hotScore: Double
    )

    fun findByAuthorIdAndDeletedFalseOrderByCreatedAtDesc(authorId: Long, pageable: Pageable): Page<Post>

    fun findByIdInAndDeletedFalse(ids: List<Long>): List<Post>
}
