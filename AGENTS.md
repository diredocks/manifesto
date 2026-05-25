# AGENTS.md

# Manifesto — AI Development Agent Guide

## 1. Project Overview

Build a Hacker News–style content aggregation and discussion platform.

Core features:

- Users submit links or text posts
- Community voting determines ranking
- Nested discussion system
- Personalized notifications
- AI-assisted summarization and tagging

Tech Stack:

### Backend
- Kotlin
- Spring Boot 3.x
- Spring Security + JWT
- Spring Data JPA / Exposed (prefer JPA for speed)
- PostgreSQL
- Redis
- RabbitMQ
- OpenFeign
- Spring AI / OpenAI compatible API

Architecture Style:

Strict frontend/backend separation.

No SSR.

No Thymeleaf/JSP.

RESTful JSON APIs only.

---

# 2. Development Goals

Priority order:

P0 (must finish)
1. Authentication
2. Submit post
3. Vote system
4. Ranking feed
5. Nested comments
6. Notifications

P1 (important)
7. AI summary
8. Auto tags
9. Moderator tools

P2 (bonus)
10. Milestone notifications
11. Search
12. Analytics dashboard

---

# 3. Scoring Alignment

This project should maximize coursework score.

Required alignment:

## Base score
- Strict layered architecture
- RESTful APIs
- >= 6 APIs
- PostgreSQL persistence
- >= 4 business tables
- Git history

## Bonus score
Must include:

### Redis
Used for:
- Top 100 feed cache
- distributed lock for voting
- hot score cache

### MQ (RabbitMQ)
Used for async:
- ranking recalculation
- notifications
- AI summary generation

### AI integration
Use:
- Spring AI

Features:
- URL summarization
- tag generation

### RBAC
Roles:
- USER
- MODERATOR
- ADMIN

---

# 4. Architecture

Use modular monolith architecture.

DO NOT split into microservices.

Structure:

src/main/kotlin/com/project/manifesto

```txt
common/
config/
security/
infra/

modules/
├── auth/
├── user/
├── submit/
├── vote/
├── ranking/
├── comment/
├── notification/
└── ai/
````

Each module:

```txt
controller/
service/
repository/
entity/
dto/
event/
```

Rules:

* Controller only handles request/response
* Service handles business logic
* Repository only data access
* DTO never directly expose Entity
* No business logic in controller

---

# 5. Domain Model

## User

Fields:

```txt
id
username
email
passwordHash
karma
role
createdAt
```

## Post

Types:

```txt
LINK
ASK
```

Fields:

```txt
id
authorId
title
url
content
summary
score
hotScore
commentCount
createdAt
deleted
```

## Vote

Unique constraint:

```txt
(user_id, post_id)
```

Fields:

```txt
id
userId
postId
createdAt
```

## Comment

Recursive structure:

```txt
id
postId
authorId
parentId
content
score
depth
createdAt
deleted
```

## Notification

Fields:

```txt
id
receiverId
type
content
isRead
createdAt
```

## Tag

```txt
id
name
```

## PostTag

Many-to-many:

```txt
post_id
tag_id
```

---

# 6. Database Rules

Naming:

snake_case only.

All tables must contain:

```txt
created_at
updated_at
```

Indexes:

```sql
post(hot_score DESC)

comment(post_id)

vote(user_id, post_id UNIQUE)

