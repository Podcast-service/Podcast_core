# Повторные попытки и DLT

Kafka error handler настроен через `DefaultErrorHandler`.

## Политика повторных попыток

| Параметр | Переменная | Default |
|---|---|---:|
| Задержка | `PODCAST_KAFKA_RETRY_BACKOFF_MS` | `1000` мс |
| Повторные попытки | `PODCAST_KAFKA_RETRY_MAX_ATTEMPTS` | `3` |

## Неретрайные ошибки

| Исключение | Причина |
|---|---|
| `InvalidKafkaMessageException` | payload не соответствует контракту или бизнес-валидации |
| `KafkaDeserializationException` | raw payload не является валидным JSON |
| `KafkaMessageValidationException` | отсутствуют обязательные поля или неизвестный `object_type`/`event` |

Неретрайные ошибки контракта не отправляются в DLT. Consumer пишет один warning-log с topic, partition, offset, key, типом исключения и причиной, после чего offset считается обработанным. Это защищает сервис от бесконечных повторов и лог-спама на сообщениях, которые не относятся к контрактам podcast-service.

## Retryable ошибки

`KafkaRetryableProcessingException` используется для временно недоступных доменных зависимостей, например когда media-событие пришло раньше создания целевой сущности.

## DLT

DLT topic вычисляется как `<sourceTopic><PODCAST_KAFKA_DLT_SUFFIX>`. При default suffix:

| Source topic | DLT topic |
|---|---|
| `podcast.user.register` | `podcast.user.register.DLT` |
| `media.upload` | `media.upload.DLT` |
| `media.worker` | `media.worker.DLT` |
| `media.subtitle` | `media.subtitle.DLT` |
| `tts.start` | `tts.start.DLT` |

Сообщение отправляется в partition исходного record. После успешной отправки в DLT offset исходного сообщения считается обработанным. DLT используется для ошибок обработки, которые не классифицированы как невалидный контракт.

## Poison messages

Сообщения с валидным контрактом, которые стабильно ломают обработку после retry, уходят в DLT. Невалидные сообщения пропускаются после диагностического warning-log. Повторная обработка из DLT выполняется отдельной эксплуатационной процедурой.
