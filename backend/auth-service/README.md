# auth-service

Authentication, OTP verification, JWT issuance and refresh-token rotation for the NAJA7I platform.

- Port: **8101**
- Database: PostgreSQL `auth_db`
- Cache: Redis (OTP codes, rate limits, password-reset tokens)
- Events: Kafka producer (`naja7i.auth.*`)
- Architecture: hexagonal-lite (`domain` / `application` / `infrastructure`)

## Prerequisites

- Java 21
- Maven 3.9+
- Docker (for Postgres, Redis, Kafka, integration tests)
- Running locally: a Postgres `auth_db`, a Redis instance, a Kafka broker, the service-discovery (Eureka) and config-server.

## Run locally

```bash
# from backend/
mvn -pl auth-service -am -B clean package
java -jar auth-service/target/auth-service.jar
```

Or via the multistage Docker build:

```bash
docker build -f auth-service/Dockerfile -t naja7i/auth-service:dev backend/
docker run --rm -p 8101:8101 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/auth_db \
  -e REDIS_HOST=host.docker.internal \
  -e KAFKA_BOOTSTRAP=host.docker.internal:9092 \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e NAJA7I_JWT_SECRET=please-replace-with-a-real-secret-32-chars \
  naja7i/auth-service:dev
```

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8101` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/auth_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `naja7i` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `naja7i` | DB password |
| `REDIS_HOST` | `localhost` | Redis host |
| `REDIS_PORT` | `6379` | Redis port |
| `REDIS_PASSWORD` | _(empty)_ | Redis password |
| `KAFKA_BOOTSTRAP` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | Eureka URL |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config-server URL |
| **`NAJA7I_JWT_SECRET`** | `dev-only-secret-change-me-32+chars-required` | **Must be ≥ 32 chars and identical to api-gateway** |

## cURL samples

### 1. Register

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"phone":"+212600000001","email":"student@example.com","password":"strong-pass-1","role":"STUDENT","locale":"fr"}'
```

### 2. Verify OTP

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/verify-otp \
  -H 'Content-Type: application/json' \
  -d '{"userId":"<UUID returned by /register>","code":"123456"}'
```

### 3. Login

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"phoneOrEmail":"+212600000001","password":"strong-pass-1"}'
```

### 4. Refresh token

```bash
curl -i -X POST http://localhost:8080/api/v1/auth/refresh \
  -H 'Content-Type: application/json' \
  -d '{"refreshToken":"<refreshToken from /login>"}'
```

### Other flows

- `POST /api/v1/auth/resend-otp` (rate limited 3/15min per phone)
- `POST /api/v1/auth/logout` (authenticated)
- `POST /api/v1/auth/password/reset-request`
- `POST /api/v1/auth/password/reset`
- `GET  /api/v1/auth/me` (authenticated)
- `POST /api/v1/auth/admin/users/{id}/suspend` (admin only)

## Events produced

Topic constants live in `ma.naja7i.shared.events.EventTopics`. Payloads are records under
`ma.naja7i.auth.events.payload.*`. Partition key is always `userId.toString()`.

| Topic | Payload class |
|---|---|
| `naja7i.auth.user.registered.v1` | `UserRegisteredPayload(userId, role, email, phone)` |
| `naja7i.auth.otp.requested.v1` | `OtpRequestedPayload(userId, phone, code, channel, expiresAt, locale)` |
| `naja7i.auth.user.logged_in.v1` | `UserLoggedInPayload(userId, ip, userAgent, at)` |
| `naja7i.auth.password.reset_requested.v1` | `PasswordResetRequestedPayload(userId, resetToken, expiresAt)` |
| `naja7i.auth.user.suspended.v1` | `UserSuspendedPayload(userId, reason)` |

## Observability

- `GET /actuator/health` — liveness/readiness probes
- `GET /actuator/prometheus` — Prometheus metrics
- `GET /actuator/info`
- Swagger UI at `http://localhost:8101/swagger-ui.html`

## Security & PII

- **Passwords**: bcrypt (strength 12) via `BCryptPasswordEncoder`.
- **JWT secret**: must match api-gateway (`NAJA7I_JWT_SECRET`).
- **Refresh tokens**: 256-bit random Base64URL, SHA-256-hashed in DB with 14-day TTL.
- **OTP**: 6-digit numeric, bcrypt-hashed in Redis, 5-minute TTL, max 5 attempts.
- **PII protection**: `password_hash` is `@JsonIgnore` on the entity. Application code never logs email or phone in plain text. Update Logback masking filter in production to redact `email=`, `phone=`, and any token-like substrings.
- **Rate limiting**: OTP resend is capped at 3 requests / 15 minutes per phone (Redis `auth:otp:rate:{phone}`).
- **Header trust**: this service does **not** re-validate JWTs. The api-gateway verifies and forwards `X-User-Id`, `X-User-Role`, `X-Locale`. The `HeaderAuthFilter` builds the Spring Security context from these headers so `@PreAuthorize` works.

## Testing

- `AuthServiceTest` — unit tests with Mockito (register / verify-otp / login / suspend edge cases).
- `TokenServiceTest` — token rotation logic.
- `PasswordResetServiceTest` — reset flow.
- `AuthControllerIT` — Testcontainers PG + Redis covering register → verify-otp → login → /me.

```bash
mvn -pl auth-service -am -B test
```

## Database

Flyway migrations live under `src/main/resources/db/migration/`. `V1__init.sql` creates `users`,
`refresh_tokens`, `audit_logs`. `V2__indexes.sql` adds extra indexes. The `social_logins` table is
intentionally deferred to v2 per scope.
