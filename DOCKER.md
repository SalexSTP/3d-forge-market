# Docker

For local development/demo use:

```bash
cp .env.example .env
docker compose up --build
```

Docker Compose reads `.env` automatically for variable substitution. For
IntelliJ or other local startup, both Spring Boot apps now also load the root
`.env` automatically when started from the repo root or a module directory.

Main app: http://localhost:8080

Custom print Swagger: http://localhost:8081/swagger-ui.html

Stripe Checkout is disabled by default. To test online payments locally, set
`STRIPE_ENABLED=true`, `STRIPE_SECRET_KEY`, `STRIPE_WEBHOOK_SECRET`,
`STRIPE_CHECKOUT_DISPLAY_NAME`, and `APP_BASE_URL` in `.env`.

Stop the stack:

```bash
docker compose down
```

Reset local Docker data:

```bash
docker compose down -v
```
