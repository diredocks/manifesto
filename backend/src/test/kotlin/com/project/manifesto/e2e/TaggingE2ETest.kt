package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.tagging.repository.PostTagRepository
import com.project.manifesto.modules.tagging.repository.TagRepository
import com.project.manifesto.modules.tagging.service.TagService
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
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
class TaggingE2ETest
    @Autowired
    constructor(
        mockMvc: MockMvc,
        userRepository: UserRepository,
        postRepository: PostRepository,
        voteRepository: VoteRepository,
        commentRepository: CommentRepository,
        notificationRepository: NotificationRepository,
        objectMapper: ObjectMapper,
        private val tagRepository: TagRepository,
        private val postTagRepository: PostTagRepository,
        private val tagService: TagService,
    ) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {
        @BeforeEach
        fun cleanTagTables() {
            postTagRepository.deleteAll()
            tagRepository.deleteAll()
        }

        private fun createPost(
            token: String,
            title: String,
            type: String = "ASK",
            postContent: String = "E2E test content",
        ): Long {
            val body = mapOf("title" to title, "type" to type, "content" to postContent)
            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(body)
                    }.andExpect { status { isOk() } }
                    .andReturn()
            return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
        }

        @Test
        fun `assign tags and retrieve via API against real PostgreSQL`() {
            val token = registerAndGetToken("e2etaguser")
            val postId = createPost(token, "E2E Tagged Post")

            tagService.assignTags(postId, listOf("postgresql", "e2e", "testing"))

            val tags = tagService.getTagsForPost(postId)
            assertEquals(3, tags.size)
            assertTrue(tags.containsAll(listOf("postgresql", "e2e", "testing")))

            val savedTags = tagRepository.findAll()
            assertEquals(3, savedTags.size)

            val postTags = postTagRepository.findByPostId(postId)
            assertEquals(3, postTags.size)
        }

        @Test
        fun `GET tags returns sorted unique tags from real PostgreSQL`() {
            val token = registerAndGetToken("e2etaguser2")
            val post1Id = createPost(token, "Post Alpha")
            val post2Id = createPost(token, "Post Beta")

            tagService.assignTags(post1Id, listOf("java", "kotlin", "spring"))
            tagService.assignTags(post2Id, listOf("rust", "java", "async"))

            mockMvc.get("/api/v1/tags").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(5) }
                jsonPath("$.data[0]") { value("async") }
                jsonPath("$.data[1]") { value("java") }
                jsonPath("$.data[2]") { value("kotlin") }
                jsonPath("$.data[3]") { value("rust") }
                jsonPath("$.data[4]") { value("spring") }
            }
        }

        @Test
        fun `GET tags by name returns posts from real PostgreSQL`() {
            val token = registerAndGetToken("e2etaguser3")
            val post1Id = createPost(token, "Rust for Beginners")
            val post2Id = createPost(token, "Advanced Rust Patterns")
            val post3Id = createPost(token, "Go Concurrency")

            tagService.assignTags(post1Id, listOf("rust", "beginners"))
            tagService.assignTags(post2Id, listOf("rust", "advanced"))
            tagService.assignTags(post3Id, listOf("go", "concurrency"))

            mockMvc.get("/api/v1/tags/rust/posts").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(2) }
                jsonPath("$.data[0].title") { value("Rust for Beginners") }
                jsonPath("$.data[1].title") { value("Advanced Rust Patterns") }
            }

            mockMvc.get("/api/v1/tags/go/posts").andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].title") { value("Go Concurrency") }
            }
        }

        @Test
        fun `post detail includes tags from join table`() {
            val token = registerAndGetToken("e2etaguser4")
            val postId = createPost(token, "Full Stack Development")

            tagService.assignTags(postId, listOf("javascript", "react", "nodejs"))

            mockMvc.get("/api/v1/posts/$postId").andExpect {
                status { isOk() }
                jsonPath("$.data.title") { value("Full Stack Development") }
                jsonPath("$.data.tags.length()") { value(3) }
                jsonPath("$.data.tags[0]") { value("javascript") }
                jsonPath("$.data.tags[1]") { value("nodejs") }
                jsonPath("$.data.tags[2]") { value("react") }
            }
        }

        @Test
        fun `reassigning tags replaces previous ones`() {
            val token = registerAndGetToken("e2etaguser5")
            val postId = createPost(token, "Evolving Topics")

            tagService.assignTags(postId, listOf("python", "ml"))
            tagService.assignTags(postId, listOf("rust", "wasm"))

            val tags = tagService.getTagsForPost(postId)
            assertEquals(2, tags.size)
            assertTrue(tags.containsAll(listOf("rust", "wasm")))

            val allTags = tagService.getAllTags()
            assertTrue(allTags.contains("python")) // orphaned tag still exists globally
        }

        @Test
        fun `tags endpoint accessible without authentication`() {
            mockMvc.get("/api/v1/tags").andExpect { status { isOk() } }

            mockMvc.get("/api/v1/tags/java/posts").andExpect { status { isOk() } }
        }

        @Test
        fun `posts with no tags return empty tags list`() {
            val token = registerAndGetToken("e2etaguser6")
            createPost(token, "Untagged E2E Post")

            mockMvc.get("/api/v1/posts").andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].tags.length()") { value(0) }
            }
        }
    }
