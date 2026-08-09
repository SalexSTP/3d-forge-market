# 3DForgeMarket

## Short Overview

3DForgeMarket is a Spring Boot 3 marketplace for 3D-printed products and custom print requests.

Guests can browse the public product catalogue. Customers can order products, pay by cash on delivery or Stripe Checkout, review delivered products, and request custom print work. Administrators manage products, users, orders, reviews, custom print offers and fulfillment, invoices, and Excel reports.

The project is split into two independent Spring Boot applications. The main MVC application owns the marketplace UI and business areas such as users, products, orders, reviews, payments, invoices, and reports. A separate REST microservice owns the custom print request lifecycle, and the main app consumes it through OpenFeign.

## Architecture

### ThreeDForgeMarket

- Main Spring MVC and Thymeleaf application.
- Runs on port `8080`.
- Owns users, products, customer orders, reviews, payments, invoices, admin reports, security, and browser UI.
- Uses MySQL database `threed_forge_market_db`.

### custom-print-service

- REST microservice for custom print requests.
- Runs on port `8081`.
- Owns custom print request data, lifecycle transitions, and scheduled maintenance flags.
- Exposes a JSON API and Swagger UI.
- Uses MySQL database `custom_print_service_db`.

The applications can run independently. Each application has its own database. The main app calls the microservice through OpenFeign. Docker Compose can run both apps and both MySQL databases together.

## Technology Stack

### Main App

- Java 17
- Spring Boot 3.4.0
- Maven
- Spring MVC
- Thymeleaf
- Spring Security
- Spring Data JPA
- Spring Validation
- Spring Cache
- OpenFeign
- MySQL
- H2 for tests
- Stripe Java SDK
- Apache PDFBox
- Apache POI
- JaCoCo
- Docker
- dotenv-java
- Lombok

### custom-print-service

- Java 17
- Spring Boot 3.4.0
- Spring Web
- Spring Data JPA
- Spring Validation
- Spring Scheduling
- MySQL
- H2 for tests
- springdoc-openapi Swagger UI
- JaCoCo
- Docker
- dotenv-java
- Lombok

## Main Application Features

### Product Catalogue

- Public home page and public catalogue.
- Search and filtering.
- Product details with image, dimensions, material, color, price, and print-time data.
- Optional public GLB preview/model URL.
- Featured products.
- Caching for catalogue, featured products, details, and admin product lists.

<img width="1919" height="920" alt="3DForgeMarket home page with featured products" src="https://github.com/user-attachments/assets/edbcc2d5-5f80-4f58-9877-2181d490f1d0" />

<img width="1919" height="919" alt="Product catalogue with search and category filtering" src="https://github.com/user-attachments/assets/bb5390c2-1ba5-4a64-83fd-0e2645d33c4e" />

<img width="1628" height="840" alt="Product details page with product information and GLB 3D preview" src="https://github.com/user-attachments/assets/17c7b54d-491d-4c8d-8280-1fb5789ef344" />

### Product Management

- Admin create, edit, hide, show, and delete products.
- Safe delete is allowed only when no order history exists.
- Products with order history are hidden instead of deleted.
- Product writes evict relevant caches.

<img width="1919" height="791" alt="Admin product management page with filters and product actions" src="https://github.com/user-attachments/assets/c8ef2f5d-97ae-43b0-8d59-c0d0ca042dc5" />

### Orders

- Customers place product orders.
- Server-side total calculation in EUR.
- Cash on delivery or Stripe Checkout payment selection.
- Customers can view, cancel, and remove eligible orders.
- Admins update order status through the allowed workflow.
- Payment status is shown separately from order status.

<img width="1339" height="311" alt="Customer order history with order statuses and actions" src="https://github.com/user-attachments/assets/27ce4768-78a2-4b57-9269-bdc4f55e4fb0" />

<img width="1254" height="574" alt="Admin order management page with status update controls" src="https://github.com/user-attachments/assets/17298e20-7419-4afe-b1d0-aec432bd3a83" />

### Reviews

- Customers can review delivered products.
- One review per product/customer.
- Customers can edit or delete their own reviews.
- Admins can moderate reviews.
- Product details show public review data.

