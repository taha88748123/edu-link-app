# NAJA7I Observability — LGTM stack

The NAJA7I platform ships with a self-contained, batteries-included
observability stack based on Grafana's open-source LGTM bundle:

| Signal  | Component                | Endpoint (host)        |
| ------- | ------------------------ | ---------------------- |
| Metrics | Prometheus               | http://localhost:9090  |
| Logs    | Loki (+ Promtail agent)  | http://localhost:3100  |
| Traces  | Tempo (OTLP gRPC + HTTP) | http://localhost:3200  |
| UI      | Grafana                  | http://localhost:3000  |

> Mimir is intentionally **not** part of v1. Prometheus single-binary is
> sufficient for the dev cluster. Swapping to Mimir later is a config-only
> change (re-point Grafana datasource).

## 1. Starting the stack

```bash
# Bring everything up (infra + platform + services + observability)
make up

# Or only the observability profile
make observability-up

# Tail the LGTM containers
make obs-logs
```

To stop:

```bash
make observability-down
# or for the whole cluster:
make down
```

## 2. Logging in to Grafana

Grafana is reachable at <http://localhost:3000>.

- Anonymous **Viewer** access is enabled — no login needed to read dashboards.
- Admin credentials: `admin` / `admin`
- All three datasources (Prometheus, Loki, Tempo) are auto-provisioned with
  the deterministic UIDs `naja7i-prometheus`, `naja7i-loki`, `naja7i-tempo`.

DEV ONLY. For production:
- Disable `[auth.anonymous]` in `infrastructure/observability/grafana/grafana.ini`.
- Override `GF_SECURITY_ADMIN_PASSWORD` via an env-var (do NOT commit secrets).

## 3. Provisioned dashboards

The `NAJA7I` folder in Grafana contains six dashboards (UIDs listed):

| File                | UID                | Focus                                                  |
| ------------------- | ------------------ | ------------------------------------------------------ |
| `01-overview.json`  | `naja7i-overview`  | Services up, RPS, error rate, p95, heap, GC, Kafka lag |
| `02-payment.json`   | `naja7i-payment`   | Escrow lock/release, balance ops, INSUFFICIENT_BALANCE |
| `03-booking.json`   | `naja7i-booking`   | Booking funnel, outbox throughput, dispute logs        |
| `04-gateway.json`   | `naja7i-gateway`   | Route RPS, p99 latency, JWT 401 rate                   |
| `05-kafka.json`     | `naja7i-kafka`     | Consumer lag, throughput per topic, producer errors    |
| `06-jvm.json`       | `naja7i-jvm`       | Heap / non-heap, GC, threads, classes, CPU             |

Dashboards live under
`infrastructure/observability/grafana/dashboards/` — edits in the UI are
preserved between container restarts because `allowUiUpdates: true` is set,
but only the JSON on disk survives a `docker compose down -v`.

## 4. Tracing requests end-to-end

1. Every service is instrumented with `micrometer-tracing-bridge-otel` and
   exports OTLP/HTTP to Tempo at `http://tempo:4318/v1/traces` (override via
   the `OTEL_EXPORTER_OTLP_ENDPOINT` env var).
2. Sampling is **100%** in dev (`management.tracing.sampling.probability=1.0`).
   Drop to ~0.1 in prod.
3. Propagation supports both **W3C traceparent** and **B3** so legacy clients
   still work.
4. Inside the api-gateway, the existing `X-Trace-Id` header is preserved for
   the `GlobalExceptionHandler`. The W3C `traceparent` is propagated
   automatically by Spring Cloud Gateway's WebClient when Micrometer tracing
   is on the classpath — no manual code needed. See the `TODO` in
   `backend/api-gateway/.../JwtAuthFilter.java` regarding aligning both ids.

To explore a trace:
1. Open Grafana → Explore → Tempo datasource.
2. Search by service / operation / duration, or paste a trace-id directly.
3. Click a span to jump to the corresponding Loki logs (auto-linked via
   the `tracesToLogsV2` datasource binding).

## 5. Finding logs by traceId in Loki

Each service uses the shared logback config in `shared-observability` which,
under the `docker` Spring profile, writes structured JSON via the Logstash
encoder. Promtail tails Docker container stdout, parses the JSON, and labels
each line with `service_name`, `level`, and the original `traceId` field.

In Grafana → Explore → Loki:

```
{service_name="payment-service"} | json | traceId="abcd1234..."
```

Or the simpler labelled form when you already know the level:

```
{service_name="booking-service", level="ERROR"}
```

The Loki datasource has a derived-field rule for `traceId` that turns the
value into a clickable link straight to the Tempo trace view.

## 6. Alerts

Prometheus rules are defined in
`infrastructure/observability/prometheus/rules/alerts.yml`:

- **EscrowStuck** — `(locked - released) > 50 for 1h`. **TODO**: counters not
  yet emitted by `payment-service`. Add `@Counter` around lock/release.
- **KafkaConsumerLag** — `kafka_consumer_records_lag_max > 1000 for 5m`.
- **ServiceDown** — `up{job=~"naja7i-.*"} == 0 for 2m`.
- **HighErrorRate** — `rate(http_5xx[5m]) > 0.5 for 5m`.
- **JVMHeapPressure** — `heap > 85% for 10m`.

Alertmanager is **not** deployed in v1; rules evaluate inside Prometheus and
are visible at <http://localhost:9090/alerts>. Wire Alertmanager + a chat /
email integration before going to prod.

## 7. TODO for production hardening

- [ ] Switch Grafana off anonymous-viewer; provision SSO (OIDC).
- [ ] Pin Grafana admin password via secret manager.
- [ ] Add Alertmanager and route alerts to Slack / Email.
- [ ] Reduce trace sampling probability (`0.1` is a sensible default).
- [ ] Replace single-binary Loki / Tempo / Prometheus with HA topology (or
      Mimir / Loki SimpleScalable).
- [ ] Move filesystem storage to object storage (S3 / MinIO).
- [ ] Add a separate retention policy for payment-service logs (e.g. 1y).
- [ ] Add an `EscrowStuck` Counter in `payment-service` to make the alert
      fire on real data.
- [ ] Add SLO panels (availability / error-budget burn) to the overview.
- [ ] Wire `requestId` and `userId` MDC into every controller filter chain.
- [ ] Audit the JSON encoder output to confirm no secret values leak via MDC.
