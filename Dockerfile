FROM eclipse-temurin:21-jre-jammy

ARG APP_VERSION=dev
ARG JAR_FILE=target/*.jar
ARG JVM_ARGS=""

# Image Metadata
LABEL maintainer="LuxFerre86"
LABEL description="Chroniqo Application Server"
LABEL version="${APP_VERSION}"

# Environment variables
ENV APP_VERSION=${APP_VERSION} \
    JVM_ARGS=${JVM_ARGS} \
    JAVA_OPTS="-XX:+UseG1GC -XX:MaxRAMPercentage=85.0 -XX:MinRAMPercentage=50.0 -XX:+UnlockExperimentalVMOptions -XX:G1NewCollectionHeuristicPercent=20 -XX:G1ReservePercent=5 -XX:InitiatingHeapOccupancyPercent=35"

# Install curl for healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    && rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -g 999 appuser \
 && useradd -r -u 999 -g appuser appuser \
 && mkdir -p /app/logs \
 && chown -R appuser:appuser /app

WORKDIR /app
COPY --chown=appuser:appuser ${JAR_FILE} /app/app.jar

USER appuser
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
    CMD curl --fail --silent http://localhost:8080/actuator/health/readiness || exit 1

# Use exec form to properly handle signals
ENTRYPOINT ["java"]
CMD ["-jar", "/app/app.jar"]
