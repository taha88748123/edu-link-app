# C4 Niveau 1 — Diagramme de contexte

Vue de plus haut niveau : qui interagit avec NAJA7I-APP et quels systèmes externes sont impliqués.

## Diagramme

```mermaid
C4Context
    title NAJA7I-APP — System Context

    Person(student, "Étudiant", "Réserve des cours, paie via balance, suit sa progression")
    Person(prof, "Professeur", "Propose des créneaux, donne des cours, reçoit des virements")
    Person(admin, "Admin", "Valide profils, recharges, virements, résout litiges")
    Person(parent, "Parent (v3)", "Recharge la balance de l'enfant, reçoit rapports")

    System(naja7i, "NAJA7I-APP", "Plateforme EdTech marocaine — Web + Mobile + Backend microservices")

    System_Ext(claudeAPI, "Claude API (Anthropic)", "Matching, QCM, modération, smart-replies")
    System_Ext(openaiAPI, "OpenAI API", "Fallback IA")
    System_Ext(twilio, "Twilio", "SMS + WhatsApp OTP, notifications")
    System_Ext(sendgrid, "SendGrid", "Emails transactionnels")
    System_Ext(fcm, "Firebase Cloud Messaging", "Push notifications mobile/web")
    System_Ext(livekit, "LiveKit / Jitsi", "Streaming vidéo WebRTC pour cours en ligne")
    System_Ext(cashplus, "CashPlus / Wafacash", "Recharges balance via points de vente")
    System_Ext(banks, "Banques marocaines", "Virements bancaires (entrants étudiants, sortants profs)")
    System_Ext(googleCal, "Google / Apple Calendar", "Synchronisation calendrier (v2+)")
    System_Ext(cndp, "CNDP", "Autorité Loi 09-08 — protection données")

    Rel(student, naja7i, "Réserve séances, paie, chatte, suit cours", "HTTPS / WebSocket / WebRTC")
    Rel(prof, naja7i, "Définit dispos, tarifs, donne cours, perçoit virements", "HTTPS / WebSocket / WebRTC")
    Rel(admin, naja7i, "Supervise plateforme", "HTTPS (back-office)")
    Rel(parent, naja7i, "Recharge balance enfant", "HTTPS")

    Rel(naja7i, claudeAPI, "Génère QCM, matching, modération")
    Rel(naja7i, openaiAPI, "Fallback IA")
    Rel(naja7i, twilio, "Envoie OTP & WhatsApp")
    Rel(naja7i, sendgrid, "Envoie emails")
    Rel(naja7i, fcm, "Push notifications")
    Rel(naja7i, livekit, "Sessions vidéo")
    Rel(naja7i, cashplus, "Valide les recharges (manuelle via preuve)")
    Rel(naja7i, banks, "Virements sortants profs (déclenchés par admin)")
    Rel(naja7i, googleCal, "Sync calendrier")
    UpdateRelStyle(naja7i, cndp, $offsetY="-30")
    Rel(naja7i, cndp, "Conformité Loi 09-08")
```

## Acteurs principaux

| Acteur | Rôle | Canaux | Auth |
|--------|------|--------|------|
| **Étudiant** | Recherche prof, réserve, paie, chatte, suit cours | Web + Mobile | OTP téléphone + JWT |
| **Professeur** | Définit tarifs/dispos, accepte séances, donne cours, demande virement | Web + Mobile | OTP + validation admin du profil |
| **Admin** | Valide profils, recharges, virements, modère litiges, KPIs | Web back-office | Email/password + 2FA obligatoire |
| **Parent** (v3) | Recharge balance enfant, lit rapports | Mobile + Web | OTP téléphone |
| **Agents IA Claude** | Matching, QCM, modération chat, smart-replies, AI Tutor | Interne | Service-to-service (API key) |

## Systèmes externes critiques

| Système | Usage | Criticité | Fallback |
|---------|-------|-----------|----------|
| **Claude API** | Toutes les fonctions IA | Haute | OpenAI API |
| **Twilio (SMS+WA)** | OTP, notifications urgentes | Critique | Email + push |
| **SendGrid** | Emails transactionnels | Moyenne | Resend, Mailgun |
| **FCM** | Push notifications | Haute | APNs direct (iOS) |
| **LiveKit/Jitsi** | Vidéo cours en ligne | Haute | Jitsi self-hosted |
| **CashPlus/Wafacash** | Recharges balance | Critique (revenu) | Virement bancaire manuel |
| **Google/Apple Calendar** | Sync calendrier | Faible (v2+) | Aucun (feature optionnelle) |

## Contraintes Maroc

- **Devise unique** : MAD (Dirham) — pas de multi-devise en v1.
- **Paiements** : pas de Stripe/PayPal pour les recharges étudiants (la grande majorité préfère cash/virement). Carte bancaire reportée à v2+ via CMI.
- **WhatsApp Business** : canal de communication dominant — à intégrer dès v1.
- **RTL Arabe** : interface bidirectionnelle obligatoire.
- **CNDP** : déclaration préalable obligatoire, DPO désigné, registre des traitements maintenu.
- **Calendrier scolaire** : rentrée septembre, BAC en juin → pics saisonniers à anticiper côté infra.
