FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY ai-agent-service/ ai-agent-service/
RUN mvn -pl ai-agent-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/ai-agent-service/target/ai-agent-service.jar app.jar
EXPOSE 8109
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
