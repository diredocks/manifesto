package com.project.manifesto.modules.submit

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
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
class PostIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val postRepository: PostRepository,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
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

        @Test
        fun `create LINK post returns post detail`() {
            val token = createUserAndGetToken("linkposter")
            val request =
                CreatePostRequest(
                    title = "Test Link",
                    type = PostType.LINK,
                    url = "https://example.com/article",
                )

            mockMvc
                .post("/api/v1/posts") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.title") { value("Test Link") }
                    jsonPath("$.data.url") { value("https://example.com/article") }
                    jsonPath("$.data.type") { value("LINK") }
                    jsonPath("$.data.authorUsername") { value("linkposter") }
                }
        }

        @Test
        fun `create ASK post returns post detail`() {
            val token = createUserAndGetToken("askposter")
            val request =
                CreatePostRequest(
                    title = "Test Ask",
                    type = PostType.ASK,
                    content = "What is Kotlin?",
                )

            mockMvc
                .post("/api/v1/posts") {
                    contentType = MediaType.APPLICATION_JSON
                    header("Authorization", "Bearer $token")
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data.content") { value("What is Kotlin?") }
                    jsonPath("$.data.type") { value("ASK") }
                }
        }

        @Test
        fun `create post without auth returns 401`() {
            val request =
                CreatePostRequest(
                    title = "Test",
                    type = PostType.ASK,
                    content = "Content",
                )

            mockMvc
                .post("/api/v1/posts") {
                    contentType = MediaType.APPLICATION_JSON
                    content = objectMapper.writeValueAsString(request)
                }.andExpect {
                    status { isUnauthorized() }
                }
        }

        @Test
        fun `get post by id returns post`() {
            val token = createUserAndGetToken("getposter")
            val createRequest =
                CreatePostRequest(
                    title = "Get Me",
                    type = PostType.ASK,
                    content = "Content",
                )
            val createResult =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(createRequest)
                    }.andReturn()
            val postId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

            mockMvc.get("/api/v1/posts/$postId").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.title") { value("Get Me") }
            }
        }

        @Test
        fun `list posts without auth is allowed`() {
            mockMvc.get("/api/v1/posts").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
            }
        }

        @Test
        fun `delete own post succeeds`() {
            val token = createUserAndGetToken("deleteposter")
            val createRequest =
                CreatePostRequest(
                    title = "Delete Me",
                    type = PostType.ASK,
                    content = "Content",
                )
            val createResult =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(createRequest)
                    }.andReturn()
            val postId = objectMapper.readTree(createResult.response.contentAsString)["data"]["id"].asLong()

            mockMvc
                .delete("/api/v1/posts/$postId") {
                    header("Authorization", "Bearer $token")
                }.andExpect {
                    status { isOk() }
                    jsonPath("$.code") { value(200) }
                    jsonPath("$.data") { value(true) }
                }

            mockMvc.get("/api/v1/posts/$postId").andExpect {
                status { isNotFound() }
            }
        }
    }
