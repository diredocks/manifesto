package com.project.manifesto.modules.auth.config

import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class AdminInitializer(
    private val adminProperties: AdminProperties,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(AdminInitializer::class.java)

    @Transactional
    override fun run(args: ApplicationArguments?) {
        if (!adminProperties.enabled) {
            return
        }

        if (userRepository.existsByUsername(adminProperties.username)) {
            return
        }

        val admin =
            User(
                username = adminProperties.username,
                email = adminProperties.email,
                passwordHash = passwordEncoder.encode(adminProperties.password),
                karma = 0,
                role = UserRole.ROLE_ADMIN,
            )
        userRepository.save(admin)
        logger.info("Admin user '{}' auto-created", adminProperties.username)
    }
}
