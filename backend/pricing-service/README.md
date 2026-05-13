# pricing-service

Prof pricing (rates and session packs), platform-fee configuration, market-suggestion cache,
and the booking-time quote engine for NAJA7I.

- Port: **8104**
- Database: PostgreSQL `pricing_db`
- Kafka: **none** — pricing-service is API-only in v1 (no producers, no consumers)
- Architecture: hexagonal-lite (`domain` / `application` / `infrastructure`)
- Java package: `ma.naja7i.pricing`

## Money model

The DB stores every amount as `BIGINT amount_minor` (centimes MAD) to match the
`ma.naja7i.shared.dto.Money` semantics — never `DECIMAL`. The API accepts and returns:

- **Input**: `amountMad` as a decimal string, e.g. `"150.00"`. Parsed with `BigDecimal`,
  rounded HALF_UP at 2 decimals, then converted to `long` centimes.
- **Output**: every monetary field is doubled: `amountMinor` (raw centimes) and
  `amountMad` (string with 2 decimals) so clients can pick either representation.

Conversions live in `infrastructure/web/mapper/MoneyConverter` and are unit-tested via
the `QuoteServiceTest` rounding test.

## Domain

| Table | Purpose |
|---|---|
| `prof_pricing` | Single-session rate per (prof, subject, level, modality, duration, day_of_week?). UNIQUE on that tuple. `day_of_week IS NULL` is the "applies to all days" fallback row. |
| `pricing_packs` | Multi-session bundle with an optional global discount; `total_amount_minor` is the whole pack price. |
| `market_suggestions` | Read-only cache populated by an external AI/batch job. |
| `platform_fees` | Platform commission config; v1 seeds one row `name='STANDARD'` at 10 %. |

All enum columns are `VARCHAR(20)` with a `CHECK` constraint — not Postgres ENUM types.

## REST endpoints

All under `/api/v1/pricing` (auth via gateway headers) and `/api/v1/internal/pricing`
(auth via `X-Internal-Key`).

### PROF (role=PROF)

| Method | Path | Purpose |
|---|---|---|
| GET    | `/api/v1/pricing/me` | List my active rates + packs |
| POST   | `/api/v1/pricing/rates` | Create a rate |
| PUT    | `/api/v1/pricing/rates/{id}` | Update a rate I own |
| DELETE | `/api/v1/pricing/rates/{id}` | Soft delete (`active=false`) |
| POST   | `/api/v1/pricing/packs` | Create a pack |
| PUT    | `/api/v1/pricing/packs/{id}` | Update a pack I own |
| DELETE | `/api/v1/pricing/packs/{id}` | Soft delete |

### Public (any authenticated user)

| Method | Path | Purpose |
|---|---|---|
| GET  | `/api/v1/pricing/profs/{profId}/rates` | List active rates of a prof (filterable by `subjectCode`, `level`, `modality`) |
| GET  | `/api/v1/pricing/profs/{profId}/packs` | List active packs of a prof |
| POST | `/api/v1/pricing/quote` | Compute price for a booking shape (rate or pack + platform fee) |
| GET  | `/api/v1/pricing/profs/{profId}/market-suggestion?subjectCode=&level=&modality=&durationMinutes=` | Latest market suggestion or 404 |

### Internal (booking-service → pricing-service)

Guarded by header `X-Internal-Key` (env `NAJA7I_INTERNAL_KEY`, default `dev-internal-key`).

| Method | Path | Purpose |
|---|---|---|
| POST | `/api/v1/internal/pricing/quote` | Same payload as the public `/quote`, server-to-server only |

## Quote engine (selection rules)

1. If `packId` is supplied, the pack overrides any individual rate. The unit price used is
   `total_amount_minor / sessions_count` (HALF_UP rounding). The pack must belong to the
   prof and match `subject`, `level`, `modality`; otherwise `VALIDATION_FAILED`.
2. Else, find the best matching `prof_pricing` row:
   - exact `(prof, subject, level, modality, duration, dayOfWeek)` if `dayOfWeek` was given;
   - otherwise the row with `day_of_week IS NULL`.
3. If nothing matches → `RESOURCE_NOT_FOUND` with message `PRICING_RATE_NOT_FOUND`
   so booking-service can detect the case explicitly.
4. Platform fee: `platform_fees WHERE name='STANDARD' AND active=true`. If missing,
   falls back to `naja7i.pricing.default-platform-fee-pct` (default `10`).
5. Final math: `fee = round(base * pct / 100, HALF_UP) + fixed_minor`,
   `total = base + fee`. All arithmetic in BIGINT centimes — never `double`/`float`.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `SERVER_PORT` | `8104` | HTTP port |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pricing_db` | PostgreSQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `naja7i` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | `naja7i` | DB password |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | Eureka URL |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config-server URL |
| `NAJA7I_INTERNAL_KEY` | `dev-internal-key` | Shared secret for `X-Internal-Key` |

## Build & run

```bash
# from backend/
mvn -pl pricing-service -am -B clean package
java -jar pricing-service/target/pricing-service.jar
```

Or via the multistage Docker build:

```bash
docker build -f pricing-service/Dockerfile -t naja7i/pricing-service:dev backend/
docker run --rm -p 8104:8104 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/pricing_db \
  -e EUREKA_CLIENT_SERVICEURL_DEFAULTZONE=http://host.docker.internal:8761/eureka/ \
  -e NAJA7I_INTERNAL_KEY=please-replace-with-a-real-internal-key \
  naja7i/pricing-service:dev
```

## Testing

- `QuoteServiceTest` — happy path, pack overrides rate, dayOfWeek fallback, rounding HALF_UP,
  missing rate throws `PRICING_RATE_NOT_FOUND`.
- `PricingServiceTest` — CRUD + ownership rules (foreign-prof updates return 404 to avoid leaking).
- `PricingControllerIT` — Testcontainers PostgreSQL: create rate → list `/me` → quote →
  internal endpoint with/without the shared key.
- `PricingServiceApplicationTests` — context load smoke test (Flyway + JPA validate).

All Testcontainers-based tests use `@Testcontainers(disabledWithoutDocker = true)` so the
build still passes on machines without Docker.

```bash
mvn -pl pricing-service -am -B verify
```

## Observability

- `GET /actuator/health` — liveness/readiness probes
- `GET /actuator/prometheus` — Prometheus metrics
- `GET /actuator/info`
- Swagger UI at `http://localhost:8104/swagger-ui.html`

## Security

- This service does NOT validate JWT signatures — the api-gateway does, then forwards
  `X-User-Id`, `X-User-Role`, `X-Locale`, `X-Trace-Id` headers which `HeaderAuthFilter`
  trusts to build a `UserContext`-keyed Spring Security context.
- PROF-only endpoints are protected by both `@PreAuthorize("hasRole('PROF')")` and a
  belt-and-braces in-controller role check.
- Ownership: rate/pack updates and soft-deletes check `prof_id == authenticated userId`;
  mismatches return 404 (not 403) to avoid leaking the existence of resources owned by others.
- Internal routes (`/api/v1/internal/**`) ignore gateway headers and require the
  `X-Internal-Key` header to match `naja7i.pricing.internal-key`.

## Out of scope (v1)

- Kafka producers/consumers — pricing-service is API-only in v1 by design.
- `market_suggestions` write paths — populated by an external AI batch job.
- Multi-currency — only MAD is supported (mirrors the shared `Currency` enum).
