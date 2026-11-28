# Multi-stage build: the first stage compiles the application with the full JDK and Maven,
# the second one only runs the jar on a smaller JRE image.

FROM eclipse-temurin:21-jdk AS build
WORKDIR /workspace

# Dependencies change less often than the code: copying the build files first lets Docker
# reuse the downloaded dependencies layer when only the sources change.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline

COPY src/ src/
# Tests need Docker (Testcontainers), which is not available inside an image build; CI runs them instead.
RUN ./mvnw -B -q package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app

# The application does not need root rights
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /workspace/target/tft-meta-hub-*.jar app.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
