FROM gradle:8.14.3-jdk21-alpine AS builder

WORKDIR /workspace

COPY build.gradle settings.gradle gradlew gradlew.bat ./
COPY gradle ./gradle
COPY src ./src

RUN chmod +x ./gradlew && ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre-jammy

ENV APP_HOME=/app
ENV STORAGE_TEMP_DIR=/tmp/voxly-temp
ENV FFMPEG_PATH=ffmpeg

WORKDIR ${APP_HOME}

RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg curl \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /workspace/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
