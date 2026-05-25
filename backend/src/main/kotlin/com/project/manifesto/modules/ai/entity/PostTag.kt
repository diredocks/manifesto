package com.project.manifesto.modules.ai.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

@Entity
@Table(
    name = "post_tags",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_post_tag", columnNames = ["post_id", "tag_id"])
    ],
    indexes = [
        Index(name = "idx_post_tags_tag_id", columnList = "tag_id")
    ]
)
class PostTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "tag_id", nullable = false)
    val tagId: Long
)
