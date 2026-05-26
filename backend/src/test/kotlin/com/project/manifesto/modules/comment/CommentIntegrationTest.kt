package com.project.manifesto.modules.comment

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class CommentIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val postRepository: PostRepository,
        private val commentRepository: CommentRepository,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            commentRepository.deleteAll()
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
        fun `create comment on post succeeds`() {
            val token = createUserAndGetToken("commenter1")
            val postId = createPost(token, "Comment Target")

            val commentRequest = mapOf("content" to "Great post!")

            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(commentRequest)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.content") { value("Great post!") }
                    jsonPath("$.data.depth") { value(0) }
                    jsonPath("$.data.parentId") { doesNotExist() }
                }
        }

        @Test
        fun `create reply to comment succeeds`() {
            val token = createUserAndGetToken("replier1")
            val postId = createPost(token, "Reply Target")

            val topComment = mapOf("content" to "Top level")
            val topResult =
                mockMvc
                    .post("/api/v1/posts/$postId/comments") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(topComment)
                    }.andReturn()
            val topId = objectMapper.readTree(topResult.response.contentAsString)["data"]["id"].asLong()

            val reply = mapOf("content" to "A reply", "parentId" to topId)

            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(reply)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data.depth") { value(1) }
                    jsonPath("$.data.parentId") { value(topId.toInt()) }
                }
        }

        @Test
        fun `get comment tree returns nested structure`() {
            val token = createUserAndGetToken("treeuser1")
            val postId = createPost(token, "Tree Post")

            val topComment = mapOf("content" to "Root comment")
            val topResult =
                mockMvc
                    .post("/api/v1/posts/$postId/comments") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(topComment)
                    }.andReturn()
            val topId = objectMapper.readTree(topResult.response.contentAsString)["data"]["id"].asLong()

            val reply = mapOf("content" to "Child reply", "parentId" to topId)
            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(reply)
                }.andReturn()

            val otherComment = mapOf("content" to "Another root")
            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(otherComment)
                }.andReturn()

            mockMvc.get("/api/v1/posts/$postId/comments").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(2) }
            }
        }

        @Test
        fun `delete own comment marks it as deleted`() {
            val token = createUserAndGetToken("deleter1")
            val postId = createPost(token, "Delete Target")

            val commentRequest = mapOf("content" to "Delete me")
            val result =
                mockMvc
                    .post("/api/v1/posts/$postId/comments") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(commentRequest)
                    }.andReturn()
            val commentId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()

            mockMvc
                .delete("/api/v1/comments/$commentId") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.data") { value(true) }
                }
        }

        @Test
        fun `comments are publicly accessible`() {
            val token = createUserAndGetToken("pubuser1")
            val postId = createPost(token, "Public Post")

            mockMvc.get("/api/v1/posts/$postId/comments").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
            }
        }

        @Test
        fun `create comment without auth returns 401`() {
            val token = createUserAndGetToken("authuser1")
            val postId = createPost(token, "Auth Post")

            val commentRequest = mapOf("content" to "Unauthorized comment")

            mockMvc
                .post("/api/v1/posts/$postId/comments") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(commentRequest)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }
    }
