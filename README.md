# Spring Boot JWT REST API

A small, production-style REST API that shows how I build secure back ends with Spring Boot:
JWT authentication, role-based access control, validated CRUD endpoints, versioned database
migrations, consistent error responses and interactive OpenAPI documentation.

The domain is deliberately simple (a product catalogue) so the structure is easy to read and
easy to adapt to another project.

## Features

- **JWT authentication** – register and login endpoints issue signed HS256 access tokens
- **Role-based access** – `USER` can read, `ADMIN` can create, update and delete
- **CRUD with paging, sorting and search** – `GET /api/v1/products?q=key&page=0&size=20&sort=price,desc`
- **Input validation** – field-level error messages on every request body
- **Consistent errors** – every failure is returned as RFC 9457 `application/problem+json`
- **Database migrations** – schema managed by Flyway, validated by Hibernate at startup
- **Swagger UI** – try every endpoint from the browser, including the bearer token flow
- **Integration tests** – the full HTTP flow is tested end to end with MockMvc
- **Docker** – one command to run the API with PostgreSQL

## Tech stack

Java 17 · Spring Boot 4 · Spring Security (OAuth2 resource server) · Spring Data JPA ·
Flyway · PostgreSQL / H2 · springdoc-openapi · JUnit 5 · Maven · Docker

## Quick start

Requires JDK 17 or newer. No database setup is needed: the default profile uses an in-memory H2 database.

```bash
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=choose-a-password ./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
$env:ADMIN_EMAIL = "admin@example.com"; $env:ADMIN_PASSWORD = "choose-a-password"; .\mvnw.cmd spring-boot:run
```

Then open <http://localhost:8080/swagger-ui.html>.

### Run with PostgreSQL (Docker)

```bash
cp .env.example .env    # then edit the values
docker compose up --build
```

## Endpoints

| Method | Path                    | Access        | Description                              |
|--------|-------------------------|---------------|------------------------------------------|
| POST   | `/api/v1/auth/register` | Public        | Create a user account, returns a token   |
| POST   | `/api/v1/auth/login`    | Public        | Exchange email and password for a token  |
| GET    | `/api/v1/products`      | Authenticated | List products (paging, sorting, search)  |
| GET    | `/api/v1/products/{id}` | Authenticated | Get one product                          |
| POST   | `/api/v1/products`      | ADMIN         | Create a product                         |
| PUT    | `/api/v1/products/{id}` | ADMIN         | Replace a product                        |
| DELETE | `/api/v1/products/{id}` | ADMIN         | Delete a product                         |

## Example

```bash
# 1. Log in
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "admin@example.com", "password": "choose-a-password"}'
# {"accessToken":"eyJhbGciOiJIUzI1NiJ9...","tokenType":"Bearer","expiresIn":3600}

# 2. Call a protected endpoint
curl -X POST http://localhost:8080/api/v1/products \
  -H "Authorization: Bearer <accessToken>" \
  -H "Content-Type: application/json" \
  -d '{"name": "Mechanical Keyboard", "description": "Hot-swappable", "price": 79.90, "stock": 25}'
```

Errors share one format:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Request validation failed",
  "instance": "/api/v1/auth/register",
  "errors": {
    "email": "must be a well-formed email address",
    "password": "size must be between 8 and 72"
  }
}
```

## Configuration

All settings come from environment variables, so no secret is stored in the repository.

| Variable                 | Default                                     | Purpose                                                        |
|--------------------------|---------------------------------------------|----------------------------------------------------------------|
| `JWT_SECRET`             | random key generated at startup             | HS256 signing key, at least 32 characters                      |
| `JWT_EXPIRATION`         | `1h`                                        | Access token lifetime                                          |
| `ADMIN_EMAIL`            | –                                           | With `ADMIN_PASSWORD`, creates the first admin account         |
| `ADMIN_PASSWORD`         | –                                           |                                                                |
| `SPRING_PROFILES_ACTIVE` | –                                           | Set to `postgres` to use PostgreSQL instead of H2              |
| `DB_URL`                 | `jdbc:postgresql://localhost:5432/productdb` | Used by the `postgres` profile                                 |
| `DB_USERNAME`            | `postgres`                                  |                                                                |
| `DB_PASSWORD`            | –                                           |                                                                |

## Project structure

```
src/main/java/com/demo/productapi
├── auth      register / login, token issuing
├── config    security filter chain, JWT beans, OpenAPI, admin seeding
├── error     exception types and the global problem+json handler
├── product   entity, repository, service, controller, DTOs
└── user      user entity, role, repository
src/main/resources/db/migration   Flyway SQL migrations
```

## Security notes

- Passwords are hashed with BCrypt and never returned by the API.
- Tokens are verified on every request for signature, expiry and issuer; the API is stateless.
- Login failures return the same message whether the email exists or not.
- Paths are denied by default: anything not explicitly opened requires authentication.

## Tests

```bash
./mvnw verify
```
