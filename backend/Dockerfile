FROM gradle:8.14-jdk21 AS build
WORKDIR /workspace
COPY build.gradle settings.gradle ./
COPY backend/src ./src
RUN gradle clean bootJar --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S investment && adduser -S investment -G investment
COPY --from=build /workspace/build/libs/*.jar app.jar
USER investment
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
