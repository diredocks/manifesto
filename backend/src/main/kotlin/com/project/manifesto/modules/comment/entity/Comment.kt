package com.project.manifesto.modules.comment.entity

import com.project.manifesto.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "comments",
    indexes = [
        Index(name = "idx_comments_post_id", columnList = "post_id"),
        Index(name = "idx_comments_author_id", columnList = "author_id"),
        Index(name = "idx_comments_parent_id", columnList = "parent_id")
    ]
)
class Comment(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "author_id", nullable = false)
    val authorId: Long,

    @Column(name = "parent_id")
    var parentId: Long? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Column(nullable = false)
    var score: Int = 0,

    @Column(nullable = false)
    var depth: Int = 0,

    @Column(nullable = false)
    var deleted: Boolean = false
) : BaseEntity()
