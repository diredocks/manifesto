package com.project.manifesto.modules.ai.repository

import com.project.manifesto.modules.ai.entity.PostTag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PostTagRepository : JpaRepository<PostTag, Long> {
    fun findByPostId(postId: Long): List<PostTag>
    fun findByTagId(tagId: Long): List<PostTag>
    fun existsByPostIdAndTagId(postId: Long, tagId: Long): Boolean
    fun deleteByPostIdAndTagId(postId: Long, tagId: Long)
}
