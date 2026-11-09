# WebP 네이티브 라이브러리 호환을 위해 런타임은 glibc 기반 이미지를 사용한다.
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

# test는 CI에서 통과했으므로 제외
RUN --mount=type=cache,target=/root/.gradle \
    --mount=type=secret,id=sentry_auth_token,env=SENTRY_AUTH_TOKEN \
    chmod +x ./gradlew && ./gradlew bootJar -x test --no-daemon --build-cache

WORKDIR /workspace/build/libs
RUN java -Djarmode=tools -jar $(ls *.jar | grep -v plain) extract --layers --launcher --destination /workspace/extracted

FROM eclipse-temurin:25-jre-noble AS runner
WORKDIR /app

RUN groupadd --system --gid 10001 spring && useradd --system --uid 10001 --gid spring --no-create-home spring

COPY --from=builder --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/application/ ./

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
