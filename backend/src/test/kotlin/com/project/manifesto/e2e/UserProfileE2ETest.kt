package com.project.manifesto.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.project.manifesto.modules.comment.repository.CommentRepository
import com.project.manifesto.modules.notification.repository.NotificationRepository
import com.project.manifesto.modules.submit.repository.PostRepository
import com.project.manifesto.modules.user.repository.UserRepository
import com.project.manifesto.modules.vote.repository.VoteRepository
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("e2e")
@Tag("e2e")
class UserProfileE2ETest @Autowired constructor(
    mockMvc: MockMvc,
    userRepository: UserRepository,
    postRepository: PostRepository,
    voteRepository: VoteRepository,
    commentRepository: CommentRepository,
    notificationRepository: NotificationRepository,
    objectMapper: ObjectMapper
) : E2EBase(mockMvc, userRepository, postRepository, voteRepository, commentRepository, notificationRepository, objectMapper) {

    @Test
    fun `user profile endpoint returns public info`() {
        registerAndGetToken("uprof")
        mockMvc.get("/api/v1/users/uprof").andExpect {
            status { isOk() }
            jsonPath("$.data.username") { value("uprof") }
            jsonPath("$.data.karma") { value(0) }
            jsonPath("$.data.createdAt") { exists() }
        }
    }

    @Test
    fun `user profile for nonexistent user returns 400`() {
        mockMvc.get("/api/v1/users/noone").andExpect {
            status { isBadRequest() }
        }
    }
}
