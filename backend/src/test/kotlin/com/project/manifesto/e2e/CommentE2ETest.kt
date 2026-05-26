package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
class CommentE2ETest
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
        fun `comment tree builds correctly with nested replies`() {
            val token = registerAndGetToken("cmtuser")

            val body = mapOf("title" to "Commentable", "type" to "ASK", "content" to "post")
            val res =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andReturn()
            val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

            val c1 =
                mockMvc
                    .post("/api/v1/posts/$postId/comments") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = """{"content":"Root"}"""
                    }.andReturn()
            val c1Id = objectMapper.readTree(c1.response.contentAsString)["data"]["id"].asLong()

            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = """{"content":"Child","parentId":$c1Id}"""
                }.andExpect { status { isOk() } }

            val tree = mockMvc.get("/api/v1/posts/$postId/comments").andReturn()
            val data = objectMapper.readTree(tree.response.contentAsString)["data"]
            assertEquals(1, data.size())
            assertEquals(1, data[0]["children"].size())
        }
    }
