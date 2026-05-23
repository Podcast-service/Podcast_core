# Kafka

Kafka используется для асинхронной синхронизации данных пользователя из сервиса аутентификации. Основной поддерживаемый входящий event type: `user.created`.

## Роли сервисов

| Сервис | Роль |
|---|---|
| `auth-service` | producer события `user.created` |
| `podcast-core` | consumer topic `podcasts.users` |
| `podcast-core` error handler | producer в DLT topic |

## Runtime настройки

| Настройка | Значение по умолчанию |
|---|---|
| Bootstrap servers | `PODCAST_KAFKA_BOOTSTRAP_SERVERS=localhost:9092` |
| Consumer group | `PODCAST_KAFKA_CONSUMER_GROUP=podcast-service` |
| Users topic | `PODCAST_KAFKA_TOPIC_USERS=podcasts.users` |
| Reserved podcasts topic | `PODCAST_KAFKA_TOPIC_PODCASTS=podcasts.podcasts` |

## Consumer flow

1. Kafka listener получает `EventEnvelope`.
2. Handler проверяет `eventType`, `occurredAt` и `payload`.
3. Payload преобразуется в `CreateUserRequest`.
4. `UserProfileService` создаёт локальный профиль.
5. Ошибки обрабатываются `DefaultErrorHandler`.

Подробности по форматам: [message-format.md](message-format.md).
