# Масштабирование

## HTTP слой

Приложение не хранит HTTP-сессию. Несколько экземпляров могут обслуживать трафик за балансировщиком при общей PostgreSQL БД, общей Kafka consumer group и одинаковой JWT конфигурации.

## PostgreSQL

Основные источники нагрузки:

| Область | Риск | Контроль |
|---|---|---|
| каталог подкастов | сортировка и фильтры | индексы по status, author, category, published_at |
| поиск | полнотекстовые запросы | GIN индексы и ограничение размера страниц |
| плейлисты | порядок элементов | индекс `playlist_id, position` |
| счётчики | частые votes/subscriptions | атомарные операции в сервисном слое |

## Kafka

Масштабирование consumer зависит от количества partition topic `podcasts.users`. Один partition обрабатывается только одним экземпляром consumer group.

## Ограничения текущей реализации

| Область | Текущее состояние |
|---|---|
| Cache | отдельный cache layer отсутствует |
| Rate limit | application-level rate limit отсутствует |
| Distributed tracing | tracing exporter не настроен |
| Метрики | exposed только `health,info`; endpoint metrics не опубликован |

> Требует уточнения: целевые SLO, допустимый размер страниц и стратегия rate limiting для публичных endpoints.
