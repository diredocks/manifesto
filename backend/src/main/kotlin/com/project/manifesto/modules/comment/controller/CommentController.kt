package com.project.manifesto.modules.comment.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.comment.dto.CommentResponse
import com.project.manifesto.modules.comment.dto.CreateCommentRequest
import com.project.manifesto.modules.comment.dto.UserCommentResponse
import com.project.manifesto.modules.comment.service.CommentService
import com.project.manifesto.modules.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Comments", description = "Comment APIs")
class CommentController(
    private val commentService: CommentService,
    private val userService: UserService
) {

    @PostMapping("/posts/{postId}/comments")
    @Operation(summary = "Create a comment on a post")
    fun createComment(
        @PathVariable postId: Long,
        @Valid @RequestBody request: CreateCommentRequest,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<CommentResponse>> {
        val user = userService.findByUsername(authentication.name)
        val response = commentService.createComment(user.id, postId, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/posts/{postId}/comments")
    @Operation(summary = "Get comment tree for a post")
    fun getComments(@PathVariable postId: Long): ResponseEntity<ApiResponse<List<CommentResponse>>> {
        val response = commentService.getCommentTree(postId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/comments/user/{username}")
    @Operation(summary = "Get comments by username")
    fun getCommentsByUser(
        @PathVariable username: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<Page<UserCommentResponse>>> {
        val user = userService.findByUsername(username)
        val pageable = PageRequest.of(page, size)
        val result = commentService.listCommentsByUser(user.id, pageable)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "Delete own comment")
    fun deleteComment(
        @PathVariable commentId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val response = commentService.deleteComment(commentId, user.id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
