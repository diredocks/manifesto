# Docker Infrastructure

## Overview

Single-image deployment: React frontend is built and embedded into the Spring Boot jar, served
as static resources. PostgreSQL 16 and RabbitMQ 4 run as separate containers.

## Directory

```
infra/docker/
├── Dockerfile           # Multi-stage: frontend → backend → runtime
├── docker-compose.yml   # App + PostgreSQL + RabbitMQ
├── .dockerignore        # Excludes node_modules, .gradle, build, etc.
└── entrypoint.sh        # Startup script with dependency wait loops
```

## Prerequisites

- Docker 29+ with Compose plugin
- Project `.env` at repo root (for runtime env vars, not for proxy)

## Dockerfile stages

| Stage | Base image | What it does |
|-------|-----------|--------------|
| `frontend` | `node:22-alpine` | `pnpm install` → `pnpm build` → `dist/` |
| `backend` | `gradle:8-jdk21` | Copies frontend `dist/` into `src/main/resources/static/`, runs `gradle bootJar` |
| runtime | `eclipse-temurin:21-jre` | Copies jar, `java -jar app.jar` |

## docker-compose services

| Service | Image | Host port |
|---------|-------|-----------|
| `app` | Local build | `8080` |
| `postgres` | `postgres:16-alpine` | `5433` |
| `rabbitmq` | `rabbitmq:4-management-alpine` | `5673` (AMQP), `15673` (management UI) |

Ports were chosen to avoid conflicts with local PostgreSQL (5432) and RabbitMQ (5672).

## Environment variables

All runtime configuration comes from `.env` at the repo root, loaded via `env_file` in the
compose file. The compose file overrides these for Docker networking:

| Variable | Value in Docker | App default |
|----------|----------------|-------------|
| `DB_URL` | `jdbc:postgresql://postgres:5432/manifesto` | `localhost:5432` |
| `DB_USERNAME` | `manifesto` | `manifesto` |
| `DB_PASSWORD` | `manifesto` | `manifesto` |
| `RABBITMQ_HOST` | `rabbitmq` | `localhost` |
| `RABBITMQ_PORT` | `5672` | `5672` |

The following are loaded from `.env` with app defaults:

| Variable | Default | Purpose |
|----------|---------|---------|
| `DEEPSEEK_API_KEY` | — | Spring AI DeepSeek API key |
| `ADMIN_USERNAME` | `admin` | Auto-created admin account |
| `ADMIN_PASSWORD` | `admin` | Admin password |
| `ADMIN_EMAIL` | `admin@manifesto.local` | Admin email |
| `ADMIN_AUTO_CREATE` | `true` | Auto-create admin on startup |
| `TAGGING_ENABLED` | `true` | AI tagging feature flag |
| `SERVER_PORT` | `8080` | App HTTP port |

## Commands

### Build

```bash
# From repo root
docker compose -f infra/docker/docker-compose.yml build
```

First build pulls ~2 GB of base images (node, gradle, temurin) and downloads all
dependencies. Subsequent builds are cached — only changed layers rebuild.

If Docker Hub is unreachable, pull base images manually first:

```bash
docker pull node:22-alpine gradle:8-jdk21 eclipse-temurin:21-jre
docker compose -f infra/docker/docker-compose.yml build
```

### Run

```bash
docker compose -f infra/docker/docker-compose.yml up -d
```

### Stop

```bash
docker compose -f infra/docker/docker-compose.yml down
```

Add `--volumes` to delete database data.

### Rebuild after code changes

```bash
docker compose -f infra/docker/docker-compose.yml build
docker compose -f infra/docker/docker-compose.yml up -d --force-recreate app
```

### View logs

```bash
docker compose -f infra/docker/docker-compose.yml logs -f app
```

## Verify

`docker compose ps` should show all three services as `Up` (postgres and rabbitmq as `healthy`).

```bash
# Frontend
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/           # 200
# SPA routing
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/login      # 200
# Static assets
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/assets/*.js # 200
# API
curl -s http://localhost:8080/api/v1/posts?page=0                        # 200
```
