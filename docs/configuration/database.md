# Конфигурация базы данных

## PostgreSQL

Сервис использует PostgreSQL и Spring Data JPA. Схема управляется Flyway.

| Настройка | Значение |
|---|---|
| JDBC URL | `PODCAST_DB_URL` |
| Пользователь | `PODCAST_DB_USER` |
| Пароль | `PODCAST_DB_PASSWORD` |
| Миграции | `classpath:db/migration` |
| Hibernate DDL | `none` |
| Open Session in View | `false` |

## Миграции

| Файл | Назначение |
|---|---|
| `V1__init_schema.sql` | базовая схема, индексы, триггеры |
| `V2__harden_author_profiles.sql` | усиление author profiles |
| `V3__add_podcast_audio_file_and_speakers.sql` | поля исходного аудиофайла и числа спикеров |
| `V4__add_podcast_upload_statuses.sql` | промежуточные статусы загрузки подкаста |
| `V5__final_media_lifecycle_contracts.sql` | финализация media lifecycle статусов |
| `V6__require_podcast_duration_for_publication.sql` | требование duration для публикации |
| `V7__add_saved_playlists.sql` | сохранённые плейлисты пользователей |
| `V8__create_outbox_events.sql` | таблица `outbox_events` для будущего outbox pattern |

## Outbox events

`outbox_events` добавлена только как схема хранения для будущей публикации событий. На текущем этапе Podcast Core не пишет outbox-события, не публикует их в Kafka и не включает новую runtime-функциональность.

## Локальный доступ

```bash
docker exec -it podcast_core-postgres-1 psql -U podcast_user -d podcast_db
```
