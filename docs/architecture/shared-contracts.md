# Contrats partagés (`shared-libs`)

Module Maven/Gradle multi-artefacts publié en interne, consommé par tous les microservices. Évite la dérive des contrats.

## Artefacts

```
shared-libs/
├── shared-dto/            # DTOs API publics & internes
├── shared-events/         # Enveloppe & schémas d'événements Kafka
├── shared-errors/         # Codes erreur normalisés + GlobalExceptionHandler
├── shared-security/       # Filtres JWT, AuthContextHolder, annotations @RequireRole
└── shared-i18n/           # Clés de traduction & utilitaires locale
```

## 1. Enveloppe API REST (réponses)

Toutes les réponses suivent une enveloppe uniforme.

### Succès — `200 OK`

```json
{
  "data": { /* la ressource */ },
  "meta": { "page": 0, "size": 20, "total": 134 } /* si pagination */
}
```

### Erreur

```json
{
  "error": {
    "code": "BOOKING_SLOT_NOT_AVAILABLE",
    "message": "Le créneau n'est plus disponible.",
    "messageKey": "errors.booking.slot_not_available",
    "details": { "slotId": "..." },
    "traceId": "uuid"
  }
}
```

## 2. Codes erreur normalisés (`shared-errors`)

Format : `<DOMAINE>_<CAUSE>`. La liste vit dans une enum `ErrorCode`.

| Domaine | Codes |
|---------|-------|
| AUTH | `AUTH_INVALID_CREDENTIALS`, `AUTH_OTP_EXPIRED`, `AUTH_OTP_INVALID`, `AUTH_USER_SUSPENDED`, `AUTH_TOKEN_EXPIRED`, `AUTH_FORBIDDEN_ROLE` |
| PROFILE | `PROFILE_NOT_FOUND`, `PROFILE_PROF_NOT_VALIDATED`, `PROFILE_DIPLOMA_REQUIRED`, `PROFILE_QCM_FAILED` |
| CALENDAR | `CALENDAR_SLOT_NOT_FOUND`, `CALENDAR_SLOT_CONFLICT`, `CALENDAR_TIMEZONE_INVALID` |
| BOOKING | `BOOKING_SLOT_NOT_AVAILABLE`, `BOOKING_INVALID_TRANSITION`, `BOOKING_TTL_EXPIRED`, `BOOKING_CONFIRM_NOT_TERMINATED` |
| PAYMENT | `PAYMENT_INSUFFICIENT_BALANCE`, `PAYMENT_TOPUP_PROOF_REQUIRED`, `PAYMENT_ESCROW_NOT_FOUND`, `PAYMENT_PAYOUT_NOT_ELIGIBLE`, `PAYMENT_NEGATIVE_BALANCE_BLOCKED` |
| CHAT | `CHAT_CONVERSATION_NOT_FOUND`, `CHAT_BYPASS_DETECTED`, `CHAT_ATTACHMENT_TOO_LARGE` |
| VIDEO | `VIDEO_SESSION_NOT_LIVE`, `VIDEO_CONSENT_REQUIRED` |
| AI | `AI_UPSTREAM_TIMEOUT`, `AI_QUOTA_EXCEEDED` |
| COMMON | `VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `RATE_LIMITED`, `INTERNAL_ERROR` |

Mapping HTTP : `400` (validation), `401` (auth absent), `403` (autorisation), `404` (not found), `409` (conflit état), `422` (règle métier), `429` (rate limit), `5xx` (interne).

## 3. Auth & headers

Tout endpoint protégé reçoit, après vérification par le Gateway :

```
Authorization: Bearer <JWT>
X-User-Id: <uuid>
X-User-Role: ADMIN|PROF|STUDENT|PARENT
X-Locale: fr|ar|en
X-Trace-Id: <uuid>
```

Le gateway extrait ces infos du JWT et les injecte. Les services backend **ne re-valident pas** la signature (sauf en interne via mTLS), mais utilisent `shared-security` pour peupler le contexte applicatif :

```java
@RequireRole({PROF, ADMIN})
@GetMapping("/api/v1/profile/me")
public ResponseEntity<ProfDto> getMe(@AuthUser UserContext ctx) { ... }
```

## 4. DTOs partagés clés (`shared-dto`)

```java
// Identifiants
public record UserId(UUID value) {}
public record BookingId(UUID value) {}

// Énumérations métier
public enum Role { ADMIN, PROF, STUDENT, PARENT }
public enum Modality { ONLINE, DOMICILE, LOCAL }
public enum SchoolLevel { PRIMAIRE, COLLEGE, LYCEE, SUPERIEUR }
public enum BookingStatus {
  DEMANDEE, ACCEPTEE_PAR_PROF, EN_COURS, TERMINEE, CONFIRMEE, ANNULEE, LITIGE
}
public enum ProfStatus { EN_ATTENTE, EN_TEST_IA, EN_ENTRETIEN_ADMIN, VALIDE, SUSPENDU, REJETE }
public enum Currency { MAD }   // v1 : MAD seulement

// Money — toujours en centimes pour éviter les flottants
public record Money(long amountMinor, Currency currency) {
  public BigDecimal toMajor() { return BigDecimal.valueOf(amountMinor).movePointLeft(2); }
}

// Pagination request
public record PageRequestDto(int page, int size, String sort) {}

// Référentiels
public record Subject(String code, Map<String, String> names) {} // names FR/AR/EN
```

## 5. Enveloppe d'événements (`shared-events`)

```java
public sealed interface DomainEvent<T> {
  UUID eventId();
  String eventType();              // "naja7i.booking.requested.v1"
  Instant occurredAt();
  String producer();
  UUID traceId();
  int schemaVersion();
  T payload();
}
```

Tous les payloads sont des records immuables. Un nouveau champ = bump `vN` ; un changement breaking = nouveau topic `.v2` + double-write pendant transition.

## 6. Convention API

- **Versionnage** : préfixe `/api/v1/`, dépréciation 6 mois minimum.
- **Pagination** : query params `?page=0&size=20&sort=createdAt,desc`.
- **Filtrage** : RSQL ou query params explicites (préférés pour la doc Swagger).
- **Idempotence** : header `Idempotency-Key` requis sur tous les `POST` financiers (topup, payout, refund) → stocké côté payment-service avec TTL 24h.
- **Localisation** : `Accept-Language` ou `X-Locale` ; les messages utilisateur sont renvoyés via clé i18n + texte par défaut.

## 7. Healthchecks & observabilité

Tous les services exposent :

- `GET /actuator/health` (liveness + readiness)
- `GET /actuator/info`
- `GET /actuator/prometheus` (métriques)
- Trace propagation via `traceparent` (W3C Trace Context).

Métriques business obligatoires (Micrometer custom) :

- `naja7i.booking.requested.count`
- `naja7i.payment.topup.amount.sum`
- `naja7i.ai.tokens.sum{agent=...}`
- `naja7i.chat.bypass.flagged.count`

## 8. Sécurité — règles transversales

- **PII** : email, téléphone, nom complet ne sont **jamais** loggés en clair.
- **Logs** : champs sensibles masqués par filtre Logback (`***`).
- **MinIO** : URLs pré-signées (TTL 15min) ; pas d'accès direct aux objets.
- **CORS** : whitelist explicite via Gateway (web origins seulement).
- **CSRF** : non applicable (JWT-only API), sauf pages back-office si session.
- **Rate limit** : configuré au Gateway (`shared-security` n'en fait pas).
