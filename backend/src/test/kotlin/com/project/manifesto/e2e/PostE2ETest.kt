package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertEquals
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class PostE2ETest
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
        fun `create LINK post persists to real PostgreSQL`() {
            val token = registerAndGetToken("linkuser")
            val body = mapOf("title" to "Link Post", "type" to "LINK", "url" to "https://example.com")

            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andExpect { status { isOk() } }
                    .andReturn()

            val postId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
            assertTrue(postId > 0)

            val saved = postRepository.findById(postId).orElse(null)
            assertNotNull(saved)
            assertEquals("Link Post", saved!!.title)
            assertEquals("https://example.com", saved.url)
        }

        @Test
        fun `create ASK post with content persists correctly`() {
            val token = registerAndGetToken("askuser")
            val body = mapOf("title" to "Ask HN", "type" to "ASK", "content" to "Question text here")

            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andExpect { status { isOk() } }
                    .andReturn()

            val postId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
            val saved = postRepository.findById(postId).orElse(null)
            assertEquals("Question text here", saved!!.content)
        }

        @Test
        fun `delete own post marks it deleted`() {
            val token = registerAndGetToken("deluser")
            val body = mapOf("title" to "Delete Me", "type" to "ASK", "content" to "bye")

            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andReturn()
            val postId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()

            mockMvc
                .delete("/api/v1/posts/$postId") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(true) }
                }

            assertTrue(postRepository.findById(postId).orElseThrow().deleted)
        }

        @Test
        fun `get single post by id returns full data`() {
            val token = registerAndGetToken("getpost")
            val body = mapOf("title" to "Single Post", "type" to "ASK", "content" to "full content")
            val res =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andReturn()
            val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

            mockMvc.get("/api/v1/posts/$postId").andExpect {
                status { isOk() }
                jsonPath("$.data.title") { value("Single Post") }
                jsonPath("$.data.content") { value("full content") }
                jsonPath("$.data.authorUsername") { value("getpost") }
            }
        }

        @Test
        fun `list posts by username returns correct posts`() {
            val token = registerAndGetToken("postlist")
            val body = mapOf("title" to "Listed Post", "type" to "ASK", "content" to "lp")
            mockMvc
                .post("/api/v1/posts") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(body)
                }.andExpect { status { isOk() } }

            mockMvc.get("/api/v1/posts/user/postlist").andExpect {
                status { isOk() }
                jsonPath("$.data.content.length()") { value(1) }
                jsonPath("$.data.content[0].title") { value("Listed Post") }
            }
        }
    }
