# =========================================================================
# VitalDrop backend - Vercel container deployment (backend/Dockerfile.vercel)
# Production-safe multi-stage build for the existing Maven Spring Boot app.
# - Builds the existing Spring Boot application (no rewrite, no Node.js)
# - Runs the generated Spring Boot JAR
# - Listens on Vercel's $PORT (server.port=${PORT:8080} in application.properties)
# =========================================================================

# ---- Stage 1: build -------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy Maven metadata first for better layer caching, then sources.
COPY pom.xml ./
COPY .mvn/ .mvn/
COPY mvnw ./
RUN chmod +x mvnw
RUN ./mvnw -q -DskipTests dependency:go-offline

COPY src/ src/
RUN ./mvnw -q -DskipTests clean package

# ---- Stage 2: runtime -----------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Minimal runtime deps (MySQL TLS). No Node.js, no extra frameworks.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Writable log directory for logging.file.name=logs/lifelink.log
RUN mkdir -p logs

COPY --from=build /app/target/lifelink-*.jar app.jar

EXPOSE 8080

# Use $PORT when provided by the platform, else 8080 (matches local dev).
# server.port=${PORT:8080} in application.properties does the actual binding;
# this keeps the JVM entrypoint explicit and platform-safe.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar --server.port=${PORT:-8080}"]
