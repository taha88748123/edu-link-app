# NAJA7I-APP

> Plateforme EdTech marocaine — mise en relation **professeurs particuliers** ⇄ **étudiants**, supervision **admin**, agents **IA Claude**.

🇲🇦 **Made in Morocco** · 🌍 i18n FR / AR (RTL) / EN · 💰 MAD

---

## Statut — MVP v1 complet, prod-ready

| Phase | Périmètre | État |
|-------|-----------|------|
| **0** | Fondations infra + monorepo + docs (C4, events, DB, contrats) | ✅ |
| **1** | `auth-service` · `user-profile-service` · `notification-service` | ✅ |
| **2** | `calendar` · `pricing` · `booking` · `payment` (escrow + audit immuable) | ✅ |
| **3** | `chat` (anti-bypass) · `video-session` (LiveKit JWT) · `ai-agent` (matching) | ✅ |
| **4** | Frontend React 18 — auth + booking + chat + vidéo + admin | ✅ |
| **5** | Observabilité LGTM · E2E Playwright · Hardening (CSP, rate limit, WS) · Helm K8s | ✅ |

**Chiffres clés** : 13 microservices · 20 modules Maven · ~135 tests backend · 54 tests frontend · 5 specs E2E · bundle frontend 80 KB initial · 6 dashboards Grafana.

---

## 🚀 Démarrage rapide (5 minutes)

### 1. Prérequis

| Outil | Version | Notes |
|---|---|---|
| Docker + Docker Compose | 24+ | Obligatoire |
| Java | **21** (Temurin) | Pour `mvn` local hors Docker |
| Maven | 3.9+ | Idem |
| Node.js | **20+** | Pour le frontend en mode dev |
| Helm | 3.x | Optionnel — déploiement K8s |
| kubectl | 1.27+ | Optionnel — K8s |

### 2. Configuration minimale (`.env`)

```bash
cp .env.example .env
# Le fichier .env est déjà fonctionnel en mode dev. Tous les drivers
# externes (SMS/WA/email/push/IA) sont en mode `logging` par défaut →
# AUCUNE clé API requise pour démarrer.
```

Pour générer un JWT secret cryptographiquement solide :
```bash
openssl rand -hex 32 | tr -d '\n' | sed 's/^/NAJA7I_JWT_SECRET=/' >> .env
```

### 3. Lancer

```bash
# OPTION A : Tout en un coup (infra + plateforme + 10 microservices + frontend)
make up

# OPTION B : Avec observabilité (Grafana + Loki + Tempo + Prometheus en plus)
make up && make observability-up

# OPTION C : Pour développer en hot-reload côté frontend
make infra-up && make platform-up && make services-up
make frontend-dev    # http://localhost:5173 avec HMR
```

### 4. Vérifier que ça tourne

| Service | URL | Notes |
|---|---|---|
| 🎨 **Frontend** | http://localhost:5173 (dev) ou http://localhost:5173 (nginx) | Landing page |
| 🚪 API Gateway | http://localhost:8080 | Toutes les routes `/api/v1/*` |
| 📜 Swagger | http://localhost:8101/swagger-ui.html | Par service (8101→8111) |
| 🔍 Eureka | http://localhost:8761 | Service discovery dashboard |
| 📊 Grafana | http://localhost:3000 | admin / admin |
| 📨 Kafka UI | http://localhost:8090 | Topics + consumer groups |
| 📦 MinIO Console | http://localhost:9001 | naja7i / naja7i_dev_changeme |
| ✉️ Mailhog | http://localhost:8025 | Emails capturés en dev |

---

## 🔑 Clés API & services externes

**Tout est optionnel en dev.** Chaque intégration externe a un mode `logging` qui simule le comportement sans appel réseau. Tu actives un service en passant son driver à autre chose que `logging` ET en fournissant les clés correspondantes.

### Pattern driver

