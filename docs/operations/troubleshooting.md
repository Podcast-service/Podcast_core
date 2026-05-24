# Диагностика типовых проблем

| Симптом | Вероятная причина | Проверка |
|---|---|---|
| `401` на защищённой ручке | нет токена, неверный secret, истёкший token | JWT payload и `PODCAST_ACCESS_TOKEN_SECRET` |
| `403` на author/admin операции | отсутствует роль | claim `roles` |
| `404` на `/authors/me` | author profile не создан | `POST /authors/me` с ролью `author` |
| `409` при создании категории | имя или position уже заняты | таблица `categories` |
| `422` при публикации | нарушено бизнес-правило статуса или медиа | статус подкаста |
| Kafka DLT растёт | нарушен формат события или целевая сущность недоступна после retry | payload в `podcast.user.register.DLT` или `media.DLT` |
| Swagger строит двойной путь | неверный `PODCAST_SWAGGER_OPENAPI_URL` | значение `/openapi/podcast-service-dev.yaml` |
