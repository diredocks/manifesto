package com.project.manifesto.modules.user

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class UserIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val passwordEncoder: PasswordEncoder,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            userRepository.deleteAll()
        }

        @Test
        fun `get user profile by username returns user info`() {
            userRepository.save(
                User(
                    username = "profileuser",
                    email = "profile@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                    karma = 42,
                ),
            )

            mockMvc.get("/api/v1/users/profileuser").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.username") { value("profileuser") }
                jsonPath("$.data.karma") { value(42) }
                jsonPath("$.data.createdAt") { exists() }
            }
        }

        @Test
        fun `get user profile for nonexistent user returns 400`() {
            mockMvc.get("/api/v1/users/nonexistent").andExpect {
                status { isBadRequest() }
                jsonPath("$.code") { value(400) }
            }
        }

        @Test
        fun `user profile is publicly accessible without auth`() {
            userRepository.save(
                User(
                    username = "publicuser",
                    email = "public@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                ),
            )

            mockMvc.get("/api/v1/users/publicuser").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
            }
        }
    }
