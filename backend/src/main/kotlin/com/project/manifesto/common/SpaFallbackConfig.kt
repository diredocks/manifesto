package com.project.manifesto.common

import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.servlet.resource.PathResourceResolver

/**
 * Serves index.html for unmatched GET paths so React Router can handle them.
 * Only applies to paths that don't match API endpoints or static assets.
 */
@Configuration
class SpaFallbackConfig : WebMvcConfigurer {

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // Serve index.html for any unmatched path (SPA client-side routing fallback)
        registry.addResourceHandler("/**")
            .addResourceLocations("classpath:/static/")
            .resourceChain(true)
            .addResolver(object : PathResourceResolver() {
                override fun getResource(resourcePath: String, location: Resource): Resource? {
                    val resource = location.createRelative(resourcePath)
                    if (resource.exists() && resource.isReadable) {
                        return resource
                    }
                    // Fallback to index.html for SPA routing
                    return ClassPathResource("/static/index.html")
                }
            })
    }
}
