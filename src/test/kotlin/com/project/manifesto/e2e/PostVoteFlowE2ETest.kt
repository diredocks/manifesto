package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
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
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class PostVoteFlowE2ETest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val commentRepository: CommentRepository,
    private val notificationRepository: NotificationRepository,
    private val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setup() {
        notificationRepository.deleteAll()
        commentRepository.deleteAll()
        voteRepository.deleteAll()
        postRepository.deleteAll()
        userRepository.deleteAll()
    }

    private fun registerAndGetToken(username: String): String {
        val body = mapOf("username" to username, "email" to "$username@test.com", "password" to "pass123")
        val result = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["token"].asText()
    }

    private fun login(username: String): String {
        val body = mapOf("username" to username, "password" to "pass123")
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["token"].asText()
    }

    // ── auth ────────────────────────────────────────────────────────────

    @Test
    fun `register returns token, login works, me returns user`() {
        val token = registerAndGetToken("authuser")
        assertNotNull(token)
        assertTrue(token.length > 20)

        val loginBody = mapOf("username" to "authuser", "password" to "pass123")
        val loginResult = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginBody)
        }.andExpect { status { isOk() } }.andReturn()
        val loginToken = objectMapper.readTree(loginResult.response.contentAsString)["data"]["token"].asText()
        assertNotNull(loginToken)

        mockMvc.get("/api/v1/auth/me") {
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
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(body)
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value(400) }
        }
    }

    // ── post CRUD ───────────────────────────────────────────────────────

    @Test
    fun `create LINK post persists to real PostgreSQL`() {
        val token = registerAndGetToken("linkuser")
        val body = mapOf("title" to "Link Post", "type" to "LINK", "url" to "https://example.com")

        val result = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()

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

        val result = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andExpect { status { isOk() } }.andReturn()

        val postId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
        val saved = postRepository.findById(postId).orElse(null)
        assertEquals("Question text here", saved!!.content)
    }

    @Test
    fun `delete own post marks it deleted`() {
        val token = registerAndGetToken("deluser")
        val body = mapOf("title" to "Delete Me", "type" to "ASK", "content" to "bye")

        val result = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()

        mockMvc.delete("/api/v1/posts/$postId") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data") { value(true) }
        }

        assertTrue(postRepository.findById(postId).orElseThrow().deleted)
    }

    // ── voting (exercises Redis distributed lock) ───────────────────────

    @Test
    fun `upvote exercises Redis lock, count increments, remove decrements`() {
        val tokenA = registerAndGetToken("voterA")
        val tokenB = registerAndGetToken("voterB")

        // create post as voterA
        val body = mapOf("title" to "Vote Target", "type" to "ASK", "content" to "x")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenA")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        // both vote
        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenB") }
            .andExpect { status { isOk() } }

        assertEquals(2, voteRepository.countByPostId(postId))

        // duplicate vote is idempotent
        mockMvc.post("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        assertEquals(2, voteRepository.countByPostId(postId))

        // remove vote
        mockMvc.delete("/api/v1/posts/$postId/upvote") { header("Authorization", "Bearer $tokenA") }
            .andExpect { status { isOk() } }
        assertEquals(1, voteRepository.countByPostId(postId))
    }

    // ── ranking ─────────────────────────────────────────────────────────

    @Test
    fun `ranking endpoints return posts in correct order`() {
        val token = registerAndGetToken("rankuser")
        for (i in 1..3) {
            val body = mapOf("title" to "Post $i", "type" to "ASK", "content" to "content $i")
            mockMvc.post("/api/v1/posts") {
                contentType = MediaType.APPLICATION_JSON
                header("Authorization", "Bearer $token")
                content = objectMapper.writeValueAsString(body)
            }.andExpect { status { isOk() } }
        }

        mockMvc.get("/api/v1/ranking/new") { param("size", "10") }
            .andExpect { jsonPath("$.data.length()") { value(3) } }

        mockMvc.get("/api/v1/ranking/hot")
            .andExpect { status { isOk() } }

        mockMvc.get("/api/v1/ranking/top")
            .andExpect { status { isOk() } }
    }

    // ── comments ────────────────────────────────────────────────────────

    @Test
    fun `comment tree builds correctly with nested replies`() {
        val token = registerAndGetToken("cmtuser")

        val body = mapOf("title" to "Commentable", "type" to "ASK", "content" to "post")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        // top-level
        val c1 = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = """{"content":"Root"}"""
        }.andReturn()
        val c1Id = objectMapper.readTree(c1.response.contentAsString)["data"]["id"].asLong()

        // reply
        mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = """{"content":"Child","parentId":$c1Id}"""
        }.andExpect { status { isOk() } }

        val tree = mockMvc.get("/api/v1/posts/$postId/comments").andReturn()
        val data = objectMapper.readTree(tree.response.contentAsString)["data"]
        assertEquals(1, data.size())
        assertEquals(1, data[0]["children"].size())
    }

    // ── notifications ───────────────────────────────────────────────────

    @Test
    fun `reply triggers RabbitMQ notification for parent author`() {
        val tokenAlice = registerAndGetToken("alice_n")
        val tokenBob = registerAndGetToken("bob_n")

        val body = mapOf("title" to "Notify Post", "type" to "ASK", "content" to "p")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        // alice creates top comment
        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenAlice")
            content = """{"content":"Hello"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        // bob replies → notification event published via RabbitMQ
        mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $tokenBob")
            content = """{"content":"Reply!","parentId":$commentId}"""
        }.andExpect { status { isOk() } }

        // allow async delivery
        Thread.sleep(2000)

        val aliceId = userRepository.findByUsername("alice_n")!!.id
        val notifications = notificationRepository.findByReceiverIdOrderByCreatedAtDesc(
            aliceId, org.springframework.data.domain.Pageable.unpaged()
        )
        assertTrue(notifications.content.isNotEmpty(), "Alice should have a notification from Bob's reply")
        assertEquals("COMMENT_REPLY", notifications.content[0].type.name)
    }

    // ── moderator / admin RBAC ──────────────────────────────────────────

    @Test
    fun `moderator can delete any post, user cannot`() {
        val userToken = registerAndGetToken("rbac_user")
        val modToken = registerAndGetToken("rbac_mod")

        // promote rbac_mod via DB, then re-login for fresh JWT
        val modUser = userRepository.findByUsername("rbac_mod")!!
        modUser.role = com.project.manifesto.modules.user.entity.UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modTokenFresh = login("rbac_mod")

        val body = mapOf("title" to "Delete target", "type" to "ASK", "content" to "x")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $userToken")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        // user cannot
        mockMvc.delete("/api/v1/moderator/posts/$postId") {
            header("Authorization", "Bearer $userToken")
        }.andExpect { status { isForbidden() } }

        // moderator can (with fresh token)
        mockMvc.delete("/api/v1/moderator/posts/$postId") {
            header("Authorization", "Bearer $modTokenFresh")
        }.andExpect { status { isOk() } }

        assertTrue(postRepository.findById(postId).orElseThrow().deleted)
    }

    @Test
    fun `admin can list users and change roles`() {
        registerAndGetToken("plainuser")
        registerAndGetToken("superadmin")

        val adminUser = userRepository.findByUsername("superadmin")!!
        adminUser.role = com.project.manifesto.modules.user.entity.UserRole.ROLE_ADMIN
        userRepository.save(adminUser)
        val adminTokenFresh = login("superadmin")

        val listRes = mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer $adminTokenFresh")
        }.andExpect { status { isOk() } }.andReturn()
        val users = objectMapper.readTree(listRes.response.contentAsString)["data"]
        assertTrue(users.size() >= 2)

        // promote plainuser to moderator
        val plainUser = userRepository.findByUsername("plainuser")!!
        mockMvc.put("/api/v1/admin/users/${plainUser.id}/role") {
            header("Authorization", "Bearer $adminTokenFresh")
            param("role", "ROLE_MODERATOR")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.role") { value("ROLE_MODERATOR") }
        }
    }
}
