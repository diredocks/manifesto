package com.project.manifesto.modules.auth.service

import com.project.manifesto.modules.auth.dto.UserListItem
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AdminService(
    private val userRepository: UserRepository
) {

    @Transactional(readOnly = true)
    fun listUsers(): List<UserListItem> {
        return userRepository.findAll().map { user ->
            UserListItem(
                id = user.id,
                username = user.username,
                email = user.email,
                karma = user.karma,
                role = user.role.name
            )
        }
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

        user.role = newRole
        val saved = userRepository.save(user)

        return UserListItem(
            id = saved.id,
            username = saved.username,
            email = saved.email,
            karma = saved.karma,
            role = saved.role.name
        )
    }
}
