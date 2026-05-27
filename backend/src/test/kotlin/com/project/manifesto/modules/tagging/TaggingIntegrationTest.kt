package com.project.manifesto.modules.tagging

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.TestConfig
import com.project.manifesto.modules.auth.dto.RegisterRequest
import com.project.manifesto.modules.submit.dto.CreatePostRequest
import com.project.manifesto.modules.submit.entity.PostType
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.tagging.entity.PostTag
import com.project.manifesto.modules.tagging.repository.PostTagRepository
import com.project.manifesto.modules.tagging.repository.TagRepository
import com.project.manifesto.modules.tagging.service.TagService
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
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestConfig::class)
class TaggingIntegrationTest
    @Autowired
    constructor(
        private val mockMvc: MockMvc,
        private val userRepository: UserRepository,
        private val postRepository: PostRepository,
        private val tagRepository: TagRepository,
        private val postTagRepository: PostTagRepository,
        private val tagService: TagService,
        private val objectMapper: ObjectMapper,
    ) {
        @BeforeEach
        fun setup() {
            postTagRepository.deleteAll()
            tagRepository.deleteAll()
            postRepository.deleteAll()
            userRepository.deleteAll()
        }

        private fun registerAndGetToken(username: String): String {
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
            type: PostType = PostType.ASK,
            postContent: String = "Test content",
        ): Long {
            val request = CreatePostRequest(title = title, type = type, content = postContent)
            val result =
                mockMvc
                    .post("/api/v1/posts") {
                        contentType = MediaType.APPLICATION_JSON
                        header("Authorization", "Bearer $token")
                        content = objectMapper.writeValueAsString(request)
                    }.andReturn()
            return objectMapper.readTree(result.response.contentAsString)["data"]["id"].asLong()
        }

        // ── TagService ──────────────────────────────────────────────

        @Test
        fun `assignTags creates new tags and links to post`() {
            val token = registerAndGetToken("taguser1")
            val postId = createPost(token, "Kotlin Coroutines Guide")

            tagService.assignTags(postId, listOf("kotlin", "coroutines", "async"))

            val tags = tagService.getTagsForPost(postId)
            assert(tags.size == 3)
            assert(tags.containsAll(listOf("kotlin", "coroutines", "async")))
        }

        @Test
        fun `assignTags normalizes tag names to lowercase`() {
            val token = registerAndGetToken("taguser2")
            val postId = createPost(token, "Spring Boot Tips")

            tagService.assignTags(postId, listOf("Spring", "BOOT", "Java"))

            val tags = tagService.getTagsForPost(postId)
            assert(tags.containsAll(listOf("spring", "boot", "java")))
        }

        @Test
        fun `assignTags skips blank strings`() {
            val token = registerAndGetToken("taguser3")
            val postId = createPost(token, "Testing 101")

            tagService.assignTags(postId, listOf("testing", "", "  ", "unit-test"))

            val tags = tagService.getTagsForPost(postId)
            assert(tags.size == 2)
            assert(tags.contains("testing"))
            assert(tags.contains("unit-test"))
        }

        @Test
        fun `assignTags reuses existing tags instead of creating duplicates`() {
            val token = registerAndGetToken("taguser4")
            val post1Id = createPost(token, "Post One")
            val post2Id = createPost(token, "Post Two")

            tagService.assignTags(post1Id, listOf("java", "kotlin"))
            tagService.assignTags(post2Id, listOf("java", "rust"))

            val allTags = tagRepository.findAll()
            val javaTags = allTags.filter { it.name == "java" }
            assert(javaTags.size == 1) { "Tag 'java' should exist only once in DB" }
        }

        @Test
        fun `assignTags removes previous tags before assigning new ones`() {
            val token = registerAndGetToken("taguser5")
            val postId = createPost(token, "Evolving Post")

            tagService.assignTags(postId, listOf("python", "django"))
            tagService.assignTags(postId, listOf("kotlin", "spring"))

            val tags = tagService.getTagsForPost(postId)
            assert(tags.size == 2)
            assert(tags.containsAll(listOf("kotlin", "spring")))
            assert(!tags.contains("python"))
        }

        @Test
        fun `getTagsForPost returns empty list when no tags assigned`() {
            val token = registerAndGetToken("taguser6")
            val postId = createPost(token, "Untagged Post")

            val tags = tagService.getTagsForPost(postId)
            assert(tags.isEmpty())
        }

        @Test
        fun `getPostIdsByTag returns posts with given tag`() {
            val token = registerAndGetToken("taguser7")
            val post1Id = createPost(token, "Rust Article")
            val post2Id = createPost(token, "Rust vs Go")
            val post3Id = createPost(token, "Python Guide")

            tagService.assignTags(post1Id, listOf("rust", "systems"))
            tagService.assignTags(post2Id, listOf("rust", "go"))
            tagService.assignTags(post3Id, listOf("python"))

            val rustPostIds = tagService.getPostIdsByTag("rust")
            assert(rustPostIds.size == 2)
            assert(rustPostIds.containsAll(listOf(post1Id, post2Id)))

            val pythonPostIds = tagService.getPostIdsByTag("python")
            assert(pythonPostIds.size == 1)
            assert(pythonPostIds.contains(post3Id))

            val unknownPostIds = tagService.getPostIdsByTag("nonexistent")
            assert(unknownPostIds.isEmpty())
        }

        @Test
        fun `getPostIdsByTag is case insensitive`() {
            val token = registerAndGetToken("taguser8")
            val postId = createPost(token, "TypeScript Handbook")

            tagService.assignTags(postId, listOf("TypeScript"))

            val lower = tagService.getPostIdsByTag("typescript")
            assert(lower.size == 1)
            assert(lower.contains(postId))

            val mixed = tagService.getPostIdsByTag("TypeScript")
            assert(mixed.size == 1)
            assert(mixed.contains(postId))
        }

        @Test
        fun `getAllTags returns all unique tags sorted`() {
            val token = registerAndGetToken("taguser9")
            val post1Id = createPost(token, "Post A")
            val post2Id = createPost(token, "Post B")

            tagService.assignTags(post1Id, listOf("kotlin", "java", "spring"))
            tagService.assignTags(post2Id, listOf("rust", "java", "async"))

            val allTags = tagService.getAllTags()
            assert(allTags == listOf("async", "java", "kotlin", "rust", "spring"))
        }

        @Test
        fun `getAllTags returns empty list when no tags exist`() {
            val tags = tagService.getAllTags()
            assert(tags.isEmpty())
        }

        @Test
        fun `assignTags is idempotent for same post and tag`() {
            val token = registerAndGetToken("taguser10")
            val postId = createPost(token, "Idempotent Test")

            tagService.assignTags(postId, listOf("java"))
            tagService.assignTags(postId, listOf("java", "kotlin"))

            val postTags = postTagRepository.findByPostId(postId)
            assert(postTags.size == 2)
        }

        @Test
        fun `PostTag entity has correct unique constraint fields`() {
            val postTag1 = PostTag(postId = 1, tagId = 2)
            val postTag2 = PostTag(postId = 1, tagId = 3)

            assert(postTag1.postId == 1L)
            assert(postTag1.tagId == 2L)
            assert(postTag2.postId == 1L)
            assert(postTag2.tagId == 3L)
        }

        // ── TagController (API) ─────────────────────────────────────

        @Test
        fun `GET tags returns empty list when no tags exist`() {
            mockMvc.get("/api/v1/tags").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(0) }
            }
        }

        @Test
        fun `GET tags returns all unique tags`() {
            val token = registerAndGetToken("taguser11")
            val postId = createPost(token, "API Tag Test")
            tagService.assignTags(postId, listOf("api", "rest", "graphql"))

            mockMvc.get("/api/v1/tags").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(3) }
                jsonPath("$.data[0]") { value("api") }
                jsonPath("$.data[1]") { value("graphql") }
                jsonPath("$.data[2]") { value("rest") }
            }
        }

        @Test
        fun `GET tags by name returns posts with that tag`() {
            val token = registerAndGetToken("taguser12")
            val post1Id = createPost(token, "Kotlin Post 1")
            val post2Id = createPost(token, "Java Post")
            tagService.assignTags(post1Id, listOf("kotlin"))
            tagService.assignTags(post2Id, listOf("java"))

            mockMvc.get("/api/v1/tags/kotlin/posts").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(1) }
                jsonPath("$.data[0].title") { value("Kotlin Post 1") }
                jsonPath("$.data[0].tags.length()") { value(1) }
                jsonPath("$.data[0].tags[0]") { value("kotlin") }
            }
        }

        @Test
        fun `GET tags by name is case insensitive`() {
            val token = registerAndGetToken("taguser13")
            val postId = createPost(token, "Mixed Case Post")
            tagService.assignTags(postId, listOf("TypeScript"))

            mockMvc.get("/api/v1/tags/TypeScript/posts").andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
            }

            mockMvc.get("/api/v1/tags/typescript/posts").andExpect {
                status { isOk() }
                jsonPath("$.data.length()") { value(1) }
            }
        }

        @Test
        fun `GET tags by name returns empty list for unknown tag`() {
            mockMvc.get("/api/v1/tags/nonexistent/posts").andExpect {
                status { isOk() }
                jsonPath("$.code") { value(200) }
                jsonPath("$.data.length()") { value(0) }
            }
        }

        @Test
        fun `GET tags is accessible without authentication`() {
            mockMvc.get("/api/v1/tags").andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `GET tags by name posts is accessible without authentication`() {
            mockMvc.get("/api/v1/tags/java/posts").andExpect {
                status { isOk() }
            }
        }

        @Test
        fun `post detail response includes tags field`() {
            val token = registerAndGetToken("taguser14")
            val postId = createPost(token, "Tagged Detail Post")
            tagService.assignTags(postId, listOf("spring", "boot"))

            val result =
                mockMvc
                    .get("/api/v1/posts/$postId")
                    .andExpect {
                        status { isOk() }
                        jsonPath("$.data.tags.length()") { value(2) }
                    }.andReturn()
            val tags = objectMapper.readTree(result.response.contentAsString)["data"]["tags"]
            val tagList = tags.map { it.asText() }
            assert(tagList.containsAll(listOf("spring", "boot")))
        }

        @Test
        fun `post list response includes tags field`() {
            val token = registerAndGetToken("taguser15")
            val postId = createPost(token, "Listed Tagged Post")
            tagService.assignTags(postId, listOf("testing"))

            mockMvc.get("/api/v1/posts").andExpect {
                status { isOk() }
                jsonPath("$.data.content[0].tags.length()") { value(1) }
                jsonPath("$.data.content[0].tags[0]") { value("testing") }
            }
        }
    }
