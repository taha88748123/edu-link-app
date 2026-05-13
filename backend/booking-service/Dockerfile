FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY booking-service/ booking-service/
RUN mvn -pl booking-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/booking-service/target/booking-service.jar app.jar
EXPOSE 8105
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
