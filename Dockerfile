# --- Build stage ---
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY docs ./docs
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn package -DskipTests -q

# --- Run stage ---
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/target/bookstore-java-0.1.0-SNAPSHOT.jar app.jar
RUN useradd --create-home --shell /usr/sbin/nologin appuser && chown appuser:appuser /app/app.jar
USER appuser
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
