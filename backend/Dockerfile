# Stage 1: Build the application using Maven
FROM maven:3.8.8-eclipse-temurin-8 AS build
WORKDIR /app

# Copy the pom.xml and source code
COPY pom.xml .
COPY src ./src

# Build the executable JAR (skipping tests for faster deployment)
RUN mvn clean package -DskipTests

# Stage 2: Run the application
FROM eclipse-temurin:8-jre-alpine
WORKDIR /app

# Copy the built jar file from the build stage
COPY --from=build /app/target/secure-student-analyzer.jar app.jar

# Expose port (must match application.properties and Render settings)
EXPOSE 8080

# Run the jar file
ENTRYPOINT ["java", "-jar", "app.jar"]
