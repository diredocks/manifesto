package com.project.manifesto.modules.vote.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.user.service.UserService
import com.project.manifesto.modules.vote.service.VoteService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Voting", description = "Vote APIs")
class VoteController(
    private val voteService: VoteService,
    private val userService: UserService
) {

    @PostMapping("/posts/{postId}/upvote")
    @Operation(summary = "Upvote a post")
    fun upvote(
        @PathVariable postId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.upvote(user.id, postId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @DeleteMapping("/posts/{postId}/upvote")
    @Operation(summary = "Remove upvote from a post")
    fun removeVote(
        @PathVariable postId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.removeVote(user.id, postId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/posts/{postId}/vote-status")
    @Operation(summary = "Check if current user has voted on a post")
    fun voteStatus(
        @PathVariable postId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.hasVoted(user.id, postId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/posts/{postId}/vote-count")
    @Operation(summary = "Get vote count for a post")
    fun voteCount(@PathVariable postId: Long): ResponseEntity<ApiResponse<Int>> {
        val result = voteService.getVoteCount(postId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @PostMapping("/comments/{commentId}/upvote")
    @Operation(summary = "Upvote a comment")
    fun upvoteComment(
        @PathVariable commentId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.upvoteComment(user.id, commentId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @DeleteMapping("/comments/{commentId}/upvote")
    @Operation(summary = "Remove upvote from a comment")
    fun removeVoteComment(
        @PathVariable commentId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.removeVoteComment(user.id, commentId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/comments/{commentId}/vote-status")
    @Operation(summary = "Check if current user has voted on a comment")
    fun voteStatusComment(
        @PathVariable commentId: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = voteService.hasVotedComment(user.id, commentId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/comments/{commentId}/vote-count")
    @Operation(summary = "Get vote count for a comment")
    fun voteCountComment(@PathVariable commentId: Long): ResponseEntity<ApiResponse<Int>> {
        val result = voteService.getCommentVoteCount(commentId)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