<img width="1361" height="780" alt="Product details page showing customer reviews and star ratings" src="https://github.com/user-attachments/assets/0fde946e-e094-4480-85da-3ae32d91ce61" />

<img width="1306" height="364" alt="Admin review moderation page with product reviews and delete actions" src="https://github.com/user-attachments/assets/05144d63-e805-4204-b01f-61dde6b8af25" />

### Profiles and Users

- Users can view and edit their own profile.
- Admins manage users.
- Admin role update.
- Account deactivate/reactivate.
- Last active admin protection.
- Deactivated users cannot log in.

<img width="1072" height="618" alt="Authenticated user profile page with account details" src="https://github.com/user-attachments/assets/177a9d38-e82f-4180-a99b-2c7ad8d38fb1" />

<img width="1040" height="501" alt="Edit profile page with username and email fields" src="https://github.com/user-attachments/assets/3c31d644-11ec-4a86-b1de-fc36cc5d2fc6" />

### Custom Print UI

- Customers create custom print requests.
- Customers edit requests while they are pending review.
- Admins send offers, reject requests, and progress fulfillment.
- Customers accept offers, request changes, cancel requests, and remove eligible history entries.
- Custom print payments use the quoted admin offer price.
- Status and payment information are visible in the customer and admin custom print tables.
- The visible workflow moves from pending review through offer, acceptance, printing, ready for delivery, and delivered.

Additional screenshots for payments, custom prints, reports, and Docker setup can be added after final manual testing.

## Custom Print Service Features

The `custom-print-service` owns the custom print request lifecycle and exposes it as a REST API.

- Create custom print request.
- Edit only while `PENDING_REVIEW`.
- List customer requests.
- List admin requests.
- Send offer.
- Reject request.
- Customer accept offer.
- Customer request changes.
- Customer cancel.
- Customer hide/remove final request.
- Admin archive final request.
- Fulfillment progression:
  - `ACCEPTED` to `PRINTING`
  - `PRINTING` to `READY_FOR_DELIVERY`
  - `READY_FOR_DELIVERY` to `DELIVERED`
- Scheduled maintenance flags:
  - admin attention required
  - customer response reminder required
  - auto archive old final requests from the admin view
- Swagger UI: `http://localhost:8081/swagger-ui.html`

## Payments and Invoices

- Currency is always EUR.
- Cash on delivery is available for product orders and custom print offers.
- Stripe Checkout is available in test mode.
- Stripe payment confirmation is webhook-based.
- The Stripe success redirect does not mark a payment as paid.
- Cancelled Stripe product orders are cleaned up safely.
- Custom print Stripe cancellation leaves the offer waiting.
- The app does not store card data.

Invoice support:

- Stripe payments use Stripe-generated invoice PDF links when Stripe invoice PDF data is available.
- Cash payments use locally generated PDF invoices.
- Customers can download only their own invoices.
- Admins can download invoices for any eligible payment.
- Invoice buttons are shown only when an invoice is legitimately available.
- Cancelled, failed, or unconfirmed Stripe payments do not expose invoice actions.

## Admin Reports

Admins can open `/admin/reports` and export `.xlsx` workbooks:

- Users report.
- Customer orders report.
- Custom print requests report.
- Full admin report with all three sheets.

Reports include useful status and payment information, but avoid password hashes, Stripe session IDs, payment intent IDs, invoice PDF URLs, webhook payloads, card data, and secrets.

## Scheduling, Caching, and Logging

### Scheduling

- `custom-print-service` has a cron job for admin attention and auto-archive maintenance.
- `custom-print-service` has a fixed-delay job for customer response reminders.
- Scheduling values are configurable through application properties.

### Caching

- The main app caches product catalogue data, featured products, product details, and admin product lists.
- Product writes evict the relevant caches.

### Logging

- Both apps log important domain operations.
- Logs avoid passwords, full addresses, full messages, Stripe secrets, card data, and raw webhook/API payloads.

## Security

- Spring Security form login.
- Custom `UserDetails` / `MarketplaceUserDetails`.
- Login by username or email.
- BCrypt password hashing.
- CSRF enabled for Thymeleaf forms.
- `/admin/**` requires `ADMIN`.
- Customer areas such as orders, reviews, custom prints, and payments require authenticated customer access where applicable.
- `/payments/stripe/webhook` is `permitAll` and excluded from CSRF only because Stripe must call it directly.
- Deactivated users cannot log in.
- Admins cannot demote or deactivate the last active admin.
- Users cannot edit other users' profiles.

