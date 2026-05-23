# REST API

Базовый путь API:

```text
http://localhost:8082/podcast/v1
```

Каноничный контракт находится в [`../../openapi-2.yaml`](../../openapi-2.yaml). Все пути в документации указаны относительно `/podcast/v1`.

## Разделы

| Документ | Содержание |
|---|---|
| [authentication.md](authentication.md) | JWT, роли, Swagger authorization |
| [endpoints.md](endpoints.md) | полный список HTTP операций |
| [pagination.md](pagination.md) | формат страниц и ограничения |
| [filtering.md](filtering.md) | фильтры, сортировки и поиск |
| [errors.md](errors.md) | формат ошибок и коды |
| [models.md](models.md) | DTO и доменные модели |
| [examples.md](examples.md) | curl и JSON примеры |
| [rate-limits.md](rate-limits.md) | ограничения частоты запросов |

## Соглашения API

| Соглашение | Значение |
|---|---|
| Формат тела | JSON |
| Авторизация | `Authorization: Bearer <access_token>` |
| Пагинация | `page` начинается с `1`, `size` по умолчанию `20` |
| Ошибки | `ApiErrorResponse` с полями `code`, `message`, `timestamp`, `details` |
| Даты | ISO-8601 в UTC |
| Идентификаторы | UUID |

## Swagger

Swagger UI в local/dev:

```text
http://localhost:8082/podcast/v1/swagger
```

Dev OpenAPI document:

```text
http://localhost:8082/podcast/v1/openapi/podcast-service-dev.yaml
```
