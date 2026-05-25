package com.project.manifesto.modules.ai.service

import com.project.manifesto.modules.ai.entity.PostTag
import com.project.manifesto.modules.ai.entity.Tag
import com.project.manifesto.modules.ai.repository.PostTagRepository
import com.project.manifesto.modules.ai.repository.TagRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class TagService(
    private val tagRepository: TagRepository,
    private val postTagRepository: PostTagRepository
) {

    @Transactional
    fun assignTags(postId: Long, tagNames: List<String>) {
        postTagRepository.findByPostId(postId)
            .forEach { postTagRepository.delete(it) }

        for (name in tagNames.map { it.lowercase().trim() }) {
            if (name.isBlank()) continue
            var tag = tagRepository.findByName(name)
            if (tag == null) {
                tag = tagRepository.save(Tag(name = name))
            }
            if (!postTagRepository.existsByPostIdAndTagId(postId, tag.id)) {
                postTagRepository.save(PostTag(postId = postId, tagId = tag.id))
            }
        }
    }

    @Transactional(readOnly = true)
    fun getTagsForPost(postId: Long): List<String> {
        val postTags = postTagRepository.findByPostId(postId)
        return postTags.map { pt ->
            tagRepository.findById(pt.tagId).orElse(null)?.name
        }.filterNotNull()
    }

    @Transactional(readOnly = true)
    fun getPostIdsByTag(tagName: String): List<Long> {
        val tag = tagRepository.findByName(tagName.lowercase())
        return if (tag != null) {
            postTagRepository.findByTagId(tag.id).map { it.postId }
        } else emptyList()
    }

    @Transactional(readOnly = true)
    fun getAllTags(): List<String> {
        return tagRepository.findAll().map { it.name }.sorted()
    }
}
