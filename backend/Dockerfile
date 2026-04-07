# Build stage
FROM gradle:8.6-jdk17 AS build
WORKDIR /app
COPY --chown=gradle:gradle . .
RUN gradle build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 보안을 위한 non-root 사용자 생성
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# JAR 파일 복사
COPY --from=build /app/build/libs/*.jar app.jar

# 포트 노출
EXPOSE 8080

# 애플리케이션 실행 (Railway의 헬스체크 사용)
ENTRYPOINT ["java", "-Dspring.profiles.active=prod", "-jar", "app.jar"]
