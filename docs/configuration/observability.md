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

Метрики и tracing exporters в текущей конфигурации не включены.
