# Kafka-консьюмеры

## `UserRegistrationConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.user-register}` |
| Default topic | `podcast.user.register` |
| DTO | `UserRegisteredEvent` |
| Domain service | `UserProfileService` |

Событие идемпотентно создаёт или обновляет `user_profiles` по `user_id`.

## `MediaUploadConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.media-upload}` |
| Default topic | `media.upload` |
| DTO | `MediaUploadEventDto` |
| Router | `MediaUploadEventRouter` |

| `object_type` | `event` | Поведение |
|---|---|---|
| `podcast_file_url` | `start_upload` | `podcasts.status = UPLOADING` |
| `podcast_file_url` | `uploaded` | сохраняются `audio_url_file`, `audio_size_file`; статус `UPLOADED` |
| `podcast_file_url` | `upload_failed`, `error` | `podcasts.status = FAILED` |
| `podcast_cover_url` | `uploaded` | обновляется `podcasts.cover_image_url` |
| `avatar` | `uploaded` | обновляется `user_profiles.avatar_url` |
| `playlist` | `uploaded` | обновляется `playlists.cover_image_url` |

## `MediaWorkerConsumer`

| Параметр | Значение |
|---|---|
| Topic | `${app.kafka.topics.media-worker}` |
| Default topic | `media.worker` |
| DTO | `MediaWorkerEventDto` |
| Router | `MediaWorkerEventRouter` |

| `object_type` | `event` | Поведение |
|---|---|---|
| `podcast_file_url` | `start_processing` | `podcasts.status = PROCESSING` |
| `podcast_file_url` | `processed` | сохраняется HLS/processed `audio_url`; статус `PROCESSED` |
| `podcast_file_url` | `processing_failed`, `error` | `podcasts.status = FAILED` |

## `MediaSubtitleConsumer`

Читает `media.subtitle`, валидирует `podcast_id` и `content`, после чего сохраняет содержимое `content` в `podcast_transcripts.content`. Для текущего контракта subtitle это JSON с `vtt_object_key`, `srt_object_key` и `ready_at`.

## `TtsStartConsumer`

Читает `tts.start`, валидирует `podcast_id` и текстовый `content`, после чего сохраняет текст в `podcast_transcripts.content`. Статус подкаста не меняется.
