package com.project.manifesto.modules.submit.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.dto.PostDetailResponse
import com.project.manifesto.modules.submit.dto.PostResponse
import com.project.manifesto.modules.submit.service.PostService
import com.project.manifesto.modules.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/posts")
@Tag(name = "Posts", description = "Post submission APIs")
class PostController(
    private val postService: PostService,
    private val userRepository: UserRepository
) {

    @PostMapping
    @Operation(summary = "Create a new post")
    fun createPost(
        @Valid @RequestBody request: CreatePostRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<PostDetailResponse>> {
        val user = userRepository.findByUsername(authentication.name)
            ?: throw IllegalArgumentException("User not found")
        val response = postService.createPost(user.id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get post by ID")
    fun getPost(@PathVariable id: Long): ResponseEntity<ApiResponse<PostDetailResponse>> {
        val response = postService.getPostById(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping
    @Operation(summary = "List posts")
    fun listPosts(
        @RequestParam(defaultValue = "new") sort: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<PostResponse>>> {
        val pageable = PageRequest.of(page, size)
        val result = when (sort.lowercase()) {
            "hot" -> postService.listHotPosts(pageable)
            "top" -> postService.listTopPosts(pageable)
            else -> postService.listNewPosts(pageable)
        }
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/user/{username}")
    @Operation(summary = "Get posts by username")
    fun getPostsByUser(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<PostResponse>>> {
        val user = userRepository.findByUsername(username)
            ?: return ResponseEntity.notFound().build()
        val pageable = PageRequest.of(page, size)
        val result = postService.listPostsByUser(user.id, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete own post")
    fun deletePost(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userRepository.findByUsername(authentication.name)
            ?: throw IllegalArgumentException("User not found")
        val response = postService.deletePost(id, user.id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
