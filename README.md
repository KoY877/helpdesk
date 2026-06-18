# Helpdesk

A full-stack ticketing / helpdesk application: users open support tickets, agents
work them through a controlled status workflow, and admins manage users and assignments.

- **Backend** — Spring Boot 3.5 (Java 23) REST API with JWT auth, JPA/PostgreSQL.
- **Frontend** — Angular 21 SPA with Angular Material.
- **Database** — PostgreSQL 16.
- **Orchestration** — Docker Compose (Postgres + backend + frontend).

## Architecture

```
Helpdesk/
├── backend/            Spring Boot REST API  (port 8081)
├── frontend/           Angular SPA           (port 4200)
├── docker-compose.yaml Postgres + backend + frontend
├── .env                Local secrets (gitignored — copy from .env.example)
└── .env.example        Template for the required environment variables
```

The frontend talks to the backend over `/api`, the backend persists to PostgreSQL,
and every request after login carries a JWT bearer token.

## Tech stack

| Layer    | Technology                                                        |
|----------|-------------------------------------------------------------------|
| Backend  | Java 23, Spring Boot 3.5, Spring Security, Spring Data JPA, JJWT   |
| Frontend | Angular 21, Angular Material/CDK, RxJS, TypeScript 5.9             |
| Database | PostgreSQL 16                                                      |
| Build    | Maven (backend), npm/Angular CLI (frontend)                       |
| Runtime  | Docker / Docker Compose                                            |

## Getting started

### Prerequisites
- Docker & Docker Compose (recommended path), **or**
- JDK 23 + Maven and Node 20+/npm 10 for running each service locally.

### 1. Configure environment
```bash
cp .env.example .env
```
Then edit `.env` and set real values:

| Variable                | Description                                                   |
|-------------------------|---------------------------------------------------------------|
| `DB_USERNAME`           | PostgreSQL user (also injected into the backend datasource).  |
| `DB_PASSWORD`           | PostgreSQL password.                                          |
| `JWT_SECRET`            | Long, random Base64 secret used to sign JWTs.                |
| `JWT_ACCESS_EXPIRATION` | Access-token lifetime in ms (default `3600000` = 1h).        |

`.env` is gitignored — never commit real secrets.

### 2. Run everything with Docker Compose
```bash
docker compose up --build
```
| Service  | URL                     |
|----------|-------------------------|
| Frontend | http://localhost:4200   |
| Backend  | http://localhost:8081   |
| Postgres | localhost:5432 (`helpdesk_db`) |

### 3. Run services individually (development)
```bash
# Backend
cd backend
./mvnw spring-boot:run

# Frontend
cd frontend
npm install
npm start
```
See [backend/Readme.md](backend/Readme.md) and [frontend/README.md](frontend/README.md)
for service-specific details.

## Domain model

- **User** — has a `Role`: `USER`, `AGENT`, or `ADMIN` (defaults to `USER` on registration).
- **Ticket** — created by a user, optionally assigned to an agent, moves through a status workflow.
- **Comment** — attached to a ticket.

### Ticket status workflow
Transitions are enforced in the `Ticketstatus` enum (`canTransitionTo`) and changed
only via `PATCH /api/tickets/{id}/status`:

```
OPEN → IN_PROGRESS → RESOLVED → CLOSED
                        └──────→ IN_PROGRESS   (reopen)
```

## API overview

All routes are prefixed with `/api`. Authenticated routes require a
`Authorization: Bearer <token>` header.

### Auth — `/api/auth`
| Method | Path        | Description                       |
|--------|-------------|-----------------------------------|
| POST   | `/register` | Register a new user (role = USER).|
| POST   | `/login`    | Authenticate, returns JWT.        |

### Tickets — `/api/tickets`
| Method | Path                 | Description                            |
|--------|----------------------|----------------------------------------|
| GET    | `/all`               | List all tickets.                      |
| GET    | `/`                  | List tickets for the current user.     |
| GET    | `/{id}`              | Get a ticket by id.                    |
| GET    | `/user/{userId}`     | List tickets for a given user.         |
| POST   | `/`                  | Create a ticket.                       |
| PATCH  | `/{id}`              | Update a ticket.                       |
| PATCH  | `/{id}/status`       | Change status (state-machine checked). |
| PATCH  | `/{id}/assign`       | Assign a ticket (ADMIN).               |
| DELETE | `/{id}`              | Delete a ticket.                       |

### Comments
| Method | Path                              | Description              |
|--------|-----------------------------------|--------------------------|
| POST   | `/api/tickets/{ticketId}/comments`| Add a comment to a ticket.|

### Users — `/api/users`
| Method | Path                  | Description                  |
|--------|-----------------------|------------------------------|
| GET    | `/all`                | List users.                  |
| GET    | `/{id}`               | Get a user.                  |
| PATCH  | `/{id}`               | Update a user.               |
| DELETE | `/{id}`               | Delete a user.               |
| PATCH  | `/admin/{id}/role`    | Change a user's role (ADMIN).|

## Testing
```bash
cd backend  && ./mvnw test     # JUnit 5 + Mockito
cd frontend && npm test        # Vitest
```

## License
Private / unpublished.
