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
| `tts.failed` | ошибка TTS-flow |

## Статусы подкаста

| Событие | Поведение |
|---|---|
| `media.upload/start_upload` | `DRAFT` переходит в `UPLOADING` |
| `media.upload/uploaded` | сохраняется `audio_url_file`; статус `UPLOADED` |
| `media.worker/start_processing` | статус `PROCESSING` |
| `media.worker/processed` | сохраняется processed/HLS `audio_url`; статус `PROCESSED` |
| error event | статус `FAILED` |

`start_upload` после `PROCESSED` не откатывает статус назад. `processed` после `processed` безопасен. Опубликованные и архивные подкасты не переводятся в media lifecycle статусы.

Для `media.upload` контракт разделён по `object_type`: аудио использует `audio_url_file` и обязательный `event`; изображения используют `image_url`, а отсутствие `event` трактуется как успешная загрузка. Длительность и размер аудиофайла приходят после обработки в `media.worker/processed`.

## Transcript storage

`media.subtitle` и `tts.start` сохраняют данные в `podcast_transcripts.content`. Для subtitle Podcast Core при обработке Kafka-события читает `.vtt`, очищает subtitle-разметку и сохраняет JSON-массив блоков `text`/`voice`, последовательно объединяя соседние cue одного спикера. Абсолютный HTTP(S) URL читается напрямую, относительный object key разрешается через `PODCAST_SUBTITLE_STORAGE_BASE_URL`. Поэтому `GET /podcasts/{podcastId}/transcript` и summary generation используют сохранённые данные без повторного обращения к subtitle storage. Для TTS сохраняется строка, JSON-объект или JSON-массив из Kafka payload. `tts.start` также переводит подкаст в `UPLOADING`, а `tts.failed` переводит подкаст в `FAILED` через общий media lifecycle.
