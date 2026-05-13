# NAJA7I-APP — Architecture Globale

> Plateforme EdTech marocaine mettant en relation **professeurs particuliers** et **étudiants**, gérée par un **admin**, augmentée par des **agents IA** (Claude).

## Index des documents d'architecture

| # | Document | Contenu |
|---|----------|---------|
| 1 | [c4-context.md](./c4-context.md) | C4 niveau 1 — Contexte système, acteurs, systèmes externes |
| 2 | [c4-containers.md](./c4-containers.md) | C4 niveau 2 — Conteneurs (microservices, bases, gateways) |
| 3 | [data-flow.md](./data-flow.md) | Flux métiers critiques (booking, payment escrow, anti-contournement) |
| 4 | [database-schema.md](./database-schema.md) | Schéma DB par service (PostgreSQL, MongoDB, Redis) |
| 5 | [events-catalog.md](./events-catalog.md) | Catalogue d'événements Kafka (topics, payloads, producers/consumers) |
| 6 | [shared-contracts.md](./shared-contracts.md) | DTOs et contrats partagés (shared-libs) |

## Principes directeurs

1. **Database-per-service** — Chaque microservice possède son schéma. Pas de jointure cross-service.
2. **Communication** — REST synchrone via API Gateway pour les commandes utilisateur ; Kafka asynchrone pour les événements métier (audit, propagation, IA, notifications).
3. **Sécurité d'abord (payment-service)** — Toutes les transactions monétaires passent par un workflow ESCROW + audit log immuable + validation admin.
4. **Anti-contournement** — Aucun contact prof↔étudiant avant `BOOKING_ACCEPTED` ; filtrage IA des messages, emails et téléphones masqués.
5. **Multi-tenant logique** — Pas de multi-tenant DB, mais isolation forte par rôle (`ADMIN`, `PROF`, `STUDENT`).
6. **i18n** — FR (défaut) / AR (RTL) / EN. Darija acceptée en transcription latine côté chat.
7. **Conformité Loi 09-08** (CNDP Maroc) — consentements explicites, droit à l'effacement, journalisation des accès aux données sensibles.

## Vue d'ensemble — Stack

```
┌────────────────────────────────────────────────────────────────┐
│  CLIENTS                                                       │
│  ├─ Web (React 18 + TS + Vite + Tailwind + shadcn/ui)         │
│  └─ Mobile (Flutter 3 + Riverpod)                              │
└──────────────────────────┬─────────────────────────────────────┘
                           │ HTTPS
                  ┌────────▼────────┐
                  │  API Gateway    │  Spring Cloud Gateway
                  │  (JWT, rate-lim)│  + Resilience4j
                  └────────┬────────┘
                           │
                  ┌────────┴────────┐
                  │                 │
             ┌────▼────┐     ┌──────▼──────┐
             │ Eureka  │     │   Config    │
             │Discovery│     │   Server    │
             └─────────┘     └─────────────┘
                           │
                ┌──────────┴──────────────────────────────┐
                │       MICROSERVICES (Spring Boot 3)     │
                │  auth · user-profile · calendar         │
                │  pricing · booking · payment · chat     │
                │  video · ai-agent · local · notif       │
                │  admin-dashboard                        │
                └──────────────┬──────────────────────────┘
                               │
        ┌──────────────────────┼──────────────────────┐
        │                      │                      │
   ┌────▼────┐          ┌──────▼─────┐         ┌─────▼─────┐
   │ Kafka   │          │ PostgreSQL │         │  Redis    │
   │ (events)│          │ (per-svc)  │         │  (cache,  │
   │         │          │ + MongoDB  │         │  sessions)│
   └─────────┘          │ (chat,logs)│         └───────────┘
                        └────────────┘
                                                      ┌──────────┐
   ┌──────────┐  ┌─────────┐  ┌──────────┐  ┌────────▶│  MinIO   │
   │ LiveKit  │  │ Twilio  │  │ SendGrid │  │ S3-comp │  (files) │
   │ (WebRTC) │  │ (SMS/WA)│  │ (email)  │  │         └──────────┘
   └──────────┘  └─────────┘  └──────────┘  │
                                            │
                                  ┌─────────┴──────────┐
                                  │  Claude API (IA)   │
                                  └────────────────────┘
```

## Roadmap (validée)

- **v1 (3-4 mois) — MVP élargi** : auth, user-profile, calendar, pricing, booking, payment, chat (filtrage regex), video (LiveKit), ai-agent (matching seul), notification, web. Auth via **Spring Security + JWT custom** (Keycloak repoussé).
- **v2 (6 mois)** : `local-management`, `admin-dashboard`, AI complète (QCM, modération Claude, smart-replies, AI Tutor), mobile Flutter, calendrier adaptatif, sync Google/Apple Calendar.
- **v3 (12 mois)** : marketplace ressources, replay 30j, vouchers parents, mode groupe, bourse sociale, mode examen blanc, OCR anti-fraude diplômes.

## Décisions techniques actées

| Sujet | Choix | Justification |
|-------|-------|---------------|
| Auth | Spring Security + JWT custom + Redis OTP | Plus rapide à livrer, contrôle total, migration Keycloak possible v2 |
| Broker | Kafka | Compaction sur `payment.*` pour audit, replay, alignement event-sourcing |
| Vidéo | LiveKit self-hosted | Open source, SDK Web+Flutter, données hébergées au Maroc |
| MVP v1 | 10 services + web | Inclut chat, video, ai-matching dès v1 |

Voir [c4-containers.md](./c4-containers.md) pour le détail des conteneurs et [data-flow.md](./data-flow.md) pour les séquences métier critiques.
