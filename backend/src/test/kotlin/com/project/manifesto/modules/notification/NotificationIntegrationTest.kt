package com.project.manifesto.modules.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.notification.entity.Notification
import com.project.manifesto.modules.notification.entity.NotificationType
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.user.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class NotificationIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val notificationRepository: NotificationRepository,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            notificationRepository.deleteAll()
            userRepository.deleteAll()
        }

        private fun createUserAndGetToken(username: String): Pair<String, Long> {
            val request = RegisterRequest(username, "$username@example.com", "password123")
            val result =
                mockMvc
                    .post("/api/v1/auth/register") {
                        contentType = MediaType.APPLICATION_JSON
                        content = objectMapper.writeValueAsString(request)
                    }.andReturn()
            val json = objectMapper.readTree(result.response.contentAsString)
            val token = json["data"]["token"].asText()

            val meResult =
                mockMvc
                    .get("/api/v1/auth/me") {
                        header("Authorization", "Bearer $token")
                    }.andReturn()
            val meJson = objectMapper.readTree(meResult.response.contentAsString)
            val userId = meJson["data"]["id"].asLong()

            return Pair(token, userId)
        }

        @Test
        fun `get notifications returns paginated list`() {
            val (token, userId) = createUserAndGetToken("notifuser1")

            notificationRepository.save(
                Notification(
                    receiverId = userId,
                    type = NotificationType.COMMENT_REPLY,
                    content = "Someone replied to your comment",
                    relatedPostId = 1,
                ),
            )
            notificationRepository.save(
                Notification(
                    receiverId = userId,
                    type = NotificationType.SYSTEM,
                    content = "Welcome to Manifesto!",
                ),
            )

            mockMvc
                .get("/api/v1/notifications") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.content.length()") { value(2) }
                }
        }

        @Test
        fun `unread count returns correct number`() {
            val (token, userId) = createUserAndGetToken("notifuser2")

            notificationRepository.save(
                Notification(
                    receiverId = userId,
                    type = NotificationType.COMMENT_REPLY,
                    content = "Reply 1",
                    isRead = false,
                ),
            )
            notificationRepository.save(
                Notification(
                    receiverId = userId,
                    type = NotificationType.COMMENT_REPLY,
                    content = "Reply 2",
                    isRead = true,
                ),
            )

            mockMvc
                .get("/api/v1/notifications/unread-count") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(1) }
                }
        }

        @Test
        fun `mark notification as read`() {
            val (token, userId) = createUserAndGetToken("notifuser3")

            val notification =
                notificationRepository.save(
                    Notification(
                        receiverId = userId,
                        type = NotificationType.SYSTEM,
                        content = "System message",
                    ),
                )

            mockMvc
                .patch("/api/v1/notifications/${notification.id}/read") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data") { value(true) }
                }

            val saved = notificationRepository.findById(notification.id).orElseThrow()
            assert(saved.isRead)
        }

        @Test
        fun `notifications require auth`() {
            mockMvc.get("/api/v1/notifications").andExpect {
                status { isUnauthorized() }
            }
        }
    }
