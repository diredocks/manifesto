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

# Run E2E tests (requires PostgreSQL)
./gradlew e2eTest --no-daemon
```

## Architecture

Spring Boot 3.2 / Kotlin 1.9 / Java 21 modular monolith. REST JSON APIs only — no SSR, no Thymeleaf.

**Package layout** under `src/main/kotlin/com/project/manifesto/`:

```
common/          # BaseEntity, ApiResponse<T>, GlobalExceptionHandler, JpaConfig, RestClientConfig
security/        # JWT filter, SecurityConfig (stateless, role-based)
infra/           # GenerateConfig (placeholder for code generation profile)
modules/
  auth/          # Register, login, moderator/admin endpoints
  user/          # User entity, UserRole enum, UserDetailsService
  submit/        # Post CRUD (LINK + ASK types)
  vote/          # Upvote/unvote with DB unique constraint
  ranking/       # Hacker News-style hot ranking, recalculation on vote events
  comment/       # Nested comments (adjacency list via parent_id)
  notification/  # Synchronous notifications (DB-backed, read/ack)
```

Each module follows: `controller/` → `service/` → `repository/` + `entity/` + `dto/`. Controllers only handle HTTP; services contain business logic; entities are never exposed directly to API responses.

## Profiles

| Profile | DB | Notes |
|---------|-----|-------|
| `default` (production) | PostgreSQL | Full stack |
| `test` (unit/integration) | H2 in-memory | Zero external dependencies |
| `e2e` | PostgreSQL `manifesto_e2e` | Real DB, tagged `@Tag("e2e")` |
| `generate` | H2 in-memory | For code generation tasks |

Tests in `test` profile require zero external dependencies. E2E tests are tagged `@Tag("e2e")` and excluded from the default `test` task.

## Key patterns

- **Synchronous voting**: `VoteService` checks `existsByUserIdAndPostId` before inserting, with a database unique constraint on `(userId, postId)` as fallback. Ranking is recalculated synchronously after each vote.
- **Hacker News ranking**: `hotScore = score / (hours + 2)^1.5`. Recalculated on each vote. Hot posts served from DB queries — no external cache.
- **Soft delete**: Posts and comments use a `deleted` boolean column, never hard-deleted.
- **API wrapper**: All responses use `ApiResponse<T>(code, message, data)`. Routes are under `/api/v1/`.
- **RBAC**: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`. Moderators can delete any post; admins can list users and change roles. `@PreAuthorize` on controller methods.
- **Notifications**: Synchronous DB-backed notifications. Created inline (e.g. on comment reply), fetched via paginated endpoint.

## Infrastructure

| Service | Port | Required for |
|---------|------|-------------|
| PostgreSQL | 5432 | Default + E2E profiles |

Only PostgreSQL is needed. The `test` profile uses H2 in-memory and requires nothing external.
