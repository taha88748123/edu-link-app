# Schéma de données global

Principe : **database-per-service**. Chaque microservice possède son propre schéma PostgreSQL (ou collection Mongo). Les relations cross-service se font par identifiants partagés (`userId`, `bookingId`, etc.) — **jamais de FK cross-DB**. La cohérence est assurée par les événements Kafka.

Migrations gérées avec **Flyway** (par service, dossier `db/migration`).

---

## 1. `auth-service` — PostgreSQL `auth_db`

```sql
users (
  id UUID PK,
  email VARCHAR(255) UNIQUE,
  phone VARCHAR(20) UNIQUE,
  password_hash VARCHAR(255),
  role ENUM('ADMIN','PROF','STUDENT','PARENT'),
  status ENUM('PENDING_OTP','ACTIVE','SUSPENDED','DELETED'),
  email_verified BOOLEAN,
  phone_verified BOOLEAN,
  two_fa_enabled BOOLEAN,
  created_at, updated_at, last_login_at
)

refresh_tokens (id, user_id FK, token_hash, expires_at, revoked_at)
otp_codes (id, user_id FK, code_hash, channel ENUM('SMS','WA'), expires_at, attempts)
social_logins (id, user_id FK, provider ENUM('GOOGLE','APPLE'), provider_uid)
audit_logs (id, user_id FK, action, ip, user_agent, created_at)
```

---

## 2. `user-profile-service` — PostgreSQL `profile_db`

```sql
profiles (
  user_id UUID PK,                       -- même UUID que auth.users.id
  role ENUM('PROF','STUDENT'),
  display_name, photo_url,
  bio TEXT,
  preferred_language ENUM('FR','AR','EN'),
  notification_prefs JSONB,              -- {push,sms,wa,email}
  created_at, updated_at
)

prof_profiles (
  user_id PK FK→profiles,
  status ENUM('EN_ATTENTE','EN_TEST_IA','EN_ENTRETIEN_ADMIN','VALIDÉ','SUSPENDU','REJETÉ'),
  intro_video_url,
  years_experience INT,
  ai_qcm_score DECIMAL,
  admin_notes TEXT,
  validated_at, validated_by
)

prof_subjects (user_id, subject_code, level ENUM('PRIMAIRE','COLLEGE','LYCEE','SUPERIEUR'))
prof_diplomas (id, user_id FK, title, institution, year, file_url MinIO, ocr_text, verified BOOLEAN)
prof_modalities (user_id, modality ENUM('ONLINE','DOMICILE','LOCAL'))
prof_locations (user_id, city, region)   -- pour DOMICILE/LOCAL

student_profiles (
  user_id PK FK→profiles,
  school_level, school_name,
  target_exam ENUM('BAC','BREVET','REGIONAL','AUCUN'),
  goals TEXT
)
student_interests (user_id, subject_code)

reviews (
  id PK, booking_id, prof_id, student_id,
  stars SMALLINT CHECK (stars BETWEEN 1 AND 5),
  comment TEXT,
  moderated BOOLEAN, moderated_at,
  created_at
)
review_stats (prof_id PK, avg_stars, count) -- materialized
```

---

## 3. `calendar-service` — PostgreSQL `calendar_db`

```sql
availability_rules (
  id PK, prof_id,
  recurrence ENUM('WEEKLY','ONE_OFF'),
  day_of_week SMALLINT,                -- pour WEEKLY (0=Lun)
  date DATE,                           -- pour ONE_OFF
  start_time TIME, end_time TIME,
  timezone VARCHAR(50),
  active BOOLEAN
)

slots (
  id PK, prof_id,
  start_at TIMESTAMPTZ, end_at TIMESTAMPTZ,
  modality ENUM('ONLINE','DOMICILE','LOCAL'),
  status ENUM('AVAILABLE','RESERVED_PENDING','BOOKED','BLOCKED'),
  booking_id NULL,
  generated_from_rule_id NULL,
  created_at, updated_at
)
-- Index unique partiel : (prof_id, start_at) where status != 'AVAILABLE' to prevent double-booking

calendar_sync_links (user_id PK, provider ENUM('GOOGLE','APPLE'), refresh_token_encrypted, last_sync_at)
```

**Note** : les `slots` sont matérialisés à partir des `availability_rules` sur une fenêtre glissante de 60 jours (job nightly).

---

## 4. `pricing-service` — PostgreSQL `pricing_db`

