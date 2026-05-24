# Стратегия тестирования

## Уровни

| Уровень | Область |
|---|---|
| Unit | сервисные бизнес-правила, маппинг, validation |
| Integration | Spring context, repository, security, Kafka handler |
| Contract | соответствие `openapi-2.yaml` и контроллеров |
| E2E | Docker Compose, PostgreSQL, Kafka, Swagger |

## Приоритетные сценарии

| Область | Сценарии |
|---|---|
| Auth | `401`, `403`, роли `user/author/admin` |
| Каталог | пагинация, фильтры, сортировки |
| Владение | чужой подкаст или плейлист возвращает `403` |
| Плейлисты | private/public доступ, reorder |
| Kafka | валидный `podcast.user.register`, media-события, невалидный payload, DLT |
| Seed | повторный запуск не ломает данные |
