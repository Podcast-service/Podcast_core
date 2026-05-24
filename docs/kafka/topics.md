# Kafka-топики

| Topic | Направление | Статус | Назначение |
|---|---|---|---|
| `podcast.user.register` | входящий | используется | регистрация пользователя из auth-service |
| `media` | входящий | используется | события загрузки медиа |
| `podcast.user.register.DLT` | исходящий от error handler | используется при ошибках | сообщения регистрации, не обработанные consumer |
| `media.DLT` | исходящий от error handler | используется при ошибках | media-сообщения, не обработанные consumer |

## Partitions

Количество partitions задаётся инфраструктурой Kafka. В локальном Docker Compose auto-create topics включён через `PODCAST_KAFKA_AUTO_CREATE_TOPICS_ENABLE=true`.

Рекомендуемые ключи сообщений:

| Topic | Kafka key |
|---|---|
| `podcast.user.register` | `user_id` |
| `media` | `object_id` |

> Требует уточнения: production количество partitions, replication factor и retention для каждого topic.

## Consumer group

`podcast-core` использует group id из `PODCAST_KAFKA_CONSUMER_GROUP`, по умолчанию `podcast-service`.
