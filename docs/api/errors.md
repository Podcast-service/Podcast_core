# Ошибки API

## Формат ответа

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-23T10:00:00Z",
  "details": {
    "fields": {
      "title": "must not be blank"
    }
  }
}
```

## Каталог кодов

| HTTP статус | Код | Описание | Действие клиента |
|---:|---|---|---|
| `400` | `VALIDATION_ERROR` | невалидные параметры, тело или enum | исправить request |
| `401` | `UNAUTHORIZED` | токен отсутствует, просрочен или невалиден | запросить новый access token |
| `403` | `FORBIDDEN` | роли или права владения не подходят | скрыть действие или запросить другую роль |
| `404` | `RESOURCE_NOT_FOUND` | ресурс отсутствует или недоступен | показать состояние “не найдено” |
| `409` | `CONFLICT` | конфликт уникальности или состояния | обновить данные и повторить действие |
| `422` | `BUSINESS_RULE_VIOLATION` | бизнес-правило запрещает операцию | показать причину пользователю |
| `500` | `INTERNAL_ERROR` | непредвиденная ошибка | повторить запрос после восстановления и передать технические детали в поддержку |

## Validation errors

Ошибки bean validation и query validation возвращаются как `VALIDATION_ERROR`. Для field-level ошибок используется `details.fields`.

Пример ошибки при создании подкаста без корректного количества спикеров:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "Request validation failed",
  "timestamp": "2026-05-24T08:00:00Z",
  "details": {
    "fields": {
      "numSpeakers": "num_speakers must be greater than 0"
    }
  }
}
```

Если JSON содержит нечисловое значение `num_speakers`, запрос завершается `400 VALIDATION_ERROR`, так как тело не соответствует модели `CreatePodcastRequest`.

## Ошибки бизнес-правил

Публикация подкаста возвращает `422 BUSINESS_RULE_VIOLATION`, если media lifecycle ещё не завершён, отсутствует processed `audioUrl` или отсутствует положительный `durationSeconds`.

```json
{
  "code": "BUSINESS_RULE_VIOLATION",
  "message": "Cannot publish a podcast without positive duration_seconds",
  "timestamp": "2026-05-24T08:00:00Z"
}
```

## Authorization errors

Неверная схема header:

```json
{
  "code": "UNAUTHORIZED",
  "message": "Authorization header must use Bearer scheme",
  "timestamp": "2026-05-23T10:00:00Z"
}
```

Недостаточная роль:

```json
{
  "code": "FORBIDDEN",
  "message": "You don't have permission to access this resource",
  "timestamp": "2026-05-23T10:00:00Z"
}
```

## Непредвиденные ошибки

`INTERNAL_ERROR` не раскрывает внутренние детали. Полный stack trace остаётся в логах приложения.
