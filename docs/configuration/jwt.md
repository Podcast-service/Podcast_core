# Конфигурация JWT

| Настройка | Значение |
|---|---|
| `PODCAST_AUTH_ENABLED` | включает или отключает JWT validation |
| `PODCAST_ACCESS_TOKEN_SECRET` | HMAC secret |
| `PODCAST_ACCESS_TOKEN_ISSUER` | ожидаемый `iss` |

Production окружение использует secret storage. Dev окружение использует безопасное локальное значение из `.env.example`.

## Совместимость с auth-service

Claims, ожидаемые сервисом:

| Claim | Тип |
|---|---|
| `iss` | string |
| `exp` | number |
| `nbf` | number, опционально |
| `user_id` | uuid string |
| `email` | string |
| `roles` | array string |
