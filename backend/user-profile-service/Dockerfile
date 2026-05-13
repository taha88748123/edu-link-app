FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY shared-libs/ shared-libs/
COPY user-profile-service/ user-profile-service/
RUN mvn -pl user-profile-service -am -B -DskipTests clean package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app
COPY --from=build /workspace/user-profile-service/target/user-profile-service.jar app.jar
EXPOSE 8102
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
