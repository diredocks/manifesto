.PHONY: dev backend frontend stop build clean

BACKEND_PORT ?= 8080
FRONTEND_PORT ?= 5173

dev: stop
	@echo "Starting backend on 0.0.0.0:$(BACKEND_PORT) ..." ; \
	echo "Starting frontend on 0.0.0.0:$(FRONTEND_PORT) ..." ; \
	echo "  Swagger:  http://0.0.0.0:$(BACKEND_PORT)/swagger-ui.html" ; \
	echo "" ; \
	(cd backend && ./gradlew bootRun --no-daemon --args='--spring.profiles.active=generate --server.address=0.0.0.0') & \
	(cd frontend && pnpm dev --host 0.0.0.0 --port $(FRONTEND_PORT)) & \
	wait

backend:
	cd backend && ./gradlew bootRun --no-daemon --args='--spring.profiles.active=generate --server.address=0.0.0.0'

frontend:
	cd frontend && pnpm dev --host 0.0.0.0

stop:
	@-fuser -k $(BACKEND_PORT)/tcp 2>/dev/null
	@-fuser -k $(FRONTEND_PORT)/tcp 2>/dev/null
	@echo "Stopped."

build:
	cd backend && ./gradlew bootJar --no-daemon
	cd frontend && pnpm tsc --noEmit && pnpm build
	@echo "Build complete."

clean:
	cd backend && ./gradlew clean --no-daemon
	cd frontend && rm -rf dist
	@echo "Cleaned."
