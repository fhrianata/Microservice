# syntax=docker/dockerfile:1

# ---- Stage 1: Build jar dari source pakai Maven ----
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
# Download dependency dulu (di-cache terpisah dari source supaya build ulang lebih cepat)
RUN mvn -B -q -DskipTests dependency:go-offline || true
COPY src ./src
RUN mvn -B -DskipTests clean package \
    && cp target/*.jar app.jar

# ---- Stage 2: Runtime image, jauh lebih kecil dari image Maven ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

# curl dipakai untuk HEALTHCHECK actuator; user non-root untuk keamanan
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/* \
    && useradd --create-home --shell /usr/sbin/nologin spring

COPY --from=build /build/app.jar /app/app.jar
RUN chown -R spring:spring /app
USER spring

EXPOSE 8083

HEALTHCHECK --interval=15s --timeout=5s --start-period=45s --retries=10 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENV JAVA_OPTS=""
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
