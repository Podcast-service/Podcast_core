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
  -e PODCAST_DB_URL="jdbc:postgresql://host.docker.internal:5432/podcast_db" \
  -e PODCAST_DB_USER="podcast_user" \
  -e PODCAST_DB_PASSWORD="podcast_pass" \
  -e PODCAST_KAFKA_BOOTSTRAP_SERVERS="host.docker.internal:9092" \
  -e PODCAST_ACCESS_TOKEN_SECRET="change-me" \
  podcast-core:local
```

## Docker Compose

Для локального окружения:

```bash
cp .env.example .env
docker compose up --build
```

Compose включает `postgres`, `kafka`, `kafka-ui`, `app` и запускает приложение с профилями `docker,dev`.

Для сервера отредактируй `.env` перед запуском. Минимально:

```env
PODCAST_SPRING_PROFILES_ACTIVE=docker
PODCAST_DEV_SEED_ENABLED=false
PODCAST_SWAGGER_ENABLED=false
PODCAST_ACCESS_TOKEN_SECRET=<strong-production-secret>
PODCAST_DB_PASSWORD=<strong-db-password>
PODCAST_CORS_ALLOWED_ORIGINS=https://frontend.example.com
PODCAST_KAFKA_EXTERNAL_HOST=<server-domain-or-ip>
```

`.env.example` хранится в Git как шаблон. Реальный `.env` с секретами находится вне репозитория.

## Production environment checklist

| Настройка | Требование |
|---|---|
| `PODCAST_SPRING_PROFILES_ACTIVE` | Не включает `dev` |
| `PODCAST_ACCESS_TOKEN_SECRET` | Secret storage |
| `PODCAST_ACCESS_TOKEN_ISSUER` | Совпадает с issuer `auth-service` |
| `PODCAST_DB_URL`, `PODCAST_DB_USER`, `PODCAST_DB_PASSWORD` | Production PostgreSQL |
| `PODCAST_KAFKA_BOOTSTRAP_SERVERS` | Production Kafka |
| `PODCAST_KAFKA_TOPIC_USERS` | Совпадает с topic в `auth-service` |
| `PODCAST_CORS_ALLOWED_ORIGINS` | Только реальные frontend origins |
| `PODCAST_DEV_SEED_ENABLED` | `false` или отсутствует без `dev` profile |
| `PODCAST_SWAGGER_ENABLED` | `false`, если Swagger не закрыт отдельным контуром доступа |
| `PODCAST_SERVER_SERVLET_CONTEXT_PATH` | `/podcast/v1` |
| Actuator | Health exposed, metrics по решению DevOps |

## Миграции

Flyway запускается при старте приложения. Перед rollout:

1. Миграции проходят на staging.
2. Убедиться, что миграции backward-compatible для текущей версии приложения.
3. Сделать backup или snapshot БД по регламенту.
4. Не изменять уже применённые migration files.

## Health checks

Для readiness/liveness можно использовать:

```text
GET /podcast/v1/actuator/health
```

Если в runtime доступны probes:

```text
GET /podcast/v1/actuator/health/liveness
GET /podcast/v1/actuator/health/readiness
```

> Требует уточнения: точные health endpoints в deployment manifests.

## Rollout

Рекомендуемый порядок:

1. Применить инфраструктурные secrets/config.
2. Убедиться, что Kafka topics созданы или auto-create разрешён политикой платформы.
3. Новая версия запускается на staging.
4. Smoke tests покрывают health, categories, public podcasts, auth endpoint и protected endpoint.
5. Consumer lag и DLT находятся в штатном состоянии.
6. Выпустить production deployment.
7. Мониторить 5xx, latency, Kafka lag, DLT и DB.

## Security notes

- `.dev/dev-token.txt`, secrets и production env files находятся вне Git.
- Не использовать dev secret в production.
- `PODCAST_AUTH_ENABLED=false` не используется в production.
- `PODCAST_SWAGGER_ENABLED=false` используется для публичного production API, если Swagger UI не закрыт инфраструктурной авторизацией.
- Не открывать CORS wildcard для browser clients.
- Публичные GET endpoint'ы работают без side effects, кроме документированных счётчиков просмотров при наличии такой логики.
- Доступность Swagger в production определяется политикой безопасности команды.

> Требует уточнения: политика доступности Swagger UI в production.
