# Build the existing React interface with a same-origin API address.
FROM node:22-bookworm-slim AS frontend
WORKDIR /build/frontend
COPY inventory-frontend/package.json inventory-frontend/package-lock.json ./
RUN npm ci
COPY inventory-frontend/ ./
RUN npm test && VITE_API_URL=/api npm run build

# Serve the compiled frontend directly from the tested Spring Boot application.
FROM maven:3.9-eclipse-temurin-17 AS backend
WORKDIR /build/backend
COPY Inventory/pom.xml ./
COPY Inventory/src/ ./src/
COPY --from=frontend /build/frontend/dist/ ./src/main/resources/static/
RUN mvn -B -ntp verify

# Only the JRE and packaged application are included in the runtime image.
FROM eclipse-temurin:17-jre-jammy AS runtime
WORKDIR /app
RUN groupadd --system inventory && useradd --system --gid inventory inventory
COPY --from=backend --chown=inventory:inventory /build/backend/target/Inventory-0.0.1-SNAPSHOT.jar /app/app.jar
COPY --chmod=755 docker-entrypoint.sh /app/docker-entrypoint.sh
USER inventory
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=50 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8082
ENTRYPOINT ["/app/docker-entrypoint.sh"]
