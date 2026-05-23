# Kafka-топики

| Topic | Направление | Статус | Назначение |
|---|---|---|---|
| `podcasts.users` | входящий | используется | события пользователей от `auth-service` |
| `podcasts.users.DLT` | исходящий от error handler | используется при ошибках | сообщения, не обработанные consumer |
| `podcasts.podcasts` | зарезервирован конфигурацией | продюсер в коде не используется | будущие события подкастов |

## Partitions

Количество partitions задаётся инфраструктурой Kafka. В локальном Docker Compose auto-create topics включён через `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`.

> Требует уточнения: production количество partitions и replication factor для каждого topic.

## Consumer group

`podcast-core` использует group id из `PODCAST_KAFKA_CONSUMER_GROUP`, по умолчанию `podcast-service`.
