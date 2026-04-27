# ============================================================
#  Dockerfile — VaultCourier Backend
#  Two-stage build:
#    Stage 1 (builder) — downloads dependencies, compiles code
#    Stage 2 (runtime) — runs the finished app, small image
# ============================================================

# ── STAGE 1: BUILDER ────────────────────────────────────────
# Uses a full JDK + Maven image to compile and package the app
FROM eclipse-temurin:17-jdk-alpine AS builder

# Install Maven (Alpine uses apk not apt)
RUN apk add --no-cache maven

WORKDIR /app

# Copy pom.xml first separately.
# Docker caches this layer — so if your code changes but
# pom.xml doesn't, Maven won't re-download everything.
COPY pom.xml .
COPY mariadb-java-client-3.1.2.jar .

# Download all dependencies declared in pom.xml
# -q = quiet mode, cleaner logs
RUN mvn dependency:resolve -q

# Now copy the rest of your source code
COPY src/ src/

# Build the final runnable JAR file
# -DskipTests = don't run tests during build (faster)
RUN mvn package -DskipTests -q


# ── STAGE 2: RUNTIME ────────────────────────────────────────
# Uses a smaller JRE-only image — no compiler needed to run
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Copy only the finished JAR from the builder stage
# The * wildcard handles the version number in the filename
COPY --from=builder /app/target/vaultcourier-*.jar app.jar

# Tell Docker this container listens on port 5000
EXPOSE 5000

# Run the app when the container starts
ENTRYPOINT ["java", "-jar", "app.jar"]
