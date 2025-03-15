# Step 1: Use OpenJDK 17 as base image
FROM openjdk:17-jdk-slim as build

# Step 2: Set the working directory inside the container
WORKDIR /app

# Step 3: Copy the build.gradle and settings.gradle files to the container
COPY build.gradle settings.gradle ./

# Step 4: Copy the gradlew and gradle directory (if present) for wrapper
COPY gradlew ./
COPY gradle /app/gradle

# Step 5: Set execute permission for gradlew
RUN chmod +x ./gradlew

# Step 6: Copy the source code (src directory) into the container
COPY src /app/src

# Step 7: Run Gradle build (make sure to include any necessary Gradle wrapper or install Gradle if needed)
RUN ./gradlew build --no-daemon

# Step 8: Use OpenJDK 17 to run the application
FROM openjdk:17-jdk-slim

# Step 9: Set the working directory for the application
WORKDIR /app

# Step 10: Copy the compiled JAR file from the build step into the final image
COPY --from=build /app/build/libs/*.jar app.jar

# Step 11: Expose the application port (default Spring Boot port is 8080)
EXPOSE 8080

# Step 12: Run the Spring Boot application
ENTRYPOINT ["java", "-jar", "app.jar"]