```sql
prof_pricing (
  id PK, prof_id,
  subject_code, level,
  modality ENUM('ONLINE','DOMICILE','LOCAL'),
  duration_minutes SMALLINT,           -- 30, 60, 90, 120
  day_of_week SMALLINT NULL,           -- NULL = tous les jours
  amount_mad DECIMAL(10,2),
  active BOOLEAN,
  UNIQUE (prof_id, subject_code, level, modality, duration_minutes, day_of_week)
)

pricing_packs (
  id PK, prof_id, subject_code, level, modality,
  sessions_count SMALLINT,             -- 5 ou 10
  discount_pct DECIMAL(5,2),
  total_amount_mad DECIMAL(10,2),
  active BOOLEAN
)

market_suggestions (id, prof_id, suggested_amount, computed_at, source) -- cache IA
```

---

## 5. `booking-service` — PostgreSQL `booking_db`

```sql
bookings (
  id UUID PK,
  student_id, prof_id, slot_id,        -- réf vers calendar
  subject_code, level, modality,
  duration_minutes,
  amount_mad DECIMAL(10,2),
  escrow_id UUID,                      -- réf vers payment.escrows
  status ENUM('DEMANDÉE','ACCEPTÉE_PAR_PROF','EN_COURS','TERMINÉE','CONFIRMÉE','ANNULÉE','LITIGE'),
  scheduled_start TIMESTAMPTZ,
  scheduled_end TIMESTAMPTZ,
  actual_start TIMESTAMPTZ, actual_end TIMESTAMPTZ,  -- mesurés par video-service
  cancel_reason, cancelled_by, cancelled_at,
  confirmed_at,
  created_at, updated_at
)

booking_state_transitions (
  id, booking_id FK, from_status, to_status, triggered_by, reason, created_at
)

disputes (
  id PK, booking_id FK,
  opened_by, cause TEXT, evidence_urls JSONB,
  status ENUM('OPEN','UNDER_REVIEW','RESOLVED_REFUND','RESOLVED_PAY_PROF','RESOLVED_SPLIT'),
  resolution_notes, resolved_by, resolved_at,
  created_at
)
```

---

## 6. `payment-service` ⚠️ — PostgreSQL `payment_db`

```sql
balances (
  user_id UUID PK,
  balance_mad DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (balance_mad >= 0),
  updated_at,
  version INT                          -- optimistic locking
)

escrows (
  id UUID PK,
  booking_id UUID UNIQUE,
  student_id, prof_id,
  amount_mad DECIMAL(10,2),
  status ENUM('LOCKED','RELEASED_TO_PROF','REFUNDED_TO_STUDENT','SPLIT'),
  prof_share_mad, student_refund_mad,   -- pour SPLIT
  locked_at, released_at
)

topup_requests (
  id UUID PK, student_id,
  method ENUM('CASHPLUS','BANK_TRANSFER','WHATSAPP','OTHER'),
  amount_mad DECIMAL(10,2),
  proof_url MinIO,
  status ENUM('EN_ATTENTE_ADMIN','APPROVED','MODIFIED','REJECTED'),
  approved_amount_mad DECIMAL(10,2),   -- si admin modifie
  admin_notes, decided_by, decided_at,
  created_at
)

payouts (
  id UUID PK, prof_id,
  requested_amount_mad DECIMAL(10,2),
  approved_amount_mad DECIMAL(10,2),
  status ENUM('REQUESTED','APPROVED','PAID','REJECTED'),
  iban_ref, payment_ref,
  requested_at, decided_by, decided_at, paid_at
)

audit_log (
  id BIGSERIAL PK,
  user_id, actor_id,                   -- actor peut être admin
  operation ENUM('TOPUP','ESCROW_LOCK','ESCROW_RELEASE','REFUND','PAYOUT','ADMIN_ADJUST','ADMIN_CREDIT','ADMIN_DEBIT'),
  amount_mad DECIMAL(12,2),
  balance_before, balance_after,
  reference_id,                        -- booking_id, escrow_id, topup_id, payout_id
  reason TEXT,
  created_at TIMESTAMPTZ DEFAULT NOW()
)
-- audit_log : append-only, INDEX(user_id, created_at) ; trigger BEFORE UPDATE/DELETE → RAISE
```

**Invariants critiques** :
- `balance_mad` ne peut jamais être négatif (CHECK constraint + verrouillage optimiste).
- Toute opération qui modifie `balances` ou `escrows` doit insérer **dans la même transaction** une ligne dans `audit_log`.
- `audit_log` est immuable (trigger qui rejette UPDATE/DELETE).
- L'admin peut créditer/débiter mais l'opération est tracée avec `actor_id`.

