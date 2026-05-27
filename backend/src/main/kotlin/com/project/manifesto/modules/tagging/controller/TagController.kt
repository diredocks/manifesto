package com.project.manifesto.modules.tagging.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.service.PostService
import com.project.manifesto.modules.tagging.service.TagService
import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/tags")
class TagController(
    private val tagService: TagService,
    private val postService: PostService,
) {
    @GetMapping
    @Operation(summary = "List all tags")
    fun listTags(): ResponseEntity<ApiResponse<List<String>>> = ResponseEntity.ok(ApiResponse.success(tagService.getAllTags()))

    @GetMapping("/{tagName}/posts")
    @Operation(summary = "Get posts by tag")
    fun getPostsByTag(
        @PathVariable tagName: String,
    ): ResponseEntity<ApiResponse<List<PostResponse>>> {
        val postIds = tagService.getPostIdsByTag(tagName)
        val posts = postService.listPostsByIds(postIds)
        return ResponseEntity.ok(ApiResponse.success(posts))
    }
}
