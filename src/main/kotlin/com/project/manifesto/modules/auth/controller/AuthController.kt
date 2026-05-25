package com.project.manifesto.modules.auth.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.auth.dto.AuthResponse
import com.project.manifesto.modules.auth.dto.LoginRequest
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.auth.dto.UserInfoResponse
import com.project.manifesto.modules.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Authentication APIs")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.register(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/login")
    @Operation(summary = "Login and get JWT token")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<ApiResponse<AuthResponse>> {
        val response = authService.login(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user info")
    fun me(authentication: Authentication): ResponseEntity<ApiResponse<UserInfoResponse>> {
        val response = authService.getCurrentUser(authentication.name)
        return ResponseEntity.ok(ApiResponse.success(response))
    }
}
