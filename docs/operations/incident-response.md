# Реакция на инциденты

## Основные сценарии

| Сценарий | Диагностика |
|---|---|
| API отдаёт `5xx` | логи app, health, DB connectivity |
| массовые `401` | issuer, secret, истечение токенов |
| Kafka lag растёт | Kafka UI, consumer logs, DLT |
| Swagger недоступен в dev | `PODCAST_SWAGGER_ENABLED`, swagger config URL |
| seed data отсутствует | profile `dev`, `PODCAST_DEV_SEED_ENABLED` |

## Минимальный набор данных для support

| Данные | Источник |
|---|---|
| HTTP method/path | клиент или access logs |
| timestamp | клиент и server logs |
| user id | JWT claim `user_id` |
| error code | `ApiErrorResponse.code` |
| Kafka topic/partition/offset | Kafka UI или consumer logs |
