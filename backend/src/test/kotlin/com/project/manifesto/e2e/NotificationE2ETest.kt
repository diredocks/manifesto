package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.Pageable
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class NotificationE2ETest @Autowired constructor(
    mockMvc: MockMvc,
    userRepository: UserRepository,
    postRepository: PostRepository,
    voteRepository: VoteRepository,
    commentRepository: CommentRepository,
    notificationRepository: NotificationRepository,
    objectMapper: ObjectMapper
) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {

    @Test
    fun `reply triggers notification for parent author`() {
        val tokenAlice = registerAndGetToken("alice_n")
        val tokenBob = registerAndGetToken("bob_n")

        val body = mapOf("title" to "Notify Post", "type" to "ASK", "content" to "p")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = """{"content":"Hello"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenBob")
            content = """{"content":"Reply!","parentId":$commentId}"""
        }.andExpect { status { isOk() } }

        val aliceId = userRepository.findByUsername("alice_n")!!.id
        val notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(
            aliceId, Pageable.unpaged()
        )
        assertTrue(notifications.content.isNotEmpty(), "Alice should have a notification from Bob's reply")
        assertEquals("COMMENT_REPLY", notifications.content[0].type.name)
    }

    @Test
    fun `notifications unread count and mark as read`() {
        val tokenAlice = registerAndGetToken("nalice")
        val tokenBob = registerAndGetToken("nbob")

        val body = mapOf("title" to "Notif Post", "type" to "ASK", "content" to "np")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = """{"content":"Hello"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenBob")
            content = """{"content":"Hey!","parentId":$commentId}"""
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/notifications/unread-count") {
            header("Authorization", "Bearer $tokenAlice")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(1) }
        }

        val notifsRes = mockMvc.get("/api/v1/notifications") {
            header("Authorization", "Bearer $tokenAlice")
        }.andReturn()
        val notifs = objectMapper.readTree(notifsRes.response.contentAsString)["data"]["content"]
        assertTrue(notifs.size() >= 1)
        val notifId = notifs[0]["id"].asLong()

        mockMvc.patch("/api/v1/notifications/$notifId/read") {
            header("Authorization", "Bearer $tokenAlice")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/notifications/unread-count") {
            header("Authorization", "Bearer $tokenAlice")
        }.andExpect {
            jsonPath("$.data") { value(0) }
        }
    }
}
