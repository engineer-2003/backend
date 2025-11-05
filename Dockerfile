# 1. 빌드 단계 (Gradle을 사용하여 JAR 파일 생성)
# Gradle 빌드를 위해 공식 OpenJDK 17을 사용
FROM gradle:8.10-jdk17-alpine AS builder

# 작업 디렉토리 설정
WORKDIR /app

# Gradle Wrapper 파일과 설정 파일을 복사
# 의존성이나 빌드 스크립트가 변경되지 않을 경우 캐시를 활용하여 빌드 속도를 높임
COPY gradlew .
COPY gradle gradle
COPY settings.gradle .
COPY build.gradle .

# 필요한 소스 코드를 복사
COPY src src

# 애플리케이션을 빌드하고 JAR 파일을 생성
RUN chmod +x gradlew
# clean 빌드 후 JAR 파일을 생성
RUN ./gradlew clean bootJar --no-daemon

# --------------------------------------------------------------------------

# 2. 실행 단계 (최종 실행 가능한 이미지 생성)
# 더 작고 안전한 실행 환경을 위해 경량화된 JRE 이미지를 사용
FROM openjdk:17-jre-slim-buster

# 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=prod
ENV TZ=Asia/Seoul

# JAR 파일 복사
# 빌드 단계에서 생성된 JAR 파일의 경로를 정확히 지정
# 프로젝트 구조상 'backend/build/libs/japanese-haru-0.0.1-SNAPSHOT.jar'에 생성될 것으로 가정
# (build.gradle에 설정된 'group'과 'version'을 기반으로 파일명을 확인해야 함)
COPY --from=builder /app/build/libs/japanese-haru-0.0.1-SNAPSHOT.jar app.jar

# 애플리케이션이 실행될 포트 노출
EXPOSE 8080

# 애플리케이션 실행 명령어
ENTRYPOINT ["java", "-jar", "app.jar"]