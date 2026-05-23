# Трейсинг

Distributed tracing exporter в текущей конфигурации отсутствует.

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
