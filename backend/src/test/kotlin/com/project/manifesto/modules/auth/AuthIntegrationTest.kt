package com.project.manifesto.modules.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.LoginRequest
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class AuthIntegrationTest
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
        fun `register new user returns JWT token`() {
            val request = RegisterRequest("testuser", "test@example.com", "password123")

            mockMvc
                .post("/api/v1/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.token") { exists() }
                    jsonPath("$.data.username") { value("testuser") }
                    jsonPath("$.data.role") { value("ROLE_USER") }
                }

            val user = userRepository.findByUsername("testuser")
            assertNotNull(user)
            assertEquals("test@example.com", user?.email)
            assertEquals(UserRole.ROLE_USER, user?.role)
        }

        @Test
        fun `register duplicate username fails with 400`() {
            val user =
                com.project.manifesto.modules.user.entity.User(
                    username = "existing",
                    email = "existing@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                )
            userRepository.save(user)

            val request = RegisterRequest("existing", "new@example.com", "password123")

            mockMvc
                .post("/api/v1/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value(400) }
                }
        }

        @Test
        fun `login with valid credentials returns token`() {
            val user =
                com.project.manifesto.modules.user.entity.User(
                    username = "loginuser",
                    email = "login@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                )
            userRepository.save(user)

            val request = LoginRequest("loginuser", "password123")

            mockMvc
                .post("/api/v1/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.token") { exists() }
                }
        }

        @Test
        fun `login with invalid credentials returns 401`() {
            val request = LoginRequest("nonexistent", "wrongpass")

            mockMvc
                .post("/api/v1/auth/login") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isUnauthorized() }
                    jsonPath("$.code") { value(401) }
                }
        }

        @Test
        fun `me endpoint returns user info with valid token`() {
            val user =
                com.project.manifesto.modules.user.entity.User(
                    username = "meuser",
                    email = "me@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                    karma = 10,
                )
            userRepository.save(user)

            val loginRequest = LoginRequest("meuser", "password123")
            val loginResult =
                mockMvc
                    .post("/api/v1/auth/login") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(loginRequest)
                    }.andReturn()

            val responseJson = objectMapper.readTree(loginResult.response.contentAsString)
            val token = responseJson["data"]["token"].asText()

            mockMvc
                .get("/api/v1/auth/me") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.username") { value("meuser") }
                    jsonPath("$.data.karma") { value(10) }
                }
        }

        @Test
        fun `me endpoint without token returns 401`() {
            mockMvc.get("/api/v1/auth/me").andExpect {
                status { isUnauthorized() }
                jsonPath("$.code") { value(401) }
            }
        }
    }
