# Модель данных

База данных управляется Flyway migrations из `src/main/resources/db/migration`.

## Основные таблицы

| Таблица | Назначение |
|---|---|
| `user_profiles` | локальные профили пользователей из `auth-service` |
| `author_profiles` | авторские профили, один к одному с `user_profiles` |
| `categories` | справочник категорий |
| `podcasts` | выпуски подкастов и их публикационный статус |
| `podcast_transcripts` | транскрипты выпусков по языкам |
| `podcast_summaries` | краткие содержания выпусков по языкам |
| `playlists` | плейлисты пользователей |
| `playlist_podcasts` | элементы плейлиста и их порядок |
| `subscriptions` | подписки пользователя на автора |
| `podcast_votes` | голоса за подкасты |
| `playlist_votes` | голоса за плейлисты |
| `listen_history` | прогресс прослушивания |
| `outbox_events` | таблица outbox pattern для асинхронной публикации событий |

## Ключевые связи

| Связь | Ограничение |
|---|---|
| `author_profiles.user_profile_id` -> `user_profiles.id` | уникальная связь один к одному |
| `podcasts.author_id` -> `author_profiles.id` | автор подкаста |
| `podcasts.category_id` -> `categories.id` | категория опциональна |
| `playlist_podcasts.playlist_id` -> `playlists.id` | каскадное удаление |
| `playlist_podcasts.podcast_id` -> `podcasts.id` | связь с выпуском |
| `subscriptions` | составной primary key: subscriber + author |
| `podcast_votes` | один голос пользователя на подкаст |
| `playlist_votes` | один голос пользователя на плейлист |
| `listen_history` | одна запись прогресса пользователя на подкаст |

## Ограничения таблицы `podcasts`

| Поле | Ограничение |
|---|---|
| `title` | непустая строка |
| `duration_seconds` | `NULL` или значение `>= 0` |
| `status` | `DRAFT`, `UPLOADING`, `UPLOADED`, `PROCESSING`, `PROCESSED`, `PUBLISHED`, `FAILED`, `ARCHIVED` |
| `views_count`, `likes_count`, `dislikes_count` | значения `>= 0` |
| `audio_url_file` | `NULL` или непустая строка |
| `audio_size_file` | `NULL` или значение `>= 0` |
| `num_speakers` | обязательное значение в диапазоне `1..32` |

Для `PUBLISHED` подкаста БД и сервисный слой требуют непустой `audio_url` и положительный `duration_seconds`.

## Индексы поиска

Для поиска используются `tsvector` и trigram indexes. Индексы есть у профилей пользователей, авторов, подкастов и плейлистов. Полнотекстовый поиск реализован в `SearchRepository`.

## Данные транскриптов

`podcast_transcripts.content` является основным полем хранения текстового содержимого транскрипта. Kafka-события `media.subtitle` и `tts.start` также пишут результат в это поле: subtitle сохраняется как исходное JSON-содержимое `content`, TTS сохраняет `content` как строку либо компактную JSON-строку для объекта или массива. `tts.failed` не пишет данные в transcript и отражает ошибку только через статус подкаста. Отдельные колонки для subtitle/TTS payload в схеме не используются.

`podcast_summaries` хранит производный артефакт от transcript. Генерация через OpenRouter запускается только при `PODCAST_OPENROUTER_ENABLED=true`: автоматически after commit после сохранения transcript или вручную через `POST /podcasts/{podcastId}/summary/generate`. Если transcript хранит subtitle pointer JSON (`srt_object_key`/`vtt_object_key`), сервис сначала читает object из storage, чистит SRT/VTT таймкоды и только затем строит prompt. Ошибка чтения storage или генерации summary не откатывает сохранение transcript и не блокирует Kafka consumer.

## Outbox events

`outbox_events` хранит контракт outbox pattern: тип агрегата, идентификатор агрегата, тип события, версию события, ключ, JSON payload/headers, статус отправки, retry metadata, timestamps и `processing_started_at` lease для recovery зависших отправок. Таблица не связана foreign key с доменными таблицами намеренно: outbox должен позволять фиксировать события разных агрегатов без изменения текущей бизнес-логики и без зависимости от доступности Kafka.

Java layer для таблицы расположен в `podcastService.infrastructure.outbox`. Он сериализует `DomainEventEnvelope` в JSON, сохраняет запись со статусом `NEW` и содержит выключенный по умолчанию publisher для отправки `NEW`/`FAILED` событий в Kafka.

Контракты recommendation events и factory-классы расположены в `podcastService.infrastructure.outbox.recommendation`. Все recommendation runtime paths защищены feature flags: запись событий требует `PODCAST_RECOMMENDATION_EVENTS_ENABLED=true`, публикация требует `PODCAST_OUTBOX_PUBLISHER_ENABLED=true` и `PODCAST_KAFKA_PRODUCER_ENABLED=true`.

## Статусы подкастов

| Статус | Значение |
|---|---|
| `DRAFT` | черновик |
| `UPLOADING` | исходный аудиофайл загружается |
| `UPLOADED` | исходный аудиофайл загружен |
| `PROCESSING` | аудиофайл обрабатывается media worker |
| `PROCESSED` | обработанный аудиопоток готов к публикации |
| `PUBLISHED` | доступен публично |
| `FAILED` | ошибка загрузки или обработки |
| `ARCHIVED` | архивирован |
