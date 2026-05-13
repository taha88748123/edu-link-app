FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY payment-service/ payment-service/
RUN mvn -pl payment-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/payment-service/target/payment-service.jar app.jar
EXPOSE 8106
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
