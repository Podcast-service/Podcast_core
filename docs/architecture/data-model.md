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

## Индексы поиска

Для поиска используются `tsvector` и trigram indexes. Индексы есть у профилей пользователей, авторов, подкастов и плейлистов. Полнотекстовый поиск реализован в `SearchRepository`.

## Статусы подкастов

| Статус | Значение |
|---|---|
| `DRAFT` | черновик |
| `PROCESSING` | публикационный процесс или обработка медиа |
| `PUBLISHED` | доступен публично |
| `FAILED` | ошибка обработки |
| `ARCHIVED` | архивирован |
