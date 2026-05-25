package com.project.manifesto.modules.user.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.user.dto.UserProfileResponse
import com.project.manifesto.modules.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "User APIs")
class UserController(
    private val userService: UserService
) {

    @GetMapping("/{username}")
    @Operation(summary = "Get public user profile by username")
    fun getUserProfile(@PathVariable username: String): ResponseEntity<ApiResponse<UserProfileResponse>> {
        val user = userService.findByUsername(username)
        val response = UserProfileResponse(
            username = user.username,
            karma = user.karma,
            createdAt = user.createdAt
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
