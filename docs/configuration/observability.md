# Конфигурация observability

## Actuator

Открытые endpoints:

| Endpoint | Назначение |
|---|---|
| `/actuator/health` | общий health |
| `/actuator/info` | информация приложения |
| `/actuator/health/liveness` | liveness probe |
| `/actuator/health/readiness` | readiness probe |

## Логи

Уровни по умолчанию:

| Logger | Уровень |
|---|---|
| `org.flywaydb` | `INFO` |
| `org.springframework.kafka` | `INFO` |

## OpenTelemetry

Трейсы, метрики и логи экспортируются в OTEL-коллектор через OpenTelemetry
Java-агент (`-javaagent`, подключается в `Dockerfile`). Параметры задаются
переменными окружения в `docker-compose.yml`:

| Переменная | Значение по умолчанию |
|---|---|
| `OTEL_SERVICE_NAME` | `podcast_core` |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | `http://otel-collector:4317` |
| `OTEL_EXPORTER_OTLP_PROTOCOL` | `grpc` |
| `OTEL_TRACES_EXPORTER` / `OTEL_METRICS_EXPORTER` / `OTEL_LOGS_EXPORTER` | `otlp` |
