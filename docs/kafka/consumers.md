# Kafka-консьюмеры

## `UserRegistrationConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.user-register}` |
| Default topic | `podcast.user.register` |
| Group id | `${spring.kafka.consumer.group-id}` |
| Тип payload | raw JSON |
| Parser | `UserRegisteredEventParser` |
| Domain service | `UserProfileService` |

Сообщение создаёт или обновляет `user_profiles` по `user_id`. Повторная доставка безопасна: запись не дублируется, `username` обновляется при изменении.

## `MediaEventConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.media}` |
| Default topic | `media` |
| Group id | `${spring.kafka.consumer.group-id}` |
| Тип payload | raw JSON |
| Parser | `MediaEventParser` |
| Router | `MediaEventRouter` |

`MediaEventRouter` использует registry обработчиков по ключу `(type,event)`. Новые `type` и `event` подключаются отдельным `MediaEventHandler` без изменения consumer.

## Поддерживаемые media-события

| Type | Event | Результат |
|---|---|---|
| `podcast_file` | `start_upload` | `podcasts.status = PROCESSING` |
| `podcast_file` | `uploaded` | обновление `audio_url`, `audio_url_file`, `audio_size_file`, статус `READY_TO_PUBLISH` |
| `podcast_file` | `error` | статус `UPLOAD_ERROR`, если подкаст ещё не готов и не опубликован |
| `podcast_cover` | `uploaded` | обновление `podcasts.cover_image_url` |
| `podcast_cover` | `start_upload`, `error` | событие логируется без отдельного статуса |
| `avatar` | `uploaded` | обновление `user_profiles.avatar_url` |
| `avatar` | `start_upload`, `error` | событие логируется без отдельного статуса |
| `playlists` | `uploaded` | обновление `playlists.cover_image_url` |
| `playlists` | `start_upload`, `error` | событие логируется без отдельного статуса |

Unknown `type/event` не обрабатывается молча: событие логируется и попадает в DLT как нарушение контракта.
