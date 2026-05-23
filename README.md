# Podcast Core

`Podcast Core` — микросервис доменной части подкаст-платформы. Он предоставляет REST API для подкастов, авторов, пользователей, категорий, плейлистов, голосов, подписок, истории прослушивания и поиска. Сервис хранит данные в PostgreSQL и принимает события пользователей из Kafka от сервиса аутентификации.

## Быстрый старт

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Проверка:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

Swagger UI:

```text
http://localhost:8082/podcast/v1/swagger
```

## Документация

Главная точка входа: [docs/README.md](docs/README.md).

| Раздел | Ссылка |
|---|---|
| Архитектура | [docs/architecture/overview.md](docs/architecture/overview.md) |
| REST API | [docs/api/README.md](docs/api/README.md) |
| Kafka | [docs/kafka/overview.md](docs/kafka/overview.md) |
| Конфигурация | [docs/configuration/environments.md](docs/configuration/environments.md) |
| Локальная разработка | [docs/development/local-setup.md](docs/development/local-setup.md) |
| Эксплуатация | [docs/operations/deployment.md](docs/operations/deployment.md) |
| Frontend/mobile | [docs/frontend/integration.md](docs/frontend/integration.md) |
| QA | [docs/qa/testing-strategy.md](docs/qa/testing-strategy.md) |

## Основные переменные

Все проектные переменные окружения используют префикс `PODCAST_*`. Полный каталог: [docs/configuration/env-vars.md](docs/configuration/env-vars.md).

Для production profile не используется `dev`, а `PODCAST_SWAGGER_ENABLED` и `PODCAST_DEV_SEED_ENABLED` отключаются, если Swagger и seed data не закрыты отдельным инфраструктурным контуром.

## Dev JWT

Локальные токены создаются скриптами из `.dev-tools/jwt/`. Подробности: [docs/development/jwt-dev-tools.md](docs/development/jwt-dev-tools.md).
