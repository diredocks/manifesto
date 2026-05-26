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

## Lint & Format

```bash
# Run all checks (ktlint + detekt)
./gradlew lint --no-daemon

# Auto-format code (ktlint)
./gradlew lintFormat --no-daemon

# Run ktlint only
./gradlew ktlintCheck --no-daemon

# Auto-format with ktlint only
./gradlew ktlintFormat --no-daemon

# Run detekt only
./gradlew detekt --no-daemon

# Update detekt baseline after fixing issues
./gradlew detektBaseline --no-daemon
```

**ktlint** enforces Kotlin style conventions and auto-formats code. Configuration lives in the `ktlint { }` block in `build.gradle.kts`.

**detekt** performs static analysis — complexity, style, naming, performance, etc. Rules are configured in `detekt.yml`. Existing violations are recorded in `detekt-baseline.xml`; only new violations will fail the build. After fixing a baseline entry, regenerate the baseline to shrink it.

## Architecture

Spring Boot 3.2 / Kotlin 2.3 / Java 21 modular monolith. REST JSON APIs only — no SSR, no Thymeleaf.

**Package layout** under `src/main/kotlin/com/project/manifesto/`:

```
common/          # BaseEntity, ApiResponse<T>, GlobalExceptionHandler, JpaConfig, RestClientConfig
security/        # JWT filter, token provider, SecurityConfig (stateless, role-based)
infra/           # GenerateConfig (placeholder for code generation profile)
modules/
  auth/          # Register, login, moderator (ban/unban), admin (user list, role change, ban/unban)
  user/          # User entity (karma, bannedUntil), UserRole enum, UserDetailsService, UserService, UserController (profile)
  submit/        # Post CRUD (LINK + ASK types)
  vote/          # Post + comment upvote/unvote with DB unique constraint (userId, postId, commentId)
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

- **Post + comment voting**: `VoteService` handles both posts and comments via a single `Vote` entity with an optional `commentId`. Unique constraint on `(userId, postId, commentId)`. `existsByUserIdAndPostIdAndCommentId` check before insert. Ranking is recalculated synchronously after each vote; karma is updated on the content author.
- **Karma**: `User.karma` is incremented/decremented on post and comment votes. Stored as a denormalized column on the User entity for fast reads.
- **Ban/Unban**: Moderators and admins can ban users for a configurable duration (`bannedUntil` timestamp). Admins cannot be banned. Banned users are checked at the security filter level. `AdminService` provides `banUser`/`unbanUser`.
- **Admin protection**: The last admin cannot be demoted to a non-admin role — enforced in `AdminService.changeUserRole`.
- **Admin auto-creation**: Configurable via `application.properties` — on startup, an admin account is created if one doesn't already exist (email/password from config).
- **Event-driven ranking**: Ranking recalculation is triggered by vote events, published after transaction commit to avoid stale score reads.
- **Hacker News ranking**: `hotScore = score / (hours + 2)^1.5`. Recalculated on each vote. Hot posts served from DB queries — no external cache.
- **Soft delete**: Posts and comments use a `deleted` boolean column, never hard-deleted.
- **API wrapper**: All responses use `ApiResponse<T>(code, message, data)`. Routes are under `/api/v1/`.
- **RBAC**: `ROLE_USER`, `ROLE_MODERATOR`, `ROLE_ADMIN`. Moderators can delete any post and ban/unban users; admins can list users, change roles, and ban/unban. `@PreAuthorize` on controller methods. Banned users are blocked at the JWT filter level.
- **Notifications**: Synchronous DB-backed notifications. Created inline (e.g. on comment reply), fetched via paginated endpoint.

## Infrastructure

| Service | Port | Required for |
|---------|------|-------------|
| PostgreSQL | 5432 | Default + E2E profiles |

Only PostgreSQL is needed. The `test` profile uses H2 in-memory and requires nothing external.
