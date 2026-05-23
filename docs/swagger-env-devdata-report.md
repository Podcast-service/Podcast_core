# Отчёт по Swagger, окружению и dev-данным

## Swagger/OpenAPI

Swagger UI использует Spring context path:

```text
http://localhost:8082/podcast/v1/swagger
```

Dev OpenAPI-документ доступен по адресу:

```text
http://localhost:8082/podcast/v1/openapi/podcast-service-dev.yaml
```

Причина проблемы со Swagger была в рассинхронизации локальных путей после введения production-префикса API. Runtime routes находятся под `/podcast/v1`, а Swagger UI config получал URL уже с context path и формировал двойной путь `/podcast/v1/podcast/v1/openapi/podcast-service-dev.yaml`. Springdoc получает servlet-relative `PODCAST_SWAGGER_OPENAPI_URL=/openapi/podcast-service-dev.yaml`, а внешний OpenAPI URL остаётся `http://localhost:8082/podcast/v1/openapi/podcast-service-dev.yaml`.

JWT bearer auth описан через OpenAPI security scheme `bearerAuth`. Swagger UI показывает Authorize dialog и отправляет header:

```http
Authorization: Bearer <access_token>
```

SpringDoc endpoints управляются переменной `PODCAST_SWAGGER_ENABLED`. В base config значение по умолчанию `false`; в local/dev profile значение по умолчанию `true`.

## Environment variables

Проектные runtime-переменные используют namespace `PODCAST_*`.

| Область | Переменные |
|---|---|
| HTTP | `PODCAST_SERVER_PORT`, `PODCAST_SERVER_SERVLET_CONTEXT_PATH`, `PODCAST_APP_PORT` |
| Profiles | `PODCAST_SPRING_PROFILES_ACTIVE` |
| Database | `PODCAST_DB`, `PODCAST_DB_URL`, `PODCAST_DB_USER`, `PODCAST_DB_PASSWORD`, `PODCAST_DB_PORT` |
| Kafka | `PODCAST_KAFKA_BOOTSTRAP_SERVERS`, `PODCAST_KAFKA_CONSUMER_GROUP`, `PODCAST_KAFKA_TOPIC_USERS`, `PODCAST_KAFKA_TOPIC_PODCASTS`, `PODCAST_KAFKA_INTERNAL_PORT`, `PODCAST_KAFKA_EXTERNAL_PORT`, `PODCAST_KAFKA_EXTERNAL_HOST`, `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE`, `PODCAST_KAFKA_UI_PORT` |
| JWT | `PODCAST_ACCESS_TOKEN_SECRET`, `PODCAST_ACCESS_TOKEN_ISSUER`, `PODCAST_AUTH_ENABLED` |
| Browser access | `PODCAST_CORS_ALLOWED_ORIGINS` |
| Dev data | `PODCAST_DEV_SEED_ENABLED` |
| Swagger | `PODCAST_SWAGGER_ENABLED`, `PODCAST_SWAGGER_OPENAPI_URL` |

Docker images для PostgreSQL и Kafka получают свои нативные container variables (`POSTGRES_*`, `KAFKA_*`) внутри `docker-compose.yml`. Их значения формируются из `PODCAST_*` inputs и не используются как проектные `.env` variables.

## Обновлённые файлы

- `.env.example`
- `docker-compose.yml`
- `src/main/resources/application.yaml`
- `src/main/resources/application-dev.yaml`
- `src/main/resources/application-docker.yaml`
- `src/main/java/podcastService/infrastructure/config/DevDataSeeder.java`
- `src/main/java/podcastService/infrastructure/security/SecurityConfiguration.java`
- `scripts/generate-dev-token.ps1`
- `scripts/generate-dev-token.sh`
- `.dev-tools/jwt/README.md`
- `README.md`
- `docs/README.md`
- `docs/configuration.md`
- `docs/local-development.md`
- `docs/dev-tools.md`
- `docs/api/auth.md`
- `docs/api/README.md`
- `docs/testing.md`
- `docs/deployment.md`
- `docs/observability.md`
- `docs/kafka.md`
- `docs/swagger-env-devdata-report.md`

