# Multi-stage build : run from the monorepo's `backend/` context.
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY config-server/ config-server/
RUN mvn -pl config-server -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server.jar app.jar
EXPOSE 8888
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
