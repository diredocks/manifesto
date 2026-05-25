.PHONY: init clean-db clean-redis clean-mq clean-all run-backend

PG_HOST ?= localhost
PG_PORT ?= 5432
PG_USER ?= manifesto
PG_PASS ?= manifesto
PG_DB   ?= manifesto
PG_E2E  ?= manifesto_e2e
PGPASSWORD=$(PG_PASS)
export PGPASSWORD

REDIS_HOST ?= localhost
REDIS_PORT ?= 6379

init:
	@echo "Initializing databases..."
	@for db in $(PG_DB) $(PG_E2E); do \
		psql -h $(PG_HOST) -p $(PG_PORT) -U $(PG_USER) -d postgres -c "CREATE DATABASE $$db OWNER $(PG_USER);" 2>/dev/null || echo "  $$db already exists, skipping."; \
	done
	@echo "Initialization complete. Start the backend to create/update schema."

clean-db:
	@echo "Dropping and recreating databases..."
	@for db in $(PG_DB) $(PG_E2E); do \
		psql -h $(PG_HOST) -p $(PG_PORT) -U $(PG_USER) -d postgres -c "DROP DATABASE IF EXISTS $$db;" 2>/dev/null || true; \
		psql -h $(PG_HOST) -p $(PG_PORT) -U $(PG_USER) -d postgres -c "CREATE DATABASE $$db OWNER $(PG_USER);" 2>/dev/null || true; \
	done
	@echo "Databases recreated."

clean-redis:
	@echo "Flushing Redis..."
	redis-cli -h $(REDIS_HOST) -p $(REDIS_PORT) FLUSHALL

clean-mq:
	@echo "Resetting RabbitMQ..."
	sudo rabbitmqctl stop_app
	sudo rabbitmqctl reset
	sudo rabbitmqctl start_app
	@echo "RabbitMQ reset."

clean-all: clean-db clean-redis clean-mq
	@echo "All services cleaned."

run-backend:
	@set -a && . ./.env && set +a && cd backend && ./gradlew bootRun --no-daemon

run-frontend:
	cd frontend && pnpm dev --host
