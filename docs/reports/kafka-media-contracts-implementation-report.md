# Отчёт по Kafka media-контрактам

## Источники

| Источник | Использование |
|---|---|
| `openapi-2.yaml` | публичные модели подкастов и статусы |
| `src/main/java/podcastService/media/messaging` | Kafka consumers, routers и handlers |
| `src/main/java/podcastService/podcast/service` | media lifecycle, публикационные правила |
| `src/main/resources/db/migration` | ограничения статусов и миграции БД |
| `docs/kafka/*` | эксплуатационное описание Kafka-контрактов |

## Реализованные входящие topics

| Topic | Назначение | DTO |
|---|---|---|
| `podcast.user.register` | создание или обновление `user_profiles` | `UserRegisteredEvent` |
| `media.upload` | загрузка исходного файла, обложки, аватара или плейлиста | `MediaUploadEventDto` |
| `media.worker` | обработка аудиофайла подкаста | `MediaWorkerEventDto` |
| `media.subtitle` | сохранение subtitle payload в `podcast_transcripts.content` | `MediaSubtitleEventDto` |
| `tts.start` | сохранение TTS content в `podcast_transcripts.content` | `TtsStartEventDto` |

## Статусы media lifecycle

| Статус | Семантика |
|---|---|
| `DRAFT` | черновик |
| `UPLOADING` | исходный файл загружается |
| `UPLOADED` | исходный файл загружен |
| `PROCESSING` | media worker обрабатывает файл |
| `PROCESSED` | обработанный `audio_url` готов |
| `PUBLISHED` | подкаст опубликован |
| `FAILED` | ошибка загрузки или обработки |
| `ARCHIVED` | подкаст архивирован |

## Persistence

| Событие | Изменяемая таблица | Поля |
|---|---|---|
| `media.upload/start_upload` | `podcasts` | `status = UPLOADING` |
| `media.upload/uploaded` | `podcasts` | `audio_url_file`, `audio_size_file`, `status = UPLOADED` |
| `media.upload/error` | `podcasts` | `status = FAILED` |
| `media.worker/start_processing` | `podcasts` | `status = PROCESSING` |
| `media.worker/processed` | `podcasts` | `audio_url`, `status = PROCESSED` |
| `media.worker/error` | `podcasts` | `status = FAILED` |
| `media.subtitle` | `podcast_transcripts` | `content` |
| `tts.start` | `podcast_transcripts` | `content` |

`podcast_transcripts` использует существующее поле `content`. Отдельные колонки для `vtt_object_key`, `srt_object_key`, `subtitles_ready_at` и `tts_content` в схеме не используются.

## Идемпотентность и порядок событий

Повторная доставка обрабатывается через модель переходов статусов. События не откатывают подкаст в более ранний media-статус. `PUBLISHED` и `ARCHIVED` считаются терминальными для media lifecycle.

## Retry и DLT

Kafka использует `DefaultErrorHandler`, фиксированный backoff и DLT topic `<source>.DLT`. Невалидные DTO, некорректные enum values и бизнес-валидация относятся к неретрайным ошибкам. Временное отсутствие целевой сущности обрабатывается как retryable-сценарий.

## Проверки

| Проверка | Результат |
|---|---|
| `.\gradlew.bat test` | успешно |
| `.\gradlew.bat build` | успешно |
| YAML parse для OpenAPI/application config | успешно |
| `docker compose config --quiet` | успешно |
| `docker compose up -d --build app` | успешно |
| `/podcast/v1/actuator/health` | `UP` |
| Kafka smoke `media.upload` + `media.worker` | подкаст переведён в `PROCESSED`, audio metadata сохранена |
| Kafka smoke `media.subtitle` + `tts.start` | payload сохранён в `podcast_transcripts.content` |

## Факты, требующие уточнения

> Требует уточнения: production значения partitions, replication factor и retention для topics.

> Требует уточнения: контракт одновременного хранения subtitle payload и TTS text для одного `podcast_id` и языка в единственном поле `podcast_transcripts.content`.
