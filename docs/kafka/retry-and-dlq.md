# Повторные попытки и DLT

Kafka error handler настроен через `DefaultErrorHandler`.

## Политика повторных попыток

| Параметр | Значение |
|---|---:|
| Задержка | `1000` мс |
| Повторные попытки | `3` |

## Неретрайные ошибки

| Исключение | Причина |
|---|---|
| `InvalidKafkaMessageException` | envelope или payload не соответствует контракту |
| `KafkaDeserializationException` | payload не преобразуется в DTO |
| `KafkaMessageValidationException` | нарушение правил валидации сообщения |
| `IllegalArgumentException` | некорректный enum или аргумент |

## DLT

DLT topic вычисляется как `<sourceTopic>.DLT`. Для `podcasts.users` это `podcasts.users.DLT`. Сообщение отправляется в partition исходного record.

## Poison messages

Сообщения, которые стабильно ломают обработку, уходят в DLT. Повторная обработка из DLT выполняется отдельной эксплуатационной процедурой.
