# Multi-stage Dockerfile for Classroom Tracker Spring Boot backend
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy maven wrapper and pom.xml
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline -B

# Copy source code and build
COPY src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/target/classroom-tracker-1.0.0.jar app.jar

ENV PORT=8080
ENV ALLOWED_ORIGINS=http://localhost:4200

EXPOSE 8080

ENTRYPOINT ["java", "-Dserver.port=${PORT}", "-Dcors.allowed-origins=${ALLOWED_ORIGINS}", "-jar", "app.jar"]

