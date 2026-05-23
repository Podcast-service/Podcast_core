# Локальная разработка

## Требования

- Java 21.
- Docker и Docker Compose.
- Доступный порт `8082` для приложения.
- Доступные порты `5432`, `9092`, `9094`, `8081`, если используется полный compose.

## Запуск через Docker Compose

Локальный `.env` создаётся из шаблона:

```bash
cp .env.example .env
```

Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Compose запускается командой:

```bash
docker compose up --build
```

Поднимаются:

| Сервис | Порт | Назначение |
|---|---:|---|
| `postgres` | `5432` | PostgreSQL |
| `kafka` | `9092`, `9094` | Kafka broker |
| `kafka-ui` | `8081` | UI для Kafka |
| `app` | `8082` | `podcast-core` |

Порты меняются в `.env`: `PODCAST_APP_PORT`, `PODCAST_DB_PORT`, `PODCAST_KAFKA_UI_PORT`, `PODCAST_KAFKA_INTERNAL_PORT`, `PODCAST_KAFKA_EXTERNAL_PORT`.

Реальный `.env` игнорируется Git. В репозитории хранится только безопасный шаблон `.env.example`.

Проверка:

```bash
curl http://localhost:8082/podcast/v1/actuator/health
curl http://localhost:8082/podcast/v1/categories
```

## Запуск через Gradle

Если PostgreSQL и Kafka уже подняты локально:

Windows PowerShell:

```powershell
$env:PODCAST_ACCESS_TOKEN_SECRET = "dev-access-token-secret-change-me"
.\gradlew.bat bootRun --args="--spring.profiles.active=dev"
```

Linux/macOS:

```bash
export PODCAST_ACCESS_TOKEN_SECRET="dev-access-token-secret-change-me"
./gradlew bootRun --args='--spring.profiles.active=dev'
```

## Dev seed

В профиле `dev` включён `DevDataSeeder`. Он добавляет стабильный набор данных:

- пользователей;
- авторов;
- категории;
- опубликованные подкасты;
- черновики, processing, failed и archived подкасты;
- transcript/summary;
- плейлисты;
- подписки;
- голоса;
- историю прослушивания.

Dev seed создаёт порядка 36 пользователей, 14 авторов, 12 категорий, 126 подкастов, 72 плейлиста и сотни связей для голосов, подписок, истории и содержимого плейлистов. Данные включают кириллицу, латиницу, разные даты, пустые optional fields и длинные тексты для проверки frontend-состояний.

Сидер идемпотентный и нужен только для разработки. Отключение:

```bash
PODCAST_DEV_SEED_ENABLED=false
```

## Dev token

Dev token не хранится в Git. Скрипты генерируют JWT и сохраняют результат в `.dev/jwt/`; директория `.dev/` находится в `.gitignore`.

Windows:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-author-token.ps1
```

Linux/macOS:

```bash
./.dev-tools/jwt/generate-author-token.sh
```

Для admin-ручек:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\.dev-tools\jwt\generate-admin-token.ps1
```

```bash
./.dev-tools/jwt/generate-admin-token.sh
```

Важно: secret в скрипте и secret приложения совпадают. Для compose по умолчанию используется `dev-access-token-secret-change-me`.

## Swagger

URL:

```text
http://localhost:8082/podcast/v1/swagger
```

В `Authorize` вставляется строка `Bearer ...` из `.dev/jwt/author-token.txt`.

Если Swagger не открывается, диагностируются следующие условия:

- приложение запущено на `8082`;
- доступен `/openapi/podcast-service-dev.yaml`;
- `PODCAST_SWAGGER_ENABLED=true` включён в local/dev окружении;
- security config разрешает `/swagger`, `/swagger-ui/**`, `/v3/api-docs/**`, `/openapi/**`;
- OpenAPI document снаружи доступен по `/podcast/v1/openapi/podcast-service-dev.yaml`;
- `PODCAST_SWAGGER_OPENAPI_URL` задаётся как `/openapi/podcast-service-dev.yaml`, без повторного `/podcast/v1`.

## Миграции БД

Flyway запускается при старте приложения. Файлы миграций лежат в:

```text
src/main/resources/db/migration
```

Текущие миграции:

- `V1__init_schema.sql`
- `V2__harden_author_profiles.sql`

## Kafka UI

Kafka UI доступен по адресу:

```text
http://localhost:8081
```

Там удобно проверять топики `podcasts.users` и `podcasts.users.DLT`.

## Troubleshooting

| Симптом | Возможная причина | Действие |
|---|---|---|
| `401` в Swagger | Не вставлен Bearer token или secret не совпадает | Генерация нового token с текущим `PODCAST_ACCESS_TOKEN_SECRET` |
| `403` на author/admin ручке | В token нет роли | Сгенерировать token с `author` или `admin` |
| `404` на `/users/me/*` | `user_id` из token отсутствует в `user_profiles` | Использовать dev user id или дождаться Kafka-события от `auth-service` |
| Swagger не открывается | Неверный URL или приложение не запущено | URL Swagger: `http://localhost:8082/podcast/v1/swagger` |
| Kafka event ушёл в DLT | Неверный envelope/payload | Формат события описан в [kafka.md](kafka.md) |