notification(receiver_id, is_read)
```

---

# 7. Authentication

Use JWT.

Flow:

```txt
register
→ login
→ issue access token
→ authenticated requests
```

Spring Security requirements:

* stateless
* JWT filter
* role based access

Roles:

```txt
ROLE_USER
ROLE_MODERATOR
ROLE_ADMIN
```

Forbidden:

* Session auth
* Cookie auth

---

# 8. Voting System (Critical)

Voting is the core transaction flow.

Requirements:

### API

```txt
POST /api/posts/{id}/upvote
DELETE /api/posts/{id}/upvote
```

### Concurrency

Use Redis distributed lock.

Pattern:

```txt
vote:post:{postId}:user:{userId}
```

Database fallback:

unique index.

Must guarantee:

No duplicate vote.

### Async event

After vote success:

publish event:

```txt
PostVotedEvent
```

Consumer recalculates:

```txt
score
hot_score
karma
```

---

# 9. Ranking Algorithm

Formula:

Use Hacker News style decay:

```txt
score / (hours + 2)^1.5
```

Store:

```txt
hot_score
```

Ranking:

```txt
/hot
/new
/top
```

Redis cache:

Top 100 posts:

Key:

```txt
feed:hot:top100
```

TTL:

```txt
5 minutes
```

---

# 10. Comment System

Infinite nested replies.

Model:

Adjacency List:

```txt
parent_id
```

API response:

Return tree structure.

Rules:

* max depth not enforced
* deleted comments remain placeholder
* recursive query optimized

Notification trigger:

reply → send event

---

# 11. Notification System

Types:

```txt
COMMENT_REPLY
POST_MILESTONE
SYSTEM
```

Async only.

Use MQ.

API:

```txt
GET /notifications

PATCH /notifications/{id}/read
```

---

# 12. AI Assistant

When submitting URL:

Pipeline:

```txt
fetch html
→ parse article
→ extract content
→ summarize
→ generate tags
→ save db
```

Queue this process.

Never block submit API.

Fallback:

AI failure should not break submit.

Prompt strategy:

Summary:
"Summarize this article in 100 words."

Tags:
"Generate 3 concise tags."

---

# 13. API Rules

Response wrapper:

```json
{
  "code": 200,
  "message": "success",
  "data": {}
}
```

Error format:

```json
{
  "code": 400,
  "message": "validation failed"
}
```

Use:

```txt
/api/v1/*
```

RESTful only.

---

# 14. Coding Rules

Kotlin guidelines:

Prefer:

```kotlin
data class
sealed class
```

Avoid:

```txt
!! operator
```

Use constructor injection only.

No field injection.

Must:

* validation annotations
* global exception handler
* OpenAPI/Swagger

---

# 15. Git Workflow

Branches:

```txt
main
develop
feature/*
```

Commit style:

```txt
feat:
fix:
refactor:
docs:
test:
```

Minimum:

Daily commits.

Meaningful history required.

---

# 16. Non-Negotiable Rules

DO NOT:

* write fat controllers
* expose entities directly
* synchronous AI processing
* use polling for notifications
* skip migration
* skip indexes

ALWAYS:

* async heavy work
* cache hot feed
* secure APIs
* write clean DTOs
* keep frontend/backend separated

# 17. Environment & Network Resilience

When network access fails or dependency installation is required, the agent is allowed to use environment variables from `.env` as fallback configuration.

This is strictly for local/dev environment support.

Allowed variables:

- ROOT_PASSWORD (for privileged local operations only)
- HTTP_PROXY
- HTTPS_PROXY
- NO_PROXY

Behavior rules:

- Never hardcode credentials in source code
- Never log sensitive values
- Proxy settings must be applied before any HTTP client initialization
- Dependency installation commands (e.g., Gradle/Maven) should respect proxy env vars automatically when present

When network conditions are slow, unstable, or external repositories are blocked, the agent must prefer Chinese mirror sources for dependency resolution.

## Gradle

Preferred mirrors:

- Aliyun Maven mirror
- Tencent Cloud mirror

# 18. Development Loop

The agent must follow a strict development loop for every feature.

## Required loop after completing ANY task

1. Implement feature
2. Run local build
3. Run tests
4. Fix failures until green
5. Verify API manually or via integration test
6. Stage changes (git add)
7. Commit with proper message
8. Proceed to next task only if current task is stable

## No skipping allowed

- Never commit broken code
- Never mark feature complete without test pass
- Never proceed with failing build/test
