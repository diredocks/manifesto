package com.project.manifesto.modules.auth.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.auth.dto.UserListItem
import com.project.manifesto.modules.auth.service.AdminService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin tools")
class AdminController(
    private val adminService: AdminService
) {

    @GetMapping("/users")
    @Operation(summary = "List all users (admin only)")
    fun listUsers(): ResponseEntity<ApiResponse<List<UserListItem>>> {
        return ResponseEntity.ok(ApiResponse.success(adminService.listUsers()))
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change user role (admin only)")
    fun changeUserRole(
        @PathVariable id: Long,
        @RequestParam role: String
    ): ResponseEntity<ApiResponse<UserListItem>> {
        return ResponseEntity.ok(ApiResponse.success(adminService.changeUserRole(id, role)))
    }
}
