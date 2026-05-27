# Docker

Development environment with the Manifesto app + PostgreSQL + RabbitMQ.

## Prerequisites

- Docker Engine 24+
- DeepSeek API key

## Quick start

```bash
# 1. Create compose config from the example
cp docker-compose.example.yml docker-compose.yml

# 2. Edit docker-compose.yml with your DEEPSEEK_API_KEY
#    Other variables (admin, JWT, etc.) use defaults — customize as needed

# 3. Start
docker compose up -d

# 4. Verify
curl http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'
```

## Services

| Service | Container | Port (internal) |
|---------|-----------|-----------------|
| app | manifesto-app | 8080 |
| postgres (16) | manifesto-postgres | 5432 |
| rabbitmq (4) | manifesto-rabbitmq | 5672 |

Postgres and RabbitMQ ports are not exposed to the host — they communicate only over the Docker internal network.

## Environment variables

Required (`DEEPSEEK_API_KEY`) and optional variables are noted in `docker-compose.example.yml`. For the full list, see `backend/src/main/resources/application.yml`.

## Data persistence

Data is stored in Docker volumes:

- `postgres_data` — database
- `rabbitmq_data` — message broker

```bash
# Wipe all data
docker compose down -v
```
