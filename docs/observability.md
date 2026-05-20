# Observability

## Логи

Сервис использует SLF4J (`@Slf4j`). Контроллеры логируют входящие операции на `INFO`:

- HTTP method и path;
- ID текущего пользователя, если есть;
- path/query параметры;
- флаги изменённых полей;
- длины чувствительных строк вместо самих значений, где это важно.

Ошибки логируются централизованно:

| Тип | Level | Где |
|---|---|---|
| Domain/base exception | `WARN` | `GlobalExceptionHandler` |
| Validation/type mismatch/malformed JSON | `WARN` | `GlobalExceptionHandler` |
| Access denied на method security | `WARN` | `GlobalExceptionHandler` |
| Data integrity violation | `WARN` | `GlobalExceptionHandler` |
| Unexpected exception | `ERROR` | `GlobalExceptionHandler` |
| Kafka retry/DLT | Level определяется `KafkaExceptionLogger` | `KafkaExceptionLogger` |

## Что не логировать

- JWT access token.
- `ACCESS_TOKEN_SECRET`.
- Пароли БД.
- Полные sensitive payloads.
- PII сверх необходимого минимума.

Текущие контроллеры не логируют token и secret. Для пользовательского ввода чаще логируются id, enum, длины и признаки изменений.

## Health endpoints

Actuator включён:

```text
GET /actuator/health
GET /actuator/info
```

Включены probes:

```yaml
management:
  endpoint:
    health:
      probes:
        enabled: true
```

Возможные endpoints Spring Boot Actuator при probes:

```text
GET /actuator/health/liveness
GET /actuator/health/readiness
```

> Требует уточнения: фактическая доступность `liveness/readiness` в целевой версии Spring Boot 4 runtime.

## Метрики

Actuator dependency подключена, но в `management.endpoints.web.exposure.include` открыты только `health,info`. `/actuator/metrics` сейчас не exposed.

Production profile с открытыми метриками обычно использует:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
```

Prometheus export зависит от выбранного metrics registry.

## Трейсинг

В текущем проекте не обнаружена настройка distributed tracing.

> Требует уточнения: целевой стек tracing, например OpenTelemetry, Micrometer Tracing, Zipkin или Tempo.

## Что мониторить в production

| Метрика/сигнал | Почему важно |
|---|---|
| HTTP 5xx rate | Ранний сигнал деградации |
| HTTP 401/403 rate | Проблемы интеграции с auth-service или ролями |
| Latency p95/p99 по endpoint'ам | UX и нагрузка на БД |
| PostgreSQL connection pool | Риск исчерпания подключений |
| Flyway migration status | Безопасность rollout |
| Kafka consumer lag `podcasts.users` | Задержка создания user profile |
| DLT message count | Невалидные события или баги consumer |
| JVM memory/GC | Производительность |
| Thread pool saturation | Риск таймаутов |
| DB slow queries по search/feed | Самые вероятные тяжёлые места |

## Корреляция запросов

В текущем коде не обнаружен request id/correlation id filter.

Типовая схема correlation id:

- принимать `X-Request-Id`;
- генерировать UUID, если заголовок отсутствует;
- класть request id в MDC;
- возвращать `X-Request-Id` в ответе;
- пробрасывать correlation id в Kafka headers.
