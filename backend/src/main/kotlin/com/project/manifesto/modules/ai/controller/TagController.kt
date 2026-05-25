package com.project.manifesto.modules.ai.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.ai.service.TagService
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
@Tag(name = "Tags", description = "Tag APIs")
class TagController(
    private val tagService: TagService,
    private val postService: PostService
) {

    @GetMapping
    @Operation(summary = "List all tags")
    fun listTags(): ResponseEntity<ApiResponse<List<String>>> {
        return ResponseEntity.ok(ApiResponse.success(tagService.getAllTags()))
    }

    @GetMapping("/{tagName}/posts")
    @Operation(summary = "Get posts by tag")
    fun getPostsByTag(@PathVariable tagName: String): ResponseEntity<ApiResponse<List<PostResponse>>> {
        val postIds = tagService.getPostIdsByTag(tagName)
        val posts = postIds.mapNotNull { postId ->
            runCatching { postService.getPostById(postId) }.getOrNull()
        }.map {
            PostResponse(
                id = it.id,
                title = it.title,
                url = it.url,
                content = it.content,
                summary = it.summary,
                score = it.score,
                hotScore = it.hotScore,
                commentCount = it.commentCount,
                type = it.type,
                authorUsername = it.authorUsername,
                createdAt = it.createdAt,
                tags = it.tags
            )
        }
        return ResponseEntity.ok(ApiResponse.success(posts))
    }
}
