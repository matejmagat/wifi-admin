FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY wifi-admin-service/gradlew ./
COPY wifi-admin-service/gradle ./gradle
COPY wifi-admin-service/build.gradle.kts ./
COPY wifi-admin-service/settings.gradle.kts ./

RUN chmod +x gradlew

# The Gradle wsdl2java task expects this exact absolute path.
COPY wsdl /wsdl

# Separate dependency resolution to improve Docker layer caching.
RUN ./gradlew dependencies --no-daemon

COPY wifi-admin-service/src ./src

RUN ./gradlew bootJar --no-daemon

FROM eclipse-temurin:21-jre

WORKDIR /app

RUN addgroup --system spring \
    && adduser --system --ingroup spring spring \
    && mkdir -p /app/logs \
    && chown -R spring:spring /app

COPY --from=build --chown=spring:spring /workspace/build/libs/*.jar app.jar

USER spring:spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
