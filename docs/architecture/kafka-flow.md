# Поток Kafka-сообщений

## Регистрация пользователя

`auth-service` публикует raw JSON в topic `podcast.user.register`. Микросервис подкастов создаёт или обновляет локальный `user_profiles`.

## Media lifecycle

Файлы через Kafka не передаются. Kafka содержит только метаданные загрузки, обработки, subtitle и TTS-flow.

| Topic | Назначение |
|---|---|
| `media.upload` | загрузка исходного файла, обложки, аватара или плейлиста |
| `media.worker` | обработка аудиофайла подкаста |
| `media.subtitle` | результат генерации субтитров |
| `tts.start` | текст, из которого TTS генерирует аудио |

## Статусы подкаста

| Событие | Поведение |
|---|---|
| `media.upload/start_upload` | `DRAFT` переходит в `UPLOADING` |
| `media.upload/uploaded` | сохраняется `audio_url_file`; статус `UPLOADED` |
| `media.worker/start_processing` | статус `PROCESSING` |
| `media.worker/processed` | сохраняются processed/HLS `audio_url`, `duration_seconds`, `audio_file_size`; статус `PROCESSED` |
| error event | статус `FAILED` |

Переходы статусов выполняются по media lifecycle: `DRAFT -> UPLOADING -> UPLOADED -> PROCESSING -> PROCESSED`. `uploaded` может перевести подкаст из `DRAFT` сразу в `UPLOADED`, если отдельное событие `start_upload` не используется producer-ом. Событие `processed` не переводит подкаст в `PROCESSED`, если перед ним не был зафиксирован статус `PROCESSING`. Повторное событие текущего статуса идемпотентно. `start_upload` после `PROCESSED` не откатывает статус назад. Поздние error-события после `PROCESSED` логируются и не переводят подкаст в `FAILED`. Опубликованные и архивные подкасты не переводятся в media lifecycle статусы.

Для `media.upload` контракт разделён по `object_type`: аудио использует `audio_url_file` и обязательный `event`; изображения используют `image_url`, а отсутствие `event` трактуется как успешная загрузка. Длительность и размер аудиофайла приходят после обработки в `media.worker/processed`. Неконтрактные события не маппятся на ближайший известный тип и не изменяют БД.

## Transcript storage

`media.subtitle` и `tts.start` сохраняют данные в `podcast_transcripts.content`. Для subtitle в `content` сохраняется JSON с ключами `vtt_object_key`, `srt_object_key`, `ready_at`; для TTS сохраняется текст из Kafka payload.
