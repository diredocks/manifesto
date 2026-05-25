package com.project.manifesto.modules.ranking

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import com.project.manifesto.modules.vote.service.VoteService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class RankingIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val userRepository: UserRepository,
    private val postRepository: PostRepository,
    private val voteRepository: VoteRepository,
    private val voteService: VoteService,
    private val objectMapper: ObjectMapper
) {

    @BeforeEach
    fun setup() {
        voteRepository.deleteAll()
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

    private fun createPost(token: String, title: String, type: PostType = PostType.ASK): Long {
        val request = if (type == PostType.LINK) {
            CreatePostRequest(title = title, type = PostType.LINK, url = "https://example.com/$title")
        } else {
            CreatePostRequest(title = title, type = PostType.ASK, content = "content for $title")
        }
        val result = mockMvc.post("/api/v1/posts") {
            contentType = MediaType.APPLICATION_JSON
            header("Authorization", "Bearer $token")
            content = objectMapper.writeValueAsString(request)
        }.andReturn()
        return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
    }

    @Test
    fun `hot ranking returns posts`() {
        val token = createUserAndGetToken("rankuser1")
        createPost(token, "Post A")
        createPost(token, "Post B")

        mockMvc.get("/api/v1/ranking/hot").andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.length()") { value(2) }
        }
    }

    @Test
    fun `new ranking returns posts in order`() {
        val token = createUserAndGetToken("rankuser2")
        createPost(token, "First")
        createPost(token, "Second")

        mockMvc.get("/api/v1/ranking/new").andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data[0].title") { value("Second") }
            jsonPath("$.data[1].title") { value("First") }
        }
    }

    @Test
    fun `top ranking returns posts`() {
        val token = createUserAndGetToken("rankuser3")
        createPost(token, "Post X")
        createPost(token, "Post Y")

        mockMvc.get("/api/v1/ranking/top").andExpect {
            status { isOk() }
            jsonPath("$.code") { value(200) }
            jsonPath("$.data.length()") { value(2) }
        }
    }

    @Test
    fun `pagination works for hot ranking`() {
        val token = createUserAndGetToken("rankuser4")
        repeat(5) { i -> createPost(token, "Post $i") }

        mockMvc.get("/api/v1/ranking/hot") {
            param("page", "0")
            param("size", "3")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(3) }
        }
    }

    @Test
    fun `ranking is publicly accessible`() {
        mockMvc.get("/api/v1/ranking/hot").andExpect {
            status { isOk() }
        }
        mockMvc.get("/api/v1/ranking/new").andExpect {
            status { isOk() }
        }
        mockMvc.get("/api/v1/ranking/top").andExpect {
            status { isOk() }
        }
    }

    @Test
    fun `new ranking with type ASK returns only ASK posts`() {
        val token = createUserAndGetToken("askfilter1")
        createPost(token, "Ask Post 1", PostType.ASK)
        createPost(token, "Link Post", PostType.LINK)
        createPost(token, "Ask Post 2", PostType.ASK)

        mockMvc.get("/api/v1/ranking/new") {
            param("type", "ASK")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[0].type") { value("ASK") }
            jsonPath("$.data[1].type") { value("ASK") }
        }
    }

    @Test
    fun `new ranking with type LINK returns only LINK posts`() {
        val token = createUserAndGetToken("linkfilter1")
        createPost(token, "Link A", PostType.LINK)
        createPost(token, "Ask Q", PostType.ASK)
        createPost(token, "Link B", PostType.LINK)

        mockMvc.get("/api/v1/ranking/new") {
            param("type", "LINK")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[0].type") { value("LINK") }
            jsonPath("$.data[1].type") { value("LINK") }
        }
    }

    @Test
    fun `hot ranking with type filter returns only matching posts`() {
        val token = createUserAndGetToken("hotfilter1")
        createPost(token, "Hot Ask", PostType.ASK)
        createPost(token, "Hot Link", PostType.LINK)
        createPost(token, "Another Ask", PostType.ASK)

        mockMvc.get("/api/v1/ranking/hot") {
            param("type", "ASK")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[0].type") { value("ASK") }
            jsonPath("$.data[1].type") { value("ASK") }
        }
    }

    @Test
    fun `top ranking with type filter returns only matching posts`() {
        val token = createUserAndGetToken("topfilter1")
        createPost(token, "Top Link 1", PostType.LINK)
        createPost(token, "Top Ask", PostType.ASK)
        createPost(token, "Top Link 2", PostType.LINK)

        mockMvc.get("/api/v1/ranking/top") {
            param("type", "LINK")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(2) }
            jsonPath("$.data[0].type") { value("LINK") }
            jsonPath("$.data[1].type") { value("LINK") }
        }
    }

    @Test
    fun `type filter with pagination works`() {
        val token = createUserAndGetToken("askpage1")
        repeat(5) { i -> createPost(token, "Ask Page $i", PostType.ASK) }

        mockMvc.get("/api/v1/ranking/new") {
            param("type", "ASK")
            param("page", "0")
            param("size", "3")
        }.andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(3) }
            jsonPath("$.data[0].type") { value("ASK") }
        }
    }

    @Test
    fun `no type filter returns all posts`() {
        val token = createUserAndGetToken("nofilter1")
        createPost(token, "Ask 1", PostType.ASK)
        createPost(token, "Link 1", PostType.LINK)
        createPost(token, "Ask 2", PostType.ASK)

        mockMvc.get("/api/v1/ranking/new").andExpect {
            status { isOk() }
            jsonPath("$.data.length()") { value(3) }
        }
    }
}