```yaml
# Exemple notification SMS
NOTIF_SMS_DRIVER=logging   # défaut : log "would send SMS" puis succès
# OU
NOTIF_SMS_DRIVER=twilio    # appelle vraiment l'API Twilio
TWILIO_ACCOUNT_SID=ACxxx
TWILIO_AUTH_TOKEN=xxx
TWILIO_FROM_NUMBER=+212...
```

### Tableau complet

| Service externe | Variables d'env | Driver flag | Active quoi ? | Comment l'obtenir |
|---|---|---|---|---|
| **Twilio SMS** | `TWILIO_ACCOUNT_SID`<br>`TWILIO_AUTH_TOKEN`<br>`TWILIO_FROM_NUMBER` | `NOTIF_SMS_DRIVER=twilio` | OTP par SMS, notifs SMS | [twilio.com/console](https://console.twilio.com) — n° marocain `+212` |
| **Twilio WhatsApp** | `TWILIO_ACCOUNT_SID`<br>`TWILIO_AUTH_TOKEN`<br>`TWILIO_WA_FROM_NUMBER` | `NOTIF_WA_DRIVER=twilio` | OTP via WA, notifs WA | Pareil + sandbox WA Twilio |
| **SendGrid** | `SENDGRID_API_KEY`<br>`NOTIF_EMAIL_FROM` | `NOTIF_EMAIL_DRIVER=sendgrid` | Emails transactionnels (vs Mailhog en dev) | [sendgrid.com](https://sendgrid.com) — free tier 100/jour |
| **Firebase FCM** | `FIREBASE_SERVICE_ACCOUNT_PATH` | `NOTIF_PUSH_DRIVER=firebase` | Notifications push mobile | [console.firebase.google.com](https://console.firebase.google.com) → Project Settings → Service Accounts → Generate key |
| **Anthropic Claude (chat)** | `ANTHROPIC_API_KEY`<br>`ANTHROPIC_MODEL` | `NAJA7I_CHAT_MODERATION_DRIVER=anthropic` | Analyse IA des messages chat pour détecter les tentatives de bypass | [console.anthropic.com](https://console.anthropic.com) — `claude-opus-4-7` recommandé |
| **Anthropic Claude (matching)** | `ANTHROPIC_API_KEY` | `NAJA7I_AI_MATCHING_DRIVER=anthropic` | Refinement IA du matching prof/étudiant + rationale qualitative | Même clé que ci-dessus |
| **LiveKit** | `LIVEKIT_URL`<br>`LIVEKIT_API_KEY`<br>`LIVEKIT_API_SECRET` | (auto par défaut) | Serveur WebRTC pour les sessions vidéo réelles. Sans serveur le JWT est émis mais la connexion browser échoue gracieusement avec un Alert. | Self-hosted ([livekit.io/docs/home/self-hosting](https://docs.livekit.io/home/self-hosting/)) ou cloud ([livekit.io](https://livekit.io)) |

### Secrets internes (toujours requis)

| Variable | Rôle | Génération |
|---|---|---|
| `NAJA7I_JWT_SECRET` | Signature JWT HS512 (auth-service) et vérification (gateway + tous services) | `openssl rand -hex 32` (32+ chars) |
| `NAJA7I_INTERNAL_KEY` | Header `X-Internal-Key` pour les appels REST inter-services (booking → calendar/pricing) | Toute string aléatoire ≥ 16 chars |

### Backdoor E2E (ne JAMAIS activer en prod)

| Variable | Effet |
|---|---|
| `NAJA7I_AUTH_TEST_MODE=true` | Expose `GET /api/v1/dev/auth/otp/{userId}` sur le port `:8101` (jamais routé par le gateway). Nécessite aussi le header `X-Test-Mode: true`. Utilisé uniquement par Playwright pour récupérer les OTPs sans SMS. |

---

## Architecture des services

| Service | Port | DB | Rôle |
|---------|------|----|----|
| `api-gateway` | 8080 | — | JWT filter, rate limit (Bucket4j+Redis), injection headers `X-User-*` |
| `service-discovery` (Eureka) | 8761 | — | Registry |
| `config-server` | 8888 | — | Configuration centralisée |
| `auth-service` | 8101 | PG `auth_db` + Redis | Register, OTP, login, refresh, password reset, audit |
| `user-profile-service` | 8102 | PG `profile_db` + MinIO | Profils prof/student, diplômes, reviews, validation admin |
| `calendar-service` | 8103 | PG `calendar_db` | Slots avec state machine + TTL 10 min, anti-double-booking |
| `pricing-service` | 8104 | PG `pricing_db` | Tarifs prof, packs, simulation prix avec frais plateforme |
| `booking-service` | 8105 | PG `booking_db` | Orchestrateur 7-statuts, anti-bypass masking, outbox pattern |
| `payment-service` | 8106 | PG `payment_db` | Wallet, escrow, payouts, **audit log immuable** (triggers PG) |
| `chat-service` | 8107 | MongoDB + Redis | Conversations, regex moderator, **WebSocket STOMP** |
| `video-session-service` | 8108 | PG `video_db` | LiveKit JWT issuer, lifecycle session, consentement enregistrement |
| `ai-agent-service` | 8109 | PG `ai_db` + Redis | Matching prof scoring + cache Redis, Claude opt-in |
| `notification-service` | 8111 | PG `notification_db` + Redis | Multi-canal SMS/WA/Email/Push |

Voir [`docs/architecture/`](docs/architecture/) pour les diagrammes C4, séquences, schémas DB et catalogue d'événements Kafka, et [`docs/observability.md`](docs/observability.md) pour la stack LGTM.

---

## Structure du monorepo

```
edu-link-app/
├── README.md                # ce fichier
├── .env.example             # template des variables d'env
├── docker-compose.yml       # 5 profiles : infra · platform · services · frontend · observability
├── Makefile                 # raccourcis (make help)
├── docs/
│   ├── architecture/        # C4, data-flow, DB schemas, events catalog
│   └── observability.md
├── infrastructure/
│   ├── postgres-init.sh     # crée 11 DBs au boot
│   ├── observability/       # Prometheus + Loki + Tempo + Grafana configs
│   └── k8s/                 # Helm chart + sealed-secrets + cert-manager
├── backend/                 # 20 modules Maven (Spring Boot 3.3, Java 21)
│   ├── pom.xml
│   ├── shared-libs/         # dto · events · errors · security · i18n · observability
│   └── <13 services>/
├── frontend-web/            # React 18 + Vite + Tailwind + shadcn/ui
│   ├── src/
│   ├── e2e/                 # Playwright golden paths
│   ├── Dockerfile           # nginx multi-stage build
│   └── nginx.conf           # SPA fallback + CSP stricte
└── .github/workflows/       # backend-ci · frontend-ci · e2e-ci · helm-ci
```

---

## 🧪 Tests

```bash
# Backend : tous les tests unit + IT Testcontainers (Docker requis pour les IT)
cd backend && mvn -B clean verify

# Frontend : 54 tests unit + typecheck + build
cd frontend-web && npm run typecheck && npm test -- --run && npm run build

# E2E : 5 golden paths Playwright (suppose make up déjà lancé)
make e2e-install        # première fois
NAJA7I_AUTH_TEST_MODE=true make up    # active le backdoor OTP
make e2e
```

Les 5 specs E2E :
- `auth-register-login` — register → OTP → login
- `student-booking-flow` — discovery → booking → prof accepts
- `admin-validate-prof` — admin approve une validation prof
- `anti-bypass-chat` — **régression sécurité** : phone masking en PRE_ACCEPT
- `admin-topup-approve` — recharge wallet → admin approve → solde crédité

---

## 🚀 Déploiement Kubernetes

Voir [`infrastructure/k8s/README.md`](infrastructure/k8s/README.md) pour le guide complet.

```bash
# Pré-requis : cluster K8s + ingress-nginx + cert-manager + sealed-secrets controller
make helm-deps                                    # télécharge subcharts Bitnami
make helm-lint                                    # validation YAML

# Déploiement dev (in-cluster infra)
helm upgrade --install naja7i infrastructure/k8s/helm/naja7i \
  -f infrastructure/k8s/helm/naja7i/values-dev.yaml \
  -n naja7i --create-namespace

# Prod : managed DB + HPA + PDB + NetworkPolicy + Let's Encrypt
# 1. Seal les vrais secrets : kubeseal -f my-secrets.yaml > sealed.yaml
# 2. Apply : kubectl apply -f sealed.yaml
# 3. Deploy : helm ... -f values-prod.yaml
```

---

## 🛡️ Garanties critiques

### Anti-bypass (chat-service)
- Masking regex synchrone (téléphones marocains stricts/spacés/dotted, emails, URLs wa.me/t.me/instagram) **avant** persistance, tant que `BOOKING_ACCEPTED` n'a pas eu lieu
- Driver `anthropic` optionnel pour analyse d'intention via Claude (fallback automatique sur `logging`)
- Strike tracker Redis (sliding 24 h) → escalation `CRITICAL` à 3 hits
- **Test E2E qui vérifie le masking en bout de chaîne** (DB Mongo + UI student + UI prof)

### Money safety (payment-service) — 4 couches de défense
1. DB `CHECK (balance_minor >= 0)` sur `balances`
2. Triggers PostgreSQL qui rejettent `UPDATE`/`DELETE` sur `audit_log` (immuable)
3. `UNIQUE (escrows.booking_id)` — anti-double-lock
4. `BalanceLedger.apply()` = chokepoint unique pour toute mutation de balance, audit_log écrit dans la même `@Transactional`

### Hardening sécurité (gateway + frontend)
- **Rate limiting** par tier : anon 60/min/IP · auth 300/min/user · admin 600/min · login 5/min/IP · register 3/min/IP
- **CSP stricte** : `default-src 'self'`, `frame-ancestors 'none'`, `script-src 'self'` (pas d'unsafe-inline)
- **HSTS** + `Permissions-Policy: camera=(self), microphone=(self), geolocation=(), payment=()`
- JWT triple-gated dev backdoor (`@ConditionalOnProperty` + header `X-Test-Mode` + non-routage gateway)

### Conformité Loi 09-08 / CNDP
- PII (téléphone, email, nom complet) **jamais en log clair**
- Audit log paiement immuable, conservé 365 jours, exportable CSV
- URLs MinIO pré-signées TTL 15 min (pas d'accès direct)
- Tous les événements Kafka horodatés avec `traceId` pour corrélation cross-service (Tempo)

---

## 📚 Commandes Make utiles

```bash
make help              # liste toutes les cibles disponibles
make up                # tout (infra + platform + services + frontend)
make down              # tout arrêter
make logs              # tail -f de tous les containers
make ps                # état des containers
make observability-up  # Grafana + Loki + Tempo + Prometheus
make frontend-dev      # Vite dev server avec HMR
make e2e               # lance les tests Playwright
make helm-lint         # valide les charts Helm
make test              # mvn clean verify (tous les tests backend)
```

---

## 🛣️ Roadmap

- [x] Phase 0 — Infra + monorepo + architecture
- [x] Phase 1 — Auth + Profil + Notification
- [x] Phase 2 — Calendar + Pricing + Booking + Payment
- [x] Phase 3 — Chat + Vidéo + IA Matching
- [x] Phase 4 — Frontend Web React (auth + booking + chat + vidéo + admin)
- [x] Phase 5 — Observabilité + E2E + Hardening + Helm K8s
- [ ] v2 — Mobile Flutter, agents IA additionnels (QCM / modération centralisée / smart-reply / révision / tutor), `local-management-service`, `admin-dashboard-service` avec read-models, payments réels (Stripe / CMI / CashPlus API)

---

## Licence

Propriétaire — NAJA7I-APP, 2026.
