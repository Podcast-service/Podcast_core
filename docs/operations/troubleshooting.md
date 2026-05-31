# Диагностика типовых проблем

| Симптом | Вероятная причина | Проверка |
|---|---|---|
| `401` на защищённой ручке | нет токена, неверный secret, истёкший token | JWT payload и `PODCAST_ACCESS_TOKEN_SECRET` |
| `403` на author/admin операции | отсутствует роль | claim `roles` |
| `404` на `GET /authors/me` | author profile не создан | `POST /authors/me` с обычным пользовательским JWT |
| `502` на `POST /authors/me` | auth-service недоступен или вернул некорректный ответ | `PODCAST_AUTH_SERVICE_BASE_URL`, доступность `/auth/me/update-roles` |
| `409` при создании категории | имя или position уже заняты | таблица `categories` |
| `422` при публикации | нарушено бизнес-правило статуса или медиа | статус подкаста |
| Kafka DLT растёт | нарушен формат события или целевая сущность недоступна после retry | payload в `<source-topic>.DLT` |
| Swagger строит двойной путь | неверный `PODCAST_SWAGGER_OPENAPI_URL` | значение `/openapi/podcast-service-dev.yaml` |
