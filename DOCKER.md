# Docker

For local development/demo use:

```bash
cp .env.example .env
docker compose up --build
```

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