## Databases

Main app:

- Database: `threed_forge_market_db`

Microservice:

- Database: `custom_print_service_db`

Both apps use MySQL in local and Docker development. Tests use H2. Domain records use UUID primary keys. Real database credentials, admin passwords, and Stripe secrets are provided through environment variables or a local `.env` file.

## Environment Variables

The root `.env.example` file is committed as a safe template. Copy it to `.env` for local development. The `.env` file is ignored by Git and should contain local real values only. Both apps load the root `.env` during local startup, and Docker Compose also reads it.

Example placeholders:

```text
DB_USERNAME=your_mysql_username
DB_PASSWORD=your_mysql_password

MYSQL_ROOT_PASSWORD=root_pass
CUSTOM_PRINT_MYSQL_ROOT_PASSWORD=root_pass

ADMIN_USERNAME=admin
ADMIN_EMAIL=admin@3dforgemarket.local
ADMIN_PASSWORD=your_admin_password

CUSTOM_PRINT_SERVICE_BASE_URL=http://localhost:8081
APP_BASE_URL=http://localhost:8080

STRIPE_ENABLED=false
STRIPE_SECRET_KEY=sk_test_replace_me
STRIPE_WEBHOOK_SECRET=whsec_replace_me
STRIPE_CHECKOUT_DISPLAY_NAME=3DForgeMarket
```

Keep `STRIPE_ENABLED=false` unless actively testing online payments.

## Running Locally Without Docker

1. Install Java 17.
2. Install and start MySQL.
3. Create both databases:
   - `threed_forge_market_db`
   - `custom_print_service_db`
4. Copy `.env.example` to `.env`.
5. Fill local database credentials and admin password.
6. Start `custom-print-service` first.
7. Start `ThreeDForgeMarket` second.

Windows PowerShell:

```powershell
cd custom-print-service
.\mvnw.cmd spring-boot:run
```

```powershell
cd ThreeDForgeMarket
.\mvnw.cmd spring-boot:run
```

macOS/Linux:

```bash
cd custom-print-service
./mvnw spring-boot:run
```

```bash
cd ThreeDForgeMarket
./mvnw spring-boot:run
```

URLs:

- Main app: `http://localhost:8080`
- Microservice Swagger UI: `http://localhost:8081/swagger-ui.html`

## Running With Docker

See `DOCKER.md` for the detailed Docker guide.

Quick start:

```bash
cp .env.example .env
docker compose up --build
```

URLs:

- Main app: `http://localhost:8080`
- Microservice Swagger UI: `http://localhost:8081/swagger-ui.html`

Stop containers:

```bash
docker compose down
```

Reset containers and database volumes:

```bash
docker compose down -v
```

Docker services:

- `main-mysql`
- `custom-print-mysql`
- `custom-print-service`
- `three-d-forge-market`

## Stripe Test-Mode Setup

Stripe is optional for local testing. Cash on delivery works without Stripe.

To test Stripe Checkout:

1. Set `STRIPE_ENABLED=true`.
2. Set `STRIPE_SECRET_KEY` to a Stripe test secret key.
3. Start webhook forwarding with Stripe CLI:

```bash
stripe listen --forward-to localhost:8080/payments/stripe/webhook
```

4. Copy the generated `whsec_...` value into `STRIPE_WEBHOOK_SECRET`.
5. Use Stripe test cards only.

Do not commit Stripe keys. Production/live payments are not configured for this university submission.

## Testing and Coverage

Main app:

```powershell
cd ThreeDForgeMarket
.\mvnw.cmd clean test jacoco:report
```

Microservice:

```powershell
cd custom-print-service
.\mvnw.cmd clean test jacoco:report
```

macOS/Linux uses `./mvnw` instead of `.\mvnw.cmd`.

JaCoCo HTML reports are generated at:

```text
target/site/jacoco/index.html
```

Both apps have unit, integration, and API-style tests. Tests use H2 and do not require MySQL. Custom print service tests disable automatic scheduler execution. Both applications are maintained above 70% line coverage in the latest local verification.

