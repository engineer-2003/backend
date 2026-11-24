# 1. 빌드 단계 (Gradle을 사용하여 JAR 파일 생성)
FROM gradle:8.10-jdk17-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 파일 및 설정 파일 복사
COPY gradlew .
COPY gradle ./gradle
COPY settings.gradle .
COPY build.gradle .

# 필요한 소스 코드를 복사
COPY src src

# 애플리케이션을 빌드하고 JAR 파일을 생성
RUN chmod +x gradlew
RUN ./gradlew clean bootJar --no-daemon

# --------------------------------------------------------------------------

# 2. 실행 단계 (최종 실행 가능한 이미지 생성)
FROM eclipse-temurin:17-jre-alpine

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul
ENV PORT=8080

# JAR 파일 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# 애플리케이션이 실행될 포트 노출
EXPOSE 8080

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]