FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY service-discovery/ service-discovery/
RUN mvn -pl service-discovery -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/service-discovery/target/service-discovery.jar app.jar
EXPOSE 8761
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