---

## 7. `chat-service` — MongoDB + Redis

**MongoDB** :

```js
// conversations
{
  _id, bookingId, profId, studentId,
  status: 'PRE_ACCEPT' | 'POST_ACCEPT' | 'CLOSED',
  lastMessageAt, unreadCounts: {profId: n, studentId: n},
  createdAt
}

// messages
{
  _id, conversationId, senderId, type: 'TEXT'|'IMAGE'|'PDF'|'VOICE'|'SMART_REPLY',
  body, attachments: [{url, mime, size}],
  moderation: { status: 'CLEAN'|'MASKED'|'BLOCKED', flags: [...], maskedFields: [...] },
  readBy: [{userId, at}],
  createdAt
}
// Index : (conversationId, createdAt desc)
```

**Redis** :
- `chat:typing:{conversationId}` (Set d'userIds, TTL 5s)
- `chat:presence:{userId}` (online/offline, TTL 30s, refresh via WS heartbeat)

---

## 8. `video-session-service` — PostgreSQL `video_db`

```sql
sessions (
  id PK, booking_id UNIQUE,
  livekit_room_id,
  scheduled_start, scheduled_end,
  actual_start, actual_end,
  duration_effective_seconds,
  recording_url MinIO,                 -- si consentement
  recording_expires_at,                -- replay 30j
  consent_given_by JSONB,
  status ENUM('SCHEDULED','LIVE','ENDED','FAILED')
)

session_artifacts (id, session_id, type ENUM('WHITEBOARD','QUIZ','NOTE','CODE'), content JSONB, created_at)
```

---

## 9. `ai-agent-service` — PostgreSQL `ai_db` + Redis

```sql
qcm_templates (id, subject_code, level, difficulty, questions JSONB, created_at)
qcm_sessions (id, prof_id, subject_code, level, generated_questions JSONB, started_at, finished_at, score DECIMAL)

matching_features (
  user_id PK, role,                    -- features matérialisées pour le matching
  features JSONB, computed_at
)

ai_calls_log (
  id BIGSERIAL, user_id, agent ENUM('MATCHING','QCM','MODERATION','SMART_REPLY','REVISION','TUTOR'),
  model, input_hash, output_hash, tokens_in, tokens_out, latency_ms, cost_usd,
  created_at
)
-- Pour observabilité coûts IA + audit
```

**Redis** : cache des embeddings (clé = hash texte, TTL 7j), cache des suggestions de matching (TTL 1h).

---

## 10. `local-management-service` — PostgreSQL `local_db`

```sql
locals (
  id PK, prof_id,
  name, address, city, region,
  geo_lat, geo_lng,                    -- pour géolocalisation
  capacity SMALLINT,
  equipments JSONB,                    -- {wifi:true, projector:true, ...}
  photos JSONB,                        -- liste d'URLs MinIO
  status ENUM('PENDING','VALIDATED','REJECTED','DISABLED'),
  validated_by, validated_at,
  created_at
)
```

---

## 11. `notification-service` — Redis (queue) + PostgreSQL `notification_db` (log)

```sql
notification_log (
  id BIGSERIAL,
  user_id, channel ENUM('PUSH','SMS','WA','EMAIL'),
  template_key, locale,
  payload JSONB,
  status ENUM('QUEUED','SENT','FAILED','DELIVERED','READ'),
  provider_message_id, error,
  created_at, sent_at, delivered_at
)
```

Redis : queues `notif:queue:push`, `notif:queue:sms`, ... (consommées par workers).

---

## 12. `admin-dashboard-service` — PostgreSQL `admin_db` (read-models)

```sql
-- Read-models alimentés par les events Kafka, dénormalisés pour la perf des dashboards.
kpi_daily (date PK, new_users, new_bookings, gmv_mad, revenue_mad, disputes_opened, ...)
prof_validations_pending_view (user_id, submitted_at, ai_score, ...)
topups_pending_view (id, student_id, amount, method, submitted_at)
payouts_pending_view (id, prof_id, amount, requested_at)
```

---

## Référentiels statiques (config-server ou table partagée)

- `subjects` : liste codifiée des matières (`MATH`, `PHYS`, `FR`, `EN`, `AR`, `PHILO`, `INFO`, ...).
- `levels` : niveaux scolaires marocains (`CP`, `CE1`, ..., `2BAC_SM`, `LICENCE`, `MASTER`).
- `regions` & `cities` Maroc (table de référence partagée).
