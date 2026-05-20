# Podcast Core: документация

`podcast-core` — Spring Boot микросервис ядра подкаст-платформы. Он хранит профили пользователей и авторов, категории, подкасты, плейлисты, голоса, подписки, историю прослушивания, transcript/summary и полнотекстовый поиск. Аутентификация выполняется через JWT access token, который выпускает отдельный `auth-service`.

Каноничная API-спецификация лежит в корне проекта: [`openapi-2.yaml`](../openapi-2.yaml). Dev-версия Swagger для ручной проверки лежит в `src/main/resources/static/openapi/podcast-service-dev.yaml` и открывается по адресу `http://localhost:8082/podcast/v1/swagger`.

## Быстрый старт

1. Подними сервис и зависимости:

```bash
docker compose up --build
```

2. Dev JWT генерируется локальным скриптом.

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
```

Linux/macOS:

```bash
./.dev-tools/jwt/generate-author-token.sh
```

3. Файл `.dev/jwt/author-token.txt` содержит значение `Bearer ...` для Swagger `Authorize`.

4. Контрольный health check:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

## Карта документации

| Файл | Для кого | Что внутри |
|---|---|---|
| [architecture.md](architecture.md) | Backend, новые разработчики | Архитектура, слои, поток запроса, зависимости |
| [api/README.md](api/README.md) | Frontend, QA, Backend | Общие правила REST API |
| [api/endpoints.md](api/endpoints.md) | Frontend, QA | Все endpoint'ы, параметры, тела, ответы, авторизация |
| [api/errors.md](api/errors.md) | Frontend, QA | Единый каталог ошибок |
| [api/auth.md](api/auth.md) | Frontend, Backend, QA | JWT, claims, роли и dev-токены |
| [api/models.md](api/models.md) | Frontend, Backend, QA | DTO, доменные сущности, валидации |
| [kafka.md](kafka.md) | Backend, DevOps | Топики, события, consumer, retry и DLT |
| [configuration.md](configuration.md) | Backend, DevOps | Переменные окружения и application config |
| [local-development.md](local-development.md) | Все участники | Локальный запуск, dev seed, Swagger, миграции |
| [testing.md](testing.md) | Backend, QA | Запуск тестов и важные сценарии |
| [observability.md](observability.md) | Backend, DevOps | Логи, health checks, метрики, мониторинг |
| [deployment.md](deployment.md) | DevOps, Backend | Docker, окружения, rollout, production checklist |
| [dev-tools.md](dev-tools.md) | Backend, QA | Dev JWT scripts и локальные утилиты |
| [documentation-audit.md](documentation-audit.md) | Команда | Источники, расхождения и факты, требующие уточнения |

## Основные сценарии

| Сценарий | Основные ручки |
|---|---|
| Профиль пользователя | `GET/PUT /users/me/profile`, `GET/PUT /users/me/settings` |
| Авторский профиль | `POST/GET/PUT /authors/me`, `GET /authors/{authorId}` |
| Каталог подкастов | `GET /podcasts`, `GET /podcasts/{podcastId}`, `GET /categories` |
| Авторские подкасты | `POST /podcasts`, `PUT /podcasts/{podcastId}`, `POST /podcasts/{podcastId}/publish` |
| Плейлисты | `GET/POST /playlists`, `PUT/DELETE /playlists/{playlistId}` |
| Реакции | `POST/DELETE /podcasts/{podcastId}/vote`, `POST/DELETE /playlists/{playlistId}/vote` |
| Подписки и лента | `POST/DELETE /authors/{authorId}/subscribe`, `GET /users/me/subscriptions/feed` |
| История | `POST /podcasts/{podcastId}/progress`, `GET /users/me/history` |
| Поиск | `GET /search`, `GET /search/suggest` |

## Важные правила

- В production токены выдаёт `auth-service`; локальные token scripts используются только в dev-окружении.
- Публичные GET-ручки могут принимать Bearer token опционально. Без токена они возвращают публичные данные, с токеном дополнительно заполняют пользовательские поля вроде `currentUserVote` и `progressPercent`.
- Роли проверяются через Spring Security method security: `hasRole('AUTHOR')`, `hasRole('ADMIN')`, `isAuthenticated()`.
- База данных управляется Flyway-миграциями, Hibernate DDL generation отключён.
- Dev seed включается только в профиле `dev`.

## Dev JWT tooling

Для dev-окружения доступны JWT scripts в `.dev-tools/jwt`. Все токены выпускаются для одного dev-пользователя:

```json
{
  "id": "00000000-0000-0000-0000-000000000001",
  "email": "dev.user@example.local",
  "username": "dev-user"
}
```

| Скрипт | Роли |
|---|---|
| `generate-user-token.*` | `user` |
| `generate-author-token.*` | `user`, `author` |
| `generate-admin-token.*` | `user`, `author`, `admin` |
