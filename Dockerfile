# Multi-stage Dockerfile for Proxmox VM or Container (LXC / Docker)
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S ritz && adduser -S ritz -G ritz
USER ritz:ritz

COPY --from=build /app/target/ritz-token-service-*.jar app.jar

EXPOSE 8085

ENV SERVER_PORT=8085 \
    DB_URL=jdbc:postgresql://postgres-db:5432/ritz_token_db \
    DB_USERNAME=postgres \
    DB_PASSWORD=secret \
    RITZ_API_KEY=ritz-secret-api-key-2026 \
    JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
