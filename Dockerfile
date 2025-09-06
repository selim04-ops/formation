# Stage 1: Build
#FROM maven:3.9.6-eclipse-temurin-17 AS builder
#WORKDIR /app
#COPY . .
#RUN mvn clean package -DskipTests

# Stage 2: Run
#FROM eclipse-temurin:17-jdk
#WORKDIR /app
#COPY --from=builder /app/target/gfromation-0.0.1-SNAPSHOT.jar app.jar
#ENTRYPOINT ["java", "-jar", "app.jar"]
# Use official OpenJDK 17 image
#FROM openjdk:17-jdk-slim

# Set working directory inside container
#WORKDIR /app

# Copy the built JAR file into the container
# Rename it to 'app.jar' for simplicity
#COPY target/*.jar app.jar

# Expose the port your app runs on (change 8080 if needed)
#EXPOSE 8888

# Command to run your application
#ENTRYPOINT ["java", "-jar", "app.jar"]
# Use official OpenJDK 17 base image
FROM openjdk:17-jdk-slim

# Install wget to download the JAR
RUN apt-get update && apt-get install -y wget && apt-get clean

# Set working directory
WORKDIR /app

# Download the JAR from Nexus
# Replace credentials with ARGs if needed
RUN wget --user=admin --password=salim \
    http://192.168.1.100:8081/repository/maven-releases/com/esprit/gfromation/0.0.1/gfromation-0.0.1.jar  \
    -O app.jar

# Expose the port used by the Spring Boot application
EXPOSE 8888

# Run the JAR
ENTRYPOINT ["java", "-jar", "app.jar"]

