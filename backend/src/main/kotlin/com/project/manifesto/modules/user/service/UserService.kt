package com.project.manifesto.modules.user.service

import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.repository.UserRepository
import org.springframework.stereotype.Service

@Service
class UserService(
    private val userRepository: UserRepository,
) {
    fun findByUsername(username: String): User =
        userRepository.findByUsername(username)
            ?: throw IllegalArgumentException("User not found: $username")
}
