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

## Локальный доступ

```bash
docker exec -it podcast_core-postgres-1 psql -U podcast_user -d podcast_db
```
