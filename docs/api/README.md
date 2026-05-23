# REST API

Каноничный контракт API описан в [`../../openapi-2.yaml`](../../openapi-2.yaml). Этот раздел переводит контракт и фактическую реализацию контроллеров в документацию для разработки и тестирования.

## Базовый URL

| Среда | URL |
|---|---|
| Docker/local | `http://localhost:8082/podcast/v1` |
| Swagger UI | `http://localhost:8082/podcast/v1/swagger` |
| OpenAPI local server в `openapi-2.yaml` | `http://localhost:8082/podcast/v1` |
| OpenAPI production server | `https://api.example.com/podcast/v1` |

Все endpoint paths в документации ниже указаны относительно base URL `/podcast/v1`.

## Авторизация

Защищённые ручки принимают заголовок:

```http
Authorization: Bearer <access_token>
```

JWT подписывается секретом из `PODCAST_ACCESS_TOKEN_SECRET`, содержит issuer из `PODCAST_ACCESS_TOKEN_ISSUER` и claim `user_id`. Для ролевых ручек используется claim `roles`.

| Тип доступа | Поведение |
|---|---|
| Public | Токен не используется |
| Optional token | Без токена возвращаются публичные данные; с токеном добавляется пользовательский контекст |
| Authenticated | Используется валидный Bearer token |
| Author | Используется Bearer token с ролью `author` |
| Admin | Используется Bearer token с ролью `admin` |

## Пагинация

Списки используют параметры:

| Параметр | Тип | По умолчанию | Ограничения |
|---|---:|---:|---|
| `page` | integer | `1` | `>= 1` |
| `size` | integer | `20` | `1..50` |

Формат ответа:

```json
{
  "items": [],
  "meta": {
    "page": 1,
    "size": 20,
    "totalElements": 0,
    "totalPages": 0
  }
}
```

## Основные документы

- [endpoints.md](endpoints.md) — все REST endpoint'ы.
- [models.md](models.md) — DTO и доменные модели.
- [errors.md](errors.md) — каталог ошибок.
- [auth.md](auth.md) — JWT, claims, роли и dev-токены.

## Общий формат ошибки

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-20T10:00:00Z",
  "details": {
    "fields": {
      "title": "title must not be blank"
    }
  }
}
```
