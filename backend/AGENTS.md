# AGENTS.md

This file provides guidance to agents when working with code in this repository.

## Build & Test

```bash
# Compile only (fast)
./gradlew compileKotlin --no-daemon

# Full jar
./gradlew bootJar --no-daemon

# Run all unit/integration tests (H2, no external infra needed)
./gradlew test --no-daemon

# Run a single test class
./gradlew test --no-daemon --tests "com.project.manifesto.modules.auth.AuthIntegrationTest"

# Run a single test method
./gradlew test --no-daemon --tests "com.project.manifesto.modules.auth.AuthIntegrationTest.register new user returns JWT token"

# Run E2E tests (requires PostgreSQL, Redis, RabbitMQ)
./gradlew e2eTest --no-daemon
```

## Architecture

Spring Boot 3.2 / Kotlin 1.9 / Java 21 modular monolith. REST JSON APIs only — no SSR, no Thymeleaf.

**Package layout** under `src/main/kotlin/com/project/manifesto/`:

```
common/          # BaseEntity, ApiResponse<T>, GlobalExceptionHandler, JpaConfig, RestClientConfig
security/        # JWT filter, SecurityConfig (stateless, role-based)
infra/
  rabbitmq/      # EventPublisher interface, RabbitEventPublisher, RabbitConfig (exchanges/queues)
  redis/         # LockService interface, RedisLockService (distributed voting lock)
modules/
  auth/          # Register, login, moderator/admin endpoints
  user/          # User entity, UserRole enum, UserDetailsService
  submit/        # Post CRUD (LINK + ASK types)
  vote/          # Upvote/unvote with Redis distributed lock
  ranking/       # Hacker News-style hot ranking, Redis-cached top-100 feed
  comment/       # Nested comments (adjacency list via parent_id)
  notification/  # Async notifications via RabbitMQ
  ai/            # Spring AI (DeepSeek): URL summarization + tag generation
```

Each module follows: `controller/` → `service/` → `repository/` + `entity/` + `dto/` + `event/`. Controllers only handle HTTP; services contain business logic; entities are never exposed directly to API responses.

## Profiles

| Profile | DB | Redis/RabbitMQ | AI Service |
|---------|----|----------------|------------|
| `default` (production) | PostgreSQL | Real | Live (DeepSeek) |
| `test` (unit/integration) | H2 in-memory | Excluded from auto-config; no-op stubs from `TestConfig.kt` | Disabled (`@Profile("!test & !e2e")`) |
| `e2e` | PostgreSQL `manifesto_e2e` | Real | Disabled |

Tests in `test` profile require zero external dependencies. E2E tests are tagged `@Tag("e2e")` and excluded from the default `test` task.

## Key patterns

- **Event-driven async**: After a post is created, voted on, or receives a reply, the service publishes an event to RabbitMQ. Consumers handle AI summarization/tagging, ranking recalculation, and notifications asynchronously — the HTTP request never blocks on these.
- **Distributed voting lock**: `VoteService` acquires a Redis lock keyed `vote:post:{postId}:user:{userId}` before inserting a vote, with a database unique constraint as fallback.
- **Hacker News ranking**: `hotScore = score / (hours + 2)^1.5`. Recalculated async on vote events. Top 100 hot posts cached in Redis (`feed:hot:top100`, 5 min TTL).
- **Soft delete**: Posts and comments use a `deleted` boolean column, never hard-deleted.
- **API wrapper**: All responses use `ApiResponse<T>(code, message, data)`. Routes are under `/api/v1/`.
- **RBAC**: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`. Moderators can delete any post; admins can list users and change roles. `@PreAuthorize` on controller methods.
- **AI is fire-and-forget**: AIService runs asynchronously via the PostCreated consumer. Failures are logged but never break the submit flow. AIService uses a proxy-aware `java.net.http.HttpClient` for fetching article content.

## Infrastructure

| Service | Port | Required for |
|---------|------|-------------|
| PostgreSQL | 5432 | Default + E2E profiles |
| Redis | 6379 | Vote locks, hot-feed cache |
| RabbitMQ | 5672 | Async event bus |

All three can be bypassed in the `test` profile. For E2E tests, PostgreSQL must have a `manifesto_e2e` database and the services must be running.
