plugins {
    id("org.springframework.boot") version "3.2.5"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "2.3.21"
    kotlin("plugin.spring") version "2.3.21"
    kotlin("plugin.jpa") version "2.3.21"
    id("org.jlleitschuh.gradle.ktlint") version "12.1.0"
    id("dev.detekt") version "2.0.0-alpha.3"
}

group = "com.project"
version = "0.0.1-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

repositories {
    mavenCentral()
    maven { url = uri("https://repo.spring.io/milestone") }
}

dependencies {
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-amqp")

    // Spring AI
    implementation(platform("org.springframework.ai:spring-ai-bom:1.1.7"))
    implementation("org.springframework.ai:spring-ai-starter-model-deepseek")
    // Kotlin
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.5")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.5")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.h2database:h2")
    testImplementation("com.h2database:h2")

    // OpenAPI / Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Test
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
    jvmArgs("-Xshare:off")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-java-parameters")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("e2e")
    }
}

tasks.register<Test>("e2eTest") {
    useJUnitPlatform {
        includeTags("e2e")
    }
    // Explicit classpath to avoid deprecation on custom Test task
    classpath = sourceSets["test"].runtimeClasspath
    testClassesDirs = sourceSets["test"].output.classesDirs
    systemProperty("spring.profiles.active", "e2e")
    mustRunAfter(tasks.test)
}

// ── ktlint ────────────────────────────────────────────────────────────────────
ktlint {
    version.set("1.8.0")
    debug.set(false)
    verbose.set(true)
    outputToConsole.set(true)
    filter {
        exclude { element -> element.file.path.contains("/build/") }
    }
}

// ── detekt ────────────────────────────────────────────────────────────────────
detekt {
    toolVersion = "2.0.0-alpha.3"
    source.setFrom("src/main/kotlin", "src/test/kotlin")
    config.setFrom("detekt.yml")
    buildUponDefaultConfig = true
    autoCorrect = true
    parallel = true
}

// ── Combined lint task ────────────────────────────────────────────────────────
tasks.register("lint") {
    group = "verification"
    description = "Run ktlint and detekt checks"
    dependsOn("ktlintCheck", "detekt")
}

tasks.register("lintFormat") {
    group = "formatting"
    description = "Auto-format with ktlint and detekt"
    dependsOn("ktlintFormat")
}
