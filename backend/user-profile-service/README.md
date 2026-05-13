# user-profile-service

Spring Boot 3 microservice managing user profiles, prof onboarding (subjects, diplomas, modalities, locations), student profiles, reviews and review aggregates.

* **Port**: `8102`
* **Database**: PostgreSQL `profile_db` (migrations via Flyway)
* **Object storage**: MinIO bucket `naja7i-profiles` (auto-created at startup)
* **Bus**: Kafka — produces `profile.*`, consumes `auth.user.registered`

## Prerequisites

* JDK 21 + Maven 3.9
* PostgreSQL 16 (`profile_db`) reachable
* Kafka 3.6 + Zookeeper (or KRaft) reachable
* MinIO instance reachable
* Spring Cloud Config server + Eureka (optional in dev — falls back gracefully)

## Run

```bash
# from backend/
mvn -pl user-profile-service -am -B clean package
java -jar user-profile-service/target/user-profile-service.jar
```

Or via Docker:

```bash
# from backend/
docker build -f user-profile-service/Dockerfile -t naja7i/user-profile-service:dev .
docker run -p 8102:8102 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://host.docker.internal:5432/profile_db \
  -e SPRING_KAFKA_BOOTSTRAP_SERVERS=host.docker.internal:9092 \
  -e MINIO_ENDPOINT=http://host.docker.internal:9000 \
  naja7i/user-profile-service:dev
```

## Environment variables

| Variable | Default | Purpose |
|----------|---------|---------|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/profile_db` | DB JDBC URL |
| `SPRING_DATASOURCE_USERNAME` | `profile_user` | DB user |
| `SPRING_DATASOURCE_PASSWORD` | `profile_pwd` | DB password |
| `SPRING_KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Kafka bootstrap |
| `MINIO_ENDPOINT` | `http://localhost:9000` | MinIO endpoint |
| `MINIO_ACCESS_KEY` | `minioadmin` | MinIO access key |
| `MINIO_SECRET_KEY` | `minioadmin` | MinIO secret key |
| `MINIO_BUCKET` | `naja7i-profiles` | Bucket name |
| `EUREKA_CLIENT_SERVICEURL_DEFAULTZONE` | `http://localhost:8761/eureka/` | Eureka URL |
| `SPRING_CLOUD_CONFIG_URI` | `http://localhost:8888` | Config server URI |

## Events

### Produces

| Topic | Trigger | Payload class |
|-------|---------|---------------|
| `naja7i.profile.prof.submitted.v1` | first `/profile/me/prof` upsert | `ProfSubmittedPayload` |
| `naja7i.profile.prof.validated.v1` | admin validate | `ProfValidatedPayload` |
| `naja7i.profile.prof.rejected.v1` | admin reject | `ProfRejectedPayload` |
| `naja7i.profile.review.created.v1` | review created | `ReviewCreatedPayload` |

### Consumes

| Topic | Action |
|-------|--------|
| `naja7i.auth.user.registered.v1` | Creates an empty `profiles` row with the right `role` and `locale`. |

## Endpoints (REST `/api/v1/`)

> All endpoints require `Authorization: Bearer <JWT>`, validated by the api-gateway, which injects `X-User-Id`, `X-User-Role`, `X-Locale` headers consumed by `HeaderAuthFilter`.

### Self-service (`/profile/`)

| Verb | Path | Role |
|------|------|------|
| GET | `/profile/me` | any |
| PUT | `/profile/me` | any |
| POST | `/profile/me/photo` (multipart) | any |
| PUT | `/profile/me/prof` | PROF |
| POST | `/profile/me/prof/diplomas` (multipart) | PROF |
| DELETE | `/profile/me/prof/diplomas/{id}` | PROF |
| PUT | `/profile/me/student` | STUDENT |
| POST | `/profile/me/reviews` | STUDENT |

### Public catalog (`/profiles/`)

| Verb | Path | Role |
|------|------|------|
| GET | `/profiles/profs` | any |
| GET | `/profiles/profs/{id}` | any |
| GET | `/profiles/profs/{id}/reviews` | any |

### Admin (`/admin/profs/`)

| Verb | Path | Role |
|------|------|------|
| GET | `/admin/profs/pending` | ADMIN |
| POST | `/admin/profs/{id}/validate` | ADMIN |
| POST | `/admin/profs/{id}/reject` | ADMIN |
| POST | `/admin/profs/{id}/suspend` | ADMIN |

Swagger UI: `http://localhost:8102/swagger-ui.html`
Prometheus: `http://localhost:8102/actuator/prometheus`

## cURL samples

```bash
TOKEN=...   # from auth-service /login

# 1) PROF completes profile (subjects, modalities, years of experience)
curl -X PUT http://localhost:8080/api/v1/profile/me/prof \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "yearsExperience": 5,
    "introVideoUrl": "https://example.com/intro.mp4",
    "subjects": [
      {"code": "MATH",  "level": "LYCEE"},
      {"code": "PHYS",  "level": "LYCEE"}
    ],
    "modalities": ["ONLINE", "DOMICILE"],
    "locations":  [{"city": "Casablanca", "region": "Casablanca-Settat"}]
  }'

# 2) PROF uploads a diploma PDF
curl -X POST http://localhost:8080/api/v1/profile/me/prof/diplomas \
  -H "Authorization: Bearer $TOKEN" \
  -F "file=@/path/to/diploma.pdf" \
  -F "title=Licence de Mathematiques" \
  -F "institution=FSR Rabat" \
  -F "year=2020"

# 3) ADMIN validates a prof
curl -X POST http://localhost:8080/api/v1/admin/profs/<PROF_UUID>/validate \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"notes": "Entretien OK"}'

# 4) STUDENT publishes a review
curl -X POST http://localhost:8080/api/v1/profile/me/reviews \
  -H "Authorization: Bearer $STUDENT_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "bookingId": "11111111-1111-1111-1111-111111111111",
    "profId":    "22222222-2222-2222-2222-222222222222",
    "stars": 5,
    "comment": "Excellent prof, tres pedagogue."
  }'

# 5) Public — search validated profs
curl "http://localhost:8080/api/v1/profiles/profs?subject=MATH&level=LYCEE&modality=ONLINE&minStars=4&page=0&size=20" \
  -H "Authorization: Bearer $TOKEN"
```

## v1 limitations / TODOs

* The QCM transition (`EN_TEST_IA` / `EN_ENTRETIEN_ADMIN`) is owned by `ai-agent-service`; the admin
  endpoints here only handle `EN_ATTENTE → VALIDE|REJETE` and `* → SUSPENDU`.
* Reviews are accepted without verifying a `CONFIRMEE` booking — booking-service will gate this later
  (see TODOs in `ProfReviewService` / `ReviewController`).
* Phone/email are stored in `auth-service` and intentionally NOT included in the public prof view;
  the "unmasking on accepted booking" logic will be wired when booking-service exists.
