# Развёртывание

## Docker image

Образ собирается многостадийным Dockerfile:

| Stage | Назначение |
|---|---|
| `eclipse-temurin:21-jdk` | сборка `bootJar` |
| `eclipse-temurin:21-jre` | runtime |

## Runtime profile

Production запуск использует profile без `dev`:

```env
PODCAST_SPRING_PROFILES_ACTIVE=docker
PODCAST_DEV_SEED_ENABLED=false
PODCAST_SWAGGER_ENABLED=false
```

## Обязательные зависимости

| Зависимость | Требование |
|---|---|
| PostgreSQL | доступен до старта приложения |
| Kafka | доступен для consumer |
| Secret storage | содержит `PODCAST_ACCESS_TOKEN_SECRET` |
| Reverse proxy | маршрутизирует `/podcast/v1` |

## Graceful shutdown

Spring Boot завершает web server, Kafka listener и Hikari pool штатно. Контейнеру требуется корректный `SIGTERM`.
