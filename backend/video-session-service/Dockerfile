FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY video-session-service/ video-session-service/
RUN mvn -pl video-session-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/video-session-service/target/video-session-service.jar app.jar
EXPOSE 8108
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
