# Documentation audit

## Созданные файлы

- `docs/README.md`
- `docs/architecture.md`
- `docs/api/README.md`
- `docs/api/endpoints.md`
- `docs/api/errors.md`
- `docs/api/models.md`
- `docs/kafka.md`
- `docs/configuration.md`
- `docs/local-development.md`
- `docs/testing.md`
- `docs/observability.md`
- `docs/deployment.md`
- `docs/documentation-audit.md`

## Использованные источники

- `openapi-2.yaml`
- `src/main/resources/application.yaml`
- `src/main/resources/application-docker.yaml`
- `src/main/resources/application-dev.yaml`
- `docker-compose.yml`
- `Dockerfile`
- `README.md`
- REST controllers в `src/main/java/podcastService/**/controller`
- DTO и entity classes в `src/main/java/podcastService/**/dto` и `src/main/java/podcastService/**/entity`
- Security classes в `src/main/java/podcastService/infrastructure/security`
- Kafka classes в `src/main/java/podcastService/infrastructure/messaging` и `src/main/java/podcastService/user/messaging`
- Exception handling в `src/main/java/podcastService/common/exception`
- Flyway migrations в `src/main/resources/db/migration`
- Тесты в `src/test/java`

## Места, требующие уточнения

| Тема | Что уточнить |
|---|---|
| API prefix | Должен ли production/local API иметь `/v1` или `/podcast/v1`, как указано в OpenAPI |
| Transcript/summary | Как именно `tts-stt-service` записывает данные: напрямую в БД, через Kafka или через REST |
| Kafka producer | Должен ли `podcast-core` публиковать события по подкастам, подпискам, голосам |
| `AUTH_ENABLED=false` | Допустимо ли выключение авторизации только локально или вообще должно быть удалено |
| Swagger в production | Должен ли UI быть публичным, закрытым или отключённым |
| Observability | Целевой стек метрик и tracing: Prometheus/OpenTelemetry/другое |
| Correlation id | Нужен ли обязательный `X-Request-Id` |
| Avatar/cover URL | Нужны ли строгие URL validation и максимальные длины везде на уровне DTO |

## Найденные несоответствия

| Место | OpenAPI | Код/конфигурация |
|---|---|---|
| Base URL | `http://localhost:8082/v1`, production `/podcast/v1` | Контроллеры работают от корня `/` без version prefix |
| DELETE `/podcasts/{podcastId}/vote` | В описании есть сценарий `204`, если голоса не было | Контроллер возвращает `200 OK` и `VoteResponse` |
| Sort enum name | `SortPodcast` | В коде `SortPodcasts`; значения совпадают |
| Dev Swagger | Каноника — `openapi-2.yaml` | Swagger UI смотрит на `/openapi/podcast-service-dev.yaml` |
| `CreateUserRequest` | Не является REST request в OpenAPI | Используется как Kafka payload `user.created` |
| `podcasts` Kafka topic | Присутствует в config | Доменный код сейчас не публикует события в этот topic |

## Рекомендации по улучшению OpenAPI

1. Явно решить вопрос с version prefix и синхронизировать `servers` с реальными контроллерами.
2. Добавить описание optional Bearer token для публичных GET-ручек, где ответ зависит от текущего пользователя.
3. Уточнить response для `DELETE /podcasts/{podcastId}/vote`: `200 VoteResponse` или `204 No Content`.
4. Добавить Kafka contract в отдельный AsyncAPI или раздел OpenAPI extension: topic, event envelope, payload, DLT.
5. Уточнить auth roles прямо в описании каждой protected operation.
6. Добавить единый пример `ApiErrorResponse` для каждого error response.
7. Синхронизировать названия DTO/enum между OpenAPI и Java, где это возможно.
8. Зафиксировать ограничения URL-полей: `avatarUrl`, `coverImageUrl`, `audioUrl`.
9. Добавить production/staging/local server URLs после решения о gateway prefix.
