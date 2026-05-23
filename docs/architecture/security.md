# Архитектура безопасности

## Аутентификация

Сервис принимает JWT access token в HTTP header:

```http
Authorization: Bearer <access_token>
```

Токен валидируется локально:

| Параметр | Значение |
|---|---|
| Алгоритм | `HS256` |
| Секрет | `PODCAST_ACCESS_TOKEN_SECRET` |
| Issuer | `PODCAST_ACCESS_TOKEN_ISSUER`, по умолчанию `auth-service` |
| Обязательные claims | `iss`, `exp`, `user_id`, `email` |
| Опциональные claims | `nbf`, `roles` |
| Допуск времени | 30 секунд |

## Авторизация

Route-level правила находятся в `SecurityConfiguration`, методовые правила представлены через `@PreAuthorize`.

| Тип доступа | Поведение |
|---|---|
| публичный GET | доступ без токена |
| публичный GET с токеном | персонализированные поля ответа |
| `isAuthenticated()` | любой валидный JWT |
| `hasRole('AUTHOR')` | роль `author` или `ROLE_AUTHOR` |
| `hasRole('ADMIN')` | роль `admin` или `ROLE_ADMIN` |

## Защита инфраструктурных путей

`JwtAuthenticationFilter` пропускает без проверки JWT:

| Путь | Назначение |
|---|---|
| `/actuator/**` | health/info |
| `/swagger`, `/swagger-ui/**` | Swagger UI |
| `/v3/api-docs/**` | SpringDoc config |
| `/openapi/**` | dev OpenAPI document |
| `/error` | servlet error endpoint |

## CORS

Разрешённые origins задаются через `PODCAST_CORS_ALLOWED_ORIGINS`. Credentials отключены. Разрешённые headers: `Authorization`, `Content-Type`, `Accept`, `Origin`.

## Production-позиция

Swagger endpoints отключены в базовой конфигурации через `PODCAST_SWAGGER_ENABLED=false` и включаются в dev/local profile. Production secret не хранится в репозитории.
