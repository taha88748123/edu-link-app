FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY notification-service/ notification-service/
RUN mvn -pl notification-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/notification-service/target/notification-service.jar app.jar
EXPOSE 8111
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
