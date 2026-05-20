# Ошибки API

Все доменные и validation ошибки возвращаются в едином формате `ApiErrorResponse`.

```json
{
  "code": "RESOURCE_NOT_FOUND",
  "message": "Podcast not found",
  "timestamp": "2026-05-20T10:00:00Z",
  "details": {
    "podcastId": "550e8400-e29b-41d4-a716-446655440010"
  }
}
```

## Каталог ошибок

| HTTP status | Код | Описание | Возможная причина | Что делать клиенту |
|---:|---|---|---|---|
| `400` | `VALIDATION_ERROR` | Некорректный request body, query/path параметр или enum | Не прошла Jakarta Validation, неверный UUID/enum, malformed JSON | Исправить параметры и повторить запрос |
| `401` | `UNAUTHORIZED` | Пользователь не аутентифицирован | Нет Bearer token, токен истёк, неверная подпись, неверный issuer | Получить новый access token в `auth-service` |
| `403` | `FORBIDDEN` | Нет прав на действие | Недостаточная роль или попытка изменить чужой ресурс | Не повторять без изменения прав/пользователя |
| `404` | `RESOURCE_NOT_FOUND` | Ресурс не найден или скрыт политикой доступа | Неверный id, приватный плейлист чужого пользователя, неопубликованный подкаст | Клиент обновляет локальное состояние и использует актуальный id |
| `409` | `CONFLICT` | Конфликт с текущим состоянием или уникальными ограничениями | Дубликат username/category/author profile, повторное добавление, DB constraint | Обновить локальное состояние и показать конфликт пользователю |
| `422` | `BUSINESS_RULE_VIOLATION` | Бизнес-операция невозможна | Публикация без audio, self-subscribe, недопустимый статус, неверный состав reorder | Исправить бизнес-условия |
| `500` | `INTERNAL_ERROR` | Непредвиденная ошибка сервера | Баг, недоступная инфраструктура, неожиданное исключение | Повторить позже, передать trace/logs backend-команде |

## Примеры

### Validation error

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-20T10:00:00Z",
  "details": {
    "fields": {
      "page": "must be greater than or equal to 1"
    }
  }
}
```

### Unauthorized

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authentication is required",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

Сообщение формирует security layer. Точный текст зависит от причины отказа в `JwtAuthenticationEntryPoint`/`SecurityErrorResponseWriter`.

### Forbidden

```json
{
  "code": "FORBIDDEN",
  "message": "You don't have permission to access this resource",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

### Conflict

```json
{
  "code": "CONFLICT",
  "message": "Request conflicts with existing data or database constraints",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

### Internal error

```json
{
  "code": "INTERNAL_ERROR",
  "message": "Unexpected internal error",
  "timestamp": "2026-05-20T10:00:00Z"
}
```

## Логирование ошибок

- `BaseException`, validation, type mismatch, malformed JSON, data integrity и authorization denied логируются на `WARN`.
- Непредвиденные исключения логируются на `ERROR` со stack trace.
- В ответ клиенту stack trace не отдаётся.

## Kafka-related errors

Ошибки Kafka consumer не возвращаются REST-клиенту. Они обрабатываются внутри listener container: часть ошибок уходит без retry в DLT, временные ошибки проходят retry policy и затем публикуются в DLT. REST-клиент видит только HTTP-ошибки текущего запроса.

## Correlation/request id

В текущем коде отдельный request id или correlation id filter не представлен.

> Требует уточнения: стандарт correlation id для HTTP и Kafka.
