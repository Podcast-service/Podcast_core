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
| `podcast_file_url` | `uploaded` | сохраняется `audio_url_file`; статус `UPLOADED` |
| `podcast_file_url` | `upload_failed`, `error` | `podcasts.status = FAILED` |
| `podcast_cover_url` | `uploaded` или отсутствует | `image_url` сохраняется в `podcasts.cover_image_url` |
| `avatar` | `uploaded` или отсутствует | `image_url` сохраняется в `user_profiles.avatar_url` |
| `playlist` | `uploaded` или отсутствует | `image_url` сохраняется в `playlists.cover_image_url` |

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
| `podcast_file_url` | `processed` | сохраняются HLS/processed `audio_url`, `duration_seconds`, `audio_file_size`; статус `PROCESSED` |
| `podcast_file_url` | `processing_failed`, `error` | `podcasts.status = FAILED` |
| `podcast_file_url` | `converted`, `subtitle_ready`, `deleted` | принимается без изменения состояния |

## `MediaSubtitleConsumer`

Читает `media.subtitle`, валидирует `podcast_id` и `content`, после чего сохраняет содержимое `content` в `podcast_transcripts.content`. Для текущего контракта subtitle это JSON с `vtt_object_key` и `srt_object_key`; `ready_at` передаётся отдельным top-level полем.

## `TtsStartConsumer`

Читает `tts.start`, валидирует `podcast_id` и текстовый `content`, после чего сохраняет текст в `podcast_transcripts.content`. Статус подкаста не меняется.
