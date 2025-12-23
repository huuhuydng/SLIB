# --- Giai đoạn 1: Build ---
# Dùng Maven bản mới nhất chạy trên Amazon Corretto 21 (Hỗ trợ Java 21)
FROM maven:3.9.9-amazoncorretto-21 AS build
WORKDIR /app

# Copy file cấu hình maven
COPY pom.xml .
# Tải thư viện
RUN mvn dependency:go-offline -B

# Copy source code
COPY src ./src
# Build package (Bỏ qua test)
RUN mvn clean package -DskipTests

# --- Giai đoạn 2: Run ---
# Dùng Amazon Corretto 21 bản Alpine (Siêu nhẹ) để chạy
FROM amazoncorretto:21-alpine-jdk
WORKDIR /app

# Copy file jar đã build sang
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]