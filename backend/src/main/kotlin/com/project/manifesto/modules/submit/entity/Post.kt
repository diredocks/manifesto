package com.project.manifesto.modules.submit.entity

import com.project.manifesto.common.entity.BaseEntity
import com.project.manifesto.common.entity.StringListConverter
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "posts",
    indexes = [
        Index(name = "idx_posts_hot_score", columnList = "hot_score DESC"),
        Index(name = "idx_posts_author", columnList = "author_id"),
        Index(name = "idx_posts_created_at", columnList = "created_at DESC"),
    ],
)
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,
    @Column(name = "author_id", nullable = false)
    val authorId: Long,
    @Column(nullable = false, length = 300)
    var title: String,
    @Column(length = 2000)
    var url: String? = null,
    @Column(columnDefinition = "TEXT")
    var content: String? = null,
    @Column(columnDefinition = "TEXT")
    var summary: String? = null,
    @Column(nullable = false)
    var score: Int = 0,
    @Column(name = "hot_score", nullable = false)
    var hotScore: Double = 0.0,
    @Column(name = "comment_count", nullable = false)
    var commentCount: Int = 0,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    var type: PostType = PostType.LINK,
    @Column(nullable = false)
    var deleted: Boolean = false,
    @Convert(converter = StringListConverter::class)
    @Column(columnDefinition = "TEXT")
    var tags: List<String>? = null,
) : BaseEntity()
