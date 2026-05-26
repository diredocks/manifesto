package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class AuthE2ETest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        userRepository: UserRepository,
        postRepository: PostRepository,
        voteRepository: VoteRepository,
        commentRepository: CommentRepository,
        notificationRepository: NotificationRepository,
        objectMapper: ObjectMapper,
    ) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {
        @Test
        fun `register returns token, login works, me returns user`() {
            val token = registerAndGetToken("authuser")
            assertNotNull(token)
            assertTrue(token.length > 20)

            val loginBody = mapOf("username" to "authuser", "password" to "pass123")
            val loginResult =
                mockMvc
                    .post("/api/v1/auth/login") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(loginBody)
                    }.andExpect { status { isOk() } }
                    .andReturn()
            val loginToken = objectMapper.readTree(loginResult.response.contentAsString)["data"]["token"].asText()
            assertNotNull(loginToken)

            mockMvc
                .get("/api/v1/auth/me") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.username") { value("authuser") }
                    jsonPath("$.data.role") { value("ROLE_USER") }
                }
        }

        @Test
        fun `duplicate register returns 400`() {
            registerAndGetToken("dupuser")
            val body = mapOf("username" to "dupuser", "email" to "other@test.com", "password" to "pass123")
            mockMvc
                .post("/api/v1/auth/register") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(body)
                }.andExpect {
                    status { isBadRequest() }
                    jsonPath("$.code") { value(400) }
                }
        }
    }
