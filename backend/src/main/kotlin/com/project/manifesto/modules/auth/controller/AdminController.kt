package com.project.manifesto.modules.auth.controller

import com.project.manifesto.common.dto.ApiResponse
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.persistence.EntityNotFoundException
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

data class UserListItem(
    val id: Long,
    val username: String,
    val email: String,
    val karma: Int,
    val role: String
)

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin tools")
class AdminController(
    private val userRepository: UserRepository
) {

    @GetMapping("/users")
    @Operation(summary = "List all users (admin only)")
    fun listUsers(): ResponseEntity<ApiResponse<List<UserListItem>>> {
        val users = userRepository.findAll().map { user ->
            UserListItem(
                id = user.id,
                username = user.username,
                email = user.email,
                karma = user.karma,
                role = user.role.name
            )
        }
        return ResponseEntity.ok(ApiResponse.success(users))
    }

    @PutMapping("/users/{id}/role")
    @Operation(summary = "Change user role (admin only)")
    fun changeUserRole(
        @PathVariable id: Long,
        @RequestParam role: String
    ): ResponseEntity<ApiResponse<UserListItem>> {
        val user = userRepository.findById(id)
            .orElseThrow { EntityNotFoundException("User not found: $id") }

        val newRole = try {
            UserRole.valueOf(role)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException("Invalid role: $role. Valid values: ${UserRole.entries.joinToString()}")
        }

        user.role = newRole
        val saved = userRepository.save(user)

        return ResponseEntity.ok(ApiResponse.success(
            UserListItem(
                id = saved.id,
                username = saved.username,
                email = saved.email,
                karma = saved.karma,
                role = saved.role.name
            )
        ))
    }
}
