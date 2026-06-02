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

## `MediaSubtitleConsumer`

Читает `media.subtitle`, валидирует `podcast_id`, `content` и обязательный `content.vtt_object_key`. Consumer скачивает VTT по абсолютному HTTP(S) URL или относительному object key, последовательно объединяет соседние cue одного спикера и сохраняет JSON-массив блоков `text`/`voice` в `podcast_transcripts.content`.

После commit сохранения transcript публикуется internal application event для best-effort генерации summary. Summary generation извлекает поля `text` из сохранённых блоков; повторное чтение subtitle storage не требуется. Для старых записей с pointer JSON сохранён legacy fallback чтения `.srt`/`.vtt`. Если `PODCAST_OPENROUTER_ENABLED=false` или OpenRouter недоступен, Kafka flow не откатывается.

## `TtsStartConsumer`

Читает `tts.start`, валидирует `podcast_id` и непустой `content`, после чего сохраняет содержимое в `podcast_transcripts.content` и переводит подкаст в `UPLOADING`. `content` может быть строкой, JSON-объектом или JSON-массивом. Summary generation запускается тем же after-commit механизмом, что и для `media.subtitle`.

## `TtsFailedConsumer`

Читает `tts.failed`, валидирует `object_type=podcast_file_url`, `object_id`, `event=error` и `error`, после чего переводит подкаст в `FAILED`. Поздние ошибки после `PROCESSED`, `PUBLISHED` или `ARCHIVED` логируются и не откатывают статус.
