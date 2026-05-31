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

`podcast_transcripts.content` является основным полем хранения текстового содержимого транскрипта. Kafka-события `media.subtitle` и `tts.start` также пишут результат в это поле: subtitle сохраняется как JSON-содержимое `content`, TTS сохраняется как текстовая строка. `tts.failed` не пишет данные в transcript и отражает ошибку только через статус подкаста. Отдельные колонки для subtitle/TTS payload в схеме не используются.

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
