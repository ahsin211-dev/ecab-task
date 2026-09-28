# syntax=docker/dockerfile:1

# ---- build: compile the jar and split it into layers ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Dependencies first, so code-only changes reuse this cached layer.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
# Tests run in CI (`mvn test`); the image build only packages.
RUN mvn -B -q -DskipTests package \
 && cp target/ride-matching-service-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --destination extracted

# ---- runtime: JRE only, non-root ----
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S app -G app
USER app

# Least-often-changed layers first so redeploys only ship the application layer.
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./

# Size the heap from the container memory limit instead of the host's RAM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080

# Exec form so the JVM is PID 1 and receives SIGTERM for a graceful shutdown.
ENTRYPOINT ["java", "-jar", "app.jar"]
