package com.project.manifesto.modules.auth.service

import com.project.manifesto.modules.auth.dto.UserListItem
import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AdminService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun listUsers(): List<UserListItem> {
        return userRepository.findAll().map { it.toListItem() }
    }

    @Transactional
    fun changeUserRole(userId: Long, role: String): UserListItem {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found: $userId") }

        val newRole = try {
            UserRole.valueOf(role)
        } catch (_: IllegalArgumentException) {
            throw IllegalArgumentException(
                "Invalid role: $role. Valid values: ${UserRole.entries.joinToString()}"
            )
        }

        if (user.role == UserRole.ROLE_ADMIN && newRole != UserRole.ROLE_ADMIN) {
            val adminCount = userRepository.countByRole(UserRole.ROLE_ADMIN)
            require(adminCount > 1) {
                "Cannot change role: at least one admin must exist"
            }
        }

        user.role = newRole
        val saved = userRepository.save(user)

        return saved.toListItem()
    }

    @Transactional
    fun banUser(userId: Long, durationHours: Long): UserListItem {
        require(durationHours > 0) { "Ban duration must be positive" }
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found: $userId") }
        require(user.role != UserRole.ROLE_ADMIN) { "Cannot ban an admin" }
        user.bannedUntil = Instant.now().plusSeconds(durationHours * 3600)
        return userRepository.save(user).toListItem()
    }

    @Transactional
    fun unbanUser(userId: Long): UserListItem {
        val user = userRepository.findById(userId)
            .orElseThrow { EntityNotFoundException("User not found: $userId") }
        user.bannedUntil = null
        return userRepository.save(user).toListItem()
    }

    private fun User.toListItem() = UserListItem(
        id = id,
        username = username,
        email = email,
        karma = karma,
        role = role.name,
        bannedUntil = bannedUntil
    )
}
