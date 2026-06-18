# Helpdesk – Backend

REST API for a ticket management (helpdesk) system, built with **Spring Boot 3.5** and **Java 23**. It handles JWT authentication, users, tickets and their comments, with role-based access control.

## Tech Stack

| Component         | Technology                           |
|-------------------|--------------------------------------|
| Language          | Java 23                              |
| Framework         | Spring Boot 3.5.14                   |
| Security          | Spring Security + JWT (jjwt 0.12.6) |
| Persistence       | Spring Data JPA / Hibernate          |
| Database          | PostgreSQL                           |
| Build             | Maven (wrapper included)             |
| Tooling           | Lombok, Spring Boot DevTools, spring-dotenv |

## Architecture

```
src/main/java/com/helpdesk/backend
├── config                  # SecurityConfig, JwtProperties
├── controller              # REST endpoints (Auth, User, Admin, Ticket, Comment)
├── Data_Transfert_Object   # DTOs (records) + Mappers
├── exception               # Business exceptions + GlobalExceptionHandler
├── model                   # JPA entities (User, Ticket, Comment, RefreshToken)
│   └── enums               # Role, Ticketstatus
├── repository              # Spring Data repositories
├── security                # JwtService, JwtAuthFilter, UserDetailsServiceImpl
└── service                 # Business logic
```

Conventions applied: the service layer never returns a `ResponseEntity`, entities are never exposed directly (always via a DTO record), and exceptions are centralized in `GlobalExceptionHandler` (`@RestControllerAdvice`).

## Prerequisites

- JDK 23
- PostgreSQL (a reachable database)
- Maven (or use the wrapper `./mvnw`)

## Configuration

Configuration is done through environment variables (loaded by `spring-dotenv`). Copy the example files:

```bash
cp .env-example .env
cp src/main/resources/application-example.properties src/main/resources/application.properties
```

Then fill in the variables:

| Variable                  | Description                                   | Example                              |
|---------------------------|-----------------------------------------------|--------------------------------------|
| `DB_URL`                  | PostgreSQL JDBC URL                           | `jdbc:postgresql://localhost:5432/helpdesk_db` |
| `DB_USERNAME`             | Database user                                 | `postgres`                           |
| `DB_PASSWORD`             | Database password                             | `secret`                             |
| `SERVER_PORT`             | HTTP server port                              | `8080`                               |
| `JWT_SECRET`              | JWT secret key (long, random)                 | `a-long-random-string`               |
| `JWT_ACCESS_EXPIRATION`   | Access token lifetime (ms)                    | `900000` (15 min)                    |
| `JWT_REFRESH_EXPIRATION`  | Refresh token lifetime (ms)                   | `604800000` (7 days)                 |
| `ALLOWED_ORIGINS`         | Allowed CORS origins                          | `http://localhost:8081`              |

## Running the Application

```bash
# Start in development mode
./mvnw spring-boot:run

# On Windows
mvnw.cmd spring-boot:run
```

The API is then available at `http://localhost:<SERVER_PORT>`.

### Build

```bash
./mvnw clean package
java -jar target/backend-0.0.1-SNAPSHOT.jar
```

## Tests

```bash
./mvnw test
```

Every service method has JUnit 5 tests (Mockito), covering both the happy path and the error case.

## Data Model

- **User** — `id`, `name`, `email` (unique), `password`, `role`, `order`, timestamps. Implements `UserDetails`.
- **Ticket** — `id`, `title`, `description`, `order`, `status`, `createdBy`, `assignedTo`, timestamps.
- **Comment** — a comment attached to a ticket and its author.

### Roles

`USER`, `AGENT`, `ADMIN` — the default role on registration is `USER` (not settable by the client).

### Ticket Lifecycle

The status is validated exclusively in the `Ticketstatus` enum via `canTransitionTo()`:

```
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
                ▲             │
                └─────────────┘
```

- `OPEN` → `IN_PROGRESS`
- `IN_PROGRESS` → `RESOLVED`
- `RESOLVED` → `CLOSED` or `IN_PROGRESS`
- `CLOSED` → (final state)

Status changes are performed only via `PATCH /api/tickets/{id}/status`.

## Main Endpoints

### Authentication — `/api/auth`

| Method | Endpoint     | Description                          | Access  |
|--------|--------------|--------------------------------------|---------|
| POST   | `/register`  | Register a new user                  | Public  |
| POST   | `/login`     | Log in, returns JWT tokens           | Public  |

### Users — `/api/users`

| Method | Endpoint              | Description              | Access |
|--------|-----------------------|--------------------------|--------|
| GET    | `/all`                | List all users           |        |
| GET    | `/{id}`               | Get a single user        |        |
| PATCH  | `/{id}`               | Partial update           |        |
| DELETE | `/{id}`               | Delete a user            |        |
| PATCH  | `/admin/{id}/role`    | Update a user's role     | ADMIN  |

### Tickets — `/api/tickets`

| Method | Endpoint          | Description                            | Access              |
|--------|-------------------|----------------------------------------|---------------------|
| GET    | `/all`            | All tickets                            |                     |
| GET    | `/`               | Tickets visible to the current user    | USER, AGENT, ADMIN  |
| GET    | `/{id}`           | Get a single ticket                    |                     |
| GET    | `/user/{userId}`  | Tickets of a given user                |                     |
| POST   | `/`               | Create a ticket                        | USER, AGENT, ADMIN  |
| PATCH  | `/{id}`           | Update a ticket                        |                     |
| PATCH  | `/{id}/status`    | Change status (validated transition)   | USER, AGENT, ADMIN  |
| PATCH  | `/{id}/assign`    | Assign a ticket                        | ADMIN               |
| DELETE | `/{id}`           | Delete a ticket                        |                     |

### Comments

| Method | Endpoint                          | Description           | Access              |
|--------|-----------------------------------|-----------------------|---------------------|
| POST   | `/api/tickets/{ticketId}/comments`| Comment on a ticket   | USER, AGENT, ADMIN  |

## Security

- **JWT** authentication (access token + refresh token) via the `Authorization: Bearer <token>` header.
- Role-based authorization (`@PreAuthorize`).
- Passwords and other personal data (email, name) are never logged nor returned in responses.