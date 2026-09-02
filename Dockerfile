FROM eclipse-temurin:21-jdk AS builder
WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=builder \
    /app/target/mini-redis-0.1.0-SNAPSHOT.jar \
    /app/mini-redis.jar
ENTRYPOINT ["java", "-jar", "/app/mini-redis.jar"]