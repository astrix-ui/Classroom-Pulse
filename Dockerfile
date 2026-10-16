# Multi-stage Dockerfile for Classroom Tracker Spring Boot backend
FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /app

# Copy Maven wrapper and pom.xml from backend
COPY backend/.mvn/ .mvn/
COPY backend/mvnw backend/pom.xml ./
RUN chmod +x ./mvnw && ./mvnw dependency:go-offline -B

# Copy backend source code and build
COPY backend/src ./src
RUN ./mvnw clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# Add non-root user for security
RUN groupadd -r spring && useradd -r -g spring spring
USER spring:spring

COPY --from=build /app/target/classroom-tracker-1.0.0.jar app.jar

ENV PORT=8080
ENV ALLOWED_ORIGINS=http://localhost:4200,https://*.vercel.app

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java -jar app.jar"]

