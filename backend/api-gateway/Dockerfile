FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY api-gateway/ api-gateway/
RUN mvn -pl api-gateway -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
