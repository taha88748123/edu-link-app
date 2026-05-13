# Catalogue d'événements Kafka

Convention de nommage des topics : `naja7i.<domaine>.<événement>.v<version>`.

Tous les événements héritent d'une enveloppe commune (voir [shared-contracts.md](./shared-contracts.md)) :

```json
{
  "eventId": "uuid",
  "eventType": "naja7i.booking.requested.v1",
  "occurredAt": "2026-05-13T14:00:00Z",
  "producer": "booking-service",
  "traceId": "uuid",
  "schemaVersion": 1,
  "payload": { /* spécifique à l'événement */ }
}
```

Sérialisation : **JSON** en v1 (lisible) ; migration possible vers **Avro/Confluent Schema Registry** en v2 pour la compatibilité forte.

Partition key : `userId` ou `bookingId` selon le topic (cf. tableau).

---

## Domaine `auth`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.auth.user.registered.v1` | `userId` | auth | profile, notif | `{userId, role, email, phone}` |
| `naja7i.auth.otp.requested.v1` | `userId` | auth | notif | `{userId, code, channel, expiresAt}` |
| `naja7i.auth.user.logged_in.v1` | `userId` | auth | admin | `{userId, ip, ua, at}` |
| `naja7i.auth.password.reset_requested.v1` | `userId` | auth | notif | `{userId, resetToken, expiresAt}` |
| `naja7i.auth.user.suspended.v1` | `userId` | auth | profile, booking, chat | `{userId, reason}` |

## Domaine `profile`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.profile.prof.submitted.v1` | `userId` | profile | ai, notif | `{userId, subjects[], diplomasCount}` |
| `naja7i.profile.prof.evaluated.v1` | `userId` | ai | profile, notif | `{userId, qcmScore, decision: 'PASS'\|'FAIL'}` |
| `naja7i.profile.prof.validated.v1` | `userId` | profile | notif, ai (matching), admin | `{userId, validatedBy, at}` |
| `naja7i.profile.prof.rejected.v1` | `userId` | profile | notif | `{userId, reason}` |
| `naja7i.profile.review.created.v1` | `profId` | profile | ai, admin | `{reviewId, profId, studentId, stars, comment}` |
| `naja7i.profile.review.moderated.v1` | `profId` | ai | profile, notif | `{reviewId, decision, flags[]}` |

## Domaine `calendar`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.calendar.slot.created.v1` | `profId` | calendar | ai (matching) | `{slotId, profId, startAt, endAt, modality}` |
| `naja7i.calendar.slot.booked.v1` | `profId` | calendar | notif, ai | `{slotId, bookingId}` |
| `naja7i.calendar.slot.released.v1` | `profId` | calendar | notif | `{slotId, reason}` |

## Domaine `booking`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.booking.requested.v1` | `bookingId` | booking | notif, payment, ai, admin | `{bookingId, studentId, profId, slotId, amountMad}` |
| `naja7i.booking.accepted.v1` | `bookingId` | booking | notif, chat, payment | `{bookingId, acceptedAt}` |
| `naja7i.booking.declined.v1` | `bookingId` | booking | notif, payment, calendar | `{bookingId, reason}` |
| `naja7i.booking.started.v1` | `bookingId` | booking | video, notif | `{bookingId, startAt}` |
| `naja7i.booking.ended.v1` | `bookingId` | video | booking, notif | `{bookingId, actualDurationSec}` |
| `naja7i.booking.confirmed.v1` | `bookingId` | booking | payment, profile (review prompt) | `{bookingId, confirmedAt}` |
| `naja7i.booking.cancelled.v1` | `bookingId` | booking | payment, notif, calendar | `{bookingId, cancelledBy, reason}` |
| `naja7i.booking.disputed.v1` | `bookingId` | booking | admin, notif | `{disputeId, bookingId, cause}` |
| `naja7i.booking.dispute_resolved.v1` | `bookingId` | booking | payment, notif | `{disputeId, resolution, amounts}` |

## Domaine `payment` ⚠️ (auditables)

Tous ces événements sont consommés en priorité par `admin-dashboard-service` et **persistés** dans un topic compacté pour audit.

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.payment.topup.submitted.v1` | `studentId` | payment | notif, admin | `{topupId, studentId, amount, method, proofUrl}` |
| `naja7i.payment.topup.decided.v1` | `studentId` | payment | notif | `{topupId, decision, approvedAmount, decidedBy}` |
| `naja7i.payment.escrow.locked.v1` | `bookingId` | payment | admin | `{escrowId, bookingId, studentId, amount}` |
| `naja7i.payment.escrow.released.v1` | `bookingId` | payment | admin | `{escrowId, target: 'PROF'\|'STUDENT'\|'SPLIT', amounts}` |
| `naja7i.payment.payout.requested.v1` | `profId` | payment | notif, admin | `{payoutId, profId, amount}` |
| `naja7i.payment.payout.decided.v1` | `profId` | payment | notif | `{payoutId, decision, amount, paymentRef}` |
| `naja7i.payment.balance.adjusted.v1` | `userId` | payment | admin, notif | `{userId, delta, reason, actorId}` |

**Rétention** : 365 jours minimum (conformité Loi 09-08), copie froide vers S3 (Glacier-like) au-delà.

## Domaine `chat`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.chat.message.posted.v1` | `conversationId` | chat | ai (modération), notif | `{messageId, conversationId, senderId, type, preview}` |
| `naja7i.chat.bypass.flagged.v1` | `conversationId` | ai | chat, admin | `{messageId, flags[], severity}` |

## Domaine `video`

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.video.session.started.v1` | `bookingId` | video | booking, notif | `{sessionId, bookingId, startAt}` |
| `naja7i.video.session.ended.v1` | `bookingId` | video | booking | `{sessionId, durationSec, recordingUrl}` |
| `naja7i.video.recording.ready.v1` | `bookingId` | video | notif | `{sessionId, recordingUrl, expiresAt}` |

## Domaine `notification` (observabilité)

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.notification.delivered.v1` | `userId` | notif | admin | `{notifId, userId, channel, providerMsgId}` |
| `naja7i.notification.failed.v1` | `userId` | notif | admin | `{notifId, channel, error}` |

## Domaine `ai` (observabilité coûts)

| Topic | Partition key | Producer | Consumers | Payload |
|-------|---------------|----------|-----------|---------|
| `naja7i.ai.call.completed.v1` | `agentType` | ai | admin | `{agent, model, tokens, latencyMs, costUsd}` |

---

## Garanties & idempotence

- **At-least-once delivery** côté Kafka : tous les consumers doivent être idempotents (clé `eventId` à dédupliquer côté handler).
- **Ordering** : garanti par partition uniquement → choisir la `partition key` avec soin (cf. tableaux).
- **Dead Letter Topic** : `naja7i.<service>.dlt` pour chaque consumer ; alerte si > 0.
- **Outbox pattern** : les services qui mettent à jour leur DB + publient un event utilisent le pattern outbox (event écrit dans la table `outbox` dans la même transaction, puis un poller publie vers Kafka).

## Topologie suggérée

- 3 brokers Kafka minimum (RF=3).
- Partitions par défaut : 6 ; topics `payment.*` : 12 (volume + audit).
- Compaction activée sur les topics dont l'état est utile (`profile.prof.validated`, `payment.balance.adjusted`).
