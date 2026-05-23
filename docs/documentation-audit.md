# Documentation audit

## Документы

| Файл | Статус |
|---|---|
| `docs/README.md` | Обновлён |
| `docs/architecture.md` | Обновлён |
| `docs/api/README.md` | Обновлён |
| `docs/api/endpoints.md` | Обновлён |
| `docs/api/errors.md` | Обновлён |
| `docs/api/models.md` | Обновлён |
| `docs/api/auth.md` | Создан |
| `docs/kafka.md` | Обновлён |
| `docs/configuration.md` | Обновлён |
| `docs/local-development.md` | Обновлён |
| `docs/testing.md` | Обновлён |
| `docs/observability.md` | Обновлён |
| `docs/deployment.md` | Обновлён |
| `docs/dev-tools.md` | Создан |
| `.dev-tools/jwt/README.md` | Создан |
| `docs/swagger-env-devdata-report.md` | Создан |

## Источники

- `openapi-2.yaml`
- `README.md`
- `docker-compose.yml`
- `.env.example`
- `Dockerfile`
- `src/main/resources/application.yaml`
- `src/main/resources/application-docker.yaml`
- `src/main/resources/application-dev.yaml`
- REST controllers в `src/main/java/podcastService/**/controller`
- DTO и entity classes в `src/main/java/podcastService/**/dto` и `src/main/java/podcastService/**/entity`
- Security layer в `src/main/java/podcastService/infrastructure/security`
- Kafka layer в `src/main/java/podcastService/infrastructure/messaging` и `src/main/java/podcastService/user/messaging`
- Exception handling в `src/main/java/podcastService/common/exception`
- Flyway migrations в `src/main/resources/db/migration`
- Тесты в `src/test/java`
- Dev JWT scripts в `scripts/` и `.dev-tools/jwt/`

## Основание разделов

| Раздел | Основной источник |
|---|---|
| REST endpoints | `openapi-2.yaml`, controllers |
| DTO и модели | `openapi-2.yaml`, Java DTO/entity classes |
| Ошибки | `GlobalExceptionHandler`, `ErrorCode`, security handlers |
| JWT | `JwtAuthenticationService`, `JwtAuthenticationFilter`, `SecurityConfiguration` |
| Kafka | Kafka config, `UserProfileConsumer`, `UserProfileEventHandler`, `KafkaErrorHandlerConfig` |
| Конфигурация | `application*.yaml`, `.env.example`, `docker-compose.yml` |
| Локальный запуск | `README.md`, Docker Compose, dev seed |
| Тестирование | `src/test/java`, Gradle config |
| Deployment | `Dockerfile`, `docker-compose.yml`, application config |

## Расхождения OpenAPI и реализации

| Область | OpenAPI | Реализация |
|---|---|---|
| DELETE `/podcasts/{podcastId}/vote` | Описание допускает `204`, если голос отсутствовал | Контроллер возвращает `200 OK` с `VoteResponse` |
| Sort enum name | `SortPodcast` | Java DTO называется `SortPodcasts`; значения совпадают |
| Dev Swagger | Каноничный контракт находится в `openapi-2.yaml` | Swagger UI использует `/podcast/v1/openapi/podcast-service-dev.yaml` |
| `CreateUserRequest` | Не описан как REST request | Используется как Kafka payload события `user.created` |
| Kafka topic `podcasts` | Присутствует в config | Доменный код не публикует события в этот topic |

## Факты, требующие уточнения

> Требует уточнения: способ доставки transcript/summary из `tts-stt-service`.

> Требует уточнения: набор исходящих Kafka-событий `podcast-core`.

> Требует уточнения: политика доступности Swagger UI в production.

> Требует уточнения: стандарт correlation id для HTTP и Kafka.
