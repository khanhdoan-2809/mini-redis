# MiniRedis

MiniRedis is a Redis-compatible server built from scratch in Java.

The purpose of this project is to study:

- TCP networking
- RESP
- event-driven architecture
- Redis data structures
- expiration
- persistence
- memory eviction
- transactions
- Pub/Sub
- replication
- clustering
- distributed-system correctness
- benchmarking and profiling

## Technology

- Java 21
- Maven
- Java NIO
- JUnit 5
- AssertJ
- SLF4J
- Logback
- Docker
- GitHub Actions

## Principles

MiniRedis intentionally avoids frameworks that hide the systems concepts
being studied.

Therefore the core server does not use:

- Spring Boot
- Spring WebFlux
- Netty
- Spring Data Redis
- Hibernate
- external databases

## Build

```bash
./mvnw clean verify