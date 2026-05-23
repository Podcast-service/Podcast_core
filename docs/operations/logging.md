# Логирование

## HTTP

Контроллеры логируют ключевые входящие операции с параметрами запроса и текущим user id, если он доступен.

## Security

`JwtAuthenticationFilter` логирует успешную аутентификацию на `debug` и ошибки JWT на `warn`.

## Ошибки

| Тип | Уровень |
|---|---|
| бизнес-исключения | `warn` |
| validation errors | `warn` |
| security denied | `warn` |
| непредвиденные ошибки | `error` |
| Kafka retry/DLT | `warn` / `error` в зависимости от обработчика |

## Чувствительные данные

JWT, secret values и production credentials не логируются.
