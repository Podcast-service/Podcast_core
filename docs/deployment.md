# Deployment

## Docker image

`Dockerfile` использует multi-stage build:

1. `eclipse-temurin:21-jdk` собирает jar через Gradle.
2. `eclipse-temurin:21-jre` запускает `/app/app.jar`.

Сборка:

```bash
docker build -t podcast-core:local .
```

Запуск standalone-контейнера требует внешних PostgreSQL и Kafka:

```bash
docker run --rm -p 8082:8082 \
  -e DB_URL="jdbc:postgresql://host.docker.internal:5432/podcast_db" \
  -e DB_USERNAME="podcast_user" \
  -e DB_PASSWORD="podcast_pass" \
  -e KAFKA_BOOTSTRAP_SERVERS="host.docker.internal:9092" \
  -e ACCESS_TOKEN_SECRET="change-me" \
  podcast-core:local
```

## Docker Compose

Для локального окружения:

```bash
docker compose up --build
```

Compose включает `postgres`, `kafka`, `kafka-ui`, `app` и запускает приложение с профилями `docker,dev`.

## Production environment checklist

| Настройка | Требование |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Не включать `dev` |
| `ACCESS_TOKEN_SECRET` | Брать из secret storage |
| `ACCESS_TOKEN_ISSUER` | Совпадает с issuer `auth-service` |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Production PostgreSQL |
| `KAFKA_BOOTSTRAP_SERVERS` | Production Kafka |
| `KAFKA_TOPIC_USERS` | Совпадает с topic в `auth-service` |
| `CORS_ALLOWED_ORIGINS` | Только реальные frontend origins |
| `DEV_SEED_ENABLED` | `false` или отсутствует без `dev` profile |
| Actuator | Health exposed, metrics по решению DevOps |

## Миграции

Flyway запускается при старте приложения. Перед rollout:

1. Проверить миграции на staging.
2. Убедиться, что миграции backward-compatible для текущей версии приложения.
3. Сделать backup или snapshot БД по регламенту.
4. Не изменять уже применённые migration files.

## Health checks

Для readiness/liveness можно использовать:

```text
GET /actuator/health
```

Если в runtime доступны probes:

```text
GET /actuator/health/liveness
GET /actuator/health/readiness
```

TODO: закрепить точные health endpoints в deployment manifests.

## Rollout

Рекомендуемый порядок:

1. Применить инфраструктурные secrets/config.
2. Убедиться, что Kafka topics созданы или auto-create разрешён политикой платформы.
3. Запустить новую версию на staging.
4. Прогнать smoke tests: health, categories, public podcasts, auth endpoint, protected endpoint.
5. Проверить consumer lag и DLT.
6. Выпустить production deployment.
7. Мониторить 5xx, latency, Kafka lag, DLT и DB.

## Security notes

- Не коммитить `.dev/dev-token.txt`, secrets, production env files.
- Не использовать dev secret в production.
- Не включать `AUTH_ENABLED=false` в production.
- Не открывать CORS wildcard для browser clients.
- Публичные GET endpoint'ы должны оставаться без side effects, кроме documented view counters если они будут добавлены в будущем.
- Swagger в production должен быть либо закрыт, либо явно разрешён политикой безопасности команды.

TODO: уточнить, должен ли Swagger UI быть доступен в production.
