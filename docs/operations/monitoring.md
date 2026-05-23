# Мониторинг

## Ключевые сигналы

| Область | Сигнал |
|---|---|
| HTTP | доля `5xx`, `401`, `403`, latency, размер страниц |
| PostgreSQL | connections, slow queries, locks, размер таблиц |
| Kafka | consumer lag, DLT rate, rebalance count |
| JVM | heap, GC pause, threads |
| Бизнес | количество публикаций, голосов, подписок, ошибок публикации |

## Alerting considerations

| Условие | Влияние |
|---|---|
| health не `UP` | сервис недоступен |
| рост DLT | входящий контракт Kafka нарушен |
| рост `INTERNAL_ERROR` | требуется диагностика приложения |
| высокий consumer lag | задержка создания user profile |
| исчерпание DB connections | деградация HTTP API |
