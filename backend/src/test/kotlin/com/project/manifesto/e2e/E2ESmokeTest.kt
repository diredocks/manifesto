package com.project.manifesto.e2e

import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("e2e")
@Tag("e2e")
class E2ESmokeTest {
    @Autowired
    private lateinit var context: ApplicationContext

    @Test
    fun `context loads with real infra`() {
        val activeProfiles = context.environment.activeProfiles
        println("Active profiles: ${activeProfiles.joinToString()}")

        val hasTestConfig = context.containsBean("com.project.manifesto.TestConfig")
        println("TestConfig loaded: $hasTestConfig")
    }
}
