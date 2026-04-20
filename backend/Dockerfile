FROM maven:3.9.6-eclipse-temurin-17 AS build

WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN mvn clean package -DskipTests

FROM openjdk:17-jdk-slim

WORKDIR /app

COPY --from=build /app/target/major-project-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
