# Podcast Core

`podcast-core` — микросервис ядра подкаст-платформы на Java 21 и Spring Boot. Он отвечает за профили пользователей и авторов, категории, подкасты, плейлисты, голоса, подписки, историю прослушивания, transcript/summary и поиск.

Полная документация лежит в [docs/README.md](docs/README.md). Каноничная OpenAPI-спецификация проекта — [openapi-2.yaml](openapi-2.yaml). Swagger для dev-проверки доступен после запуска по адресу `http://localhost:8082/podcast/v1/swagger`.

## Быстрый старт

1. Локальный `.env` создаётся из шаблона:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

1. Сервис и инфраструктура запускаются командой:

```bash
docker compose up --build
```

1. Контрольный health check:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

1. Swagger доступен по адресу:

```text
http://localhost:8082/podcast/v1/swagger
```

## Dev token для Swagger

При профиле `dev` сервис добавляет тестовые данные. Дефолтный токен из скриптов уже совпадает с dev-пользователем:

- `user_id`: `00000000-0000-0000-0000-000000000001`
- `email`: `dev.user@example.local`
- `username`: `dev-user`
- роли по умолчанию: `user,author`

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
```

Linux/macOS:

```bash
./.dev-tools/jwt/generate-author-token.sh
```

Скрипт сохранит результат в `.dev/jwt/author-token.txt`. В Swagger `Authorize` используется строка `Bearer ...`.

Для admin-ручек:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-admin-token.ps1
```

```bash
./.dev-tools/jwt/generate-admin-token.sh
```

`.dev/` игнорируется Git; локальные токены остаются вне репозитория.

## Конфигурация через `.env`

Шаблон переменных лежит в [.env.example](.env.example). Docker Compose автоматически читает файл `.env`, если он находится в корне проекта.

Реальный `.env` добавлен в `.gitignore`. В репозитории хранится только `.env.example`.

Самые важные переменные:

| Переменная | Для чего |
|---|---|
| `PODCAST_SPRING_PROFILES_ACTIVE` | Для локальной разработки обычно `docker,dev`; на сервере чаще `docker` без `dev` |
| `PODCAST_ACCESS_TOKEN_SECRET` | Секрет проверки JWT. В production брать только из secret storage |
| `PODCAST_ACCESS_TOKEN_ISSUER` | Issuer токена от `auth-service` |
| `PODCAST_CORS_ALLOWED_ORIGINS` | Разрешённые frontend origins через запятую |
| `PODCAST_DB_PASSWORD` | Пароль PostgreSQL |
| `PODCAST_KAFKA_TOPIC_USERS` | Топик, куда `auth-service` публикует `user.created` |
| `PODCAST_KAFKA_EXTERNAL_HOST` | Host/IP для подключения к Kafka снаружи Docker-сети |
| `PODCAST_DEV_SEED_ENABLED` | Включает тестовые данные в `dev` profile |
| `PODCAST_SWAGGER_ENABLED` | Включает Swagger UI/OpenAPI endpoints для local/dev |

Для серверного окружения используются production-секреты и пароли в `.env`, профиль `docker` без `dev`, production origins в `PODCAST_CORS_ALLOWED_ORIGINS`, внешний адрес Kafka в `PODCAST_KAFKA_EXTERNAL_HOST` и `PODCAST_SWAGGER_ENABLED=false`, если Swagger не публикуется наружу.

Подробно все переменные описаны в [docs/configuration.md](docs/configuration.md).

## Основные ссылки

- Архитектура: [docs/architecture.md](docs/architecture.md)
- REST API: [docs/api/endpoints.md](docs/api/endpoints.md)
- Модели: [docs/api/models.md](docs/api/models.md)
- Ошибки: [docs/api/errors.md](docs/api/errors.md)
- Kafka: [docs/kafka.md](docs/kafka.md)
- Локальная разработка: [docs/local-development.md](docs/local-development.md)
- Deployment: [docs/deployment.md](docs/deployment.md)

## Тесты

```bash
./gradlew test
```

Windows:

```powershell
.\gradlew.bat test
```
