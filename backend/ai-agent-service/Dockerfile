FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
# Copy the full backend reactor — the parent POM enumerates all 13 modules,
# so Maven needs every module directory present even when we only build one
# of them. `.dockerignore` keeps the context lean by excluding target/.
COPY . .
RUN mvn -pl ai-agent-service -am -B -DskipTests clean package
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/ai-agent-service/target/ai-agent-service.jar app.jar
EXPOSE 8109
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
