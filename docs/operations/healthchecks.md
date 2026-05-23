# Healthchecks

## Служебные HTTP пути

| Endpoint | Назначение |
|---|---|
| `/podcast/v1/actuator/health` | общий health |
| `/podcast/v1/actuator/health/liveness` | liveness probe |
| `/podcast/v1/actuator/health/readiness` | readiness probe |
| `/podcast/v1/actuator/info` | информация приложения |

## Пример

```bash
curl http://localhost:8082/podcast/v1/actuator/health
```

Ответ:

```json
{
  "status": "UP",
  "groups": ["liveness", "readiness"]
}
```
