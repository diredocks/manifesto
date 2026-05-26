package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.post

abstract class E2EBase(
    protected val mockMvc: MockMvc,
    protected val userRepository: UserRepository,
    protected val postRepository: PostRepository,
    protected val voteRepository: VoteRepository,
    protected val commentRepository: CommentRepository,
    protected val notificationRepository: NotificationRepository,
    protected val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
        commentRepository.deleteAll()
        voteRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    protected fun registerAndGetToken(username: String): String {
        val body = mapOf("username" to username, "email" to "$username@test.com", "password" to "pass123")
        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["token"].asText()
    }

    protected fun login(username: String): String {
        val body = mapOf("username" to username, "password" to "pass123")
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["token"].asText()
    }
}
