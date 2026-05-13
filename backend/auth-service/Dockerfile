FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY auth-service/ auth-service/
RUN mvn -pl auth-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/auth-service/target/auth-service.jar app.jar
EXPOSE 8101
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
