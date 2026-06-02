# Документация микросервиса подкастов

Микросервис подкастов отвечает за пользовательские профили, авторские профили, категории, подкасты, медиа-данные подкастов, плейлисты, голоса, подписки, историю прослушивания и поиск. Сервис предоставляет HTTP API для web/mobile клиентов, принимает события из Kafka от сервиса аутентификации и хранит данные в PostgreSQL.

## Быстрый старт

```powershell
Copy-Item .env.example .env
docker compose up -d --build
```

Проверка сервиса:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

Swagger UI для локальной разработки:

```text
http://localhost:8082/podcast/v1/swagger
```

## Навигация

| Раздел | Назначение |
|---|---|
| [Архитектура](architecture/overview.md) | зоны ответственности, слои, потоки запросов и данных |
| [REST API](api/README.md) | HTTP контракт, авторизация, ошибки, модели, примеры |
| [Kafka](kafka/overview.md) | топики, consumer, retry, DLT и формат событий |
| [Recommendation events](recommendation-events.md) | outbox envelope, payload contracts, rollout и recovery |
| [Конфигурация](configuration/environments.md) | окружения, переменные, БД, Kafka, JWT и observability |
| [Разработка](development/local-setup.md) | локальный запуск, Docker, seed data, JWT tooling, отладка |
| [Эксплуатация](operations/deployment.md) | развёртывание, healthcheck endpoints, мониторинг, логи и восстановление |
| [Интеграция frontend/mobile](frontend/integration.md) | auth flow, пагинация, обработка ошибок, примеры |
| [QA](qa/testing-strategy.md) | стратегия тестирования, тестовые данные, edge cases |

## Основные возможности

| Возможность | API область |
|---|---|
| Профиль пользователя и настройки | `/users/me/profile`, `/users/me/settings` |
| Авторский профиль | `/authors`, `/authors/me`, `/authors/me/podcasts`, `/authors/{authorId}` |
| Каталог подкастов | `/podcasts`, `/podcasts/{podcastId}` |
| Плейлисты | `/playlists`, `/playlists/{playlistId}/save`, `/users/me/playlists`, `/users/me/library/playlists`, `/authors/{authorId}/playlists` |
| Голоса | `/podcasts/{podcastId}/vote`, `/playlists/{playlistId}/vote` |
| Подписки | `/authors/{authorId}/subscribe`, `/users/me/subscriptions` |
| История прослушивания | `/users/me/history`, `/podcasts/{podcastId}/progress` |
| Поиск | `/search`, `/search/suggest` |

## Контракты и окружение

Каноничный OpenAPI-файл находится в корне проекта: [`../openapi-2.yaml`](../openapi-2.yaml). Dev-спецификация для Swagger UI публикуется из `src/main/resources/static/openapi/podcast-service-dev.yaml`.

Все проектные переменные окружения используют префикс `PODCAST_*`. Нативные переменные контейнеров PostgreSQL и Kafka остаются внутри `docker-compose.yml`, но их значения формируются из `PODCAST_*`.

## Локальные JWT

Для локальной проверки защищённых ручек используются скрипты в `.dev-tools/jwt/`. Основной dev-пользователь:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

Подробности: [development/jwt-dev-tools.md](development/jwt-dev-tools.md).
