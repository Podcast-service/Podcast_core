# Формат сообщений Kafka

## EventEnvelope

```json
{
  "eventType": "user.created",
  "occurredAt": "2026-05-23T10:00:00Z",
  "payload": {
    "userId": "00000000-0000-0000-0000-000000000001",
    "username": "dev-user"
  }
}
```

| Поле | Тип | Обязательность | Описание |
|---|---|---|---|
| `eventType` | string | да | тип события |
| `occurredAt` | instant | да | время возникновения события |
| `payload` | object | да | полезная нагрузка |

## `user.created`

| Поле payload | Тип | Обязательность |
|---|---|---|
| `userId` | uuid | да |
| `username` | string | да |

## Ключ сообщения

Код consumer не требует конкретного Kafka key. Для production ordering рекомендуется использовать `userId` как key.

> Требует уточнения: утверждённый key strategy со стороны `auth-service`.
