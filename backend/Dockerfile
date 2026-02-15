# syntax=docker/dockerfile:1.4
# --- Giai đoạn 1: Build ---
# Dùng Maven bản mới nhất chạy trên Amazon Corretto 21 (Hỗ trợ Java 21)
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /app

# Copy file cấu hình maven
COPY pom.xml .

# Tải thư viện với BuildKit cache mount
# Cache được lưu giữa các lần build, giảm thời gian từ ~300s xuống ~10-30s
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn dependency:go-offline -B

# Copy source code
COPY src ./src

# Build package với cache mount (Bỏ qua test)
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean package -DskipTests

# --- Giai đoạn 2: Run ---
# Dùng Amazon Corretto 21 bản Alpine (Siêu nhẹ) để chạy
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy file jar đã build sang
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

# Default Spring profile for Docker
ENV SPRING_PROFILES_ACTIVE=docker

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=5 \
    CMD curl -f http://localhost:8080/actuator/health || exit 1

# Set timezone to match PostgreSQL
ENV TZ=Asia/Ho_Chi_Minh

ENTRYPOINT ["java", "-Duser.timezone=Asia/Ho_Chi_Minh", "-jar", "app.jar"]