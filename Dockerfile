# ---- 빌드 단계: JDK 이미지에서 Gradle 빌드 ----
FROM eclipse-temurin:21-jdk-noble AS build
WORKDIR /workspace

# 의존성 관련 파일을 먼저 복사해 소스만 바뀔 때 레이어 캐시 재사용
COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon > /dev/null

COPY src ./src
# 테스트는 CI 별도 단계 몫. 이미지 빌드는 실행 jar만 생성
RUN ./gradlew bootJar --no-daemon

# ---- 실행 단계: JRE 이미지 + root가 아닌 사용자 ----
FROM eclipse-temurin:21-jre-noble
WORKDIR /app

# K8s runAsNonRoot 검증이 가능하도록 숫자 UID 사용
RUN groupadd --system --gid 10001 app \
    && useradd --system --uid 10001 --gid app --no-create-home app

COPY --from=build --chown=app:app /workspace/build/libs/*.jar app.jar

USER 10001:10001
EXPOSE 8080

# 컨테이너 메모리 limit 기준으로 힙 크기 결정
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/app.jar"]
