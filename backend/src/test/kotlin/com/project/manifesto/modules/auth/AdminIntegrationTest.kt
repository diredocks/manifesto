package com.project.manifesto.modules.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.user.entity.User
import com.project.manifesto.modules.user.entity.UserRole
import com.project.manifesto.modules.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class AdminIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setup() {
        userRepository.deleteAll()
    }

    private fun createUserAndGetToken(username: String, role: UserRole = UserRole.ROLE_USER): String {
        userRepository.save(
            User(
                username = username,
                email = "$username@example.com",
                passwordHash = passwordEncoder.encode("password123"),
                role = role
            )
        )
        val loginReq = mapOf("username" to username, "password" to "password123")
        val result = mockMvc.post("/api/v1/auth/login") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(loginReq)
        }.andReturn()
        val json = objectMapper.readTree(result.response.contentAsString)
        return json["data"]["token"].asText()
    }

    @Test
    fun `moderator can ban a user for a duration`() {
        createUserAndGetToken("target")
        val modToken = createUserAndGetToken("mod1", UserRole.ROLE_MODERATOR)
        val target = userRepository.findByUsername("target")!!

        mockMvc.post("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "24")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.bannedUntil") { exists() }
        }

        val updated = userRepository.findByUsername("target")!!
        assertTrue(updated.isBanned())
    }

    @Test
    fun `moderator can unban a user`() {
        createUserAndGetToken("target2")
        val modToken = createUserAndGetToken("mod2", UserRole.ROLE_MODERATOR)
        val target = userRepository.findByUsername("target2")!!

        mockMvc.post("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "24")
        }.andExpect { status { isOk() } }

        mockMvc.delete("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.bannedUntil") { doesNotExist() }
        }

        val updated = userRepository.findByUsername("target2")!!
        assertTrue(!updated.isBanned())
    }

    @Test
    fun `moderator cannot ban an admin`() {
        createUserAndGetToken("admin1", UserRole.ROLE_ADMIN)
        val modToken = createUserAndGetToken("mod3", UserRole.ROLE_MODERATOR)
        val admin = userRepository.findByUsername("admin1")!!

        mockMvc.post("/api/v1/moderator/users/${admin.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "24")
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value(400) }
        }
    }

    @Test
    fun `ban duration must be positive`() {
        createUserAndGetToken("target3")
        val modToken = createUserAndGetToken("mod4", UserRole.ROLE_MODERATOR)
        val target = userRepository.findByUsername("target3")!!

        mockMvc.post("/api/v1/moderator/users/${target.id}/ban") {
            header("Authorization", "Bearer $modToken")
            param("durationHours", "0")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `regular user cannot ban`() {
        val userToken = createUserAndGetToken("user1")
        createUserAndGetToken("target4")
        val targetUser = userRepository.findByUsername("target4")!!

        mockMvc.post("/api/v1/moderator/users/${targetUser.id}/ban") {
            header("Authorization", "Bearer $userToken")
            param("durationHours", "24")
        }.andExpect {
            status { isForbidden() }
        }
    }

    @Test
    fun `moderator can list users`() {
        createUserAndGetToken("userA")
        createUserAndGetToken("userB")
        val modToken = createUserAndGetToken("mod5", UserRole.ROLE_MODERATOR)

        mockMvc.get("/api/v1/moderator/users") {
            header("Authorization", "Bearer $modToken")
        }.andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.length()") { value(3) }
        }
    }

    @Test
    fun `change role with invalid role returns 400`() {
        createUserAndGetToken("target5")
        val adminToken = createUserAndGetToken("admin2", UserRole.ROLE_ADMIN)
        val target = userRepository.findByUsername("target5")!!

        mockMvc.put("/api/v1/admin/users/${target.id}/role") {
            header("Authorization", "Bearer $adminToken")
            param("role", "INVALID_ROLE")
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
