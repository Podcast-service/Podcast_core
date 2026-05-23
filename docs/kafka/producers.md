# Kafka-продюсеры

## Внешний продюсер

`auth-service` публикует событие `user.created` в topic `podcasts.users`. Событие содержит envelope и payload пользователя.

## Внутренний продюсер

В коде присутствует `KafkaEventPublisher` и `KafkaDomainEventProducer`, однако текущие доменные операции подкастов не публикуют события в `podcasts.podcasts`.

## Producer DLT

`DeadLetterPublishingRecoverer` публикует сообщения в topic `<sourceTopic>.DLT` после исчерпания политики повторных попыток или при неретрайных ошибках.
