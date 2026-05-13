# calendar-service

NAJA7I scheduling backbone. Lives on port **8103**, owns PostgreSQL `calendar_db`.

## Responsibilities

- PROFs declare **availability rules** (WEEKLY or ONE_OFF).
- A **materializer** generates concrete `slots` over a 60-day window (idempotent).
- Booking-service drives the slot lifecycle through internal S2S endpoints:
  `AVAILABLE → RESERVED_PENDING → BOOKED`, with a 10-minute TTL on RESERVED_PENDING
  enforced by a scheduled sweep.

## Layers

```
ma.naja7i.calendar
├── domain                AvailabilityRule, Slot (@Version), ProcessedEvent, enums
├── application           AvailabilityRuleService, SlotService, SlotMaterializer,
│                         SlotExpiryJob, ProfStatusService
├── infrastructure
│   ├── persistence       Spring Data JPA repositories
│   ├── messaging         KafkaEventPublisher, payload records, consumers
│   ├── security          HeaderAuthFilter, InternalKeyFilter, SecurityConfig
│   └── web               *Controller, dto/, AuthUser + resolver
└── config                AppConfig, CalendarProperties, KafkaConfig, OpenApiConfig, WebConfig
```

## REST endpoints

| Method | Path | Auth | Purpose |
|--------|------|------|---------|
| POST | `/api/v1/calendar/availability-rules` | `ROLE_PROF` | Create a rule |
| GET  | `/api/v1/calendar/availability-rules/me` | `ROLE_PROF` | List own rules |
| PUT  | `/api/v1/calendar/availability-rules/{id}` | `ROLE_PROF` | Update rule |
| DELETE | `/api/v1/calendar/availability-rules/{id}` | `ROLE_PROF` | Soft-delete + block future AVAILABLE slots |
| POST | `/api/v1/calendar/slots:generate` | `ROLE_PROF` | Materialize slots (idempotent) |
| POST | `/api/v1/calendar/slots/{id}:block` | `ROLE_PROF` | AVAILABLE → BLOCKED |
| POST | `/api/v1/calendar/slots/{id}:unblock` | `ROLE_PROF` | BLOCKED → AVAILABLE |
| GET  | `/api/v1/calendar/slots/me` | `ROLE_PROF` | List own slots, paginated (max 200) |
| GET  | `/api/v1/calendar/profs/{profId}/slots` | any | Public catalog (AVAILABLE only) |
| POST | `/api/v1/internal/calendar/slots/{id}:reserve` | `X-Internal-Key` | AVAILABLE → RESERVED_PENDING |
| POST | `/api/v1/internal/calendar/slots/{id}:confirm` | `X-Internal-Key` | RESERVED_PENDING → BOOKED |
| POST | `/api/v1/internal/calendar/slots/{id}:release` | `X-Internal-Key` | → AVAILABLE + event |

Public/PROF routes use the gateway-injected headers `X-User-Id`, `X-User-Role`,
`X-Locale`, `X-Trace-Id`. Internal routes use the static `X-Internal-Key`
(env `NAJA7I_INTERNAL_KEY`, default `dev-internal-key`).

## Kafka

### Produced

| Topic | Key | When |
|-------|-----|------|
| `naja7i.calendar.slot.created.v1` | `profId` | new AVAILABLE slot materialized / unblocked |
| `naja7i.calendar.slot.booked.v1` | `profId` | RESERVED_PENDING → BOOKED |
| `naja7i.calendar.slot.released.v1` | `profId` | release (manual, cancel, TTL expiry) |

### Consumed (idempotent via `processed_events`)

| Topic | Action |
|-------|--------|
| `naja7i.profile.prof.validated.v1` | log only (v1 stub) |
| `naja7i.profile.prof.rejected.v1` | block every future slot of the prof |
| `naja7i.auth.user.suspended.v1` | block every future slot of the user (no-op for non-profs) |

## Properties

```yaml
naja7i.calendar:
  internal-key: ${NAJA7I_INTERNAL_KEY:dev-internal-key}
  ttl-minutes: 10                 # RESERVED_PENDING auto-release after this
  generation-window-days: 60      # materializer horizon
  default-slot-duration-minutes: 60
  default-modality: ONLINE
  expiry-scan-cron: "0 * * * * *" # every minute
```

## Running

```bash
# From backend/
mvn -pl calendar-service -am clean verify

# With Docker
docker build -f calendar-service/Dockerfile -t naja7i/calendar-service:dev .
```

A `disabledWithoutDocker = true` Testcontainers smoke test boots Spring against a
real PostgreSQL container if Docker is available, otherwise it is skipped.
