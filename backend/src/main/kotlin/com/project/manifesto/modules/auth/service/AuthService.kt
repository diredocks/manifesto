package com.project.manifesto.modules.auth.service

import com.project.manifesto.modules.auth.dto.AuthResponse
import com.project.manifesto.modules.auth.dto.LoginRequest
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.auth.dto.UserInfoResponse
import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.security.JwtTokenProvider
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider,
    private val authenticationManager: AuthenticationManager,
) {
    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        require(!userRepository.existsByUsername(request.username)) { "Username already taken" }
        require(!userRepository.existsByEmail(request.email)) { "Email already registered" }

        val user =
            User(
                username = request.username,
                email = request.email,
                passwordHash = passwordEncoder.encode(request.password),
                karma = 0,
                role = UserRole.ROLE_USER,
            )
        userRepository.save(user)

        val token = jwtTokenProvider.generateToken(user.username, user.role.name)
        return AuthResponse(
            token = token,
            username = user.username,
            role = user.role.name,
        )
    }

    fun login(request: LoginRequest): AuthResponse {
        val authToken = UsernamePasswordAuthenticationToken(request.username, request.password)
        authenticationManager.authenticate(authToken)

        val user =
            userRepository.findByUsername(request.username)
                ?: throw IllegalArgumentException("User not found")

        val token = jwtTokenProvider.generateToken(user.username, user.role.name)
        return AuthResponse(
            token = token,
            username = user.username,
            role = user.role.name,
        )
    }

    fun getCurrentUser(username: String): UserInfoResponse {
        val user =
            userRepository.findByUsername(username)
                ?: throw IllegalArgumentException("User not found")

        return UserInfoResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            karma = user.karma,
            role = user.role.name,
        )
    }
}
