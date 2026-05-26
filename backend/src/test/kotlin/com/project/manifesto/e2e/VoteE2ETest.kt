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
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class VoteE2ETest @Autowired constructor(
    mockMvc: MockMvc,
    userRepository: UserRepository,
    postRepository: PostRepository,
    voteRepository: VoteRepository,
    commentRepository: CommentRepository,
    notificationRepository: NotificationRepository,
    objectMapper: ObjectMapper
) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {

    // ── post voting ───────────────────────────────────────────────────

    @Test
    fun `upvote count increments, remove decrements`() {
        val tokenA = registerAndGetToken("voterA")
        val tokenB = registerAndGetToken("voterB")

        val body = mapOf("title" to "Vote Target", "type" to "ASK", "content" to "x")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenB") }
            .andExpect { status { isOk() } }

        assertEquals(2, voteRepository.countByPostId(postId))

        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        assertEquals(2, voteRepository.countByPostId(postId))

        mockMvc.delete("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        assertEquals(1, voteRepository.countByPostId(postId))
    }

    @Test
    fun `post vote status and count endpoints work`() {
        val token = registerAndGetToken("vstatus")
        val body = mapOf("title" to "Status Post", "type" to "ASK", "content" to "x")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        mockMvc.get("/api/v1/posts/$postId/vote-status") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(false) }
        }

        mockMvc.get("/api/v1/posts/$postId/vote-count").andExpect {
            status { isOk() }
            jsonPath("$.data") { value(0) }
        }

        mockMvc.post("/api/v1/posts/$postId/upvote") {
            header("Authorization", "Bearer $token")
        }.andExpect { status { isOk() } }

        mockMvc.get("/api/v1/posts/$postId/vote-status") {
            header("Authorization", "Bearer $token")
        }.andExpect { jsonPath("$.data") { value(true) } }

        mockMvc.get("/api/v1/posts/$postId/vote-count").andExpect {
            jsonPath("$.data") { value(1) }
        }
    }

    // ── comment voting ────────────────────────────────────────────────

    @Test
    fun `comment upvote and remove works correctly`() {
        val tokenA = registerAndGetToken("cauthor")
        val tokenB = registerAndGetToken("cvoter")

        val body = mapOf("title" to "CV Post", "type" to "ASK", "content" to "post")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = """{"content":"Vote me"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }
        assertEquals(1, voteRepository.countByCommentId(commentId))

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }
        assertEquals(1, voteRepository.countByCommentId(commentId))

        mockMvc.delete("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }
        assertEquals(0, voteRepository.countByCommentId(commentId))
    }

    // ── karma ─────────────────────────────────────────────────────────

    @Test
    fun `post upvote increases author karma, remove decreases it`() {
        val tokenA = registerAndGetToken("karmaauth")
        val tokenB = registerAndGetToken("karmavoter")

        val body = mapOf("title" to "Karma Post", "type" to "ASK", "content" to "test")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        assertEquals(0, userRepository.findByUsername("karmaauth")!!.karma)

        mockMvc.post("/api/v1/posts/$postId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }
        assertEquals(1, userRepository.findByUsername("karmaauth")!!.karma)

        mockMvc.delete("/api/v1/posts/$postId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }
        assertEquals(0, userRepository.findByUsername("karmaauth")!!.karma)
    }

    @Test
    fun `comment upvote increases comment author karma`() {
        val tokenA = registerAndGetToken("ckarmaauth")
        val tokenB = registerAndGetToken("ckarmavoter")

        val body = mapOf("title" to "CKarma Post", "type" to "ASK", "content" to "p")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = """{"content":"comment"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        mockMvc.post("/api/v1/comments/$commentId/upvote") {
            header("Authorization", "Bearer $tokenB")
        }.andExpect { status { isOk() } }

        assertEquals(1, userRepository.findByUsername("ckarmaauth")!!.karma)
    }
}
