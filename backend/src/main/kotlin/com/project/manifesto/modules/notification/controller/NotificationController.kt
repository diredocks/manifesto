package com.project.manifesto.modules.notification.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.notification.dto.NotificationResponse
import com.project.manifesto.modules.notification.service.NotificationService
import com.project.manifesto.modules.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Notification APIs")
class NotificationController(
    private val notificationService: NotificationService,
    private val userService: UserService
) {

    @GetMapping
    @Operation(summary = "Get user notifications")
    fun getNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Page<NotificationResponse>>> {
        val user = userService.findByUsername(authentication.name)
        val result = notificationService.getUserNotifications(user.id, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get unread notification count")
    fun getUnreadCount(authentication: Authentication): ResponseEntity<ApiResponse<Long>> {
        val user = userService.findByUsername(authentication.name)
        val result = notificationService.getUnreadCount(user.id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark notification as read")
    fun markAsRead(
        @PathVariable id: Long,
        authentication: Authentication
    ): ResponseEntity<ApiResponse<Boolean>> {
        val user = userService.findByUsername(authentication.name)
        val result = notificationService.markAsRead(id, user.id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }
}
