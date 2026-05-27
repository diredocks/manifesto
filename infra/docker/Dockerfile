# Stage 1: Frontend build
FROM node:22-alpine AS frontend
ENV CI=true

WORKDIR /app/frontend
RUN corepack enable && corepack prepare pnpm@latest --activate
COPY frontend/package.json frontend/pnpm-lock.yaml frontend/pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile
COPY frontend/ ./
RUN pnpm build

# Stage 2: Backend build
FROM gradle:8-jdk21 AS backend
USER root

WORKDIR /app
COPY backend/ ./
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN gradle bootJar --no-daemon

# Stage 3: Runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=backend /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
