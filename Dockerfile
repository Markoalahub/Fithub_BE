# syntax=docker/dockerfile:1

FROM eclipse-temurin:21-jdk-jammy AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

ENV TZ=Asia/Seoul
ENV JAVA_OPTS=""
ENV SERVER_PORT=8080

RUN groupadd -r app && useradd -r -g app app \
    && mkdir -p /app/data \
    && chown -R app:app /app

COPY --from=build /workspace/build/libs/*.jar /app/app.jar

USER app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
