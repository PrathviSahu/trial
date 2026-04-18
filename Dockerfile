FROM eclipse-temurin:17-jdk

WORKDIR /app

# Install curl for health check
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

COPY backend/target/major-project-1.0.0.jar app.jar

EXPOSE 8080

CMD ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
