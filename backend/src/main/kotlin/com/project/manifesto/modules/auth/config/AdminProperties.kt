package com.project.manifesto.modules.auth.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
@ConfigurationProperties(prefix = "app.admin")
class AdminProperties {
    var enabled: Boolean = true
    lateinit var username: String
    lateinit var email: String
    lateinit var password: String
}
