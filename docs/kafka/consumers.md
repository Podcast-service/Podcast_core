# Kafka-консьюмеры

## `UserProfileConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.users}` |
| Default topic | `podcasts.users` |
| Group id | `${spring.kafka.consumer.group-id}` |
| Тип payload | `EventEnvelope` |
| Handler | `UserProfileEventHandler` |

## Поддерживаемые события

| Тип события | Payload | Результат |
|---|---|---|
| `user.created` | `CreateUserRequest` | создание `user_profiles` |

## Валидация consumer

Сообщение считается невалидным, если отсутствует envelope, `eventType`, `occurredAt`, `payload`, `payload.userId` или `payload.username`.
