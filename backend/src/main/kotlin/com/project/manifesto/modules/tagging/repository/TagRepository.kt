package com.project.manifesto.modules.tagging.repository

import com.project.manifesto.modules.tagging.entity.Tag
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TagRepository : JpaRepository<Tag, Long> {
    fun findByName(name: String): Tag?

    fun findByNameIn(names: List<String>): List<Tag>
}