Проверенные OpenAPI-контракты:

- `openapi-2.yaml`
- `src/main/resources/static/openapi/podcast-service-dev.yaml`

## Dev profiles

| Profile | Поведение |
|---|---|
| `docker` | Использует Docker network defaults для PostgreSQL и Kafka |
| `dev` | Активирует `DevDataSeeder`, local Swagger и данные для frontend-разработки |

`PODCAST_DEV_SEED_ENABLED=false` отключает seed execution даже при активном profile `dev`.

## Dev seed data

`DevDataSeeder` работает только под Spring profile `dev`. Dataset детерминированный и идемпотентный: записи используют stable UUID, существующие author/category записи переиспользуются по уникальным ключам, seeded playlist items пересобираются, счётчики votes/subscriptions пересчитываются из relation tables.

Плановый объём dev dataset:

| Сущность | Объём |
|---|---:|
| User profiles | 36 |
| Author profiles | 14 |
| Categories | 12 |
| Podcasts | 126 |
| Published podcasts | около 100 |
| Playlists | 72 |
| Playlist items | около 576 |
| Subscriptions | около 175 |
| Podcast votes | около 648 |
| Playlist votes | около 360 |
| Listen history rows | около 864 |
| Transcript/summary rows | больше 200 |

Фактическая dev-БД после проверки содержала больше строк в некоторых таблицах, потому что в локальном volume уже были ручные/старые dev-записи. Seed применился поверх существующих данных без нарушения foreign keys.

Dataset включает русский и английский текст, разные publication statuses, nullable optional fields, длинные и короткие descriptions, разные dates, durations, public/private playlists и interaction data для pagination, filtering, search и sorting.

## Dev users

Основной JWT-compatible dev user:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Дополнительные синтетические профили представлены безопасными local usernames: `qa_reader`, `frontend_olga`, `backend_max`, `admin_local`, `author_local`, `unicode_юзер` и другими.

Roles поставляются через JWT и не хранятся в `user_profiles`.

## Проверки

| Проверка | Результат |
|---|---|
| `docker compose config --quiet` | Пройдена |
| `./gradlew clean test` | Пройдена |
| `docker compose up -d --build` | Пройдена |
| Health endpoint | `UP` |
| Swagger UI | `/podcast/v1/swagger-ui/index.html` вернул `200` |
| Swagger redirect | `/podcast/v1/swagger` вернул `302` |
| OpenAPI static document | `/podcast/v1/openapi/podcast-service-dev.yaml` вернул `200` |
| Dev seed loading | Пройдена; seed log: `users=36, authors=14, categories=12, podcasts=126, playlists=72` |
| DB connectivity | Пройдена через Flyway validation, JPA startup и SQL count queries |
| Kafka connectivity | Пройдена через consumer assignment на `podcasts.users-0` |
| Логи приложения | Нет `ERROR`, `Application run failed`, `DuplicateKeyException`, `PSQLException`; generated security password warning отсутствует |
| Documentation/gitignore visibility | `docs/` и `.dev-tools/jwt/` не игнорируются Git |
| Старые env names | Не обнаружены в проектных config/docs/scripts |

OpenAPI validation:

| Спецификация | Результат |
|---|---|
| `openapi-2.yaml` | YAML parsed, 220 `$ref`, 0 missing refs, 0 duplicate operation IDs |
| `src/main/resources/static/openapi/podcast-service-dev.yaml` | YAML parsed, 241 `$ref`, 0 missing refs, 0 duplicate operation IDs |

> Требует уточнения: project-approved OpenAPI validator CLI для автоматизированной проверки JSON Schema/OpenAPI compatibility.

## Production behavior

Production secrets не добавлены в репозиторий. `.env.example` содержит только local/example values. Dev seed data привязан к Spring profile `dev`; production profile `docker` не активирует `DevDataSeeder`. Swagger/OpenAPI endpoints отключены в base config по умолчанию и включаются для local/dev через `PODCAST_SWAGGER_ENABLED=true`.
