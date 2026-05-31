# Трейсинг

Distributed tracing включён через OpenTelemetry Java-агент (`-javaagent` в
[Dockerfile](../../Dockerfile)). Агент авто-инструментирует HTTP, JPA и Kafka и
экспортирует трейсы, метрики и логи в OTEL-коллектор по OTLP/gRPC. Настройка —
через переменные `OTEL_*` в [docker-compose.yml](../../docker-compose.yml):
endpoint по умолчанию `http://otel-collector:4317`, сервис `podcast_core`.

## Текущие идентификаторы

HTTP request id или correlation id отдельным фильтром не создаётся. Kafka envelope содержит `occurredAt`, но не содержит обязательный `correlationId`.

> Требует уточнения: стандарт correlation id между frontend, auth-service и podcast-core.

## Рекомендуемый production-контур

| Область | Подход |
|---|---|
| HTTP | входящий correlation id из gateway |
| Kafka | propagation correlation id в headers или envelope |
| Логи | включение correlation id в MDC |
| Трейсы | OpenTelemetry exporter |
