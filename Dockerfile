# Build the application with Maven, then copy only the runnable JAR into a small runtime image.
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
ARG APP_VERSION=0.0.1
RUN mvn -B -DskipTests -Drevision=${APP_VERSION} package \
    && cp target/*.jar target/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
COPY --from=build --chown=app:app /build/target/app.jar ./app.jar
USER app

ARG SERVER_PORT=5200
ENV SERVER_PORT=${SERVER_PORT}
EXPOSE ${SERVER_PORT}

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
