# =============================
# Stage 1: Build
# =============================
FROM gradle:8.14.5-jdk21 AS build

WORKDIR /app

# Copy gradle wrapper and build files first (for caching)
COPY build.gradle settings.gradle gradle/ gradle/wrapper/ ./
COPY gradlew .
RUN chmod +x gradlew

# Copy source code
COPY src ./src

# Build the application
RUN ./gradlew clean bootJar --no-daemon

# =============================
# Stage 2: Runtime
# =============================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="medisalud-dev"
LABEL description="Medisalud Appointments - Sistema de Agendamiento de Citas Médicas"

WORKDIR /app

# Create non-root user
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copy the built JAR from the build stage
COPY --from=build /app/build/libs/*.jar app.jar

# Change ownership to non-root user
RUN chown -R appuser:appgroup /app

# Switch to non-root user
USER appuser

# Expose port
EXPOSE 8080

# Health check (Alpine uses curl, not wget)
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
  CMD curl -f http://localhost:8080/actuator/health || exit 1

# Run the application
ENTRYPOINT ["java", "-jar", "app.jar"]
