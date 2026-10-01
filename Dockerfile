# =============================================================================
# Backend Dockerfile — Java Spring Boot 3 Microservice
# =============================================================================

# Stage 1: Build the JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app

# Copy Maven wrapper and POM first (layer cache for dependencies)
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

# Copy source and build
COPY src/ ./src/
RUN ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 2: Minimal runtime image
# =============================================================================
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for ECR/EC2 security best practices
RUN addgroup -S appgroup && adduser -S appuser -G appgroup
USER appuser

COPY --from=builder /app/target/s3-microservice-0.0.1-SNAPSHOT.jar app.jar

# Inside Docker there is no Jenkins conflict — always use 8080
EXPOSE 8080

ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENV SERVER_PORT=8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=20s \
  CMD wget -qO- http://localhost:8080/api/s3/status || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Dserver.port=${SERVER_PORT} -jar app.jar"]
