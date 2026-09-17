# 자원 절약이 최우선으로, 외부 의존성 등 사용으로 문제가 발생할 요소나 대규모 트래픽 환경이 아니라서 alpine 사용으로 인한 문제가 없다.
FROM eclipse-temurin:25-jdk-alpine AS builder
WORKDIR /workspace

COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./

RUN chmod +x ./gradlew && ./gradlew dependencies --no-daemon

# test는 CI에서 통과했으므로 제외
COPY src src
RUN ./gradlew bootJar -x test --no-daemon

WORKDIR /workspace/build/libs
RUN java -Djarmode=tools -jar *.jar extract --layers --launcher --destination /workspace/extracted

FROM eclipse-temurin:25-jre-alpine AS runner
WORKDIR /app

RUN addgroup -g 10001 -S spring && adduser -u 10001 -S spring -G spring

COPY --from=builder --chown=spring:spring /workspace/extracted/dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/spring-boot-loader/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/snapshot-dependencies/ ./
COPY --from=builder --chown=spring:spring /workspace/extracted/application/ ./

USER 10001:10001

EXPOSE 8080

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]