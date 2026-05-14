FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY chat-service/ chat-service/
RUN mvn -pl chat-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/chat-service/target/chat-service.jar app.jar
EXPOSE 8107
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
