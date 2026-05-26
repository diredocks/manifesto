package com.project.manifesto.modules.vote

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class VoteIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val postRepository: PostRepository,
        private val voteRepository: VoteRepository,
        private val passwordEncoder: PasswordEncoder,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            voteRepository.deleteAll()
            postRepository.deleteAll()
            userRepository.deleteAll()
        }

        private fun createUserAndGetToken(username: String): String {
            val request = RegisterRequest(username, "$username@example.com", "password123")
            val result =
                mockMvc
                    .post("/api/v1/auth/register") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
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
        fun `upvote post succeeds`() {
            val token = createUserAndGetToken("voter1")
            val postId = createPost(token, "Vote Target")

            mockMvc
                .post("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data") { value(true) }
                }

            mockMvc.get("/api/v1/posts/$postId/vote-count").andExpect {
                status { isOk() }
                jsonPath("$.data") { value(1) }
            }
        }

        @Test
        fun `duplicate upvote is idempotent`() {
            val token = createUserAndGetToken("voter2")
            val postId = createPost(token, "Vote Target 2")

            mockMvc
                .post("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect { status { isOk() } }

            mockMvc
                .post("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect { status { isOk() } }

            mockMvc.get("/api/v1/posts/$postId/vote-count").andExpect {
                jsonPath("$.data") { value(1) }
            }
        }

        @Test
        fun `remove upvote succeeds`() {
            val token = createUserAndGetToken("voter3")
            val postId = createPost(token, "Vote Target 3")

            mockMvc
                .post("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect { status { isOk() } }

            mockMvc
                .delete("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(true) }
                }

            mockMvc.get("/api/v1/posts/$postId/vote-count").andExpect {
                jsonPath("$.data") { value(0) }
            }
        }

        @Test
        fun `vote status reflects current state`() {
            val token = createUserAndGetToken("voter4")
            val postId = createPost(token, "Vote Target 4")

            mockMvc
                .get("/api/v1/posts/$postId/vote-status") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(false) }
                }

            mockMvc
                .post("/api/v1/posts/$postId/upvote") {
                    header("Authorization", "Bearer $token")
                }.andExpect { status { isOk() } }

            mockMvc
                .get("/api/v1/posts/$postId/vote-status") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(true) }
                }
        }

        @Test
        fun `upvote without auth returns 401`() {
            val token = createUserAndGetToken("voter5")
            val postId = createPost(token, "Vote Target 5")

            mockMvc.post("/api/v1/posts/$postId/upvote").andExpect {
                status { isUnauthorized() }
            }
        }
    }
