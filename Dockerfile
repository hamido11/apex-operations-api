# syntax=docker/dockerfile:1

############################
# Build stage
############################
# Pinned by multi-arch (index) digest for reproducible, portable builds.
FROM maven:3.9-eclipse-temurin-21@sha256:99e61abcff91a9b1333463bd8451fb18495d6eba9250ac66a338b518f8278320 AS build
WORKDIR /build

# Resolve dependencies first so this layer caches unless pom.xml changes.
COPY pom.xml ./
RUN --mount=type=cache,target=/root/.m2 mvn -B -q dependency:go-offline

# Build the application jar. Tests run in CI (not during the image build).
COPY src/ ./src/
RUN --mount=type=cache,target=/root/.m2 mvn -B -q clean package -DskipTests

# Extract the Spring Boot layered jar for efficient runtime image layering.
RUN mkdir -p extracted && \
    java -Djarmode=tools -jar target/apex-operations-api-*.jar \
        extract --layers --launcher --destination extracted

############################
# Runtime stage
############################
# Minimal JRE base, pinned by multi-arch (index) digest. Alpine's busybox provides
# wget for the container HEALTHCHECK without adding extra packages.
FROM eclipse-temurin:21-jre-alpine@sha256:82cf3d9ccbe1ed46252008d51b2f2329e6b9e8a2f4394c4bebefc3ea7f4b1372 AS runtime

# Dedicated, unprivileged user. Numeric UID is referenced in USER for clarity.
RUN addgroup -S apex && adduser -S -G apex -u 10001 apex

WORKDIR /app

# Copy extracted layers in increasing order of change frequency for better caching.
COPY --from=build --chown=apex:apex /build/extracted/dependencies/ ./
COPY --from=build --chown=apex:apex /build/extracted/spring-boot-loader/ ./
COPY --from=build --chown=apex:apex /build/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=apex:apex /build/extracted/application/ ./

# Run as non-root.
USER 10001:10001

EXPOSE 8080

# Let the JVM size its heap from the container's memory limit.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -XX:InitialRAMPercentage=50.0"

# Container health check against the readiness probe (busybox wget fails on non-2xx).
HEALTHCHECK --interval=15s --timeout=3s --start-period=40s --retries=5 \
  CMD wget -q -O /dev/null http://localhost:8080/actuator/health/readiness || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