## Main Routes

### Public

| Route | Purpose |
| --- | --- |
| `/` | Home page |
| `/products` | Product catalogue |
| `/products/{id}` | Product details |
| `/auth/login` | Login |
| `/auth/register` | Registration |

### Customer

| Route | Purpose |
| --- | --- |
| `/profile` | Profile |
| `/profile/edit` | Edit profile |
| `/orders` | Customer orders |
| `/orders/create?productId={id}` | Create product order |
| `/reviews/new?productId={id}` | Create product review |
| `/custom-prints` | Customer custom print requests |
| `/custom-prints/new` | Create custom print request |
| `/custom-prints/{id}` | Custom print request details |
| `/custom-prints/{id}/edit` | Edit pending custom print request |
| `/payments/custom-prints/{requestId}` | Pay for custom print offer |
| `/payments/{paymentTransactionId}/invoice` | Customer invoice download |

### Admin

| Route | Purpose |
| --- | --- |
| `/admin/products` | Product management |
| `/admin/orders` | Order management |
| `/admin/reviews` | Review moderation |
| `/admin/users` | User management |
| `/admin/custom-prints` | Custom print administration |
| `/admin/payments/{paymentTransactionId}/invoice` | Admin invoice download |
| `/admin/reports` | Admin report page |
| `/admin/reports/users.xlsx` | Users Excel report |
| `/admin/reports/orders.xlsx` | Orders Excel report |
| `/admin/reports/custom-prints.xlsx` | Custom print requests Excel report |
| `/admin/reports/full.xlsx` | Combined admin Excel report |

### Stripe

| Route | Purpose |
| --- | --- |
| `/payments/stripe/success` | Stripe Checkout success redirect |
| `/payments/stripe/cancel` | Stripe Checkout cancel redirect |
| `/payments/stripe/webhook` | Stripe webhook endpoint |

## REST Microservice API Overview

Base path:

```text
/api/custom-print-requests
```

Customer-facing endpoints:

- `POST /api/custom-print-requests`
- `GET /api/custom-print-requests/customer/{customerId}`
- `GET /api/custom-print-requests/customer/{customerId}/{requestId}`
- `PUT /api/custom-print-requests/customer/{customerId}/{requestId}`
- `PUT /api/custom-print-requests/customer/{customerId}/{requestId}/accept`
- `PUT /api/custom-print-requests/customer/{customerId}/{requestId}/request-changes`
- `PUT /api/custom-print-requests/customer/{customerId}/{requestId}/cancel`
- `PUT /api/custom-print-requests/customer/{customerId}/{requestId}/hide`

Admin-facing endpoints:

- `GET /api/custom-print-requests`
- `GET /api/custom-print-requests/{requestId}`
- `PUT /api/custom-print-requests/{requestId}/offer`
- `PUT /api/custom-print-requests/{requestId}/reject`
- `PUT /api/custom-print-requests/{requestId}/fulfillment-status`
- `PUT /api/custom-print-requests/{requestId}/archive`

Swagger UI:

```text
http://localhost:8081/swagger-ui.html
```

## Project Structure

```text
3DForgeMarket/
├── ThreeDForgeMarket/
├── custom-print-service/
├── docker-compose.yml
├── DOCKER.md
├── .env.example
└── README.md
```

- `ThreeDForgeMarket/` - main Spring MVC marketplace application.
- `custom-print-service/` - REST microservice for custom print requests.
- `docker-compose.yml` - local multi-container setup for both apps and both databases.
- `DOCKER.md` - Docker setup and troubleshooting notes.
- `.env.example` - safe environment variable template.
- `README.md` - project documentation.

## Notes About Secrets and Local Files

- Do not commit `.env`.
- Do not commit database passwords, admin passwords, Stripe keys, webhook secrets, tokens, or private files.
- Do not export or log password hashes, card data, raw Stripe payloads, or webhook bodies.
- Product image/model URLs should point only to public preview assets.
- Customer reference files for custom print requests should be treated as user-provided data.

## Future Improvements

- Real production Stripe/live mode setup.
- Cloud deployment.
- Email or in-app notifications.
- Richer custom print quote and payment lifecycle.
- Cloud file upload API for customer reference files.
- More admin analytics.
