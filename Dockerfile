# Define your base image
FROM debian:bookworm-slim
ENV JAVA_HOME=/opt/java/openjdk
ENV PATH "${JAVA_HOME}/bin:${PATH}"
COPY --from=eclipse-temurin:21-jre /opt/java/openjdk /opt/java/openjdk

# Continue with your application deployment
RUN groupadd -g 999 appuser && \
    useradd -r -u 999 -g appuser appuser && \
    mkdir -p /app/logs && \
    chown appuser:appuser /app/logs
USER appuser

ARG JAR_FILE=target/*.jar
COPY ${JAR_FILE} /app/app.jar

ARG JVM_ARGS

WORKDIR "/app"
ENTRYPOINT ["sh","-c","java $JVM_ARGS -jar app.jar"]