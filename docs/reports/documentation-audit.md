# Аудит документации

## Использованные источники

| Источник | Область |
|---|---|
| `openapi-2.yaml` | REST API, DTO, статусы, security scheme |
| исходный код | контроллеры, сервисы, security, Kafka, ошибки |
| `application*.yaml` | runtime configuration |
| `docker-compose.yml` | local infrastructure |
| Flyway migrations | модель данных и ограничения |
| `.env.example` | переменные окружения |

## Структура

Документация разделена на архитектуру, API, Kafka, конфигурацию, разработку, эксплуатацию, frontend, QA и отчёты. Старые одиночные документы заменены новой структурой или совместимыми ссылками.

## Факты, требующие уточнения

| Область | Формулировка |
|---|---|
| Ограничение частоты запросов | Требует уточнения: production-политика на уровне gateway |
| Метрики | Требует уточнения: целевой exporter и набор dashboard |
| Трейсинг | Требует уточнения: стандарт correlation id |
| Kafka | Требует уточнения: production partitions, replication factor и retention |
| Восстановление | Требует уточнения: RPO/RTO |
