# LifeLink - Smart Blood Donation Network

> Final year capstone project. A full-stack blood donation platform built with
> Java 21 + Spring Boot (REST API) and HTML/CSS/JS + Bootstrap 5 (frontend).

## Tech Stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| Backend    | Java 21, Spring Boot 3.5, Spring Security, JWT     |
| Data       | Spring Data JPA, Hibernate, Flyway migrations      |
| Database   | MySQL 8 (local)                                   |
| Frontend   | HTML5, CSS3, JavaScript, Bootstrap 5               |
| Build      | Maven (wrapper included)                          |
| Notify     | In-app notifications + email (SMTP, console fallback) |

## Prerequisites

- JDK 21 or newer (JDK 25 verified)
- MySQL 8 installed and running locally
- A browser with internet access for CDN assets (Bootstrap 5, jsPDF)

## Quick Start

### 1. Create the local database

Run these once against your local MySQL server (e.g. `mysql -u root -p`):

```sql
CREATE DATABASE lifesaver CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'lifelink'@'localhost' IDENTIFIED BY 'lifelink123';
GRANT ALL PRIVILEGES ON lifesaver.* TO 'lifelink'@'localhost';
FLUSH PRIVILEGES;
```

Tables are created automatically by Flyway on the first backend startup.

### 2. Run the backend

```bash
./mvnw spring-boot:run
```

On first run the Maven wrapper downloads Maven + dependencies, Flyway creates
the schema, and the app seeds an administrator.

The backend API runs at **http://localhost:8080**.

### 3. Run the frontend

The frontend is a separate static app served from its own dev server:

```bash
npx serve ../frontend -l 3000
```

Open **http://localhost:3000** in your browser. It talks to the backend at
http://localhost:8080 (CORS is already enabled).

### 4. Use the app

| Page            | URL                          | Access         |
|-----------------|------------------------------|----------------|
| Landing         | `/`                          | Public         |
| Login           | `/login.html`                | Public         |
| Register        | `/register.html`             | Public         |
| Home dashboard  | `/home.html`                 | Any logged in  |
| Search donors   | `/search.html`               | Any logged in  |
| Profile         | `/profile.html`              | Any logged in  |
| My requests     | `/request.html`              | Any logged in  |
| History         | `/history.html`              | Any logged in  |
| Admin           | `/admin.html`                | ADMIN only     |

**Seeded admin:** `admin@lifelink.com` / `Admin@123`

Register a donor account and a patient account to exercise the full flow:
register → search → request → donor accepts → patient confirms → donation
completes → history is written.

## Features

- JWT authentication (remember-me = 7 day tokens), BCrypt passwords
- Role-based access: DONOR, PATIENT, ADMIN
- Registration with browser geolocation (manual fallback)
- Nearby donor search using the Haversine formula (15 km radius for alerts)
- Blood request lifecycle: PENDING → ACCEPTED → COMPLETED (also REJECTED/CANCELLED)
- In-app notifications with a live bell + unread counter (15 s polling)
- Email notifications (request created, accepted, completed, password reset)
- Permanent donation history for donors and patients
- Profile editing with photo upload, availability toggle, password change
- Last-donation >90 day reminder banner
- History/admin tables: search, status filter, pagination, Export PDF (jsPDF)
- Dark mode, toasts, loading spinners, responsive red/white theme

## Configuration

All settings live in `src/main/resources/application.properties` and can be
overridden by environment variables.

| Property                  | Purpose                                          | Default      |
|---------------------------|--------------------------------------------------|--------------|
| `APP_JWT_SECRET`          | HS256 signing secret (base64, ≥32 bytes)         | see file     |
| `APP_EMAIL_ENABLED`       | `true` sends real email, `false` logs to console | `false`      |
| `SPRING_MAIL_*`           | SMTP credentials (e.g. Gmail app password)       | gmail prefill|
| `APP_FRONTEND_URL`        | Used in password-reset email links               | localhost:8080 |

Example for real email with Gmail:

```bash
APP_EMAIL_ENABLED=true \
SPRING_MAIL_USERNAME=you@gmail.com \
SPRING_MAIL_PASSWORD=your-app-password \
./mvnw spring-boot:run
```

## API Overview

Public: `POST /api/auth/register`, `POST /api/auth/login`,
`POST /api/auth/forgot-password`, `POST /api/auth/reset-password`.

Authenticated (Bearer token):
`GET|PUT /api/user/profile`, `PUT /api/user/password`,
`PUT /api/user/availability`, `POST /api/user/photo`,
`GET /api/donors`, `GET /api/search`,
`POST /api/requests`, `GET /api/requests/mine`, `GET /api/requests/donor`,
`PUT /api/requests/{id}/accept|reject|complete|cancel`,
`GET /api/notifications`, `GET /api/notifications/unread-count`,
`PUT /api/notifications/{id}/read`, `PUT /api/notifications/read-all`,
`POST /api/notifications/{id}/accept`, `POST /api/notifications/{id}/reject`,
`GET /api/history`.

Admin only: `GET /api/admin/stats|users|requests|history`.

## Project Layout

```
src/main/java/com/lifelink
├── config      Security, CORS, email, data initializer
├── controller  REST controllers
├── dto         Request/response records
├── entity      JPA entities
├── exception   Global exception handler + typed exceptions
├── repository  Spring Data repositories
├── security    JWT service + auth filter
├── service     Business logic
└── util        Haversine, email templates, constants

src/main/resources
├── application.properties
├── db/migration/V1__init.sql   Flyway schema
└── static                      Frontend (HTML/CSS/JS/images)
```

## Testing

```bash
./mvnw test
```

Covers the Haversine distance utility, request lifecycle rules, and search
ranking logic.

## Notes for production

- Replace `app.jwt.secret` with a new random secret (e.g. `openssl rand -base64 48`).
- Change the seeded admin password and set `app.email.enabled=true`.
- The app serves static files from the same origin; put a reverse proxy
  (Nginx) in front for HTTPS.
