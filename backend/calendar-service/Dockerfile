FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY calendar-service/ calendar-service/
RUN mvn -pl calendar-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/calendar-service/target/calendar-service.jar app.jar
EXPOSE 8103
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
