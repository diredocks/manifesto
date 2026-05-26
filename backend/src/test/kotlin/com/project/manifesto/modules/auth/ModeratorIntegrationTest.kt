package com.project.manifesto.modules.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class ModeratorIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val postRepository: PostRepository,
        private val commentRepository: CommentRepository,
        private val passwordEncoder: PasswordEncoder,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            commentRepository.deleteAll()
            postRepository.deleteAll()
            userRepository.deleteAll()
        }

        private fun createUserAndGetToken(
            username: String,
            role: UserRole = UserRole.ROLE_USER,
        ): String {
            // Directly create user with specific role
            userRepository.save(
                User(
                    username = username,
                    email = "$username@example.com",
                    passwordHash = passwordEncoder.encode("password123"),
                    role = role,
                ),
            )
            // Login
            val loginReq = mapOf("username" to username, "password" to "password123")
            val result =
                mockMvc
                    .post("/api/v1/auth/login") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(loginReq)
                    }.andReturn()
            val json = objectMapper.readTree(result.response.contentAsString)
            return json["data"]["token"].asText()
        }

        private fun createPost(
            token: String,
            title: String,
        ): Long {
            val request = CreatePostRequest(title = title, type = PostType.ASK, content = "content")
            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(request)
                    }.andReturn()
            return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
        }

        @Test
        fun `moderator can delete any post`() {
            val userToken = createUserAndGetToken("user1")
            val modToken = createUserAndGetToken("mod1", UserRole.ROLE_MODERATOR)
            val postId = createPost(userToken, "User's post")

            mockMvc
                .delete("/api/v1/moderator/posts/$postId") {
                    header("Authorization", "Bearer $modToken")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(true) }
                }

            mockMvc.get("/api/v1/posts/$postId").andExpect {
                status { isNotFound() }
            }
        }

        @Test
        fun `regular user cannot use moderator endpoints`() {
            val userToken = createUserAndGetToken("user2")
            val postId = createPost(userToken, "My post")

            mockMvc
                .delete("/api/v1/moderator/posts/$postId") {
                    header("Authorization", "Bearer $userToken")
                }.andExpect {
                    status { isForbidden() }
                }
        }

        @Test
        fun `admin can list users`() {
            createUserAndGetToken("user3")
            val adminToken = createUserAndGetToken("admin1", UserRole.ROLE_ADMIN)

            mockMvc
                .get("/api/v1/admin/users") {
                    header("Authorization", "Bearer $adminToken")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.length()") { value(2) }
                }
        }

        @Test
        fun `admin can change user role`() {
            createUserAndGetToken("user4")
            val adminToken = createUserAndGetToken("admin2", UserRole.ROLE_ADMIN)

            val user = userRepository.findByUsername("user4")!!

            mockMvc
                .put("/api/v1/admin/users/${user.id}/role") {
                    header("Authorization", "Bearer $adminToken")
                    param("role", "ROLE_MODERATOR")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.role") { value("ROLE_MODERATOR") }
                }
        }

        @Test
        fun `moderator cannot access admin endpoints`() {
            val modToken = createUserAndGetToken("mod2", UserRole.ROLE_MODERATOR)

            mockMvc
                .get("/api/v1/admin/users") {
                    header("Authorization", "Bearer $modToken")
                }.andExpect {
                    status { isForbidden() }
                }
        }

        @Test
        fun `cannot demote the last admin to user`() {
            val adminToken = createUserAndGetToken("admin3", UserRole.ROLE_ADMIN)
            val admin = userRepository.findByUsername("admin3")!!

            mockMvc
                .put("/api/v1/admin/users/${admin.id}/role") {
                    header("Authorization", "Bearer $adminToken")
                    param("role", "ROLE_USER")
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.message") { value("Cannot change role: at least one admin must exist") }
                }
        }

        @Test
        fun `can demote an admin when another admin exists`() {
            val adminToken = createUserAndGetToken("admin4", UserRole.ROLE_ADMIN)
            createUserAndGetToken("admin5", UserRole.ROLE_ADMIN)
            val target = userRepository.findByUsername("admin5")!!

            mockMvc
                .put("/api/v1/admin/users/${target.id}/role") {
                    header("Authorization", "Bearer $adminToken")
                    param("role", "ROLE_USER")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.role") { value("ROLE_USER") }
                }
        }
    }
