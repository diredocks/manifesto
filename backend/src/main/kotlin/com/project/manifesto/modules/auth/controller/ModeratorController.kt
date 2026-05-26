package com.project.manifesto.modules.auth.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.auth.dto.UserListItem
import com.project.manifesto.modules.auth.service.AdminService
import com.project.manifesto.modules.comment.service.CommentService
import com.project.manifesto.modules.submit.service.PostService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/moderator")
@PreAuthorize("hasAnyRole('MODERATOR', 'ADMIN')")
@Tag(name = "Moderator", description = "Moderator tools")
class ModeratorController(
    private val postService: PostService,
    private val commentService: CommentService,
    private val adminService: AdminService,
) {
    @DeleteMapping("/posts/{id}")
    @Operation(summary = "Delete any post (moderator/admin)")
    fun deletePost(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Boolean>> {
        postService.deletePostAsModerator(id)
        return ResponseEntity.ok(ApiResponse.success(true))
    }

    @DeleteMapping("/comments/{id}")
    @Operation(summary = "Delete any comment (moderator/admin)")
    fun deleteComment(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<Boolean>> {
        commentService.deleteCommentAsModerator(id)
        return ResponseEntity.ok(ApiResponse.success(true))
    }

    @GetMapping("/users")
    @Operation(summary = "List all users (moderator/admin)")
    fun listUsers(): ResponseEntity<ApiResponse<List<UserListItem>>> = ResponseEntity.ok(ApiResponse.success(adminService.listUsers()))

    @PostMapping("/users/{id}/ban")
    @Operation(summary = "Ban a user for a duration in hours (moderator/admin)")
    fun banUser(
        @PathVariable id: Long,
        @RequestParam durationHours: Long,
    ): ResponseEntity<ApiResponse<UserListItem>> = ResponseEntity.ok(ApiResponse.success(adminService.banUser(id, durationHours)))

    @DeleteMapping("/users/{id}/ban")
    @Operation(summary = "Unban a user (moderator/admin)")
    fun unbanUser(
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<UserListItem>> = ResponseEntity.ok(ApiResponse.success(adminService.unbanUser(id)))
}
