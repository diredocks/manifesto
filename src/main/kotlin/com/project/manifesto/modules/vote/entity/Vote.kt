package com.project.manifesto.modules.vote.entity

import com.project.manifesto.common.entity.BaseEntity
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
    name = "votes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_vote_user_post", columnNames = ["user_id", "post_id"])
    ],
    indexes = [
        Index(name = "idx_votes_post_id", columnList = "post_id")
    ]
)
class Vote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(name = "user_id", nullable = false)
    val userId: Long,

    @Column(name = "post_id", nullable = false)
    val postId: Long
) : BaseEntity()
