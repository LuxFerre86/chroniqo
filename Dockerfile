FROM eclipse-temurin:21-jre-jammy
ARG APP_VERSION=dev
ARG JAR_FILE=target/*.jar
ARG JVM_ARGS=""
ENV APP_VERSION=${APP_VERSION}

RUN groupadd -g 999 appuser \
 && useradd -r -u 999 -g appuser appuser \
 && mkdir -p /app/logs \
 && chown -R appuser:appuser /app

WORKDIR /app
COPY --chown=appuser:appuser ${JAR_FILE} /app/app.jar

USER appuser
EXPOSE 8080

ENTRYPOINT ["sh","-c","exec java $JVM_ARGS -jar /app/app.jar"]