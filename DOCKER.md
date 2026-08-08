# Docker

For local development/demo use:

```bash
cp .env.example .env
docker compose up --build
```

Main app: http://localhost:8080

Custom print Swagger: http://localhost:8081/swagger-ui.html

Stop the stack:

```bash
docker compose down
```

Reset local Docker data:

```bash
docker compose down -v
```
