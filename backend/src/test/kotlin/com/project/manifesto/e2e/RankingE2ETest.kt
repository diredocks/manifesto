package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
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
class RankingE2ETest
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
        fun `ranking endpoints return posts in correct order`() {
            val token = registerAndGetToken("rankuser")
            for (i in 1..3) {
                val body = mapOf("title" to "Post $i", "type" to "ASK", "content" to "content $i")
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andExpect { status { isOk() } }
            }

            mockMvc
                .get("/api/v1/ranking/new") { param("size", "10") }
                .andExpect { jsonPath("$.data.length()") { value(3) } }

            mockMvc
                .get("/api/v1/ranking/hot")
                .andExpect { status { isOk() } }

            mockMvc
                .get("/api/v1/ranking/top")
                .andExpect { status { isOk() } }
        }
    }
