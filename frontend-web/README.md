# NAJA7I — Frontend Web (React + Vite)

React 18 + TypeScript 5 + Vite 5 frontend for the NAJA7I-APP Moroccan EdTech platform.
Phase 4 Vague A scope: **auth flow + i18n (FR/AR/EN) + role-based routing + empty role dashboards**. No booking / pricing / chat / video / admin pages yet — those land in Vague B/C.

## Stack

- Vite 5 + React 18 + TypeScript 5 (strict)
- Tailwind CSS v3 + shadcn/ui primitives (copied locally — no runtime dep)
- React Router v6 (`createBrowserRouter`)
- TanStack Query v5 (server state) + Zustand (client/auth state)
- React Hook Form + Zod (forms + validation)
- react-i18next (FR / AR / EN) with `dir="rtl"` toggle for Arabic
- axios with JWT injection + auto-refresh on 401

## Local dev

```bash
pnpm install      # or: npm install
cp .env.example .env
pnpm dev          # http://localhost:5173
```

The dev server proxies `/api/*` to `http://localhost:8080` (the Spring Cloud Gateway).
Set `VITE_API_BASE_URL` in `.env` to point at a different backend.

## Build

```bash
pnpm typecheck
pnpm build
pnpm preview
```

## Project structure

```
src/
├── api/             # axios client, typed auth endpoints, DTO types
├── auth/            # Zustand store, useAuth hook, guards, JWT decode
├── components/
│   ├── ui/          # shadcn primitives copied locally
│   └── layout/      # AppShell, Header, Sidebar, PublicShell, LocaleSwitcher
├── i18n/            # i18next config + fr / ar / en JSON
├── lib/             # cn, validation, formatters
├── pages/
│   ├── public/      # Landing, Login, Register, VerifyOtp, Password reset
│   └── protected/   # student / prof / admin dashboards (placeholders)
├── styles/          # globals.css (Tailwind + CSS variables)
├── App.tsx
├── main.tsx
└── routes.tsx
```

## RTL (Arabic)

When the locale is `ar`, `<html dir="rtl" lang="ar">` is set by `LocaleSwitcher`.
Custom layout components use Tailwind logical properties (`ms-*`, `me-*`, `text-start`, `text-end`) so flipping the document direction is enough.

## Auth flow

1. Public routes (`/login`, `/register`, `/auth/verify-otp`, `/auth/forgot-password`, `/auth/reset-password`) live under `PublicShell`.
2. Tokens are stored in `localStorage` under `naja7i.access` / `naja7i.refresh`.
3. `axios` request interceptor injects `Authorization: Bearer <access>`.
4. On HTTP 401, the response interceptor calls `/api/v1/auth/refresh` once and retries the request; on failure it clears storage and redirects to `/login?next=...`.
5. `RequireAuth` hydrates the `user` from `GET /api/v1/auth/me` on mount, then `RequireRole` filters by `Role`.

## Backend contract

All endpoints go through the gateway on `:8080`. The gateway extracts `X-User-Id`, `X-User-Role`, `X-Locale`, `X-Trace-Id` from the JWT and forwards them to downstream services — the frontend only sends `Authorization: Bearer <accessToken>`.
