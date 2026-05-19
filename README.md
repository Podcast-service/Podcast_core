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
