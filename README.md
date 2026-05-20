# Podcast Core

`podcast-core` — микросервис ядра подкаст-платформы на Java 21 и Spring Boot. Он отвечает за профили пользователей и авторов, категории, подкасты, плейлисты, голоса, подписки, историю прослушивания, transcript/summary и поиск.

Полная документация лежит в [docs/README.md](docs/README.md). Каноничная OpenAPI-спецификация проекта — [openapi-2.yaml](openapi-2.yaml). Swagger для dev-проверки доступен после запуска по адресу `http://localhost:8082/swagger`.

## Быстрый старт

1. Создай локальный `.env` из шаблона:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

1. Запусти сервис и инфраструктуру:

```bash
docker compose up --build
```

1. Проверь, что приложение живо:

```bash
curl http://localhost:8082/actuator/health
curl http://localhost:8082/categories
```

1. Открой Swagger:

```text
http://localhost:8082/swagger
```

## Dev token для Swagger

При профиле `dev` сервис добавляет тестовые данные. Дефолтный токен из скриптов уже совпадает с dev-пользователем:

- `user_id`: `550e8400-e29b-41d4-a716-446655440000`
- роли по умолчанию: `user,author`

Windows PowerShell:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-dev-token.ps1
```

Linux/macOS:

```bash
./scripts/generate-dev-token.sh
```

Скрипт сохранит результат в `.dev/dev-token.txt`. В Swagger нажми `Authorize` и вставь строку `Bearer ...`.

Для admin-ручек:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-dev-token.ps1 -Roles user,author,admin
```

```bash
./scripts/generate-dev-token.sh --roles user,author,admin
```

`.dev/` игнорируется Git, поэтому локальный токен не должен попасть в репозиторий.

## Конфигурация через `.env`

Шаблон переменных лежит в [.env.example](.env.example). Docker Compose автоматически читает файл `.env`, если он находится в корне проекта.

Реальный `.env` добавлен в `.gitignore`. В репозиторий должен попадать только `.env.example`.

Самые важные переменные:

| Переменная | Для чего |
|---|---|
| `SPRING_PROFILES_ACTIVE` | Для локальной разработки обычно `docker,dev`; на сервере чаще `docker` без `dev` |
| `ACCESS_TOKEN_SECRET` | Секрет проверки JWT. В production брать только из secret storage |
| `ACCESS_TOKEN_ISSUER` | Issuer токена от `auth-service` |
| `CORS_ALLOWED_ORIGINS` | Разрешённые frontend origins через запятую |
| `POSTGRES_PASSWORD` | Пароль PostgreSQL |
| `KAFKA_TOPIC_USERS` | Топик, куда `auth-service` публикует `user.created` |
| `KAFKA_EXTERNAL_HOST` | Host/IP для подключения к Kafka снаружи Docker-сети |
| `DEV_SEED_ENABLED` | Включает тестовые данные в `dev` profile |

Для сервера обязательно замени dev-секреты и пароли в `.env`, убери `dev` из `SPRING_PROFILES_ACTIVE`, проверь `CORS_ALLOWED_ORIGINS` и `KAFKA_EXTERNAL_HOST`.

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
