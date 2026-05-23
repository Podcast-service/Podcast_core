# Метрики

Actuator dependency подключена, но в текущей конфигурации наружу открыты только `health` и `info`.

## Текущее состояние

| Endpoint | Статус |
|---|---|
| `/actuator/health` | открыт |
| `/actuator/info` | открыт |
| `/actuator/metrics` | не открыт |
| `/actuator/prometheus` | не настроен |

> Требует уточнения: целевая система метрик и формат экспорта для production.
