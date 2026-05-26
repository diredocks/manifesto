package com.project.manifesto.modules.vote

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.comment.repository.CommentRepository
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class CommentVoteIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val voteRepository: VoteRepository,
    private val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setup() {
        voteRepository.deleteAll()
        commentRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun createUserAndGetToken(username: String): String {
        val request = RegisterRequest(username, "$username@example.com", "password123")
        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(request)
        }.andReturn()
        val json = objectMapper.readTree(result.response.contentAsString)
        return json["data"]["token"].asText()
    }

    private fun createPost(token: String, title: String): Long {
        val request = CreatePostRequest(title = title, type = PostType.ASK, content = "content")
        val result = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(request)
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
    }

    private fun createComment(token: String, postId: Long, commentContent: String): Long {
        val commentRequest = mapOf("content" to commentContent)
        val result = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(commentRequest)
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
    }

    @Test
    fun `upvote comment succeeds`() {
        val posterToken = createUserAndGetToken("poster1")
        val voterToken = createUserAndGetToken("voter1")
        val postId = createPost(posterToken, "Comment Vote Post")
        val commentId = createComment(posterToken, postId, "Good point")

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data") { value(true) }
        }

        mockMvc.get("/api/v1/comments/$commentId/vote-count").andExpect {
            status { isOk() }
            jsonPath("$.data") { value(1) }
        }
    }

    @Test
    fun `duplicate comment upvote is idempotent`() {
        val posterToken = createUserAndGetToken("poster2")
        val voterToken = createUserAndGetToken("voter2")
        val postId = createPost(posterToken, "Dup Comment Vote")
        val commentId = createComment(posterToken, postId, "Nice")

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect { status { isOk() } }

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/comments/$commentId/vote-count").andExpect {
            jsonPath("$.data") { value(1) }
        }
    }

    @Test
    fun `remove comment upvote succeeds`() {
        val posterToken = createUserAndGetToken("poster3")
        val voterToken = createUserAndGetToken("voter3")
        val postId = createPost(posterToken, "Remove Comment Vote")
        val commentId = createComment(posterToken, postId, "Interesting")

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect { status { isOk() } }

        mockMvc.delete("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(true) }
        }

        mockMvc.get("/api/v1/comments/$commentId/vote-count").andExpect {
            jsonPath("$.data") { value(0) }
        }
    }

    @Test
    fun `comment vote status reflects current state`() {
        val posterToken = createUserAndGetToken("poster4")
        val voterToken = createUserAndGetToken("voter4")
        val postId = createPost(posterToken, "Vote Status Comment")
        val commentId = createComment(posterToken, postId, "Status check")

        mockMvc.get("/api/v1/comments/$commentId/vote-status") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(false) }
        }

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/comments/$commentId/vote-status") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(true) }
        }
    }

    @Test
    fun `comment upvote without auth returns 401`() {
        val posterToken = createUserAndGetToken("poster5")
        val postId = createPost(posterToken, "Auth Comment Vote")
        val commentId = createComment(posterToken, postId, "Needs auth")

        mockMvc.post("/api/v1/comments/$commentId/upvote").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `comment vote count is publicly accessible`() {
        val posterToken = createUserAndGetToken("poster6")
        val voterToken = createUserAndGetToken("voter6")
        val postId = createPost(posterToken, "Public Count")
        val commentId = createComment(posterToken, postId, "Public")

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $voterToken")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/comments/$commentId/vote-count").andExpect {
            status { isOk() }
            jsonPath("$.data") { value(1) }
        }
    }
}
