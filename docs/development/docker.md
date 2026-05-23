# Docker Compose

`docker-compose.yml` поднимает PostgreSQL, Kafka, Kafka UI и приложение.

## Команды

```bash
docker compose up -d --build
docker compose ps
docker compose logs -f app
docker compose down
```

## Переменные портов

| Переменная | Default |
|---|---|
| `PODCAST_APP_PORT` | `8082` |
| `PODCAST_DB_PORT` | `5432` |
| `PODCAST_KAFKA_UI_PORT` | `8081` |
| `PODCAST_KAFKA_INTERNAL_PORT` | `9092` |
| `PODCAST_KAFKA_EXTERNAL_PORT` | `9094` |

## Volumes

PostgreSQL использует volume `postgres_data`. Повторный запуск сохраняет данные и повторно применяет идемпотентный dev seed.
