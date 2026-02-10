# ---------- Build stage ----------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /app

# Copy pom first to leverage Docker layer caching
COPY pom.xml .
RUN mvn -q -e -DskipTests dependency:go-offline

# Copy sources
COPY src ./src

# Build
RUN mvn -q -DskipTests package

# ---------- Runtime stage ----------
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy the built jar(s)
COPY --from=build /app/target/*.jar /app/app.jar

# App port
EXPOSE 8080

# Optional: pass config file path via ENV (you can override in docker run)
ENV VERTX_CONF=/app/config/application.json

# If you want to ship a default config in the image, copy it (optional)
# If you don't have it, you can delete the next line.
COPY src/main/resources/application.json /app/config/application.json

# Run Vert.x Launcher
ENTRYPOINT ["java", "-jar", "/app/app.jar", "run", "com.asml.api.MainVerticle", "-conf", "/app/config/application.json"]
