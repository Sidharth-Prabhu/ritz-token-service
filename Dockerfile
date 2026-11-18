# Multi-stage Dockerfile for RITZ Wallet Backend & Unified Service
# Stage 1: Build Java Application
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Cache Maven dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build jar
COPY src ./src
RUN mvn clean package -DskipTests

# Stage 2: Runtime Image
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create unprivileged user for security
RUN addgroup -S ritz && adduser -S ritz -G ritz
USER ritz:ritz

# Copy compiled jar
COPY --from=build /app/target/ritz-token-service-*.jar app.jar

# Expose backend port
EXPOSE 8085

# Default environment configuration
ENV SERVER_PORT=8085 \
    SPRING_PROFILES_ACTIVE=default \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200"

# Healthcheck using wget against Spring Boot Actuator
HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8085/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
