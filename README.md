# Podcast-Core

## Сборка и запуск
- `docker compose build`
- `docker compose up`

## Swagger
Расположение: http://localhost:8082/swagger

## Авторизация в DEV
В обычном сценарии токен приходит из `auth-service`: логинимся, берём `access_token`, открываем Swagger и вставляем токен в `Authorize` для схемы `bearerAuth`.

Если нужно быстро проверить `podcast-core` отдельно от `auth-service`, можно выпустить локальный dev JWT через готовый скрипт.

Главное правило: `podcast-core` и скрипт должны использовать один и тот же `ACCESS_TOKEN_SECRET`.

При запуске через `docker compose up` dev-секрет уже прокидывается в контейнер по умолчанию: `dev-access-token-secret-change-me`. Если запускаешь сервис напрямую через Gradle или IDE, задай переменную окружения сам.

### Windows PowerShell

Задаём секрет в консоли, из которой запускается сервис:

```powershell
$env:ACCESS_TOKEN_SECRET = "dev-access-token-secret-change-me"
```

Генерируем токен:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-dev-token.ps1
```

Для конкретного пользователя из локальной БД:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-dev-token.ps1 -UserId "<user_profiles.user_id>" -Roles user,author
```

Для админских ручек:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\generate-dev-token.ps1 -UserId "<user_profiles.user_id>" -Roles user,author,admin
```

Если у тебя разрешён запуск локальных PowerShell-скриптов, можно короче: `.\scripts\generate-dev-token.ps1`.

### Linux/macOS

Задаём секрет в консоли, из которой запускается сервис:

```bash
export ACCESS_TOKEN_SECRET="dev-access-token-secret-change-me"
```

Генерируем токен:

```bash
chmod +x ./scripts/generate-dev-token.sh
./scripts/generate-dev-token.sh
```

Для конкретного пользователя из локальной БД:

```bash
./scripts/generate-dev-token.sh --user-id "<user_profiles.user_id>" --roles user,author
```

Для админских ручек:

```bash
./scripts/generate-dev-token.sh --user-id "<user_profiles.user_id>" --roles user,author,admin
```

Скрипт печатает готовый JWT и строку `Bearer ...`. В Swagger открываем `Authorize`, выбираем `bearerAuth` и вставляем токен.

Важно: `user_id` в токене должен существовать в таблице `user_profiles`, если ручка работает с профилем, плейлистами или авторским контентом. Роль `author` нужна для авторских операций, роль `admin` — для управления категориями. Это только локальный dev-инструмент, в проде токены выдаёт `auth-service`.

## Профиль автора
Профиль пользователя создаётся через Kafka-событие от `auth-service`, поэтому перед авторскими ручками в `podcast-core` уже должна быть запись в `user_profiles`.

Рабочий поток такой:

1. Пользователь регистрируется или создаётся в `auth-service`.
2. `auth-service` отправляет пользовательское событие, а `podcast-core` создаёт `user_profiles`.
3. Пользователю выдаётся роль `author`, и новый access token уже содержит эту роль.
4. Фронт вызывает `POST /authors/me` с Bearer token и создаёт запись в `author_profiles`.

`avatarUrl` у автора берётся из профиля пользователя и меняется через `PUT /users/me/profile`. Сам профиль автора хранит `authorName`, `description` и счётчик подписчиков.

## CORS
В dev-режиме сервис по умолчанию принимает браузерные запросы только с:

- `http://localhost:3000`
- `http://localhost:5173`
- `http://localhost:8082`

Для другого фронтенда задай переменную:

```bash
CORS_ALLOWED_ORIGINS="https://frontend.example.com,https://admin.example.com"
```

Wildcard `*` намеренно не используется: API работает с Bearer-токенами, поэтому источники браузерных запросов должны быть явными.
