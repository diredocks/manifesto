package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertFalse
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
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class ModerationE2ETest @Autowired constructor(
    mockMvc: MockMvc,
    userRepository: UserRepository,
    postRepository: PostRepository,
    voteRepository: VoteRepository,
    commentRepository: CommentRepository,
    notificationRepository: NotificationRepository,
    objectMapper: ObjectMapper
) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {

    // ── moderator post / comment management ───────────────────────────

    @Test
    fun `moderator can delete any post, user cannot`() {
        val userToken = registerAndGetToken("rbac_user")
        registerAndGetToken("rbac_mod")

        val modUser = userRepository.findByUsername("rbac_mod")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modTokenFresh = login("rbac_mod")

        val body = mapOf("title" to "Delete target", "type" to "ASK", "content" to "x")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $userToken")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        mockMvc.delete("/api/v1/moderator/posts/$postId") {
            header("Authorization", "Bearer $userToken")
        }.andExpect { status { isForbidden() } }

        mockMvc.delete("/api/v1/moderator/posts/$postId") {
            header("Authorization", "Bearer $modTokenFresh")
        }.andExpect { status { isOk() } }

        assertTrue(postRepository.findById(postId).orElseThrow().deleted)
    }

    @Test
    fun `moderator can delete any comment`() {
        val userToken = registerAndGetToken("cmtuser2")
        registerAndGetToken("cmtmod")

        val modUser = userRepository.findByUsername("cmtmod")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modToken = login("cmtmod")

        val body = mapOf("title" to "Mod Cmt Post", "type" to "ASK", "content" to "p")
        val res = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $userToken")
            content = objectMapper.writeValueAsString(body)
        }.andReturn()
        val postId = objectMapper.readTree(res.response.contentAsString)["data"]["id"].asLong()

        val cRes = mockMvc.post("/api/v1/posts/$postId/comments") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $userToken")
            content = """{"content":"delete me"}"""
        }.andReturn()
        val commentId = objectMapper.readTree(cRes.response.contentAsString)["data"]["id"].asLong()

        mockMvc.delete("/api/v1/moderator/comments/$commentId") {
            header("Authorization", "Bearer $modToken")
        }.andExpect { status { isOk() } }

        assertTrue(commentRepository.findById(commentId).orElseThrow().deleted)
    }

    @Test
    fun `moderator can list all users`() {
        registerAndGetToken("mlist1")
        registerAndGetToken("mlist2")
        registerAndGetToken("mlistmod")

        val modUser = userRepository.findByUsername("mlistmod")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modToken = login("mlistmod")

        mockMvc.get("/api/v1/moderator/users") {
            header("Authorization", "Bearer $modToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(3) }
        }
    }

    // ── admin role management ─────────────────────────────────────────

    @Test
    fun `admin can list users and change roles`() {
        registerAndGetToken("plainuser")
        registerAndGetToken("superadmin")

        val adminUser = userRepository.findByUsername("superadmin")!!
        adminUser.role = UserRole.ROLE_ADMIN
        userRepository.save(adminUser)
        val adminTokenFresh = login("superadmin")

        val listRes = mockMvc.get("/api/v1/admin/users") {
            header("Authorization", "Bearer $adminTokenFresh")
        }.andExpect { status { isOk() } }.andReturn()
        val users = objectMapper.readTree(listRes.response.contentAsString)["data"]
        assertTrue(users.size() >= 2)

        val plainUser = userRepository.findByUsername("plainuser")!!
        mockMvc.put("/api/v1/admin/users/${plainUser.id}/role") {
            header("Authorization", "Bearer $adminTokenFresh")
            param("role", "ROLE_MODERATOR")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.role") { value("ROLE_MODERATOR") }
        }
    }

    @Test
    fun `cannot demote the last admin`() {
        registerAndGetToken("lastadmin")

        val adminUser = userRepository.findByUsername("lastadmin")!!
        adminUser.role = UserRole.ROLE_ADMIN
        userRepository.save(adminUser)
        val adminToken = login("lastadmin")

        mockMvc.put("/api/v1/admin/users/${adminUser.id}/role") {
            header("Authorization", "Bearer $adminToken")
            param("role", "ROLE_USER")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.message") { value("Cannot change role: at least one admin must exist") }
        }
    }

    // ── ban / unban ───────────────────────────────────────────────────

    @Test
    fun `moderator can ban and unban a user`() {
        registerAndGetToken("bantarget")
        registerAndGetToken("banmod")

        val modUser = userRepository.findByUsername("banmod")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modToken = login("banmod")

        val target = userRepository.findByUsername("bantarget")!!

        mockMvc.post("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "24")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.bannedUntil") { exists() }
        }
        assertTrue(userRepository.findByUsername("bantarget")!!.isBanned())

        mockMvc.delete("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.bannedUntil") { doesNotExist() }
        }
        assertFalse(userRepository.findByUsername("bantarget")!!.isBanned())
    }

    @Test
    fun `moderator cannot ban an admin`() {
        registerAndGetToken("banadmin")
        registerAndGetToken("banmod2")

        val adminUser = userRepository.findByUsername("banadmin")!!
        adminUser.role = UserRole.ROLE_ADMIN
        userRepository.save(adminUser)

        val modUser = userRepository.findByUsername("banmod2")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modToken = login("banmod2")

        mockMvc.post("/api/v1/moderator/users/${adminUser.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "24")
        }.andExpect { status { isBadRequest() } }
    }

    @Test
    fun `banned user cannot post comment or vote`() {
        registerAndGetToken("baduser")
        registerAndGetToken("banmod3")

        val modUser = userRepository.findByUsername("banmod3")!!
        modUser.role = UserRole.ROLE_MODERATOR
        userRepository.save(modUser)
        val modToken = login("banmod3")

        val target = userRepository.findByUsername("baduser")!!
        mockMvc.post("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "1")
        }.andExpect { status { isOk() } }

        val bannedToken = login("baduser")

        mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $bannedToken")
            content = """{"title":"Blocked","type":"ASK","content":"nope"}"""
        }.andExpect { status { isBadRequest() } }

        mockMvc.get("/api/v1/auth/me") {
            header("Authorization", "Bearer $bannedToken")
        }.andExpect { status { isOk() } }
    }
}
