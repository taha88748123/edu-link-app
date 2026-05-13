FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY pricing-service/ pricing-service/
RUN mvn -pl pricing-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/pricing-service/target/pricing-service.jar app.jar
EXPOSE 8104
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
